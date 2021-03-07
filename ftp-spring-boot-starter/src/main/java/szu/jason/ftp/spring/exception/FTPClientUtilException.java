package szu.jason.ftp.spring.exception;

import lombok.Getter;
import szu.jason.ftp.spring.FTPClientUtilProperties;

/**
 * @author : Vander
 * @date :   2020/9/2
 * @description :
 */
@Getter
public class FTPClientUtilException extends RuntimeException {

    protected FTPClientUtilProperties ftpClientUtilProperties;

    public FTPClientUtilException() {
    }

    public FTPClientUtilException(String message) {
        super(message);
    }

    public FTPClientUtilException(FTPClientUtilProperties ftpClientUtilProperties, String message, Throwable cause) {
        super(message, cause);
        this.ftpClientUtilProperties = ftpClientUtilProperties;
    }

    public FTPClientUtilException(String message, Throwable cause) {
        super(message, cause);
    }

}
