package szu.jason.ftp.spring.factory;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import szu.jason.ftp.spring.FTPClientUtilProperties;
import szu.jason.ftp.spring.exception.FTPClientUtilException;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * @author : Vander
 * @date :   2020/9/3
 * @description :
 */
@Slf4j
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FTPClientPooledObjectFactory implements PooledObjectFactory<FTPClient> {

    /**
     * client pool 基本信息
     */
    private FTPClientUtilProperties properties;

    @Override
    public PooledObject<FTPClient> makeObject() throws Exception {
        FTPClient ftpClient = new FTPClient();
        String host = properties.getHostname();
        int port = properties.getPort();
        String username = properties.getUsername();
        try {
            ftpClient.setControlEncoding("UTF-8");
            // 不需要写死ftp server的OS TYPE,FTPClient getSystemType()方法会自动识别
            // ftpClient.configure(new FTPClientConfig(FTPClientConfig.SYST_UNIX));
            ftpClient.setDefaultTimeout(properties.getDefaultTimeout());
            ftpClient.setConnectTimeout(properties.getConnectTimeout());
            ftpClient.setDataTimeout(properties.getDataTimeout());

            // 连接登录
            ftpClient.connect(host, port);
            ftpClient.login(username, properties.getPassword());

            ftpClient.enterRemotePassiveMode();
            ftpClient.enterLocalPassiveMode();
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                String message = String
                        .format("与ftp服务器建立连接失败,host:%s, port:%s, username:%s, replyCode:%s",
                                host, port, username, reply);
                log.error(message);
                throw new FTPClientUtilException(message);
            }
        } catch (UnknownHostException e) {
            String message = String.format(
                    "请确认ftp服务器地址是否正确，无法连接到地址为: [%s] 的ftp服务器, errorMessage:%s",
                    host, e.getMessage());
            log.error(message);
            throw new FTPClientUtilException(message, e);
        } catch (IllegalArgumentException e) {
            String message = String.format(
                    "请确认连接ftp服务器端口是否正确，错误的端口: [%s], errorMessage:%s", port,
                    e.getMessage());
            log.error(message);
            throw new FTPClientUtilException(message, e);
        } catch (Exception e) {
            String message = String
                    .format("与ftp服务器建立连接失败,host:%s, port:%s, username:%s, errorMessage:%s",
                            host, port, username, e.getMessage());
            log.error(message);
            throw new FTPClientUtilException(message, e);
        }
        return new DefaultPooledObject<>(ftpClient);
    }

    @Override
    public void destroyObject(PooledObject<FTPClient> p) throws Exception {
        FTPClient ftpClient = p.getObject();
        if (ftpClient.isConnected()) {
            try {
                ftpClient.logout();
            } catch (IOException e) {
                String message = String.format(
                        "与ftp服务器断开连接失败, errorMessage:%s", e.getMessage());
                log.error(message);
                throw new FTPClientUtilException(message, e);
            } finally {
                if (ftpClient.isConnected()) {
                    try {
                        ftpClient.disconnect();
                    } catch (IOException e) {
                        String message = String.format(
                                "与ftp服务器断开连接失败, errorMessage:%s",
                                e.getMessage());
                        log.error(message);
                        throw new FTPClientUtilException(message, e);
                    }
                }
                // help gc
                ftpClient = null;
                p = null;
            }
        }
    }

    @Override
    public boolean validateObject(PooledObject<FTPClient> p) {
        FTPClient ftpClient = p.getObject();
        if (ftpClient.isConnected()) {
            try {
                ftpClient.sendNoOp();
            } catch (IOException e) {
                String message = String.format(
                        "发送noop消息到ftp服务器失败, errorMessage:%s", e.getMessage());
                log.error(message);
                return false;
            }
        }
        return true;
    }

    @Override
    public void activateObject(PooledObject<FTPClient> p) throws Exception {
        // noop
    }

    @Override
    public void passivateObject(PooledObject<FTPClient> p) throws Exception {
        // noop
    }
}
