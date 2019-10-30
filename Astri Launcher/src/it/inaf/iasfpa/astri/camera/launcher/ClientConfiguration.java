package it.inaf.iasfpa.astri.camera.launcher;

public class ClientConfiguration {

	private final String name;
	private final String ipAddress;
	private final String usr;
	private final String pwd;
	private final String dir_path;
	
	
	public String getName() {
		return name;
	}
	
	public String getIpAddress() {
		return ipAddress;
	}

	public String getUsr() {
		return usr;
	}

	public String getPwd() {
		return pwd;
	}
	
	public String getDir_path() {
		return dir_path;
	}

	

	public ClientConfiguration(String name, String ipAddress, String usr, String pwd, String dir_path) {
		super();
		this.name = name;
		this.ipAddress = ipAddress;
		this.usr = usr;
		this.pwd = pwd;
		this.dir_path = dir_path;
	}

		
}
