package szu.jason.ftp.spring;

import org.apache.commons.net.ftp.FTPClient;

/**
 * @author : Vander
 * @date :   2020/9/7
 * @description :
 */
public interface FTPConnectFactory {

    /**
     * 子类实现获取连接的方式
     *
     * @return
     */
    FTPClient getFtpClient();
    /**
     * 子类实现归还连接的方式
     *
     * @return
     */
    void returnFtpClient(FTPClient fTPClient);

}
