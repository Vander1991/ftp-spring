package szu.jason.ftp.spring;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author : Vander
 * @date :   2020/9/4
 * @description :
 */
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class SFTPConnector {
    /**
     * 连接建立起的会话，一段时间不用，会话会断开
     */
    private Session session;
    /**
     * 通过会话建立起的通道
     */
    private ChannelSftp channelSftp;

}
