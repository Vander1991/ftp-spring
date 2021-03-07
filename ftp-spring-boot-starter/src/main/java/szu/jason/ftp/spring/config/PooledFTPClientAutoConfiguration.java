package szu.jason.ftp.spring.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import szu.jason.ftp.spring.FTPClientUtil;
import szu.jason.ftp.spring.FTPClientUtilProperties;
import szu.jason.ftp.spring.FtpClientProtocolConstant;
import szu.jason.ftp.spring.exception.FTPProtocolNotFoundUtilException;
import szu.jason.ftp.spring.factory.FTPClientPooledObjectFactory;
import szu.jason.ftp.spring.factory.SFTPClientPooledObjectFactory;
import szu.jason.ftp.spring.impl.PooledFTPClientUtilImpl;
import szu.jason.ftp.spring.impl.PooledSFTPClientUtilImpl;
import szu.jason.ftp.spring.pool.FTPClientPool;

/**
 * @author : Vander
 * @date :   2020/9/4
 * @description : 根据协议选择，注入需要的工厂和工具类
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
        prefix = "ftp.client",
        value = "enabled")
@EnableConfigurationProperties({FTPClientUtilProperties.class})
public class PooledFTPClientAutoConfiguration {

    @Primary
    @Bean(name = "ftpClientUtilPool")
    @ConditionalOnProperty(
            name = "ftp.client.protocol",
            havingValue = "ftp",
            matchIfMissing = true
    )
    public FTPClientPool ftpClientUtilCommonPool(FTPClientUtilProperties ftpClientUtilProperties) throws Exception {
        return new FTPClientPool(new FTPClientPooledObjectFactory(ftpClientUtilProperties), ftpClientUtilProperties);
    }

    @Bean(name = "sftpClientUtilPool")
    @ConditionalOnProperty(
            name = "ftp.client.protocol",
            havingValue = "sftp"
    )
    public FTPClientPool sftpClientUtilCommonPool(FTPClientUtilProperties ftpClientUtilProperties) throws Exception {
        return new FTPClientPool(new SFTPClientPooledObjectFactory(ftpClientUtilProperties), ftpClientUtilProperties);
    }

    @Bean(name = "pooledFTPClientUtil")
    public FTPClientUtil pooledFTPClientUtil(FTPClientUtilProperties ftpClientUtilProperties,
                                             GenericObjectPool ftpClientPool) {
        if (ftpClientUtilProperties.getProtocol().equals(FtpClientProtocolConstant.FTP_PROTOCOL)) {
            return new PooledFTPClientUtilImpl(ftpClientPool);
        } else if (ftpClientUtilProperties.getProtocol().equals(FtpClientProtocolConstant.SFTP_PROTOCOL)) {
            return new PooledSFTPClientUtilImpl(ftpClientPool);
        } else {
            throw new FTPProtocolNotFoundUtilException(
                    String.format("FTP客户端暂不支持此协议", ftpClientUtilProperties.getProtocol()));
        }
    }

}
