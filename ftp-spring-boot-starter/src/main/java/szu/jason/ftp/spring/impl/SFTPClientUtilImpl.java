package szu.jason.ftp.spring.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.springframework.util.Assert;
import szu.jason.ftp.spring.FTPClientUtilProperties;
import szu.jason.ftp.spring.SFTPConnector;
import szu.jason.ftp.spring.exception.FTPClientUtilException;
import szu.jason.ftp.spring.factory.SFTPClientPooledObjectFactory;

/**
 * @author : Vander
 * @date :   2020/9/3
 * @description :
 */
@Slf4j
public class SFTPClientUtilImpl extends GenericSFTPClientUtilImpl {

    private SFTPClientPooledObjectFactory sftpClientPooledObjectFactory;

    public SFTPClientUtilImpl(FTPClientUtilProperties properties) {
        sftpClientPooledObjectFactory = new SFTPClientPooledObjectFactory(properties);
    }

    @Override
    public SFTPConnector getConnector() {
        PooledObject<SFTPConnector> sftpConnectorPooledObject;
        try {
            sftpConnectorPooledObject = sftpClientPooledObjectFactory.makeObject();
            Assert.notNull(sftpConnectorPooledObject, "创建SFTPClient连接失败！");
        } catch (Exception e) {
            throw new FTPClientUtilException("创建SFTP连接失败！", e);
        }
        return sftpConnectorPooledObject.getObject();
    }

    @Override
    public void returnConnector(SFTPConnector sftpConnector) {
        try {
            sftpClientPooledObjectFactory.destroyObject(new DefaultPooledObject<>(sftpConnector));
        } catch (Exception e) {
            throw new FTPClientUtilException("销毁SFTP连接失败！", e);
        }
    }

}
