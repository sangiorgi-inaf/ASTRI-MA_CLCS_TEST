package it.inaf.iasfpa.astri.camera.launcher;

import java.io.ByteArrayInputStream;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Date;
import java.util.Properties;
import java.util.ResourceBundle;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;

/**
 * @author P. Sangiorgi, IASFPA, sangiorgi@ifc.inaf.it
 *
 */
public class LauncherGuiController implements Initializable {

	@FXML
	private TextArea serverShellText, clientShellText;

	public Session session;
	public Channel channel;

	private PrintStream ps;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		ps = new PrintStream(new Console(serverShellText));
		System.setOut(ps);
		System.setErr(ps);
	}

	@FXML
	private void startServer() {

	}

	@FXML
	private void stopServer() {

	}

	@FXML
	private void startClient() {
	}

	public class Console extends OutputStream {
		private TextArea console;

		public Console(TextArea console) {
			this.console = console;
		}

		public void appendText(String valueOf) {
			Platform.runLater(() -> console.appendText(valueOf));
		}

		public void write(int b) throws IOException {
			appendText(String.valueOf((char) b));
		}
	}

}
