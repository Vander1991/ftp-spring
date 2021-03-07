package szu.jason.ftp.spring.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.springframework.util.Assert;
import szu.jason.ftp.spring.FTPClientUtilProperties;
import szu.jason.ftp.spring.exception.FTPClientUtilException;
import szu.jason.ftp.spring.factory.FTPClientPooledObjectFactory;

/**
 * @author : Vander
 * @date :   2020/9/3
 * @description : 实现单连接的FTP客户端，每次操作都会销毁连接
 */
@Slf4j
public class FTPClientUtilImpl extends GenericFTPClientUtilImpl {

    private FTPClientPooledObjectFactory ftpClientPooledObjectFactory;

    public FTPClientUtilImpl(FTPClientUtilProperties properties) {
        ftpClientPooledObjectFactory = new FTPClientPooledObjectFactory(properties);
    }

    @Override
    public FTPClient getFtpClient() {
        PooledObject<FTPClient> ftpClientPooledObject;
        try {
            ftpClientPooledObject = ftpClientPooledObjectFactory.makeObject();
            Assert.notNull(ftpClientPooledObject, "创建FTPClient连接失败！");
        } catch (Exception e) {
            throw new FTPClientUtilException("创建FTP连接失败！", e);
        }
        return ftpClientPooledObject.getObject();
    }

    @Override
    public void returnFtpClient(FTPClient fTPClient) {
        try {
            ftpClientPooledObjectFactory.destroyObject(new DefaultPooledObject<>(fTPClient));
        } catch (Exception e) {
            throw new FTPClientUtilException("销毁FTP连接失败！", e);
        }
    }
}
