package ftp.test;

import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;
import szu.jason.ftp.spring.FTPClientUtil;
import szu.jason.ftp.spring.impl.GenericFTPClientUtilImpl;
import szu.vander.FtpDemoStarter;
import szu.jason.ftp.spring.RemoteFileInfo;
import szu.jason.ftp.spring.impl.GenericSFTPClientUtilImpl;

import java.io.File;
import java.util.List;

/**
 * @author : Vander
 * @date :   2020/9/4
 * @description : 此测试类既能满足SFTP协议也能满足FTP协议，仅仅需要修改application-ftp.properties中的配置
 * 即能做到随意切换协议
 */
@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ImportAutoConfiguration(FtpDemoStarter.class)
@PropertySource("application.properties")
// 按方法名称的进行排序，由于是按字符的字典顺序
@FixMethodOrder(value = MethodSorters.NAME_ASCENDING)
public class FTPClientUtilTest {


    private final static String LOCAL_DIR = "work";

    private final static String LOCAL_FILENAME = "测试-ftp.txt";

    private final static String REMOTE_DIR = "/incoming/1/2/3";

    private final static String REMOTE_DEST_DIR = "/work/a/b";

    private final static String REMOTE_FILENAME = "测试-ftp.txt";

    @Autowired
    private FTPClientUtil ftpClientUtil;

    @Test
    public void test0Put() throws Exception {
        File file = new File(LOCAL_DIR + File.separator + LOCAL_FILENAME);
        ftpClientUtil.put(file, REMOTE_DIR, file.getName(), ".doing");
        System.out.println("\n\n");
        log.info("上传本地文件{}/{}到远程目录{}成功！", LOCAL_DIR, LOCAL_FILENAME, REMOTE_DIR);
        System.out.println("\n\n");
    }

    @Test
    public void test1Get() throws Exception {
        String remoteFilename = REMOTE_FILENAME;
        ftpClientUtil.get(LOCAL_DIR, REMOTE_DIR, remoteFilename);
        System.out.println("\n\n");
        log.info("下载远程文件{}/{}到本地{}成功！", REMOTE_DIR, remoteFilename, LOCAL_DIR);
        System.out.println("\n\n");
    }

    @Test
    public void test2Get() throws Exception {
        String remoteFilePathUri = REMOTE_DIR + "/" + REMOTE_FILENAME;
        ftpClientUtil.get(LOCAL_DIR, remoteFilePathUri);
        System.out.println("\n\n");
        log.info("下载远程文件{}到本地{}成功！", remoteFilePathUri, LOCAL_DIR);
        System.out.println("\n\n");
    }

    @Test
    public void test3Move() throws Exception {
        // 将文件从远程目录1移动到远程目录2
        ftpClientUtil.move(REMOTE_DIR, REMOTE_DEST_DIR, REMOTE_FILENAME);
        System.out.println("\n\n");
        log.info("移动远程文件{}/{}到远程目录{}成功！", REMOTE_DIR, REMOTE_FILENAME, REMOTE_DEST_DIR);
        System.out.println("\n\n");
    }

    @Test
    public void test4Copy() throws Exception {
        // 将文件从远程目录1复制到远程目录2，复制后重命名
        try {
            ftpClientUtil.copy(REMOTE_DIR, REMOTE_DEST_DIR, REMOTE_FILENAME, ".doing", REMOTE_FILENAME);
        } catch (Exception e) {
            System.out.println("\n\n");
            log.error("将FTP源文件{}/{}复制到{}/{}失败！", REMOTE_DIR, REMOTE_FILENAME, REMOTE_DEST_DIR, REMOTE_FILENAME, e);
            System.out.println("\n\n");
        }
        System.out.println("\n\n");
        log.info("将FTP源文件{}/{}复制到{}/{}成功！", REMOTE_DIR, REMOTE_FILENAME, REMOTE_DEST_DIR, REMOTE_FILENAME);
        System.out.println("\n\n");
    }

    @Test
    public void test5DeleteRemoteFile() throws Exception {
        String remoteFilePathUri = REMOTE_DEST_DIR + "/" + REMOTE_FILENAME;
        Assert.isTrue(ftpClientUtil.deleteFile(remoteFilePathUri),
                String.format("删除远程文件：%s失败！！！", remoteFilePathUri));
        System.out.println("\n\n");
        log.info("删除FTP源文件{}成功！", remoteFilePathUri);
        System.out.println("\n\n");
    }

    @Test
    public void test6RemoveDirectory() throws Exception {
        Assert.isTrue(ftpClientUtil.removeDirectory("/work/tmp"),
                String.format("删除远程目录：%s失败！！！", "/work/tmp"));
        System.out.println("\n\n");
        log.info("删除FTP远程目录{}成功！", "/work/tmp");
        System.out.println("\n\n");
    }

    @Test
    public void test7GetRemoteDirFilenames() throws Exception {
        System.out.println("\n\n");
        List<String> remoteDirFilenames = ftpClientUtil.getRemoteDirFilenames("/incoming");
        System.out.println(remoteDirFilenames);
        System.out.println("\n\n");
    }

    @Test
    public void test8GetRemoteDirFTPFiles() throws Exception {
        System.out.println("\n\n");
        List<FTPFile> remoteDirFiles = ftpClientUtil.getRemoteDirFTPFiles("/incoming");
        System.out.println(remoteDirFiles);
        System.out.println("\n\n");
    }

    @Test
    public void test9GetRemoteFileByDir() throws Exception {
        System.out.println("\n\n");
        System.out.println("---------- 展示一层目录下的文件 ------------");
        List<RemoteFileInfo> remoteFiles = ftpClientUtil.getRemoteFileByDir("/incoming");
        System.out.println(remoteFiles);
        System.out.println("---------- 展示多层目录下的文件 ------------");
        List<RemoteFileInfo> remoteFileByDir = ftpClientUtil.getRemoteFileByDir("/incoming", true);
        System.out.println(remoteFileByDir);
        System.out.println("\n\n");
    }

    /**
     * 测试isRemotePathExist方法
     * 环境：预先在ftp服务器创建/incoming目录，/incoming/1.txt文件,删除/incomingg/1.txt(存在的话）
     * @throws Exception
     */
    @Test
    public void test10IsRemotePathExist() throws Exception{
        String remoteFile = "/incoming/" + REMOTE_FILENAME;
        String remoteDir = "/incoming";
        Assert.isTrue(ftpClientUtil.isRemotePathExist(remoteFile), "不存在文件"+remoteFile);
        Assert.isTrue(ftpClientUtil.isRemotePathExist(remoteDir), "不存在目录"+remoteDir);

        String notExistFile = "/incoming/1.txt";
        Assert.isTrue(!ftpClientUtil.isRemotePathExist(notExistFile), "存在文件"+notExistFile);
    }

    @Test
    public void test11MkDir1() throws SftpException {
        try {
            ((GenericFTPClientUtilImpl) ftpClientUtil).mkDir("/work/0077/20200825/200/upay/abc");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test11MkDir2() throws SftpException {
        try {
            ((GenericSFTPClientUtilImpl) ftpClientUtil).mkDir("/work/0077/20200825/200/upay/abc");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
