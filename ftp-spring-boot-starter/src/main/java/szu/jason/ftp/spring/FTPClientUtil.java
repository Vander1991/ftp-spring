package szu.jason.ftp.spring;

import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @author : Vander
 * @date :   2020/9/2
 * @description :
 */
public interface FTPClientUtil {
    /**
     * 上传文件到ftp服务器
     *
     * @param localInputStream 本地输入流，即需要传输到服务器的数据
     * @param remotePathUri    ftp服务器目录
     * @param remoteFilename   上传后，在ftp服务器上的文件名
     * @param suffix           文件上传时添加的文件后缀，上传完成时，重名为name; suffix 为null 时，则不加后缀
     * @throws Exception 连接不到ftp服务器，登录失败，文件上传失败等异常
     */
    void put(InputStream localInputStream,
             String remotePathUri,
             String remoteFilename,
             String suffix) throws Exception;

    /**
     * 上传文件到ftp服务器 重载put()函数，将InputStream参数变为File
     *
     * @param localFile      本地文件，File类型参数
     * @param remoteDir      ftp服务器目录
     * @param remoteFilename 上传后，在ftp服务器上的文件名
     * @param suffix         文件上传时添加的文件后缀，上传完成时，重名为name; suffix 为null 时，则不加后缀
     * @throws Exception 连接不到ftp服务器，登录失败，文件上传失败等异常
     */
    void put(File localFile,
             String remoteDir,
             String remoteFilename,
             String suffix) throws Exception;

    /**
     * 上传文件到ftp服务器 重载put()函数，将InputStream参数变为 本地文件路径
     *
     * @param localFileAbsolutePathUri 本地文件路径
     * @param remoteDir                ftp	服务器目录
     * @param remoteFilename           上传后，在ftp服务器上的文件名
     * @param suffix                   文件上传时添加的文件后缀，上传完成时，重名为name; suffix 为null 时，则不加后缀
     * @throws Exception 连接不到ftp服务器，登录失败，文件上传失败等异常
     */
    void put(String localFileAbsolutePathUri,
             String remoteDir,
             String remoteFilename,
             String suffix) throws Exception;

    /**
     * 从ftp服务器下载文件，并写入到本地outputStream中
     *
     * @param localOutputStream 本地保存数据的输出流， 即下载的数据将写入该输出流
     * @param remoteDir         ftp 服务器目录
     * @param remoteFilename    要下载的文件名
     * @throws Exception ftp连接，登录失败，传输失败等异常。
     */
    void get(OutputStream localOutputStream, String remoteDir, String remoteFilename) throws Exception;

    /**
     * 从ftp服务器下载文件
     *
     * @param localFile      本地文件， File类型参数
     * @param remoteDir      ftp 服务器目录
     * @param remoteFilename 要下载的文件名
     * @throws Exception
     */
    void get(File localFile, String remoteDir, String remoteFilename) throws Exception;

    /**
     * 从ftp服务器下载文件, 下载后的文件名与ftp服务器上的文件名一致
     *
     * @param localFileDir   本地路径
     * @param remoteDir      ftp 服务器目录
     * @param remoteFilename 要下载的文件名
     * @throws Exception
     */
    void get(String localFileDir, String remoteDir, String remoteFilename) throws Exception;

    /**
     * 从ftp服务器下载文件,下载后的文件名与ftp服务器上的文件名一致
     *
     * @param localDir          本地路径
     * @param remoteFilePathUri ftp服务器上文件的完整路径名（包括文件名）
     * @throws Exception
     */
    void get(String localDir, String remoteFilePathUri) throws Exception;

    /**
     * ftp 服务器文件move
     *
     * @param remoteSrcDir   FTP远程源目录
     * @param remoteDestDir  FTP远程目的目录
     * @param remoteFilename FTP远程文件名
     * @throws Exception 连接不到ftp服务器，登录失败，文件上传失败等异常
     */
    void move(String remoteSrcDir, String remoteDestDir, String remoteFilename) throws Exception;

    /**
     * ftp 服务器文件copy
     *
     * @param remoteSrcDir      FTP远程源目录
     * @param remoteDestDir     FTP远程目的目录
     * @param remoteSrcFilename FTP远程文件名
     * @param suffix            后缀
     * @param remoteNewFilename 重命名后名字(为空则不重命名)
     * @throws Exception 连接不到ftp服务器，登录失败，文件上传失败等异常
     */
    void copy(String remoteSrcDir,
              String remoteDestDir,
              String remoteSrcFilename,
              String suffix,
              String remoteNewFilename) throws Exception;

    /**
     * 刪除ftp服務器上的文件
     *
     * @param remoteFileAbsolutePathUri 文件在ftp服務器上的全路徑（包括文件名）
     * @return true 成功刪除， false 刪除失敗。
     * @throws Exception
     */
    boolean deleteFile(String remoteFileAbsolutePathUri) throws Exception;

    /**
     * 刪除ftp服務器上的目录（必须确保删除执行该方法时，需删除的目录下不能存在文件否则删除失败）
     *
     * @param remoteDir 文件夹路径
     * @return true 成功刪除， false 刪除失敗。
     * @throws Exception
     */
    boolean removeDirectory(String remoteDir) throws Exception;

    /**
     * 获取ftp服务目录下文件或目录名,
     *
     * @param remoteDir ftp服务器路径
     * @return 注意返回的路径是全路径名
     * @throws Exception
     */
    List<String> getRemoteDirFilenames(String remoteDir) throws Exception;

    /**
     * 获取ftp服务器目录下的文件或目录
     *
     * @param remoteDir ftp服务器路径
     * @return 返回 FTPFile对象， FTPFile对象可能是文件或目录，
     * 通过FTPFile.isFile或FTPFile.isDirectory判断,获取单独的文件通过FTPFile.getName()函数获取
     * @throws Exception
     * @Description: 获取ftp服务器目录下的文件或目录
     */
    List<FTPFile> getRemoteDirFTPFiles(String remoteDir) throws Exception;

    /**
     * 获取远程ftp目录中的所有的文件
     *
     * @param remoteDir 用户访问ftp路径
     * @param recursive 是否扫描子文件夹
     * @return 文件列表
     * @throws Exception
     */
    List<RemoteFileInfo> getRemoteFileByDir(String remoteDir, boolean recursive) throws Exception;

    /**
     * 获取远程ftp目录中的所有的文件
     *
     * @param remoteDir 用户访问ftp路径
     * @return 文件列表
     * @throws Exception
     */
    List<RemoteFileInfo> getRemoteFileByDir(String remoteDir) throws Exception;

    /**
     * 判断远程ftp目录或文件是否存在
     * @param remotePath
     * @return
     */
    boolean isRemotePathExist(String remotePath) throws Exception;
}
