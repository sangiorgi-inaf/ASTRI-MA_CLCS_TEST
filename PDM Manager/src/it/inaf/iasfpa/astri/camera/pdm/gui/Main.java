package it.inaf.iasfpa.astri.camera.pdm.gui;
	
import java.io.File;
import java.util.Arrays;
import java.util.Scanner;

import it.inaf.iasfpa.astri.camera.pdm.AsicTableData;
import it.inaf.iasfpa.astri.camera.pdm.Pdm;
import it.inaf.iasfpa.astri.camera.pdm.PdmConstants;
import it.inaf.iasfpa.astri.camera.pdm.PdmControl;
import it.inaf.iasfpa.astri.camera.pdm.PdmUtils;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.fxml.FXMLLoader;

public class Main extends Application {
	
	Pdm pdm = new Pdm();
	private byte[] asicTable = new byte[286];
	
	private AsicTableData asicTableData = new AsicTableData();
		
	@Override
	public void start(Stage primaryStage) {
		try {			
			AnchorPane root = (AnchorPane)FXMLLoader.load(getClass().getResource("PDMGui.fxml"));
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setTitle("ASTRI PDM Controller");
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	} 
	
}
