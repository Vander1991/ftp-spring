package szu.jason.ftp.spring.factory;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.springframework.util.StringUtils;
import szu.jason.ftp.spring.FTPClientUtilProperties;
import szu.jason.ftp.spring.SFTPConnector;
import szu.jason.ftp.spring.exception.FTPClientUtilException;

import java.util.Properties;

/**
 * @author : Vander
 * @date :   2020/9/3
 * @description :
 */
@Slf4j
public class SFTPClientPooledObjectFactory implements PooledObjectFactory<SFTPConnector> {

    /**
     * client pool 基本信息
     */
    private FTPClientUtilProperties properties;

    public SFTPClientPooledObjectFactory(FTPClientUtilProperties properties) {
        this.properties = properties;
    }

    @Override
    public PooledObject<SFTPConnector> makeObject() throws Exception {
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp channelSftp = null;
        String username = properties.getUsername();
        String host = properties.getHostname();
        int port = properties.getPort();
        try {
            if (StringUtils.hasText(properties.getPrivateKey())) {
                // 使用密钥验证方式，密钥可以使有口令的密钥，也可以是没有口令的密钥
                if (StringUtils.hasText(properties.getPassPhrase())) {
                    jsch.addIdentity(properties.getPrivateKey(), properties.getPassPhrase());
                } else {
                    jsch.addIdentity(properties.getPrivateKey());
                }
            }
            session = jsch.getSession(username, host, port);
            if (session == null) {
                throw new FTPClientUtilException(
                        "创建ftp连接session失败,无法通过sftp与服务器建立链接，请检查主机名和用户名是否正确.");
            }
            session.setPassword(properties.getPassword());
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            // config.put("PreferredAuthentications", "password");
            session.setConfig(config);
            session.setTimeout(properties.getConnectTimeout());
            session.connect();

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
        } catch (JSchException e) {
            if (null != e.getCause()) {
                String cause = e.getCause().toString();
                String unknownHostException = "java.net.UnknownHostException: "
                        + host;
                String illegalArgumentException = "java.lang.IllegalArgumentException: port out of range:"
                        + port;
                String wrongPort = "java.net.ConnectException: Connection refused";
                if (unknownHostException.equals(cause)) {
                    String message = String
                            .format("请确认ftp服务器地址是否正确，无法连接到地址为: [%s] 的ftp服务器, errorMessage:%s",
                                    host, e.getMessage());
                    log.error(message);
                    throw new FTPClientUtilException(message, e);
                } else if (illegalArgumentException.equals(cause)
                        || wrongPort.equals(cause)) {
                    String message = String.format(
                            "请确认连接ftp服务器端口是否正确，错误的端口: [%s], errorMessage:%s",
                            port, e.getMessage());
                    log.error(message);
                    throw new FTPClientUtilException(message, e);
                }
            } else {
                String message = String
                        .format("与ftp服务器建立连接失败,请检查主机、用户名、密码是否正确, host:%s, port:%s, username:%s, errorMessage:%s",
                                host, port, username, e.getMessage());
                log.error(message);
                throw new FTPClientUtilException(message, e);
            }
        }
        return new DefaultPooledObject<>(new SFTPConnector(session, channelSftp));
    }

    @Override
    public void destroyObject(PooledObject<SFTPConnector> p) throws Exception {
        SFTPConnector sftpConnector = null;
        ChannelSftp channelSftp = null;
        Session session = null;
        if (p == null) {
            return;
        }

        sftpConnector = p.getObject();

        if (sftpConnector != null) {
            channelSftp = sftpConnector.getChannelSftp();
            channelSftp.disconnect();
            channelSftp = null;
        }
        if (sftpConnector != null) {
            session = sftpConnector.getSession();
            session.disconnect();
            session = null;
        }
    }

    @Override
    public boolean validateObject(PooledObject<SFTPConnector> p) {
        SFTPConnector sftpConnector = null;
        if (p != null) {
            sftpConnector = p.getObject();
        }
        if (sftpConnector.getChannelSftp().isConnected()) {
            try {
                sftpConnector.getChannelSftp().sendSignal("hello!!!");
            } catch (Exception e) {
                String message = String.format(
                        "发送信号消息到ftp服务器失败, errorMessage:%s", e.getMessage());
                log.error(message);
                return false;
            }
        }
        return true;
    }

    @Override
    public void activateObject(PooledObject<SFTPConnector> p) throws Exception {

    }

    @Override
    public void passivateObject(PooledObject<SFTPConnector> p) throws Exception {

    }
}
