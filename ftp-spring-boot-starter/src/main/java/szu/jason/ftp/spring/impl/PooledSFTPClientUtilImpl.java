package szu.jason.ftp.spring.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import szu.jason.ftp.spring.SFTPConnector;
import szu.jason.ftp.spring.exception.FTPClientUtilException;

/**
 * @author : Vander
 * @date :   2020/9/3
 * @description : 实现池化的SFTP客户端，每次操作完将连接放回
 */
@Slf4j
public class PooledSFTPClientUtilImpl extends GenericSFTPClientUtilImpl {

    private GenericObjectPool<SFTPConnector> pool;

    public PooledSFTPClientUtilImpl(GenericObjectPool<SFTPConnector> pool) {
        this.pool = pool;
    }

    @Override
    public SFTPConnector getConnector() {
        SFTPConnector sftpConnector;
        try {
            sftpConnector = pool.borrowObject();
        } catch (Exception e) {
            throw new FTPClientUtilException("从SFTP连接池中获取连接异常！", e);
        }
        return sftpConnector;
    }

    @Override
    public void returnConnector(SFTPConnector sftpConnector) {
        try {
            pool.returnObject(sftpConnector);
        } catch (Exception e) {
            throw new FTPClientUtilException("归还连接到SFTP连接池异常！", e);
        }
    }
}
