package it.inaf.iasfpa.astri.camera.pdm.gui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.TreeMap;

import gnu.io.CommPortIdentifier;
import it.inaf.iasfpa.astri.camera.pdm.AsicProbeTableData;
import it.inaf.iasfpa.astri.camera.pdm.AsicTableData;
import it.inaf.iasfpa.astri.camera.pdm.FpgaTableData;
import it.inaf.iasfpa.astri.camera.pdm.Pdm;
import it.inaf.iasfpa.astri.camera.pdm.PdmConstants;
import it.inaf.iasfpa.astri.camera.pdm.PdmUtils;
import it.inaf.iasfpa.astri.camera.pdm.SwitchTableData;
import it.inaf.iasfpa.astri.camera.utils.PrintUtils;
import it.inaf.iasfpa.astri.camera.pdm.AsicProbeTableData.AnalogModule;
import it.inaf.iasfpa.astri.camera.pdm.AsicProbeTableData.DigitalModule;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;

public class PDMFlasherController implements Initializable {

	final FileChooser fileChooser = new FileChooser();

	@FXML
	ComboBox comCombo, bootCombo;

	@FXML
	TextField fileText, fileInfoText, sectorText, pageText, pdmIdText;
	
	@FXML
	TextArea resultText;
	
	@FXML
	CheckBox readbackCheck;
	
	Pdm pdm = new Pdm();
	byte[] flashData;
	
	int pagesNum, sectorsNum;
	
	byte pdmId = 0;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		scanPort();
	}
		
	@FXML
	public void scanPort() {
		comCombo.getItems().clear();
		// scan delle porte
		Enumeration ports = CommPortIdentifier.getPortIdentifiers();
		while (ports.hasMoreElements()) {
			CommPortIdentifier curPort = (CommPortIdentifier) ports.nextElement();
			// get only serial ports --> check se vuole veramente serial o rs485
			// come sarebbe giusto
			if (curPort.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				if (curPort.getCurrentOwner() == null) {
					comCombo.getItems().add(curPort.getName());
				}
			}
		}
	}

	@FXML
	public void connect() {
		String port = comCombo.getSelectionModel().getSelectedItem().toString();
		boolean result = pdm.connect(port);
		if (result) {
			System.out.println("Connection OK");
		} else
			System.out.println("Connection Failed");
	}
	
	@FXML
	public void disconnect() {
		pdm.releaseDevice();
	}

	@FXML
	private void openFile() {
		File file = fileChooser.showOpenDialog(null);
		if (file != null) {
			flashData = loadFlashFile(file);
			
			pagesNum = flashData.length / 256;
			if ((flashData.length % 256) != 0)
				pagesNum++;
			
			sectorsNum = pagesNum / 256;
			if ((pagesNum % 256) != 0)
				sectorsNum++;
			
			fileText.setText(file.getAbsolutePath());
			fileInfoText.setText("File size = " + flashData.length + " bytes. " + sectorsNum + " sectors for " + pagesNum + " pages of 256 bytes needed.");
		}
	}
	
	@FXML
	private void flash() {
		boolean readBackControl = readbackCheck.isSelected();
		pdmId = Byte.parseByte(pdmIdText.getText());
		int sector = Integer.valueOf(sectorText.getText());
		int page = Integer.valueOf(pageText.getText());
		
		boolean flashResult = true;
		
		for (int i = 0; i < sectorsNum; i++) {
			if (flashResult) {
				for (int j = 0; j < 256; j++) {
					if (flashResult) {
						if ((i*256) + (j) < pagesNum) {
		//					System.out.println("write page " + j + " in sector " + sector);
							int startBytes = ((i*256) + (j))*256;
							int stopBytes = startBytes+256;
							if (stopBytes >= flashData.length)
								stopBytes = flashData.length;
			//				System.out.println("from byte " + startBytes + " to byte " + stopBytes);
							boolean result;
							//set settore e pagina
							byte[] address = new byte[38];
							address[0] = (byte) ((sector >> 8) & 0xFF);
							address[1] = (byte) (sector & 0xFF);
							address[2] = (byte) j;
							
							try {
								result = pdm.sendTable(pdmId, PdmConstants.ARG_FPGA_TABLE, address);
								flashResult &= result;
							} catch (Exception e) {
								e.printStackTrace();
								result = false;
								flashResult &= result;
							}
							resultText.appendText("Set Sector " + sector + " - Page " + j + " result: " + result + "\n");
							
							//send data
							byte[] sendData = Arrays.copyOfRange(flashData, startBytes, stopBytes);
							
							if (sendData.length < 256) {
								byte[] data2 = new byte[256];
								System.arraycopy(sendData, 0, data2, 0, sendData.length);
								sendData = data2;
							}
							
							try {
								result = pdm.sendTable(pdmId, PdmConstants.ARG_ASIC_TABLE, sendData);
								flashResult &= result;
							} catch (Exception e) {
								e.printStackTrace();
								result = false;
								flashResult &= result;
							}
							resultText.appendText("Send data to Sector " + sector + " - Page " + j + " result: " + result + "\n");
							
							if (readBackControl) {
								try {
									byte[] readData = pdm.requestData(pdmId, PdmConstants.ARG_ACQUIRE_MODULE);

									if (!Arrays.equals(sendData, readData)) {
										System.out.println("READBACK CONTROL MISMATCH");
										flashResult = false;
									}
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							
						}
					} else {								
						System.out.println("FLASH FAILED!");
						break;
					}
				}
				sector++;
			} else {
				System.out.println("FLASH FAILED!");
				break;
			}
		}
		
		if (flashResult) {
			System.out.println("FLASH COMPLETED");
			Alert alert = new Alert(AlertType.INFORMATION, "FLASH COMPLETED", ButtonType.OK);
			alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
			alert.show();
		} else {
			Alert alert = new Alert(AlertType.ERROR, "FLASH FAILED", ButtonType.OK);
			alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
			alert.show();
		}
		
	}
	
	@FXML
	private void load() {
		pdmId = Byte.parseByte(pdmIdText.getText());
		int sector = Integer.valueOf(sectorText.getText());
		int page = Integer.valueOf(pageText.getText());

		//set settore e pagina
		byte[] address = new byte[38];
		address[0] = (byte) ((sector >> 8) & 0xFF);
		address[1] = (byte) (sector & 0xFF);
		address[2] = (byte) page;
		try {
			boolean result = pdm.sendTable(pdmId, PdmConstants.ARG_FPGA_TABLE, address);
			Platform.runLater(() ->resultText.appendText("Set Sector " + sector + " - Page " + page + " result: " + result + "\n"));
		} catch (Exception e) {
			e.printStackTrace();
			Platform.runLater(() ->resultText.appendText("Set Sector " + sector + " - Page " + page + " result: false \n"));

		}
		//request data					
		byte[] resp;
		try {
			resp = pdm.requestData(pdmId, PdmConstants.ARG_ACQUIRE_MODULE);
			resultText.appendText("Data read in sector " + sector + " page " + page + ":\n");
			Platform.runLater(() -> resultText.appendText(PrintUtils.byteArrayToHexString(resp)));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	@FXML
	private void selectBoot() {
		pdmId = Byte.parseByte(pdmIdText.getText());
		byte boot = (byte) bootCombo.getSelectionModel().getSelectedItem();
		boolean result;
		try {
			result = pdm.setBoot(pdmId, boot);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
		}
		resultText.appendText("Select boot number " + boot + " result:" + result + "\n");
	}

	private byte[] loadFlashFile(File file) {
		try {
			Path path = file.toPath();
		    return Files.readAllBytes(path);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


}
