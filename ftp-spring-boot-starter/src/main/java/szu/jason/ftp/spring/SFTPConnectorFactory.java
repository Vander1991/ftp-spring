package szu.jason.ftp.spring;

/**
 * @author : Vander
 * @date :   2020/9/7
 * @description :
 */
public interface SFTPConnectorFactory {

    /**
     * 子类实现获取连接的方式
     *
     * @return
     */
    SFTPConnector getConnector();
    /**
     * 子类实现归还连接的方式
     *
     * @return
     */
     void returnConnector(SFTPConnector sftpConnector);

}
