package szu.jason.ftp.spring;

import java.util.Date;

/**
 * @author : Vander
 * @date :   2021/2/26
 * @description :
 */
public class RemoteFileInfo {

	/**
	 * 文件大小,The file size in bytes
	 */
	private long size;
	/**
	 * 文件名,The name of the file
	 */
	private String name;
	/**
	 * 是不是目录,Determine if the file is a directory
	 */
	private boolean isDirectory;
	/**
	 * 是不是文件,Determine if the file is a regular file
	 */
	private boolean isFile;
	/**
	 * 是不是连接,Determine if the file is a symbolic link.
	 */
	private boolean isLink;
	/**
	 * 用户访问ftp服务器上文件路径，即相对于用户ftp服务器 的home目录
	 */
	private String path;
	
	/**
	 * 最后修改时间
	 */
	private Date lastModifiedTime;
	
	
	public RemoteFileInfo() {
		super();
	}

	/**
	 * @param fileSize
	 * @param filename
	 * @param isDirectory
	 * @param isFile
	 * @param isLink
	 * @param path
	 */
	public RemoteFileInfo(long fileSize, String filename, boolean isDirectory, boolean isFile, boolean isLink, String path) {
		super();
		this.size = fileSize;
		this.name = filename;
		this.isDirectory = isDirectory;
		this.isFile = isFile;
		this.isLink = isLink;
		this.path = path;
	}

	/**
	 * @return the size
	 */
	public long getSize() {
		return size;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @param size
	 *            the size to set
	 */
	public void setSize(long size) {
		this.size = size;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the isDirectory
	 */
	public boolean isDirectory() {
		return isDirectory;
	}

	/**
	 * @param isDirectory
	 *            the isDirectory to set
	 */
	public void setDirectory(boolean isDirectory) {
		this.isDirectory = isDirectory;
	}

	/**
	 * @return the isFile
	 */
	public boolean isFile() {
		return isFile;
	}

	/**
	 * @param isFile
	 *            the isFile to set
	 */
	public void setFile(boolean isFile) {
		this.isFile = isFile;
	}

	/**
	 * @return the isLink
	 */
	public boolean isLink() {
		return isLink;
	}

	/**
	 * @param isLink
	 *            the isLink to set
	 */
	public void setLink(boolean isLink) {
		this.isLink = isLink;
	}

	public Date getLastModifiedTime() {
		return lastModifiedTime;
	}

	public void setLastModifiedTime(Date lastModifiedTime) {
		this.lastModifiedTime = lastModifiedTime;
	}

	@Override
	public String toString() {
		return name;
	}

}
