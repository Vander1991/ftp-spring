package szu.jason.ftp.spring;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author : Vander
 * @date :   2020/9/2
 * @description : FTPClientPool配置{@link FTPClient}
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ftp.client")
public class FTPClientUtilProperties extends GenericObjectPoolConfig {
    /**
     * ftp client 协议
     */
    private String protocol;
    /**
     * ftp服务器主机名，ip地址
     */
    private String hostname;
    /**
     * ftp 端口号
     */
    private int port;
    /**
     * ftp 用户名
     */
    private String username;
    /**
     * ftp 用户密码
     */
    private String password;
    /**
     * Sftp  密钥文件路径
     */
    private String privateKey;
    /**
     * Sftp  密钥口令
     */
    private String passPhrase;
    /**
     * ftp 文件模式
     */
    private int fileType = FTP.BINARY_FILE_TYPE;
    /**
     * ftp 文件传输模式
     */
    private int fileTransferMode = FTP.STREAM_TRANSFER_MODE;
    /**
     * ACTIVE_LOCAL_DATA_CONNECTION_MODE
     * ACTIVE_REMOTE_DATA_CONNECTION_MODE
     * PASSIVE_LOCAL_DATA_CONNECTION_MODE
     * PASSIVE_REMOTE_DATA_CONNECTION_MODE
     */
    private int dataConnectMode = FTPClient.PASSIVE_REMOTE_DATA_CONNECTION_MODE;
    /**
     * 传输缓存大小，单位字节（byte）
     * Set the internal buffer size for buffered data streams.
     */
    private int bufferSize = 10240;
    /**
     * The timeout in milliseconds to use for the socket connection
     */
    private int defaultTimeout = 60000;
    /**
     * Sets the timeout in milliseconds to use when reading from the data connection.
     */
    private int dataTimeout = 60000;
    /**
     * The connection timeout to use (in ms)
     */
    private int connectTimeout = 60000;
    /**
     * 默认编码为UTF-8
     */
    private String encoding = "UTF-8";

}
