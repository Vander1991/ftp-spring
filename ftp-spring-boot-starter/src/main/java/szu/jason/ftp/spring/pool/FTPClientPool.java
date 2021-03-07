package szu.jason.ftp.spring.pool;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import szu.jason.ftp.spring.FTPClientUtilProperties;

/**
 * SFTP连接池<br>
 *
 * 继承{@ org.apache.commons.pool2.impl.GenericObjectPool<SftpClientImpl>}，使用父类连接管理的能力<br>
 *
 * getPoolInfo()提供了基本的连接池信息，并在DEBUG日志体现<br>
 *
 * 扩充连接池监控信息，如使用率、空闲率等<br>
 *
 * @author : Vander
 * @date :   2020/9/2
 * @description : FTPClientUtil接口通用的的连接池
 * 实现{@link org.apache.commons.pool2.ObjectPool}接口
 */
@Slf4j
public class FTPClientPool extends GenericObjectPool<FTPClient> {
    /**
     * 工厂负责生成特定的FTPClientUtil
     */
    private PooledObjectFactory factory;
    /**
     * client pool 基本信息
     */
    private FTPClientUtilProperties properties;

    /**
     * @param factory
     * @param properties
     * @throws Exception
     */
    public FTPClientPool(PooledObjectFactory factory, FTPClientUtilProperties properties) throws Exception {
        super(factory, properties);
        this.factory = factory;
        this.properties = properties;
        // 初始化连接池
        initPool(getMaxTotal());
    }

    /**
     * 初始化连接池，需要注入一个工厂来提供FTPClientUtil实例
     *
     * @param maxPoolSize
     * @throws Exception
     */
    private void initPool(int maxPoolSize) throws Exception {
        for (int i = 0; i < maxPoolSize; i++) {
            //往池中添加对象
            addObject();
        }
    }

}
