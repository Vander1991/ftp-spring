package szu.jason.ftp.spring.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.util.Assert;
import szu.jason.ftp.spring.FTPClientUtil;
import szu.jason.ftp.spring.RemoteFileInfo;
import szu.jason.ftp.spring.SFTPConnector;
import szu.jason.ftp.spring.SFTPConnectorFactory;
import szu.jason.ftp.spring.exception.FTPClientUtilException;
import szu.jason.ftp.spring.util.FileUtil;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;

/**
 * @author : Vander
 * @date :   2020/9/5
 * @description : 对FTPClientUtil接口的SFTP通用实现
 */
@Slf4j
public abstract class GenericSFTPClientUtilImpl implements FTPClientUtil, SFTPConnectorFactory {

    @Override
    public void put(InputStream localInputStream,
                    String remotePathUri,
                    String remoteFilename,
                    String suffix) throws Exception {
        SFTPConnector sftpConnector = getConnector();
        ChannelSftp channelSftp = sftpConnector.getChannelSftp();
        String remoteFileAbsolutePathUri = "";
        try {
            // 远程目录不存在则传建
            mkDirRecursive(channelSftp, remotePathUri);
            // 上传文件到远程
            remoteFileAbsolutePathUri = remotePathUri + "/" + remoteFilename;
            channelSftp.put(localInputStream, remoteFileAbsolutePathUri + suffix);
            channelSftp.rename(remoteFileAbsolutePathUri + suffix, remoteFileAbsolutePathUri);
        } catch (Exception e) {
            String errorMsg = String.format("上传本地文件流到远程文件：%s失败！", remoteFileAbsolutePathUri);
            throw new FTPClientUtilException(errorMsg, e);
        } finally {
            localInputStream.close();
            returnConnector(sftpConnector);
        }
    }

    @Override
    public void put(File localFile, String remoteDir, String remoteFilename, String suffix) throws Exception {
        try {
            this.put(new FileInputStream(localFile), remoteDir, remoteFilename, suffix);
        } catch (Exception e) {
            String errorMsg = String.format("上传本地文件%s到远程目录：%s失败！", localFile.getAbsolutePath(), remoteDir);
            throw new FTPClientUtilException(errorMsg, e);
        }
    }

    @Override
    public void put(String localFileAbsolutePathUri, String remoteDir, String remoteFilename, String suffix) throws Exception {
        try {
            this.put(new FileInputStream(localFileAbsolutePathUri), remoteDir, remoteFilename, suffix);
        } catch (Exception e) {
            String errorMsg = String.format("上传本地文件%s到远程目录：%s失败！", localFileAbsolutePathUri, remoteDir);
            throw new FTPClientUtilException(errorMsg, e);
        }
    }

    @Override
    public void get(OutputStream localOutputStream,
                    String remoteDir,
                    String remoteFilename) throws Exception {
        SFTPConnector sftpConnector = getConnector();
        ChannelSftp channelSftp = sftpConnector.getChannelSftp();
        try {
            channelSftp.get(remoteDir + "/" + remoteFilename, localOutputStream);
        } catch (Exception e) {
            String errorMsg = String.format("下载远程文件：%s/%s到本地文件流失败！", remoteDir, remoteFilename);
            throw new FTPClientUtilException(errorMsg, e);
        } finally {
            localOutputStream.flush();
            localOutputStream.close();
            returnConnector(sftpConnector);
        }
    }

    @Override
    public void get(File localFile, String remoteDir, String remoteFilename) throws Exception {
        try {
            this.get(new FileOutputStream(localFile), remoteDir, remoteFilename);
        } catch (Exception e) {
            String errorMsg = String.format("下载远程文件：%s/%s到本地文件：%s失败！",
                    remoteDir, remoteFilename, localFile.getAbsolutePath());
            throw new FTPClientUtilException(errorMsg, e);
        }

    }

    @Override
    public void get(String localFileDir, String remoteDir, String remoteFilename) throws Exception {
        try {
            FileUtil.createLocalDir(localFileDir);
            File localFile = new File(localFileDir, remoteFilename);
            this.get(new FileOutputStream(localFile.getAbsolutePath()), remoteDir, remoteFilename);
        } catch (Exception e) {
            String errorMsg = String.format("下载远程文件：%s/%s到本地目录：%s失败！",
                    remoteDir, remoteFilename, localFileDir);
            throw new FTPClientUtilException(errorMsg, e);
        }
    }

    @Override
    public void get(String localDir,
                    String remoteFilePathUri) throws Exception {
        if (StringUtils.isAnyBlank(localDir, remoteFilePathUri)) {
            throw new FTPClientUtilException(
                    String.format("传入的本地目录或远程文件路径为空，" +
                            "localDir：%s，remoteFilePathUri：%s！", localDir, remoteFilePathUri));
        }

        int index = remoteFilePathUri.lastIndexOf("/");
        String remoteDir = "";
        String remoteFilename = "";
        SFTPConnector sftpConnector = null;
        try {
            sftpConnector = getConnector();
            ChannelSftp channelSftp = sftpConnector.getChannelSftp();
            remoteDir = remoteFilePathUri.substring(0, index);
            remoteFilename = remoteFilePathUri.substring(index + 1);
            FileUtil.createLocalDir(localDir);
            channelSftp.get(remoteFilePathUri, localDir + File.separator + remoteFilename);
        } catch (Exception e) {
            String errorMsg = String.format("下载远程文件：%s到本地目录：%s失败！", remoteFilePathUri, localDir);
            throw new FTPClientUtilException(errorMsg, e);
        } finally {
            returnConnector(sftpConnector);
        }
    }

    @Override
    public void move(String remoteSrcDir,
                     String remoteDestDir,
                     String remoteFilename) throws Exception {
        SFTPConnector sftpConnector = getConnector();
        ChannelSftp channelSftp = sftpConnector.getChannelSftp();
        try {
            // 校验路径是否存在，不存在则创建目的路径
            mkDirRecursive(channelSftp, remoteDestDir);
            // 检查文件是否存在
            Assert.isTrue(checkRemoteFileExist(remoteSrcDir, remoteFilename, channelSftp),
                    String.format("使用sftp客户端移动文件出错，源文件%s/%s不存在", remoteSrcDir, remoteFilename));
            // 尝试文件转移: linux文件分隔符
            String srcPath = String.format("%s/%s", remoteSrcDir, remoteFilename);
            String desPath = String.format("%s/%s", remoteDestDir, remoteFilename);
            channelSftp.rename(srcPath, desPath);
        } catch (Exception e) {
            String errorMsg = String.format("无法将文件[%s]从远程目录：[%s] 移动到 [%s]!",
                    remoteFilename, remoteSrcDir, remoteDestDir);
            throw new FTPClientUtilException(errorMsg, e);
        } finally {
            returnConnector(sftpConnector);
        }
    }

    @Override
    public void copy(String remoteSrcDir,
                     String remoteDestDir,
                     String remoteSrcFilename,
                     String suffix,
                     String remoteNewFilename) throws Exception {
        try {
            copyByInputStreamAndOutputStream(remoteSrcDir,
                    remoteDestDir, remoteSrcFilename, suffix, remoteNewFilename);
        } catch (Exception e) {
            String errorMsg = String.format("复制源文件: [%s/%s] 到目的文件: [%s/%s]失败！",
                    remoteSrcDir, remoteSrcFilename, remoteDestDir, remoteNewFilename);
            throw new FTPClientUtilException(errorMsg, e);
        }

    }

    @Override
    public boolean deleteFile(String remoteFileAbsolutePathUri) throws Exception {
        SFTPConnector sftpConnector = getConnector();
        ChannelSftp channelSftp = sftpConnector.getChannelSftp();
        try {
            channelSftp.rm(remoteFileAbsolutePathUri);
        } catch (Exception e) {
            String errorMsg = String.format("删除远程文件：[%s] 失败！", remoteFileAbsolutePathUri);
            throw new FTPClientUtilException(errorMsg, e);
        }
        return true;
    }

    @Override
    public boolean removeDirectory(String remoteDir) throws Exception {
        SFTPConnector sftpConnector = getConnector();
        ChannelSftp channelSftp = sftpConnector.getChannelSftp();
        try {
            channelSftp.rmdir(remoteDir);
        } catch (Exception e) {
            String errorMsg = String.format("删除远程目录：[%s] 失败", remoteDir);
            throw new FTPClientUtilException(errorMsg, e);
        } finally {
            returnConnector(sftpConnector);
        }
        return true;
    }

    @Override
    public List<String> getRemoteDirFilenames(String remoteDir) throws Exception {
        return listRemoteDirFilenames(remoteDir);
    }

    @Override
    public List<FTPFile> getRemoteDirFTPFiles(String remoteDir) throws Exception {
        return listRemoteDirFTPFiles(remoteDir);
    }

    /**
     * 列出远程目录的文件
     *
     * @param remoteDir
     * @return
     * @throws Exception
     */
    private List<String> listRemoteDirFilenames(String remoteDir) throws Exception {
        SFTPConnector sftpConnector = getConnector();
        ChannelSftp channelSftp = sftpConnector.getChannelSftp();
        List<String> allFilenames = new ArrayList<>();
        try {
            printWorkingDirectory(channelSftp);
            @SuppressWarnings("rawtypes")
            Vector allFiles = channelSftp.ls(remoteDir);
            log.debug(String.format("ls: %s", JSON.toJSONString(allFiles,
                    SerializerFeature.UseSingleQuotes)));
            for (int i = 0; i < allFiles.size(); i++) {
                LsEntry lsEntry = (LsEntry) allFiles.get(i);
                String strName = lsEntry.getFilename();
                if (lsEntry.getFilename().equals(".") || lsEntry.getFilename().equals("..")) {
                    continue;
                }
                allFilenames.add(trimSeparator(remoteDir) + strName);
            }
        } catch (SftpException e) {
            String errorMsg = String
                    .format("获取path:[%s] 下文件列表时发生I/O异常,请确认与ftp服务器的连接正常,拥有目录ls权限, errorMessage:%s",
                            remoteDir, e.getMessage());
            throw new FTPClientUtilException(errorMsg, e);
        } finally {
            returnConnector(sftpConnector);
        }
        return allFilenames;
    }

    /**
     * 列出远程目录的文件并包装成FTPFile
     *
     * @param remoteDir
     * @return
     * @throws Exception
     */
    private List<FTPFile> listRemoteDirFTPFiles(String remoteDir) throws Exception {
        SFTPConnector sftpConnector = getConnector();
        ChannelSftp channelSftp = sftpConnector.getChannelSftp();
        // 校验远程目录是否存在,不检查则直接创建
        if (!checkRemoteDirExist(channelSftp, remoteDir)) {
            log.info("远程目录%s不存在，创建新目录", remoteDir);
            mkDirRecursive(channelSftp, remoteDir);
        }

        // 列出远程目录的文件并包装成FTPClient
        Vector<LsEntry> vector = null;
        List<FTPFile> ftpFiles = null;
        try {
            vector = channelSftp.ls(remoteDir);
            ftpFiles = new ArrayList<>();
            for (LsEntry lsEntry : vector) {
                //当前目录和上级目录不包含在内
                if (lsEntry.getFilename().equals(".") || lsEntry.getFilename().equals("..")) {
                    continue;
                }
                FTPFile ftpFile = new FTPFile();
                String remoteFilename = lsEntry.getFilename();
                ftpFile.setName(remoteFilename);
                long size = lsEntry.getAttrs().getSize();
                ftpFile.setSize(size);
                // 获取文件最后修改时间
                long time = lsEntry.getAttrs().getMTime() * 1000L;
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(time);
                ftpFile.setTimestamp(calendar);
                StringBuilder rawListingStrBuilder = new StringBuilder();
                Date calendarTime = calendar.getTime();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-hh:mm:ss");
                String formatCalendarTime = dateFormat.format(calendarTime);
                rawListingStrBuilder.append(size)
                        .append(" ")
                        .append(formatCalendarTime)
                        .append(" ")
                        .append(remoteFilename);
                ftpFile.setRawListing(rawListingStrBuilder.toString());
                ftpFiles.add(ftpFile);
            }
        } catch (Exception e) {
            String errorMsg = String.format("Get directory：%s file list failed", remoteDir);
            throw new FTPClientUtilException(errorMsg, e);
        } finally {
            returnConnector(sftpConnector);
        }
        return ftpFiles;
    }

    /**
     * 获取远程目录的文件并以RemoteFileInfo包装
     *
     * @param remoteDir
     * @param recursive
     * @return
     * @throws Exception
     */
    @Override
    public List<RemoteFileInfo> getRemoteFileByDir(String remoteDir,
                                                   boolean recursive) throws Exception {
        return listRemoteDirFileInfo(remoteDir, recursive);
    }

    /**
     * 获取远程目录的文件并以RemoteFileInfo包装（不读取目录下的子目录）
     *
     * @param remoteDir 用户访问ftp路径
     * @return
     * @throws Exception
     */
    @Override
    public List<RemoteFileInfo> getRemoteFileByDir(String remoteDir) throws Exception {
        return listRemoteDirFileInfo(remoteDir, false);
    }

    /**
     * 判断远程文件或路径是否存在
     *
     * @param remotePath
     * @return
     * @throws SftpException
     */
    @Override
    public boolean isRemotePathExist(String remotePath) throws SftpException {
        SFTPConnector sftpConnector = getConnector();
        ChannelSftp channelSftp = sftpConnector.getChannelSftp();
        try {
            return isRemotePathExist(remotePath, sftpATTRS -> true, channelSftp);
        } finally {
            returnConnector(sftpConnector);
        }

    }

    /**
     * 通过获取两条管道，以本地内存作为中转，实现文件复制
     *
     * @param remoteSrcDir
     * @param remoteDestDir
     * @param remoteSrcFilename
     * @param suffix
     * @param remoteNewFilename
     * @throws Exception
     */
    private void copyByInputStreamAndOutputStream(String remoteSrcDir,
                                                  String remoteDestDir,
                                                  String remoteSrcFilename,
                                                  String suffix,
                                                  String remoteNewFilename) throws Exception {
        SFTPConnector inputSftpConnector = getConnector();
        SFTPConnector outputSftpConnector = getConnector();
        ChannelSftp inputChannelSftp = inputSftpConnector.getChannelSftp();
        ChannelSftp outputChannelSftp = outputSftpConnector.getChannelSftp();
        InputStream remoteSrcFileInputStream = null;
        try {
            // 校验路径是否存在，不存在则创建目的路径
            mkDirRecursive(inputChannelSftp, remoteDestDir);

            // 验证需要copy的源文件是否存在
            Assert.isTrue(checkRemoteFileExist(remoteSrcDir, remoteSrcFilename, inputChannelSftp),
                    String.format("远程文件%s/%s不存在", remoteSrcDir, remoteSrcFilename));
            String remoteSrcFilePathUri = remoteSrcDir + "/" + remoteSrcFilename;
            remoteSrcFileInputStream = inputChannelSftp.get(remoteSrcFilePathUri);
            String remoteTmpFilePathUri = remoteDestDir + "/" + remoteNewFilename + suffix;
            // 生成空文件并获取远程目的文件流
            OutputStream remoteTmpFileOutputStream = getOutputStream(outputChannelSftp, remoteTmpFilePathUri);
            // 写入将读取到的远程文件流写入到远程目的文件流
            write(remoteSrcFileInputStream, remoteTmpFileOutputStream);
            String remoteDestFilePathUri = remoteDestDir + "/" + remoteNewFilename;
            outputChannelSftp.rename(remoteTmpFilePathUri, remoteDestFilePathUri);
        } catch (Exception e) {
            throw e;
        } finally {
            if (remoteSrcFileInputStream != null) {
                try {
                    remoteSrcFileInputStream.close();
                } catch (IOException e) {
                    log.warn("close file IOException", e);
                }
            }
            returnConnector(inputSftpConnector);
            returnConnector(outputSftpConnector);
        }
    }

    /**
     * 创建目录，若目录已经存在则跳过
     *
     * @param directoryPath
     * @throws Exception
     */
    private void mkdir(ChannelSftp channelSftp, String directoryPath) throws Exception {
        boolean isDirExist = checkRemoteDirExist(channelSftp, directoryPath);
        if (!isDirExist) {
            try {
                // warn 检查mkdir -p
                channelSftp.mkdir(directoryPath);
            } catch (SftpException e) {
                String errorMsg = String
                        .format("创建目录:%s时发生I/O异常,请确认与ftp服务器的连接正常,拥有目录创建权限, errorMessage:%s",
                                directoryPath, e.getMessage());
                throw new FTPClientUtilException(errorMsg, e);
            }
        }
    }

    /**
     * 打印工作目录
     *
     * @param channelSftp
     */
    private static void printWorkingDirectory(ChannelSftp channelSftp) {
        try {
            log.info(String.format("current working directory:%s",
                    channelSftp.pwd()));
        } catch (Exception e) {
            log.warn(String.format("printWorkingDirectory error:%s",
                    e.getMessage()));
        }
    }

    /**
     * 校验远程文件是否存在
     *
     * @param remoteDir
     * @param remoteFilename
     * @param channelSftp
     * @return
     * @throws SftpException
     */
    private static boolean checkRemoteFileExist(String remoteDir,
                                                String remoteFilename,
                                                ChannelSftp channelSftp) throws SftpException {
        String remoteFilePath = trimSeparator(remoteDir) + remoteFilename;
        return isRemotePathExist(remoteFilePath,
                sftpATTRS -> !sftpATTRS.isLink() && !sftpATTRS.isDir(), channelSftp);
    }

    /**
     * 校验远程目录是否存在
     *
     * @param remoteDir
     * @param channelSftp
     * @return
     */
    private static boolean checkRemoteDirExist(ChannelSftp channelSftp,
                                               String remoteDir) throws SftpException {
        return isRemotePathExist(remoteDir, sftpATTRS -> sftpATTRS.isDir(), channelSftp);
    }

    /**
     * 解决远程文件复制时，读取SFTP服务端的InputStream，直接写到远程文件时
     * 出现一直卡住的现象
     *
     * @param inputStream
     * @param outputStream
     * @throws Exception
     */
    public static void write(InputStream inputStream,
                             OutputStream outputStream) throws Exception {
        BufferedInputStream bufferedInputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        try {
            bufferedInputStream = new BufferedInputStream(inputStream);
            bufferedOutputStream = new BufferedOutputStream(outputStream);
            byte[] buf = new byte[8196]; //代表一次最多读取8KB的内容
            int bytesRead;
            while ((bytesRead = bufferedInputStream.read(buf)) > 0) {
                bufferedOutputStream.write(buf, 0, bytesRead);
            }
            bufferedOutputStream.flush();
        } catch (Exception e) {
            log.error("write remote file exception", e);
            throw e;
        } finally {
            try {
                bufferedOutputStream.close();
                bufferedInputStream.close();
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                log.error("进行SFTP文件复制时，关闭BufferedInputStream异常！", e);
                throw e;
            }
        }
    }

    /**
     * 获取远程文件流
     *
     * @param channelSftp
     * @param filePath
     * @return
     */
    private static OutputStream getOutputStream(ChannelSftp channelSftp, String filePath) {
        try {
            OutputStream writeOutputStream = channelSftp.put(filePath);
            String errorMsg = String.format(
                    "打开FTP文件[%s]获取写出流时出错,请确认文件%s有权限创建，有权限写出等", filePath,
                    filePath);
            if (null == writeOutputStream) {
                throw new FTPClientUtilException(errorMsg);
            }
            return writeOutputStream;
        } catch (SftpException e) {
            String errorMsg = String.format(
                    "写出文件[%s] 时出错,请确认文件%s有权限写出, errorMessage:%s", filePath,
                    filePath, e.getMessage());
            throw new FTPClientUtilException(errorMsg, e);
        }
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
     * @throws Exception
     */
    protected List<RemoteFileInfo> listRemoteDirFileInfo(String remoteDir,
                                                         boolean recursive) throws Exception {
        SFTPConnector sftpConnector = getConnector();
        ChannelSftp channelSftp = sftpConnector.getChannelSftp();
        // 需要递归目录
        List<RemoteFileInfo> remoteFileInfoList = new ArrayList<>();
        try {
            listRemoteDirFileInfoSup(channelSftp, remoteFileInfoList, trimSeparator(remoteDir), recursive);
        } catch (IOException e) {
            String errorMsg = String.format("sftp client list remoteDir: %s files exception!", remoteDir);
            throw new FTPClientUtilException(errorMsg, e);
        } finally {
            returnConnector(sftpConnector);
        }
        return remoteFileInfoList;
    }

    /**
     * 递归地列出远程文件信息
     *
     * @param remoteFileInfoList
     * @param remoteDir
     * @param recursive
     * @throws Exception
     */
    private void listRemoteDirFileInfoSup(ChannelSftp channelSftp,
                                          List<RemoteFileInfo> remoteFileInfoList,
                                          String remoteDir,
                                          boolean recursive)
            throws Exception {
        Vector<LsEntry> vector = null;
        try {
            vector = channelSftp.ls(remoteDir);
        } catch (Exception e) {
            String errorMsg = String.format("Get directory：%s file list failed", remoteDir);
            throw new FTPClientUtilException(errorMsg, e);
        }

        for (LsEntry lsEntry : vector) {
            if (lsEntry.getFilename().equals(".") || lsEntry.getFilename().equals("..")) {
                continue;
            }
            //文件夹
            if (lsEntry.getAttrs().isDir() || lsEntry.getAttrs().isLink()) {
                if (recursive) {
                    listRemoteDirFileInfoSup(channelSftp,
                            remoteFileInfoList, remoteDir + lsEntry.getFilename() + "/", true);
                }
            } else {//文件
                remoteFileInfoList.add(toRemoteFileInfo(lsEntry, remoteDir));
            }
        }
    }

    /**
     * 将LsEntry转化为RemoteFileInfo
     *
     * @param lsEntry
     * @param path
     * @return
     */
    private RemoteFileInfo toRemoteFileInfo(LsEntry lsEntry, String path) {
        RemoteFileInfo remoteFileInfo = new RemoteFileInfo();

        remoteFileInfo.setPath(path);
        remoteFileInfo.setName(lsEntry.getFilename());

        remoteFileInfo.setDirectory(lsEntry.getAttrs().isDir());
        remoteFileInfo.setFile(!(lsEntry.getAttrs().isDir() || lsEntry.getAttrs().isLink()));
        remoteFileInfo.setLink(lsEntry.getAttrs().isLink());

        long mTime = lsEntry.getAttrs().getMTime();
        Date date = new Date((mTime) * 1000L);
        remoteFileInfo.setLastModifiedTime(date);

        remoteFileInfo.setSize(lsEntry.getAttrs().getSize());
        return remoteFileInfo;
    }

    /**
     * 判断远程目录是否存在
     *
     * @param remotePath
     * @param filter
     * @param channelSftp
     * @return
     * @throws SftpException
     */
    private static boolean isRemotePathExist(String remotePath, Predicate<SftpATTRS> filter, ChannelSftp channelSftp) throws SftpException {
        try {
            SftpATTRS sftpATTRS = channelSftp.lstat(remotePath);
            return filter.test(sftpATTRS);
        } catch (SftpException e) {
            //id为2代表文件不存在
            if (e.id == 2) {
                return false;
            }
            throw new FTPClientUtilException(String.format("判断远程目录：%s是否存在时发生异常！", remotePath), e);
        }
    }

    /**
     * 支持mkdir -p的创建目录方式
     *
     * @param channelSftp
     * @param directoryPath
     */
    private static void mkDirRecursive(ChannelSftp channelSftp, String directoryPath) {
        boolean isDirExist = false;
        try {
            SftpATTRS sftpATTRS = channelSftp.lstat(directoryPath);
            isDirExist = sftpATTRS.isDir();
        } catch (SftpException e) {
            if (e.getMessage().toLowerCase().equals("no such file")) {
                log.info(String.format(
                        "您的配置项path:[%s]不存在，将尝试进行目录创建, errorMessage:%s",
                        directoryPath, e.getMessage()));
                isDirExist = false;
            }
        }
        if (!isDirExist) {
            StringBuilder dirPath = new StringBuilder();
            dirPath.append(IOUtils.DIR_SEPARATOR_UNIX);
            String[] dirSplit = StringUtils.split(directoryPath, IOUtils.DIR_SEPARATOR_UNIX);
            try {
                // ftp server不支持递归创建目录,只能一级一级创建
                for (String dirName : dirSplit) {
                    dirPath.append(dirName);
                    mkDirSingleHierarchy(channelSftp, dirPath.toString());
                    dirPath.append(IOUtils.DIR_SEPARATOR_UNIX);
                }
            } catch (SftpException e) {
                String errorMsg = String
                        .format("创建目录:%s时发生I/O异常,请确认与ftp服务器的连接正常,拥有目录创建权限, errorMessage:%s",
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
    private static boolean mkDirSingleHierarchy(ChannelSftp channelSftp, String directoryPath) throws SftpException {
        boolean isDirExist = false;
        SftpATTRS sftpATTRS = null;
        try {
            sftpATTRS = channelSftp.lstat(directoryPath);
        } catch (SftpException e) {
            log.info(String.format("正在逐级创建目录 [%s]", directoryPath));
            channelSftp.mkdir(directoryPath);
            return true;
        }
        isDirExist = sftpATTRS.isDir();
        if (!isDirExist) {
            log.info(String.format("正在逐级创建目录 [%s]", directoryPath));
            channelSftp.mkdir(directoryPath);
        }
        return true;
    }

    /**
     * 创建单层目录
     *
     * @param directoryPath
     * @return
     * @throws SftpException
     */
    public boolean mkDir(String directoryPath) throws SftpException {

        boolean isDirExist = false;
        SFTPConnector sftpConnector = null;
        try {
            sftpConnector = getConnector();
            ChannelSftp channelSftp = sftpConnector.getChannelSftp();
            channelSftp.mkdir(directoryPath);
//            mkDirRecursive(channelSftp, directoryPath);
        } catch (Exception e) {
            log.error(String.format("创建目录 [%s]发生异常", directoryPath), e);
        }
        return true;
    }
}
