package it.inaf.iasfpa.astri.camera.launcher;

import com.jcraft.jsch.*;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Properties;

public class SSHExecutor {

	public Session session;
	public Channel channel;

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
			System.out.println("SSH Connection...");
			session.connect();
			System.out.println("Connected to the ssh.");
			// Execution end time
			long lEndTime = new Date().getTime();
			System.out.println("---------------------------------------------");
			System.out.println("SSH connection in : " + (lEndTime - lStartTime));

		} catch (JSchException e) {
			System.out.println("Failed to connect");
			e.printStackTrace();
		}
	}

	public void disconnectSession() {
		session.disconnect();
	}

	public void disconnectChannel() {
		channel.disconnect();
	}

	public void executeCommand(String cmd, String exitCondition) {
		StringBuilder outputBuffer = new StringBuilder();
		StringBuilder errorBuffer = new StringBuilder();
		try {
			channel = session.openChannel("exec");
			channel.setXForwarding(true);
			
			((ChannelExec) channel).setCommand(cmd);
			InputStream in = channel.getInputStream();
			InputStream err = channel.getExtInputStream();
			
			channel.connect();
			String lastString = "";
			byte[] tmp = new byte[1024];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					lastString = new String(tmp, 0, i);
					System.out.println(lastString);
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
					System.out.println("exit-status: " + channel.getExitStatus());
					break;
				}
				if (exitCondition != null) {
					if (lastString.contains(exitCondition)) {
						System.out.println("### EXIT CONDITION ###");
						break;
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

	public void shellCommand(InputStream input) {
		try {
			channel = session.openChannel("shell");
			channel.setXForwarding(true);
			
			channel.setInputStream(input);
			channel.setOutputStream(System.out);
			channel.setXForwarding(true);
			channel.connect();
			while (true) {
				try {
					Thread.sleep(1000);
				} catch (Exception ee) {
				}
			}
		} catch (Exception e) {
			System.out.println(e);
			channel.disconnect();
			session.disconnect();
		}
	}

}
