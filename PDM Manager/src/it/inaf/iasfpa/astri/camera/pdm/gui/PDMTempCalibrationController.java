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
import java.util.Locale;
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
import it.inaf.iasfpa.astri.camera.pdm.data.HkData;
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

public class PDMTempCalibrationController implements Initializable {

	@FXML
	ComboBox comCombo;

	@FXML
	TextField pdmIdText, tRef1Text, samples1Text, out1FileText, tRef2Text, samples2Text, out2FileText;
	
	Pdm pdm = new Pdm();
	
	byte pdmId = 0;	
	
	double t1Ref, t2Ref;
	String fileName1, fileName2;

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
	private void temperatureCalibration() {
		double[] m_def = new double[] { 6.4500, 6.4500, 6.4500, 6.4500, 6.4500, 6.4500, 6.4500, 6.4500, 6.4500 };
		double[] qb = new double[] { 514.4616, 514.4616, 514.4616, 514.4616, 514.4616, 514.4616, 514.4616, 514.4616,
				514.4616 };

		pdmId = Byte.valueOf(pdmIdText.getText());
		int nSample1 = Integer.valueOf(samples1Text.getText());
		t1Ref = Double.valueOf(tRef1Text.getText());
		
		fileName1 = "./output/TEMP_CALIB/PDM_" + pdmId + "_temperature_sensors.txt";
		out1FileText.setText(fileName1);

		boolean[] calibrated = new boolean[9];
		double[][] temperature = new double[100][9];

		double[] tMedia = new double[9];

		double start = System.currentTimeMillis();

		while (!areAllTrue(calibrated)) {
			for (int k = 0; k < nSample1; k++) {
				try {
					byte[] resp = pdm.requestData(pdmId, PdmConstants.ARG_HK_MODULE);
					int[] adcValues = PdmUtils.convertHkData(resp);
					HkData hkData = PdmUtils.parseHkData((byte) pdmId, adcValues, m_def, qb);
					temperature[k] = hkData.getTemperature();
					// Thread.sleep(25);
				} catch (Exception e) {
					System.out.println("PERFORM HK: FAILURE\n");
					e.printStackTrace();
				}
			}

			// faccio la media dei campioni
			for (int i = 0; i < 9; i++) {
				double somma = 0.0;
				for (int k = 0; k < nSample1; k++) {
					somma += temperature[k][i];
					tMedia[i] = somma / nSample1;
				}

				if (!calibrated[i]) {
					if ((tMedia[i] > (t1Ref + 0.05)) || (tMedia[i] < (t1Ref - 0.05))) {
						double delta = (t1Ref - tMedia[i]);

						System.out.println("DELTA[" + i + "]= " + delta);

						double mu = 0;
						if ((Math.abs(delta) >= 0.0) & (Math.abs(delta) <= 0.1))
							mu = 0.05;
						else if ((Math.abs(delta) > 0.1) & (Math.abs(delta) <= 0.5))
							mu = 2;
						else if ((Math.abs(delta) > 0.5) & (Math.abs(delta) <= 1))
							mu = 3;
						else if ((Math.abs(delta) > 1) & (Math.abs(delta) <= 2))
							mu = 4;
						else if (Math.abs(delta) > 2)
							mu = 5;

						System.out.println("MU[" + i + "]= " + mu);

						if (tMedia[i] >= (t1Ref + 0.05))
							qb[i] = qb[i] + mu * Math.abs(delta);
						if (tMedia[i] < (t1Ref - 0.05))
							qb[i] = qb[i] - mu * Math.abs(delta);
					} else {
						calibrated[i] = true;
						System.out.println("QB[" + i + "] calibrated = " + qb[i]);
					}
				}
			}
		}

		double stop = System.currentTimeMillis();

		System.out.println("ELAPSED TIME = " + (stop - start) / 1000);

		// file di output
		try {
			File file = new File(fileName1);
			PrintWriter out = new PrintWriter(new FileWriter(file), true);
			// intestazione del file
			String header = "PDM_" + pdmId + "\t" + "m" + "\t" + "q";
			out.println(header);

			for (int i = 0; i < 9; i++) {
				out.println(i + 1 + "\t" + m_def[i] + "\t" + qb[i]);
			}
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void temperatureCalibration2() {
		double[] sipm_M = new double[9];
		double[] sipm_Q = new double[9];

		pdmId = Byte.valueOf(pdmIdText.getText());
		int nSample2 = Integer.valueOf(samples2Text.getText());
		t2Ref = Double.valueOf(tRef2Text.getText());
		
		fileName2 = "./output/TEMP_CALIB/PDM_" + pdmId + "_@_" + t1Ref + "_" + t2Ref + ".txt";
		out2FileText.setText(fileName2);

		try {
			// apro file con q temporaneo
			Scanner scan = new Scanner(new File(fileName1));
			int j = 0;
			while (scan.hasNextLine()) {
				String line = scan.nextLine();
				if (line.contains("PDM_")) {
					// salta, prima riga
				} else {
					String[] values = line.split("\t");
					sipm_M[j] = Double.valueOf(values[1]);
					sipm_Q[j] = Double.valueOf(values[2]);
					j++;
				}
			}
			scan.close();

			// file di output
			File file = new File(fileName2);
			PrintWriter out = new PrintWriter(new FileWriter(file), true);

			// intestazione del file
			String header = "TSetPoint ";
			for (int i = 1; i < 20; i++) {
				header += "ADC" + i + " ";
			}
			for (int i = 1; i < 20; i++) {
				header += "T" + i + " ";
			}
			out.println(header);

			for (int k = 0; k < nSample2; k++) {
				byte[] resp = pdm.requestData(pdmId, PdmConstants.ARG_HK_MODULE);
				int[] adcValues = PdmUtils.convertHkData(resp);
				HkData hkData = PdmUtils.parseHkData((byte) pdmId, adcValues, sipm_M, sipm_Q);

				String output = t2Ref + " ";
				for (int i = 0; i < adcValues.length; i++) {
					output += adcValues[i] + " ";
				}

				double[] sipmTemp = hkData.getTemperature();
				for (int i = 0; i < sipmTemp.length; i++) {
					output += String.format(Locale.US, "%.2f", sipmTemp[i]) + " ";
				}
				output += String.format(Locale.US, "%.2f", hkData.getCitirocTemp()) + " ";
				output += String.format(Locale.US, "%.2f", hkData.getCitirocCurr()[0]) + " ";
				output += String.format(Locale.US, "%.2f", hkData.getCitirocCurr()[1]) + " ";
				output += String.format(Locale.US, "%.2f", hkData.getCitirocCurr()[2]) + " ";
				output += "0.00 ";
				output += "0.00 ";
				output += String.format(Locale.US, "%.2f", hkData.getFpgaTemp()) + " ";
				output += "0.00 ";
				output += String.format(Locale.US, "%.2f", hkData.getFpgaCurr()) + " ";

				out.println(output);

				// Thread.sleep(25);
			}
			out.close();
		} catch (Exception e) {
			System.out.println("PERFORM HK: FAILURE\n");
			e.printStackTrace();
		}
	}
	
	private boolean areAllTrue(boolean[] array) {
		for (boolean b : array)
			if (!b)
				return false;
		return true;
	}

}
