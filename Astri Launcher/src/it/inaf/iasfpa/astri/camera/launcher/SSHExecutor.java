package it.inaf.iasfpa.astri.camera.launcher;

import com.jcraft.jsch.*;

import javafx.scene.control.TextArea;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

public class SSHExecutor {

	public Session session;
	public Channel channel;
	public Boolean stopTrue = false;
	private TextArea outputText;
	
	public SSHExecutor(TextArea outputText) {
		this.outputText = outputText;
	}
	
	public void connect(String ip, int port, String user, String pwd) {
		try {
			JSch jsch = new JSch();
			session = jsch.getSession(user, ip, port);
			session.setPassword(pwd);
			Properties config = new Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			// Execution start time
			long lStartTime = new Date().getTime();
//			System.out.println("SSH Connection...");
			outputText.appendText("SSH Connection...\n");
			session.connect();
//			System.out.println("Connected to the ssh.");
			outputText.appendText("Connected to the ssh.\n");
			// Execution end time
			long lEndTime = new Date().getTime();
//			System.out.println("---------------------------------------------");
//			System.out.println("SSH connection in : " + (lEndTime - lStartTime));
			outputText.appendText("---------------------------------------------\n");
			outputText.appendText("SSH connection in : " + (lEndTime - lStartTime)+ "\n");
		} catch (JSchException e) {
//			System.out.println("Failed to connect");
			outputText.appendText("Failed to connect\n");
			e.printStackTrace();
		}
	}

	public void disconnectSession() {
		session.disconnect();
	}


//	public String executeCommand(String cmd, boolean resp) {
//		try {
//			channel = session.openChannel("exec");
//		} catch (JSchException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		channel.setXForwarding(true);
//		((ChannelExec) channel).setCommand(cmd);
//		try {
//			StringBuilder errorBuffer = new StringBuilder();
//			StringBuilder outputBuffer = new StringBuilder();
//			InputStream in = channel.getInputStream();
//			InputStream err = channel.getExtInputStream();
//			channel.connect();
//			String lastString = "";
//			byte[] tmp = new byte[1024];
//			while (in.available() <= 0 && resp == true) {
//				
//			}
//			while (in.available() > 0) {
//				int i = in.read(tmp, 0, 1024);
//				if (i < 0)
//					break;
//				lastString = new String(tmp, 0, i);
//				outputBuffer.append(lastString);
//				
//			}
//				
//			
//			
//			return  outputBuffer.substring(0);
//			
//		} catch (IOException e1) {
//			e1.printStackTrace();
//			return null;
//		} catch (JSchException e) {
//			e.printStackTrace();
//			return null;
//		} 
//		}
		
		
	public void executeCommand(String cmd, String exitCondition) {
		StringBuilder outputBuffer = new StringBuilder();
		StringBuilder errorBuffer = new StringBuilder();
		try {
			channel = session.openChannel("exec");
			channel.setXForwarding(true);
			
			((ChannelExec) channel).setCommand(cmd);
			InputStream in = channel.getInputStream();
			InputStream err = channel.getExtInputStream();
			
			if(!channel.isConnected()) {
				channel.connect();
			}
			stopTrue = false;
			String lastString = "";
			byte[] tmp = new byte[1024];
			while (!stopTrue) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					lastString = new String(tmp, 0, i);
//					System.out.println(lastString);
					outputText.appendText(lastString +"\n");
					outputBuffer.append(lastString);
				}
				while (err.available() > 0) {
					int i = err.read(tmp, 0, 1024);
					if (i < 0)
						break;
					errorBuffer.append(new String(tmp, 0, i));
				}
				if (channel.isClosed()) {
					if ((in.available() > 0) || (err.available() > 0))
						continue;
//					System.out.println("exit-status: " + channel.getExitStatus());
					outputText.appendText("exit-status: " + channel.getExitStatus() +"\n");
					break;
				}
				if (exitCondition != null) {
					if (lastString.contains(exitCondition)) {
//						System.out.println("### EXIT CONDITION ###");
						//break;
					}
				}
				try {
					Thread.sleep(1000);
				} catch (Exception ee) {
				}
			}
			System.out.println("error: " + errorBuffer.toString());
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	
	public void disconnectChannel() {
		channel.disconnect();
		
	}
	
	public void setStop(){
		this.stopTrue = true;
	}
	
}
