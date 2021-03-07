package szu.jason.ftp.spring.util;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件处理工具类
 * @author liyc02
 * @version V1.0
 */
public class FileUtil {
	/**
	 * 当前运行classpath
	 */
	private static String currentClasspath;
	private final static String WINDOWS = "Windows";



	static {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		currentClasspath = classLoader.getResource("").getPath();
		// 当前操作系统
		String osName = System.getProperty("os.name");
		if (osName.startsWith(WINDOWS)) {
			// 删除path中最前面的/
			currentClasspath = currentClasspath.substring(1);
		}
	}
	
	/**
	 * 获取当前运行环境的classpath全路径
	 * @return
	 */
	public static String getCurrentClasspath() {
		if (currentClasspath == null) {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			currentClasspath = classLoader.getResource("").getPath();
			// 当前操作系统
			String osName = System.getProperty("os.name");
			if (osName.startsWith(WINDOWS)) {
				// 删除path中最前面的/
				currentClasspath = currentClasspath.substring(1);
			}
		}
		return currentClasspath;
	}
	
	/**
	 * 使用UTF-8编码将整个文件读取到一整行，"#"开头的行会被当成配置忽略
	 * @param fileName 文件名全路径
	 * @return
	 */
	public static String readAllFileToOneLine(String fileName) {
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader reader = Files.newBufferedReader(Paths.get(fileName));
			String line = reader.readLine();
		    while (line != null) {
		    	if (line.trim().startsWith("#")) {
		    		// 注释行，不处理
		    	} else {
		    		sb.append(line);
		    	}
		    	line = reader.readLine();
		    }
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return sb.toString().replaceAll("\r|\t|\n", "");
	}

	/**
	 * 复制文件
	 * @param pathUri
	 * @param filename
	 * @param newFilename
	 * @return
	 */
	public static File backupFile(String pathUri, String filename, String newFilename) {
		File file = new File(pathUri, filename);
		File newFile = new File(pathUri, newFilename);
		try {
			FileUtils.copyFile(file, newFile);
		} catch (IOException e) {
			throw new RuntimeException(String.format("对文件%s进行备份失败，使用的新文件名为：%s！", filename,
					newFilename), e);
		}
		return newFile;
	}

	/**
	 * 将源目录直接移动到备份目录
	 *
	 * @param srcDir
	 * @param backupDir
	 */
	public static void backupFilesByDir(String srcDir, String backupDir) {
		FileUtil.moveDir(srcDir, backupDir);
	}


	/**
	 * 创建本地目录
	 *
	 * @param localDir
	 * @return
	 * @throws Exception
	 */
	public static String createLocalDir(String localDir) throws Exception {
		File dir = Paths.get(localDir).toFile();
		if (!dir.exists()) {
			try {
				dir.mkdirs();
			} catch (Exception e) {
				throw new RuntimeException(String.format("can't create localPath: %s", localDir));
			}
		}
		return localDir;
	}

	/**
	 * 创建文件的同时创建文件的父目录
	 *
	 * @param fileDir
	 * @param filename
	 * @return
	 */
	public static File createFileByDir(String fileDir, String filename) {
		File file = new File(fileDir, filename);
		if(!file.getParentFile().exists()) {
			//如果目标文件所在的目录不存在，则创建父目录
			if(!file.getParentFile().mkdirs()) {
				throw new RuntimeException(String.format("创建目录：%s失败！", file.getParentFile().getAbsolutePath()));
			}
		}
		return file;
	}

	/**
	 * 移动源目录到目的目录
	 * @param srcDir
	 * @param destDir
	 */
	public static void moveDir(String srcDir, String destDir) {
		File sirFileDir = new File(srcDir);
		File destFileDir = new File(destDir);
		try {
			FileUtils.moveDirectoryToDirectory(sirFileDir, destFileDir, true);
		} catch (IOException e) {
			throw new RuntimeException(String.format("移动目录%s到目录%s失败！", srcDir, destDir), e);
		}
	}

	/**
	 * 删除文件
	 * @param fileName 文件名全路径
	 * @return
	 */
	public static boolean deleteFile(String fileName) {
		try {
			File file = Paths.get(fileName).toFile();
			return file.delete();
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 递归删除目录及其所有文件
	 * @param file
	 * @return
	 */
	public static boolean deleteDirectory(File file) {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (File f : files) {
				// 递归调用
				if (!deleteDirectory(f)) {
					return false;
				}
			}
			// 最后把空目录删除
			return file.delete();
		}
		return file.delete();
	}
	
	/**
	 * 递归删除目录及其所有文件
	 * @param path
	 * @return
	 */
	public static boolean deleteDirectory(Path path) {
		return deleteDirectory(path.toFile());
	}


}
