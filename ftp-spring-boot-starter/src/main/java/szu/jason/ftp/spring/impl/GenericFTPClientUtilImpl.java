package szu.jason.ftp.spring.impl;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.springframework.util.Assert;
import szu.jason.ftp.spring.FTPClientUtil;
import szu.jason.ftp.spring.FTPConnectFactory;
import szu.jason.ftp.spring.RemoteFileInfo;
import szu.jason.ftp.spring.exception.FTPClientUtilException;
import szu.jason.ftp.spring.util.FileUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author : Vander
 * @date :   2020/9/5
 * @description : 对FTPClientUtil接口的FTP通用实现
 */
@Slf4j
public abstract class GenericFTPClientUtilImpl implements FTPClientUtil, FTPConnectFactory {

    @Override
    public void put(InputStream localInputStream,
                    String remotePathUri,
                    String remoteFilename,
                    String suffix) throws Exception {
        FTPClient ftpClient = getFtpClient();
        try {
            // 目录不存在则进行逐级创建
            mkDirRecursive(ftpClient, remotePathUri);
            // 进入对应的远程目录
            ftpClient.changeWorkingDirectory(remotePathUri);
            // 上传文件
            ftpClient.storeFile(remoteFilename + suffix, localInputStream);
            // 重命名文件
            ftpClient.rename(remoteFilename + suffix, remoteFilename);
        } catch (Exception e) {
            String errorMsg = String.format("上传本地文件流到远程文件：%s/%s失败！", remotePathUri, remoteFilename);
            throw new FTPClientUtilException(errorMsg, e);
        } finally {
            localInputStream.close();
            returnFtpClient(ftpClient);
        }
    }

    @Override
    public void put(File localFile,
                    String remoteDir,
                    String remoteFilename,
                    String suffix) throws Exception {
        this.put(new FileInputStream(localFile), remoteDir, remoteFilename, suffix);
    }

    @Override
    public void put(String localFileAbsolutePathUri,
                    String remoteDir,
                    String remoteFilename,
                    String suffix) throws Exception {
        this.put(new File(localFileAbsolutePathUri), remoteDir, remoteFilename, suffix);
    }

    @Override
    public void get(OutputStream localOutputStream,
                    String remoteDir,
                    String remoteFilename) throws Exception {
        FTPClient ftpClient = getFtpClient();
        try {
            // 判断远程目录是否存在
            Assert.isTrue(isRemotePathExist(remoteDir), String.format("远程目录：[%s]不存在，无法获取远程文件", remoteDir));
            // 下载文件到本地
            ftpClient.retrieveFile(remoteFilename, localOutputStream);
        } catch (Exception e) {
            String errorMsg = String.format("下载远程文件：%s/%s到本地文件流失败！", remoteDir, remoteFilename);
            throw new FTPClientUtilException(errorMsg, e);
        } finally {
            localOutputStream.flush();
            localOutputStream.close();
            returnFtpClient(ftpClient);
        }
    }

    @Override
    public void get(File localFile,
                    String remoteDir,
                    String remoteFilename) throws Exception {
        this.get(new FileOutputStream(localFile), remoteDir, remoteFilename);
    }

    @Override
    public void get(String localFileDir,
                    String remoteDir,
                    String remoteFilename) throws Exception {
        FileUtil.createLocalDir(localFileDir);
        this.get(new FileOutputStream(new File(localFileDir, remoteFilename)), remoteDir, remoteFilename);
    }

    @Override
    public void get(String localDir,
                    String remoteFilePathUri) throws Exception {
        if (StringUtils.isAnyBlank(localDir, remoteFilePathUri)) {
            throw new FTPClientUtilException(
                    String.format("传入的本地目录或远程文件路径为空，" +
                            "本地目录：[%s]，远程目录文件：[%s]", localDir, remoteFilePathUri));
        }

        String remoteDir = "";
        String remoteFilename = "";
        try {
            int index = remoteFilePathUri.lastIndexOf("/");
            remoteDir = remoteFilePathUri.substring(0, index);
            remoteFilename = remoteFilePathUri.substring(index + 1);
            FileUtil.createLocalDir(localDir);
            this.get(localDir, remoteDir, remoteFilename);
        } catch (Exception e) {
            String errorMsg = String.format("下载远程文件：%s到本地目录：%s失败！", remoteFilePathUri, localDir);
            throw new FTPClientUtilException(errorMsg, e);
        }
    }

    @Override
    public void move(String remoteSrcDir,
                     String remoteDestDir,
                     String remoteFilename) throws Exception {
        FTPClient ftpClient = getFtpClient();
        try {
            // 校验源目录是否存在
            Assert.isTrue(isRemotePathExist(remoteSrcDir),
                    String.format("远程目录：[%s]不存在，无法获取远程文件", remoteSrcDir));
            // 创建目的路径
            mkDirRecursive(ftpClient, remoteDestDir);
            // 尝试文件转移: linux文件分隔符
            String srcPath = String.format("%s/%s", remoteSrcDir, remoteFilename);
            String desPath = String.format("%s/%s", remoteDestDir, remoteFilename);
            boolean renameResult = ftpClient.rename(srcPath, desPath);
            if (renameResult == false) {
                String errorMsg = String.format("无法将文件[%s]从远程目录：[%s] 移动到 [%s]!",
                        remoteFilename, remoteSrcDir, remoteDestDir);
                throw new FTPClientUtilException(errorMsg);
            }
        } catch (Exception e) {
            String errorMsg = String.format("无法将文件[%s]从远程目录：[%s] 移动到 [%s]!",
                    remoteFilename, remoteSrcDir, remoteDestDir);
            throw new FTPClientUtilException(errorMsg, e);
        } finally {
            returnFtpClient(ftpClient);
        }
    }

    @Override
    public void copy(String remoteSrcDir,
                     String remoteDestDir,
                     String remoteSrcFilename,
                     String suffix,
                     String remoteNewFilename) throws Exception {
        FTPClient ftpClient = getFtpClient();
        InputStream remoteSrcFileInputStream = null;
        try {
            Assert.notEmpty(new String[]{remoteSrcDir, remoteSrcFilename}, "源目录或源文件不允许为空！");
            // 判断远程源文件是否存在
            String remoteFileUri = remoteSrcDir + IOUtils.DIR_SEPARATOR_UNIX + remoteSrcFilename;
            Assert.isTrue(isRemotePathExist(remoteFileUri),
                    String.format("源文件：[%s]不存在，无法获取远程文件", remoteFileUri));
            // 获取源文件流
            remoteSrcFileInputStream = ftpClient.retrieveFileStream(remoteSrcDir + "/" + remoteSrcFilename);
            // 将文件流写入目的文件
            if (StringUtils.isBlank(remoteNewFilename)) {
                remoteNewFilename = remoteSrcFilename;
            }
            put(remoteSrcFileInputStream, remoteDestDir, remoteNewFilename, suffix);
        } catch (Exception e) {
            String errorMsg = String.format("复制源文件: [%s/%s] 到目的文件: [%s/%s]失败！",
                    remoteSrcDir, remoteSrcFilename, remoteDestDir, remoteNewFilename);
            throw new FTPClientUtilException(errorMsg, e);
        } finally {
            if (remoteSrcFileInputStream != null) {
                try {
                    remoteSrcFileInputStream.close();
                } catch (IOException e) {
                    log.warn("close file IOException", e);
                }
            }
            returnFtpClient(ftpClient);
        }
    }

    @Override
    public boolean deleteFile(String remoteFileAbsolutePathUri) throws Exception {
        boolean result;
        FTPClient ftpClient = getFtpClient();
        try {
            result = ftpClient.deleteFile(remoteFileAbsolutePathUri);
        } catch (Exception e) {
            String errorMsg = String.format("删除远程文件：[%s] 失败！", remoteFileAbsolutePathUri);
            throw new FTPClientUtilException(errorMsg, e);
        } finally {
            returnFtpClient(ftpClient);
        }
        return result;
    }

    @Override
    public boolean removeDirectory(String remoteDir) throws Exception {
        boolean result;
        FTPClient ftpClient = getFtpClient();
        try {
            result = ftpClient.removeDirectory(remoteDir);
        } catch (Exception e) {
            String errorMsg = String.format("删除远程目录：[%s] 失败", remoteDir);
            throw new FTPClientUtilException(errorMsg, e);
        } finally {
            returnFtpClient(ftpClient);
        }
        return result;
    }

    @Override
    public List<String> getRemoteDirFilenames(String remoteDir) throws Exception {
        String[] allFilenameArray = null;
        List<String> allFilenames = null;
        FTPClient ftpClient = null;
        try {
            ftpClient = getFtpClient();
            allFilenameArray = ftpClient.listNames(remoteDir);
            allFilenames = Arrays.asList(allFilenameArray);
        } catch (IOException e) {
            String errorMsg = String
                    .format("获取path:[%s] 下文件列表时发生I/O异常,请确认与ftp服务器的连接正常", remoteDir);
            throw new FTPClientUtilException(errorMsg, e);
        } finally {
            returnFtpClient(ftpClient);
        }
        return allFilenames;
    }

    @Override
    public List<FTPFile> getRemoteDirFTPFiles(String remoteDir) throws Exception {
        FTPFile[] allFilesArray;
        List<FTPFile> allFiles;
        FTPClient ftpClient = getFtpClient();
        try {
            allFilesArray = ftpClient.listFiles(remoteDir);
            allFiles = Arrays.asList(allFilesArray);
        } catch (IOException e) {
            String errorMsg = String
                    .format("获取path:[%s] 下文件列表时发生I/O异常,请确认与ftp服务器的连接正常", remoteDir);
            throw new FTPClientUtilException(errorMsg, e);
        } finally {
            returnFtpClient(ftpClient);
        }
        return allFiles;
    }

    @Override
    public List<RemoteFileInfo> getRemoteFileByDir(String remoteDir,
                                                   boolean recursive) throws Exception {
        return listRemoteDirFileInfo(remoteDir, recursive);
    }

    @Override
    public List<RemoteFileInfo> getRemoteFileByDir(String remoteDir) throws Exception {
        return listRemoteDirFileInfo(remoteDir, false);
    }

    /**
     * 传入过滤器，列出远程目录
     *
     * @param remoteDir
     * @param filter
     * @return
     * @throws Exception
     */
    public List<FTPFile> listRemoteDirFiles(String remoteDir,
                                            FTPFileFilter filter) throws Exception {
        FTPFile[] filteredFilesArray = null;
        List<FTPFile> filteredFiles = null;
        FTPClient ftpClient = null;
        try {
            ftpClient = getFtpClient();
            filteredFilesArray = ftpClient.listFiles(remoteDir, filter);
            filteredFiles = Arrays.asList(filteredFilesArray);
        } catch (IOException e) {
            String errorMsg = String
                    .format("获取path:[%s] 下文件列表时发生I/O异常,请确认与ftp服务器的连接正常", remoteDir);
            throw new FTPClientUtilException(errorMsg, e);
        } finally {
            returnFtpClient(ftpClient);
        }
        return filteredFiles;
    }

    /**
     * 检查源文件是否存在
     *
     * @param filePath
     * @return
     */
    public boolean isFileExist(String filePath) {
        boolean isExitFlag = false;
        FTPClient ftpClient = getFtpClient();
        try {
            FTPFile[] ftpFiles = ftpClient.listFiles(new String(filePath.getBytes(), FTP.DEFAULT_CONTROL_ENCODING));
            if (ftpFiles.length == 1 && ftpFiles[0].isFile()) {
                isExitFlag = true;
            }
        } catch (IOException e) {
            String errorMsg = String.format("获取文件：[%s] 属性时发生I/O异常,请确认与ftp服务器的连接正常", filePath);
            throw new FTPClientUtilException(errorMsg, e);
        }
        return isExitFlag;
    }

    /**
     * 保证ftp路径右端有且仅有一个/
     *
     * @param path
     * @return
     */
    private static String trimSeparator(String path) {
        int end = path.length() - 1;
        for (; end >= 0 && path.charAt(end) == '/'; end--) ;
        return path.substring(0, end + 1) + "/";
    }

    /**
     * 列出远程目录的文件信息，以RemoteFileInfo包裹
     *
     * @param remoteDir
     * @param recursive
     * @return
     */
    protected List<RemoteFileInfo> listRemoteDirFileInfo(String remoteDir,
                                                         boolean recursive) {
        FTPClient ftpClient = null;
        List<RemoteFileInfo> remoteFileInfoList = new ArrayList<>();
        try {
            ftpClient = getFtpClient();
            return listRemoteDirFileInfo(ftpClient, remoteFileInfoList, trimSeparator(remoteDir), recursive);
        } catch (Exception e) {
            throw new FTPClientUtilException(String.format("列出远程目录：%s下的文件异常", remoteDir), e);
        } finally {
            returnFtpClient(ftpClient);
        }
    }


    /**
     * 递归扫描path下的所有文件
     *
     * @param ftpClient 远程ftp
     * @param ftpFiles
     * @param remoteDir 远程路径
     * @param recursive 是否扫描子文件
     * @throws Exception
     */
    private static List<RemoteFileInfo> listRemoteDirFileInfo(FTPClient ftpClient,
                                                              List<RemoteFileInfo> ftpFiles,
                                                              String remoteDir,
                                                              boolean recursive)
            throws Exception {
        FTPFile[] remoteFiles = ftpClient.listFiles(remoteDir);
        if (remoteFiles == null || remoteFiles.length == 0) {
            return ftpFiles;
        }

        for (FTPFile ftpFile : remoteFiles) {
            if (ftpFile.isDirectory()) {//文件夹
                if (recursive) {
                    listRemoteDirFileInfo(ftpClient, ftpFiles,
                            remoteDir + ftpFile.getName() + "/", true);
                }
            } else {//文件
                RemoteFileInfo remoteFileInfo = new RemoteFileInfo(ftpFile.getSize(),
                        ftpFile.getName(),
                        ftpFile.isDirectory(),
                        ftpFile.isFile(),
                        ftpFile.isSymbolicLink(),
                        remoteDir);
                ftpFiles.add(remoteFileInfo);
            }
        }
        return ftpFiles;
    }

    /**
     * 支持mkdir -p的创建目录方式
     *
     * @param ftpClient
     * @param directoryPath
     */
    private void mkDirRecursive(FTPClient ftpClient, String directoryPath) throws Exception {
        // 判断远程目录是否存在
        boolean isDirExist = isRemotePathExist(directoryPath);

        if (!isDirExist) {
            StringBuilder dirPath = new StringBuilder();
            dirPath.append(IOUtils.DIR_SEPARATOR_UNIX);
            String[] dirSplit = StringUtils.split(directoryPath, IOUtils.DIR_SEPARATOR_UNIX);
            try {
                // ftp server不支持递归创建目录,只能一级一级创建
                for (String dirName : dirSplit) {
                    dirPath.append(dirName);
                    mkDirSingleHierarchy(ftpClient, dirPath.toString());
                    dirPath.append(IOUtils.DIR_SEPARATOR_UNIX);
                }
            } catch (Exception e) {
                String errorMsg = String
                        .format("创建目录:%s时发生I/O异常,请确认与ftp服务器的连接正常, errorMessage:%s",
                                directoryPath, e.getMessage());
                throw new FTPClientUtilException(errorMsg, e);
            }
        }
    }

    /**
     * 创建单层目录
     *
     * @param directoryPath
     * @return
     * @throws SftpException
     */
    private boolean mkDirSingleHierarchy(FTPClient ftpClient, String directoryPath) throws Exception {
        boolean isDirExist = false;
        try {
            isDirExist = isRemotePathExist(directoryPath);
        } catch (SftpException e) {
            log.info(String.format("正在逐级创建目录 [%s]", directoryPath));
            ftpClient.makeDirectory(directoryPath);
            return true;
        }
        if (!isDirExist) {
            log.info(String.format("正在逐级创建目录 [%s]", directoryPath));
            ftpClient.makeDirectory(directoryPath);
        }
        return true;
    }

    /**
     * 创建单层目录
     *
     * @param directoryPath
     * @return
     */
    public void mkDir(String directoryPath) {
        boolean isDirExist = false;
        FTPClient ftpClient = getFtpClient();
        try {
            isDirExist = ftpClient.changeWorkingDirectory(
                    new String(directoryPath.getBytes(), FTP.DEFAULT_CONTROL_ENCODING));
        } catch (UnsupportedEncodingException e) {
            String errorMsg = String.format("文件编码:[%s]不存在, errorMessage:%s",
                    FTP.DEFAULT_CONTROL_ENCODING, e.getMessage());
            throw new FTPClientUtilException(errorMsg, e);
        } catch (IOException e) {
            log.info(String.format(
                    "您的配置项path:[%s]不存在，将尝试进行目录创建, errorMessage:%s",
                    directoryPath, e.getMessage()));
        }
        // 目录不存在则创建目录
        if (!isDirExist) {
            try {
                ftpClient.makeDirectory(directoryPath);
            } catch (IOException e) {
                String errorMsg = String.format("创建目录:%s时发生I/O异常,请确认与ftp服务器的连接正常, errorMessage:%s",
                        directoryPath, e.getMessage());
                throw new FTPClientUtilException(errorMsg, e);
            }
        }
    }

    /**
     * 判断远程路径是否存在
     *
     * @param remotePath
     * @return
     * @throws FTPClientUtilException
     */
    @Override
    public boolean isRemotePathExist(String remotePath) throws Exception{
        FTPClient ftpClient = getFtpClient();
        try {
            String[] names = ftpClient.listNames(remotePath);
            return names != null && names.length > 0;
        } catch (IOException e) {
            log.error("判断远程ftp上{}是否存在时发生异常", remotePath);
            throw e;
        } finally {
            returnFtpClient(ftpClient);
        }
    }

}
