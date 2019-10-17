package it.inaf.iasfpa.astri.camera.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ResourceBundle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;

/**
 * @author P. Sangiorgi, IASFPA, sangiorgi@ifc.inaf.it
 *
 */
public class LauncherGuiController implements Initializable {

	@FXML
	private TextArea serverShellText, clientShellText;

	@FXML
	private Button startServerButton, stopServerButton, startClientButton;

	@FXML
	private ComboBox<String> serverNameComboBox, clientNameComboBox;

	@FXML
	private CheckBox aClientCheckBox;

	private TerminalConfigurations tC;
//	private PrintStream ps;
	private ServerConfiguration currentServer;
	private ClientConfiguration currentClient;
	private ObservableList<ServerConfiguration> serverList = FXCollections.observableArrayList();
	private ObservableList<ClientConfiguration> clientList = FXCollections.observableArrayList();
	private boolean serverConnected, clientConnected = false;
	private SSHExecutor sshExServer;
	private SSHExecutor sshExClient;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

		this.startServerButton.setDisable(false);
		this.stopServerButton.setDisable(true);
		this.startClientButton.setDisable(true);

		File jsonfile = new File("./terminalconf.json");
		Gson gs = new GsonBuilder().setPrettyPrinting().create();
		JsonReader reader;
		try {
			reader = new JsonReader(new FileReader(jsonfile));
			tC = gs.fromJson(reader, TerminalConfigurations.class);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		serverList.addAll(tC.getServer());
		clientList.addAll(tC.getClient());
		ObservableList<String> serverNameList = FXCollections.observableArrayList();
		ObservableList<String> clientNameList = FXCollections.observableArrayList();

		for (int i = 0; i < serverList.size(); i++) {
			serverNameList.add(serverList.get(i).getName());
		}

		for (int i = 0; i < serverList.size(); i++) {
			clientNameList.add(clientList.get(i).getName());
		}
		this.serverNameComboBox.setItems(serverNameList);
		this.clientNameComboBox.setItems(clientNameList);

		aClientCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (aClientCheckBox.isSelected()) {
					startClientButton.setDisable(true);
				} else {
					startClientButton.setDisable(false);
				}
			}
		});
        
		this.sshExServer = new SSHExecutor(serverShellText);
		this.sshExClient = new SSHExecutor(clientShellText);
//		ps = new PrintStream(new Console(serverShellText));
//		System.setOut(ps);
//		System.setErr(ps);
	}

	@FXML
	private void startServer() {
		sshExServer.stopTrue = true;
		serverShellText.clear();
		Task<Void> taskStartServer = new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				if(!serverConnected) {
					sshExServer.connect(currentServer.getIpAddress(), 22, currentServer.getUsr(), currentServer.getPwd());
					serverConnected = true;
				}
				String controlCmd = "ps aux|grep java";
				sshExServer.executeCommand(controlCmd, null);
				int c = serverShellText.getText().indexOf("astri_camera_control_server");
				if (c > 0) {
					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							Alert alert = new Alert(AlertType.INFORMATION);
							alert.setTitle("Information Dialog");
							alert.setHeaderText(null);
							alert.setContentText("Server already starts");
							alert.show();
						}
					});
					sshExServer.disconnectChannel();
//					sshEx.disconnectSession();
					startServerButton.setDisable(true);
					stopServerButton.setDisable(false);
					startClientButton.setDisable(false);

				} else {
					String cmd = "cd /opt/Astri_Camera_Sim; bash -c \"exec -a astri_camera_control_server_sh ./start_bee_server.sh\"";
					String exitCondition = "- Enter x to close the server";
					try {
						stopServerButton.setDisable(false);
						startClientButton.setDisable(false);
						startServerButton.setDisable(true);
						sshExServer.executeCommand(cmd, exitCondition);
//						sshEx.disconnectChannel();
//						sshEx.disconnectSession();

					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}
				return null;
			}

		};

		new Thread(taskStartServer).start();

	}

	@FXML
	private void stopServer() {
		
		sshExServer.stopTrue = true;
		sshExClient.stopTrue = true;
		Task<Void> taskStopServer = new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				
				if(!clientConnected) {
					sshExClient.connect(currentClient.getIpAddress(), 22, currentClient.getUsr(), currentClient.getPwd());
					clientConnected = true;
				}
				clientShellText.clear();
				String controlCmd = "ps aux|grep java";
				sshExClient.executeCommand(controlCmd, null);
				int c = clientShellText.getText().indexOf("astri_camera_control_client");
				if (c > 0) {
					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							Alert alert = new Alert(AlertType.INFORMATION);
							alert.setTitle("Information Dialog");
							alert.setHeaderText(null);
							alert.setContentText("Client still started, close the client before stopping the server");
							alert.show();
						}
					});
					sshExClient.disconnectChannel();
//					sshEx.disconnectSession();
				} else {
					if(!serverConnected) {
						sshExServer.connect(currentServer.getIpAddress(), 22, currentServer.getUsr(), currentServer.getPwd());
						serverConnected = true;
					}
					String cmd1 = "pkill -9 -f astri_camera_control_server_sh";
					String cmd2 = "pkill -9 -f astri_camera_control_server";
					try {

						sshExServer.executeCommand(cmd1, null);
						sshExServer.executeCommand(cmd2, null);
						sshExServer.disconnectChannel();
						sshExServer.disconnectSession();
						serverConnected = false;
						serverShellText.appendText(("Server stopped.\n"));
						startServerButton.setDisable(false);
					} catch (Exception e) {
						e.printStackTrace();
					}

					
				}

			
			return null;

		}};

		new Thread(taskStopServer).start();

	}

	@FXML
	private void startClient() {
		if(!clientConnected) {
			sshExClient.connect(currentClient.getIpAddress(), 22, currentClient.getUsr(), currentClient.getPwd());
			clientConnected = true;
			String controlCmd = "command";
			sshExClient.executeCommand(controlCmd, null);
		}
		
	}

	@FXML
	private void comboServerChangeEvent(ActionEvent event) {
		currentServer = serverList.get(this.serverNameComboBox.getSelectionModel().getSelectedIndex());

	}

	@FXML
	private void comboClientChangeEvent(ActionEvent event) {
		currentClient = clientList.get(this.clientNameComboBox.getSelectionModel().getSelectedIndex());

	}

//	public class Console extends OutputStream {
//		private TextArea console;
//
//		public Console(TextArea console) {
//			this.console = console;
//		}
//
//		public void appendText(String valueOf) {
//			Platform.runLater(() -> console.appendText(valueOf));
//		}
//
//		public void write(int b) throws IOException {
//			appendText(String.valueOf((char) b));
//		}
//	}
//
}
