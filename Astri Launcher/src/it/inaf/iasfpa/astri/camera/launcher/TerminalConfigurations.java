package it.inaf.iasfpa.astri.camera.launcher;

public class TerminalConfigurations {
	
	public ServerConfiguration[] getServer() {
		return server;
	}
	public void setServer(ServerConfiguration[] server) {
		this.server = server;
	}
	public ClientConfiguration[] getClient() {
		return client;
	}
	public void setClient(ClientConfiguration[] client) {
		this.client = client;
	}
	private ServerConfiguration[] server;
	private ClientConfiguration[] client;
	
	
	

}
