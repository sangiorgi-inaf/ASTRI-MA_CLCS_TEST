package it.inaf.iasfpa.astri.camera.launcher;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

public class Main extends Application {

	private FXMLLoader loader = new FXMLLoader();

	private SSHExecutor sshEx = new SSHExecutor();

	@Override
	public void start(Stage primaryStage) {
		try {
			AnchorPane root = (AnchorPane) loader.load(getClass().getResource("LauncherGui.fxml").openStream());
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setTitle("ASTRI Camera Launcher");

			primaryStage.show();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void provaServerExec() {
		sshEx.connect("10.10.2.164", 22, "root", "none");
		String cmd = "cd /opt/Astri_Camera_Sim; ./start_bee_server.sh";
		String exitCondition = "- Enter x to close the server";

		sshEx.executeCommand(cmd, exitCondition);
		sshEx.disconnectChannel();
		sshEx.disconnectSession();
	}

	public void provaClientShell() {
		sshEx.connect("10.10.1.150", 22, "ccuser", "CC1cam01");
		try {
			String temporaryShellFileName = "shellscript.sh";
			File fileStream = new File(temporaryShellFileName);
			PrintStream outStream = new PrintStream(new FileOutputStream(fileStream));
			outStream.println("cd /home/ccuser/ASTRI_Viewer");
			outStream.println("./start.sh");
			outStream.close();
			FileInputStream fin = new FileInputStream(fileStream);
			byte fileContent[] = new byte[(int) fileStream.length()];
			fin.read(fileContent);
			InputStream input = new ByteArrayInputStream(fileContent);
			
			sshEx.shellCommand(input);
			sshEx.disconnectChannel();
			sshEx.disconnectSession();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Main main = new Main();
		main.provaServerExec();
//		main.provaClientShell();
		
		launch(args);

	}
}
