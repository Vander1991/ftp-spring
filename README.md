# ftp-spring-boot-starter

支持FTP和SFTP的切换，通过common-pools管理FTP/SFTP的连接池，支持单连接和池化两种形式

## Quick Start

1. 添加依赖

```xml
<dependency>
    <groupId>szu.jason</groupId>
    <artifactId>ftp-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

2. 添加配置

```properties
#-------------------- FTP设置 ------------------------
ftp.client.enabled=true
ftp.client.protocol=ftp
# FTP的IP
ftp.client.hostname=192.168.118.8
# FTP端口
ftp.client.port=21
# FTP用户
ftp.client.username=vsftp
# FTP用户对应的密码
ftp.client.password=vsftp
#ftp.client.password=ENC(4D990CD36EC00447514B0610B97A3102)
#设置ftp缓冲大小
ftp.client.bufferSize=10240

ftp.client.data-timeout=60000

ftp.client.maxTotal=32
ftp.client.maxIdle=16
ftp.client.minIdle=8

ftp.client.jmx-enabled=false
ftp.client.jmx-name-prefix=ftp.client
```

3. 测试用例

```java
/**
 * @author : Vander
 * @date :   2020/9/4
 * @description : 此测试类既能满足SFTP协议也能满足FTP协议，仅仅需要修改application-ftp.properties中的配置
 * 即能做到随意切换协议
 */
@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ImportAutoConfiguration(FtpDemoStarter.class)
@PropertySource("application.properties")
// 按方法名称的进行排序，由于是按字符的字典顺序
@FixMethodOrder(value = MethodSorters.NAME_ASCENDING)
public class FTPClientUtilTest {


    private final static String LOCAL_DIR = "work";

    private final static String LOCAL_FILENAME = "测试-ftp.txt";

    private final static String REMOTE_DIR = "/incoming/1/2/3";

    private final static String REMOTE_DEST_DIR = "/work/a/b";

    private final static String REMOTE_FILENAME = "测试-ftp.txt";

    @Autowired
    private FTPClientUtil ftpClientUtil;

    @Test
    public void test0Put() throws Exception {
        File file = new File(LOCAL_DIR + File.separator + LOCAL_FILENAME);
        ftpClientUtil.put(file, REMOTE_DIR, file.getName(), ".doing");
        System.out.println("\n\n");
        log.info("上传本地文件{}/{}到远程目录{}成功！", LOCAL_DIR, LOCAL_FILENAME, REMOTE_DIR);
        System.out.println("\n\n");
    }
}
```

4. 测试用例见ftp-spring-boot-starter-test模块