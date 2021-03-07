package szu.jason.ftp.spring.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.pool2.impl.GenericObjectPool;
import szu.jason.ftp.spring.exception.FTPClientUtilException;

/**
 * @author : Vander
 * @date :   2020/9/3
 * @description : 实现池化的FTP客户端，每次操作完将连接放回
 */
@Slf4j
public class PooledFTPClientUtilImpl extends GenericFTPClientUtilImpl {

    private GenericObjectPool<FTPClient> pool;

    public PooledFTPClientUtilImpl(GenericObjectPool<FTPClient> pool) {
        this.pool = pool;
    }

    @Override
    public FTPClient getFtpClient() {
        FTPClient ftpClient;
        try {
            ftpClient = pool.borrowObject();
        } catch (Exception e) {
            throw new FTPClientUtilException("从SFTP连接池中获取连接异常！", e);
        }
        return ftpClient;
    }

    @Override
    public void returnFtpClient(FTPClient fTPClient) {
        try {
            pool.returnObject(fTPClient);
        } catch (Exception e) {
            throw new FTPClientUtilException("归还连接到SFTP连接池异常！", e);
        }
    }
}
