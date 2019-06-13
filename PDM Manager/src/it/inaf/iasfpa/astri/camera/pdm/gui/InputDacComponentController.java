package it.inaf.iasfpa.astri.camera.pdm.gui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Scanner;

import it.inaf.iasfpa.astri.camera.pdm.AsicTableData;
import it.inaf.iasfpa.astri.camera.pdm.FpgaTableData;
import it.inaf.iasfpa.astri.camera.pdm.Pdm;
import it.inaf.iasfpa.astri.camera.pdm.SwitchTableData;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class InputDacComponentController implements Initializable {
	
	@FXML
	private TextField valueText;
	
	@FXML
	private Label chLabel;
	
	@FXML
	private CheckBox enableCheck;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
	}

	public void setChNumber(int value) {
		chLabel.setText("Ch "+ String.format("%2s", value).replace(' ', '0'));
	}

	public void setDacValue(short value) {
		valueText.setText(""+value);
	}
	
	public void setDatFlag(boolean enabled) {
		enableCheck.setSelected(enabled);
	}
	
	public short getDacValue() {
		return Short.parseShort(valueText.getText());
	}
	
	public boolean getDacFlag() {
		return enableCheck.isSelected();
	}
	
}
