package szu.jason.ftp.spring.exception;


import szu.jason.ftp.spring.FTPClientUtilProperties;

/**
 * @author : Vander
 * @date :   2020/9/2
 * @description :
 */
public class FTPProtocolNotFoundUtilException extends FTPClientUtilException {

    public FTPProtocolNotFoundUtilException(String message) {
        super(message);
    }

    public FTPProtocolNotFoundUtilException(FTPClientUtilProperties ftpClientUtilProperties) {
        this.ftpClientUtilProperties = ftpClientUtilProperties;
    }

    public FTPProtocolNotFoundUtilException(FTPClientUtilProperties ftpClientUtilProperties, String message, Throwable cause) {
        super(ftpClientUtilProperties, message, cause);
    }

    public FTPProtocolNotFoundUtilException(String message, Throwable cause) {
        super(message, cause);
    }

}
