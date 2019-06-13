package it.inaf.iasfpa.astri.camera.pdm.gui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;

import javafx.stage.FileChooser;

public class PDMGuiController implements Initializable {

//	int acqFileIndex, varFileIndex, trigFileIndex, hkFileIndex;
	int c11_12_FileIndex, c13_14_FileIndex;

	final FileChooser fileChooser = new FileChooser();

	@FXML
	ComboBox comCombo;

	@FXML
	GridPane dacInputGrid, triggerMaskGrid;

	@FXML
	TextArea outText;

	@FXML
	LineChart<Number, Number> outChart;

	// FPGA TABLE
	@FXML
	TextField pdmIdFpgaText, hkPeriodText, majorityText, timeWndText, timeWndCalcText, acq_validText, acq_muxText, acq_convText,
			var_delayText, acq_cyclesText, data_offsText;

	@FXML
	CheckBox hk_enCheck, verify_asicCheck, stand_aloneCheck, clkExtCheck, rstb_pscCheck, ps_globalCheck, raz_pCheck,
			val_evtCheck, reset_paCheck, filter_outCheck, acq_holdCheck;

	// ASIC TABLE
	@FXML
	TextField thresholdText;
	@FXML
	CheckBox shaperCheck, pdCheck, lgPreampCheck;
	@FXML
	ComboBox shapingCombo;

	// PROBE TABLE
	@FXML
	TextField probeAnChText, probeDigChText;
	@FXML
	ComboBox probeAnModCombo, probeDigModCombo;

	// SWITCH TABLE
	@FXML
	CheckBox acq_enCheck, acq_var_enCheck, acq_enableCheck, acq_clearCheck;
	@FXML
	ComboBox trig_selCombo, wind_monCombo;

	// COMMANDS
	@FXML
	TextField pdmIdText;

	@FXML
	ComboBox clkCombo, moduleResetCombo, tableCombo, tableLoadCombo, dataCombo, moduleRequestCombo;

	// COMBINED COMMANDS
	@FXML
	TextField startThreshText, stopThreshText, deltaThreshText, c13_14ThreshText, c13_14IterText;
	
	

	InputDacComponentController[] dacCtrl = new InputDacComponentController[64];

	ToggleButton[] tb = new ToggleButton[64];

	Pdm pdm = new Pdm();
	private byte[] asicTable = new byte[286];
	private byte[] asicProbeTable = new byte[56];
	private byte[] switchTable = new byte[2];
	private byte[] fpgaTable = new byte[38];

	AsicTableData asicTableData = new AsicTableData();
	AsicProbeTableData probeTableData = new AsicProbeTableData();
	FpgaTableData fpgaTableData = new FpgaTableData();
	SwitchTableData switchTableData = new SwitchTableData();

	byte pdmId = 0;
	
	boolean c13_14Flag = false;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		scanPort();
		
		//fill griglia dei dac
		try {                        
			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 16; j++) {
					FXMLLoader loader = new FXMLLoader(getClass().getResource("InputDacComponent.fxml"));
					dacInputGrid.add((Node) loader.load(), i, j);
					dacCtrl[j+(i*16)] = loader.getController();
					dacCtrl[j+(i*16)].setChNumber((j)+(i*16));
				}
			}
		} catch (IOException e){
		    e.printStackTrace();
		}
		
		int k = 0;
		//fill griglia dei pixel mask              
		for (int i = 7; i >= 0; i--) {
			for (int j = 0; j < 8; j++) {
				tb[k] = new ToggleButton();
//				tb[k].setText(""+(((7-i)*8)+j+1)); //testo == num pxl
				tb[k].setText(""+PdmUtils.CH_FROM_PIXEL[(((7-i)*8)+j+1)]); //testo == num ch
				tb[k].setMinSize(30, 30);
				tb[k].setMaxSize(30, 30);
				tb[k].setStyle("-fx-font-size: 12; -fx-base: red;");
				triggerMaskGrid.add(tb[k], j, i);
				tb[k].setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						
//						int sel_pxl = Integer.valueOf(((ToggleButton) (event.getSource())).getText());
//						int sel_ch = PdmConstants.CH_FROM_PIXEL[sel_pxl];
//						System.out.println("selected pixel: " + sel_pxl);
//						System.out.println("selected channel: " + sel_ch);
						
						if (((ToggleButton) (event.getSource())).isSelected()) {
							((ToggleButton) (event.getSource())).setStyle("-fx-font-size: 12; -fx-base: green;");
						} else {
							((ToggleButton) (event.getSource())).setStyle("-fx-font-size: 12; -fx-base: red;");
						}
					}
				});
				k++;
			}
		}
		
		timeWndText.textProperty().addListener(new ChangeListener<String>() {
		    @Override
		    public void changed(final ObservableValue<? extends String> observable, final String oldValue, final String newValue) {
				int tw = Integer.valueOf(timeWndText.getText());
				int time = PdmUtils.convertTimeWnd(tw);
				timeWndCalcText.setText(String.valueOf(time));
		    }
		});
		
		openDefaultConfig();
	
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
	private void openAsicTable() {
		File file = fileChooser.showOpenDialog(null);
		if (file != null) {
			loadAsicTable(file);
		}
	}
	
	private void test() {
		try {
			byte[] asicTab = new byte[286];

			File f = new File("./tables/asic_config.txt");
			Scanner scan = new Scanner(f);
			int i = 0;
			while (scan.hasNextLine()) {
				asicTab[i] = (byte) Short.parseShort(scan.nextLine(), 16);
				i++;
			}
			System.out.println("old asic: " + Arrays.toString(asicTab));
			AsicTableData asicDat = pdm.parseAsicTable(asicTab);
			System.out.println("old thresh = " + asicDat.getThreshold());

			asicDat.setThreshold((short) 250);
			asicTab = pdm.createAsicTable(asicDat);
			System.out.println("new asic: " + Arrays.toString(asicTab));
		} catch (Exception e) {
			
		}
		
	}
	

	private void loadAsicTable(File file) {
		try {
			Scanner scan = new Scanner(file);
			int i = 0;
			while (scan.hasNextLine()) {
				asicTable[i] = (byte) Short.parseShort(scan.nextLine(), 16);
				i++;
			}
			asicTableData = pdm.parseAsicTable(asicTable);

			thresholdText.setText("" + asicTableData.getThreshold());
			shaperCheck.setSelected(asicTableData.isSlowShaper());
			shapingCombo.getSelectionModel().select(asicTableData.getShapingTime());
			pdCheck.setSelected(asicTableData.isPeakDetector());

			for (int j = 0; j < 64; j++) {
				dacCtrl[j].setDatFlag(asicTableData.getDacInputFlag()[j]);
				dacCtrl[j].setDacValue(asicTableData.getDacInputValue()[j]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void saveAsicTable() {
		try {
			File file = fileChooser.showSaveDialog(null);
			if (file != null) {
				PrintWriter out = new PrintWriter(file);
				createAsicTable();
				for (int i = 0; i < asicTable.length; i++) {
					String thisByte = String.format("%02x", asicTable[i]).toUpperCase();
					out.println(thisByte);
				}
				out.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void createAsicTable() {
		asicTableData.setThreshold(Short.parseShort(thresholdText.getText()));
		asicTableData.setSlowShaper(shaperCheck.isSelected());
		asicTableData.setShapingTime((byte) shapingCombo.getSelectionModel().getSelectedIndex());
		asicTableData.setPeakDetector(pdCheck.isSelected());

		boolean[] flags = new boolean[64];
		short[] values = new short[64];
		for (int i = 0; i < 64; i++) {
			values[i] = dacCtrl[i].getDacValue();
			flags[i] = dacCtrl[i].getDacFlag();
		}
		asicTableData.setDacInputFlag(flags);
		asicTableData.setDacInputValue(values);

		asicTable = pdm.createAsicTable(asicTableData);
	}

	@FXML
	private void createProbeTable() {
//		probeTableData.setAnalogCh(Byte.valueOf(probeAnChText.getText()));
//		probeTableData.setDigitalCh(Byte.valueOf(probeDigChText.getText()));
//
//		String anModule = probeAnModCombo.getSelectionModel().getSelectedItem().toString();
//		String digModule = probeDigModCombo.getSelectionModel().getSelectedItem().toString();
//
//		switch (anModule) {
//		case "Out_fs":
//			probeTableData.setAnalogMod(AnalogModule.Out_fs);
//			break;
//		case "Out_ssh_LG":
//			probeTableData.setAnalogMod(AnalogModule.Out_ssh_LG);
//			break;
//		case "Out_ssh_HG":
//			probeTableData.setAnalogMod(AnalogModule.Out_ssh_HG);
//			break;
//		case "Out_PA_LG":
//			probeTableData.setAnalogMod(AnalogModule.Out_PA_LG);
//			break;
//		case "Out_PA_HG":
//			probeTableData.setAnalogMod(AnalogModule.Out_PA_HG);
//			break;
//		default:
//			break;
//		}
//
//		switch (digModule) {
//		case "PeakSensing_modeb_LG":
//			probeTableData.setDigitalMod(DigitalModule.PeakSensing_modeb_LG);
//			break;
//		case "PeakSensing_modeb_HG":
//			probeTableData.setDigitalMod(DigitalModule.PeakSensing_modeb_HG);
//			break;
//		default:
//			break;
//		}

		asicProbeTable = pdm.createAsicProbeTable(probeTableData);
	}

	@FXML
	private void saveProbeTable() {
		try {
			File file = fileChooser.showSaveDialog(null);
			if (file != null) {
				PrintWriter out = new PrintWriter(file);
				createProbeTable();
				for (int i = 0; i < asicProbeTable.length; i++) {
					String thisByte = String.format("%02x", asicProbeTable[i]).toUpperCase();
					out.println(thisByte);
				}
				out.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void openProbeTable() {
		File file = fileChooser.showOpenDialog(null);
		if (file != null) {
			loadProbeTable(file);
		}
	}

	private void loadProbeTable(File file) {
		try {
			Scanner scan = new Scanner(file);
			int i = 0;
			while (scan.hasNextLine()) {
				asicProbeTable[i] = (byte) Short.parseShort(scan.nextLine(), 16);
				i++;
			}
			probeTableData = pdm.parseAsicProbeTable(asicProbeTable);

			probeAnChText.setText(String.valueOf(probeTableData.getAnalogCh()));
			probeDigChText.setText(String.valueOf(probeTableData.getDigitalCh()));

			switch (probeTableData.getAnalogMod()) {
			case Out_fs:
				probeAnModCombo.getSelectionModel().select(AnalogModule.Out_fs.toString());
				break;
			case Out_ssh_LG:
				probeAnModCombo.getSelectionModel().select(AnalogModule.Out_ssh_LG.toString());
				break;
			case Out_ssh_HG:
				probeAnModCombo.getSelectionModel().select(AnalogModule.Out_ssh_HG.toString());
				break;
			case Out_PA_LG:
				probeAnModCombo.getSelectionModel().select(AnalogModule.Out_PA_LG.toString());
				break;
			case Out_PA_HG:
				probeAnModCombo.getSelectionModel().select(AnalogModule.Out_PA_HG.toString());
				break;
			default:
				break;
			}

			switch (probeTableData.getDigitalMod()) {
			case PeakSensing_modeb_LG:
				probeAnModCombo.getSelectionModel().select(DigitalModule.PeakSensing_modeb_LG.toString());
				break;
			case PeakSensing_modeb_HG:
				probeAnModCombo.getSelectionModel().select(DigitalModule.PeakSensing_modeb_HG.toString());
				break;
			default:
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void createFpgaTable() {
		fpgaTableData.setPdmId(Byte.valueOf(pdmIdFpgaText.getText()));
		fpgaTableData.setHk_en(hk_enCheck.isSelected());
		fpgaTableData.setAsic_table_verify(verify_asicCheck.isSelected());
		fpgaTableData.setStand_alone(stand_aloneCheck.isSelected());
		// fpgaTableData.setHk_samp_period(Byte.valueOf(hkPeriodText.getText()));
		// fpgaTableData.setExternalClock(clkExtCheck.isSelected());
		fpgaTableData.setRstb_psc(rstb_pscCheck.isSelected());
		fpgaTableData.setPs_global_trig(ps_globalCheck.isSelected());
		fpgaTableData.setRaz_p(raz_pCheck.isSelected());
		fpgaTableData.setVal_evt(val_evtCheck.isSelected());
		fpgaTableData.setReset_pa(reset_paCheck.isSelected());
		fpgaTableData.setMajority_in(Byte.valueOf(majorityText.getText()));
		fpgaTableData.setFilter_out_en(filter_outCheck.isSelected());
		fpgaTableData.setTime_window(Byte.valueOf(timeWndText.getText()));
		fpgaTableData.setAcq_valid_cycles(Byte.valueOf(acq_validText.getText()));
		fpgaTableData.setAcq_mux_cycles(Byte.valueOf(acq_muxText.getText()));
		fpgaTableData.setAcq_conv_cycles(Byte.valueOf(acq_convText.getText()));
		fpgaTableData.setVar_delay_cycles(Byte.valueOf(var_delayText.getText()));
		fpgaTableData.setAcq_hold_en(acq_holdCheck.isSelected());
		fpgaTableData.setAcq_cycles(Integer.valueOf(acq_cyclesText.getText()));
		fpgaTableData.setData_offs(Integer.valueOf(data_offsText.getText()));

		BitSet maskBit = new BitSet();
		for (int i = 0; i < tb.length; i++) {
			if (tb[i].isSelected()) {
				// int chEn =
				// PdmConstants.CH_FROM_PIXEL[Integer.valueOf(tb[i].getText())];
				int chEn = Integer.valueOf(tb[i].getText());
				maskBit.set(chEn);
			}
		}
		long maskLong = 0;
		if (!maskBit.isEmpty())
			maskLong = maskBit.toLongArray()[0];
		byte[] trMask = new byte[8];
		trMask[0] = (byte) maskLong;
		trMask[1] = (byte) (maskLong >> 8);
		trMask[2] = (byte) (maskLong >> 16);
		trMask[3] = (byte) (maskLong >> 24);
		trMask[4] = (byte) (maskLong >> 32);
		trMask[5] = (byte) (maskLong >> 40);
		trMask[6] = (byte) (maskLong >> 48);
		trMask[7] = (byte) (maskLong >> 56);
		fpgaTableData.setTriggerMask(trMask);

		fpgaTable = pdm.createFpgaTable(fpgaTableData);

		// System.out.println("--- FPGA TABLE ---");
		// for (int i = 0; i < fpgaTable.length; i++) {
		// System.out.println(fpgaTable[i]);
		// }
	}

	@FXML
	private void saveFpgaTable() {
		try {
			File file = fileChooser.showSaveDialog(null);
			if (file != null) {
				PrintWriter out = new PrintWriter(file);
				createFpgaTable();
				for (int i = 0; i < fpgaTable.length; i++) {
					String thisByte = String.format("%02x", fpgaTable[i]).toUpperCase();
					out.println(thisByte);
				}
				out.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void openFpgaTable() {
		File file = fileChooser.showOpenDialog(null);
		if (file != null) {
			loadFpgaTable(file);
		}
	}

	private void loadFpgaTable(File file) {
		try {
			Scanner scan = new Scanner(file);
			int i = 0;
			while (scan.hasNextLine()) {
				fpgaTable[i] = (byte) Short.parseShort(scan.nextLine(), 16);
				i++;
			}
			fpgaTableData = pdm.parseFpgaTable(fpgaTable);

			pdmIdFpgaText.setText(String.valueOf(fpgaTableData.getPdmId()));
			hk_enCheck.setSelected(fpgaTableData.isHk_en());
			verify_asicCheck.setSelected(fpgaTableData.isAsic_table_verify());
			stand_aloneCheck.setSelected(fpgaTableData.isStand_alone());
			// hkPeriodText.setText(String.valueOf(fpgaTableData.getHk_samp_period()));
			// clkExtCheck.setSelected(fpgaTableData.isExternalClock());
			rstb_pscCheck.setSelected(fpgaTableData.isRstb_psc());
			ps_globalCheck.setSelected(fpgaTableData.isPs_global_trig());
			raz_pCheck.setSelected(fpgaTableData.isRaz_p());
			val_evtCheck.setSelected(fpgaTableData.isVal_evt());
			reset_paCheck.setSelected(fpgaTableData.isReset_pa());
			filter_outCheck.setSelected(fpgaTableData.isFilter_out_en());
			acq_holdCheck.setSelected(fpgaTableData.isAcq_hold_en());

			majorityText.setText(String.valueOf(fpgaTableData.getMajority_in()));
			timeWndText.setText(String.valueOf(fpgaTableData.getTime_window()));
			acq_validText.setText(String.valueOf(fpgaTableData.getAcq_valid_cycles()));
			acq_muxText.setText(String.valueOf(fpgaTableData.getAcq_mux_cycles()));
			acq_convText.setText(String.valueOf(fpgaTableData.getAcq_conv_cycles()));
			var_delayText.setText(String.valueOf(fpgaTableData.getVar_delay_cycles()));
			acq_cyclesText.setText(String.valueOf(fpgaTableData.getAcq_cycles()));
			data_offsText.setText(String.valueOf(fpgaTableData.getData_offs()));

			byte[] trMask = fpgaTableData.getTriggerMask();
			long maskLong = (long) (trMask[0] & 0xff) + ((long) (trMask[1] & 0xff) << 8)
					+ ((long) (trMask[2] & 0xff) << 16) + ((long) (trMask[3] & 0xff) << 24)
					+ ((long) (trMask[4] & 0xff) << 32) + ((long) (trMask[5] & 0xff) << 40)
					+ ((long) (trMask[6] & 0xff) << 48) + ((long) (trMask[7] & 0xff) << 56);
			BitSet maskBit = BitSet.valueOf(new long[] { maskLong });
			for (int j = 0; j < tb.length; j++) {
				int ch = Integer.valueOf(tb[j].getText());
				if (maskBit.get(ch)) {
					tb[j].setSelected(true);
					tb[j].setStyle("-fx-font-size: 12; -fx-base: green;");
				} else {
					tb[j].setSelected(false);
					tb[j].setStyle("-fx-font-size: 12; -fx-base: red;");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void createSwitchTable() {
		switchTableData.setAcq_en(acq_enCheck.isSelected());
		switchTableData.setAcq_var_en(acq_var_enCheck.isSelected());
		switchTableData.setTrig_sel((byte) trig_selCombo.getSelectionModel().getSelectedIndex());
		switchTableData.setAcq_enable(acq_enableCheck.isSelected());
		switchTableData.setAcq_clear(acq_clearCheck.isSelected());
		switchTableData.setWind_mon((byte) wind_monCombo.getSelectionModel().getSelectedIndex());

		switchTable = pdm.createSwitchTable(switchTableData);

		// System.out.println("--- SWITCH TABLE ---");
		// for (int i = 0; i < switchTable.length; i++) {
		// System.out.println(switchTable[i]);
		// }
	}

	@FXML
	private void saveSwitchTable() {
		try {
			File file = fileChooser.showSaveDialog(null);
			if (file != null) {
				PrintWriter out = new PrintWriter(file);
				createSwitchTable();
				for (int i = 0; i < switchTable.length; i++) {
					String thisByte = String.format("%02x", switchTable[i]).toUpperCase();
					out.println(thisByte);
				}
				out.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void openSwitchTable() {
		File file = fileChooser.showOpenDialog(null);
		if (file != null) {
			loadSwitchTable(file);
		}
	}

	private void loadSwitchTable(File file) {
		try {
	        Scanner scan = new Scanner(file);
	        int i = 0;
	    	while(scan.hasNextLine()){
	    		switchTable[i] = (byte) Short.parseShort(scan.nextLine(), 16);
	    		i++;
    		}
    		switchTableData = pdm.parseSwitchTable(switchTable);
    			
    		acq_enCheck.setSelected(switchTableData.isAcq_en());
    		acq_var_enCheck.setSelected(switchTableData.isAcq_var_en());
    		acq_enableCheck.setSelected(switchTableData.isAcq_enable());
    		acq_clearCheck.setSelected(switchTableData.isAcq_clear());
    		trig_selCombo.getSelectionModel().select(switchTableData.getTrig_sel());
    		wind_monCombo.getSelectionModel().select(switchTableData.getWind_mon());    
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
	}

	@FXML
	private void allPxlOn() {
		for (int i = 0; i < 64; i++) {
			tb[i].setSelected(true);
			tb[i].setStyle("-fx-font-size: 12; -fx-base: green;");
		}
	}

	@FXML
	private void allPxlOff() {
		for (int i = 0; i < 64; i++) {
			tb[i].setSelected(false);
			tb[i].setStyle("-fx-font-size: 12; -fx-base: red;");
		}
	}

	@FXML
	private void init() {
		pdmId = Byte.parseByte(pdmIdText.getText());
		pdmIdFpgaText.setText(pdmIdText.getText());
		try {
			boolean result = pdm.init((byte) 0, pdmId);
			outText.appendText("PDM INIT " + pdmId + ": " + result + "\n");
		} catch (Exception e) {
			outText.appendText("PDM INIT " + pdmId + ": failure\n");
			e.printStackTrace();
		}
	}

	@FXML
	private void clockSelect() {
		pdmId = Byte.parseByte(pdmIdText.getText());
		int value = clkCombo.getSelectionModel().getSelectedIndex();
		try {
			boolean result = pdm.clockSelect(pdmId, (byte) value);
			outText.appendText("CLOCK SELECT " + value + " of pdm " + pdmId + ": " + result + "\n");
		} catch (Exception e) {
			outText.appendText("CLOCK SELECT " + value + " of pdm " + pdmId + ": failure\n");
			e.printStackTrace();
		}
	}

	@FXML
	private void resetModule() {
		pdmId = Byte.parseByte(pdmIdText.getText());
		byte moduleId = 0;
		switch (moduleResetCombo.getSelectionModel().getSelectedIndex()) {
		case 0:
			moduleId = PdmConstants.ARG_ACQUIRE_MODULE;
			break;
		case 1:
			moduleId = PdmConstants.ARG_VARIANCE_MODULE;
			break;
		case 2:
			moduleId = PdmConstants.ARG_TRIGGER_MODULE;
			break;
		default:
			break;
		}
		try {
			boolean result = pdm.resetModule(pdmId, moduleId);
			outText.appendText("RESET MODULE " + moduleId + " of pdm " + pdmId + ": " + result + "\n");
		} catch (Exception e) {
			outText.appendText("RESET MODULE " + moduleId + " of pdm " + pdmId + ": failure\n");
			e.printStackTrace();
		}
	}

	@FXML
	private void startCount() {
		pdmId = Byte.parseByte(pdmIdText.getText());
		try {
			boolean result = pdm.startCount(pdmId);
			outText.appendText("START COUNT of pdm " + pdmId + ": " + result + "\n");
		} catch (Exception e) {
			outText.appendText("START COUNT of pdm " + pdmId + ": failure\n");
			e.printStackTrace();
		}
	}

	@FXML
	private void sendTable() {
		pdmId = Byte.parseByte(pdmIdText.getText());
		byte tableId = 0;
		byte[] table = null;
		switch (tableCombo.getSelectionModel().getSelectedIndex()) {
		case 0:
			tableId = PdmConstants.ARG_FPGA_TABLE;
			createFpgaTable();
			table = fpgaTable;
			break;
		case 1:
			tableId = PdmConstants.ARG_ASIC_TABLE;
			createAsicTable();
			table = asicTable;
			break;
		case 2:
			tableId = PdmConstants.ARG_ASIC_PROBE_TABLE;
			createProbeTable();
			table = asicProbeTable;
			break;
		default:
			break;
		}
		try {
			boolean result = pdm.sendTable(pdmId, tableId, table);
			outText.appendText("SEND TABLE " + tableId + " to pdm " + pdmId + ": " + result + "\n");
		} catch (Exception e) {
			outText.appendText("SEND TABLE " + tableId + " to pdm " + pdmId + ": failure\n");
			e.printStackTrace();
		}
	}

	@FXML
	private void writeTable() {
		pdmId = Byte.parseByte(pdmIdText.getText());
		byte tableId = 0;
		switch (tableCombo.getSelectionModel().getSelectedIndex()) {
		case 0:
			tableId = PdmConstants.ARG_FPGA_TABLE;
			break;
		case 1:
			tableId = PdmConstants.ARG_ASIC_TABLE;
			break;
		case 2:
			tableId = PdmConstants.ARG_ASIC_PROBE_TABLE;
			break;
		default:
			break;
		}
		try {
			boolean result = pdm.writeTable(pdmId, tableId);
			outText.appendText("WRITE TABLE " + tableId + " to pdm " + pdmId + ": " + result + "\n");
		} catch (Exception e) {
			outText.appendText("WRITE TABLE " + tableId + " to pdm " + pdmId + ": failure\n");
			e.printStackTrace();
		}
	}

	@FXML
	private void loadTable() {
		pdmId = Byte.parseByte(pdmIdText.getText());
		byte tableId = 0;
		switch (tableLoadCombo.getSelectionModel().getSelectedIndex()) {
		case 0:
			tableId = PdmConstants.ARG_ASIC_TABLE;
			break;
		case 1:
			tableId = PdmConstants.ARG_ASIC_PROBE_TABLE;
			break;
		default:
			break;
		}
		try {
			boolean result = pdm.loadTable(pdmId, tableId);
			outText.appendText("LOAD TABLE " + tableId + " to pdm " + pdmId + ": " + result + "\n");
		} catch (Exception e) {
			outText.appendText("LOAD TABLE " + tableId + " to pdm " + pdmId + ": failure\n");
			e.printStackTrace();
		}
	}

	@FXML
	private void execTable() {
		pdmId = Byte.parseByte(pdmIdText.getText());
		byte tableId = 0;
		byte[] table = null;
		switch (tableCombo.getSelectionModel().getSelectedIndex()) {
		case 0:
			tableId = PdmConstants.ARG_FPGA_TABLE;
			createFpgaTable();
			table = fpgaTable;
			break;
		case 1:
			tableId = PdmConstants.ARG_ASIC_TABLE;
			createAsicTable();
			table = asicTable;
			break;
		case 2:
			tableId = PdmConstants.ARG_ASIC_PROBE_TABLE;
			createProbeTable();
			table = asicProbeTable;
			break;
		default:
			break;
		}
		try {
			boolean result = pdm.execTable(pdmId, tableId, table);
			outText.appendText("EXEC TABLE " + tableId + " to pdm " + pdmId + ": " + result + "\n");
		} catch (Exception e) {
			outText.appendText("EXEC TABLE " + tableId + " to pdm " + pdmId + ": failure\n");
			e.printStackTrace();
		}
	}

	@FXML
	private void requestTable() {
		pdmId = Byte.parseByte(pdmIdText.getText());
		byte tableId = 0;
		switch (tableCombo.getSelectionModel().getSelectedIndex()) {
		case 0:
			tableId = PdmConstants.ARG_FPGA_TABLE;
			break;
		case 1:
			tableId = PdmConstants.ARG_ASIC_TABLE;
			break;
		case 2:
			tableId = PdmConstants.ARG_ASIC_PROBE_TABLE;
			break;
		default:
			break;
		}
		try {
			byte[] resp = pdm.requestTable(pdmId, tableId);
			outText.appendText("REQUEST TABLE " + tableId + " to pdm " + pdmId + ": \n"
					+ PrintUtils.byteArrayToHexString(resp) + "\n");
		} catch (Exception e) {
			outText.appendText("REQUEST TABLE " + tableId + " to pdm " + pdmId + ": failure\n");
			e.printStackTrace();
		}
	}

	@FXML
	private void requestData() {
		pdmId = Byte.parseByte(pdmIdText.getText());
		byte moduleId = 0;
		switch (moduleRequestCombo.getSelectionModel().getSelectedIndex()) {
		case 0:
			moduleId = PdmConstants.ARG_ACQUIRE_MODULE;
			try {
				byte[] resp = pdm.requestData(pdmId, moduleId);
				outText.appendText("REQUEST DATA " + moduleId + " to pdm " + pdmId + ": \n"
						+ PrintUtils.byteArrayToHexString(resp) + "\n");				
			} catch (Exception e) {
				outText.appendText("REQUEST DATA " + moduleId + " to pdm " + pdmId + ": failure\n");
				e.printStackTrace();
			}
			break;
		case 1:
			moduleId = PdmConstants.ARG_VARIANCE_MODULE;
			try {
				byte[] resp = pdm.requestData(pdmId, moduleId);
				outText.appendText("REQUEST DATA " + moduleId + " to pdm " + pdmId + ": \n"
						+ PrintUtils.byteArrayToHexString(resp) + "\n");
			} catch (Exception e) {
				outText.appendText("REQUEST DATA " + moduleId + " to pdm " + pdmId + ": failure\n");
				e.printStackTrace();
			}
			break;
		case 2:
			moduleId = PdmConstants.ARG_TRIGGER_MODULE;
			try {
				byte[] resp = pdm.requestData(pdmId, moduleId);
				outText.appendText("REQUEST DATA " + moduleId + " to pdm " + pdmId + ": \n"
						+ PrintUtils.byteArrayToHexString(resp) + "\n");
				
				int time = PdmUtils.convertTimeWnd(fpgaTableData.getTime_window());
				double time_sec = ((double) time) / 1000;
				
				int[] result = PdmUtils.convertStairsData(asicTableData.getThreshold(), resp, time_sec);
				
				outText.appendText("COUNTERS: " + Arrays.toString(result) + "\n");
				
			} catch (Exception e) {
				outText.appendText("REQUEST DATA " + moduleId + " to pdm " + pdmId + ": failure\n");
				e.printStackTrace();
			}
			break;
		case 3:
			moduleId = PdmConstants.ARG_HK_MODULE;
			try {
				byte[] resp = pdm.requestData(pdmId, moduleId);
				outText.appendText("REQUEST DATA " + moduleId + " to pdm " + pdmId + ": \n"
						+ PrintUtils.byteArrayToHexString(resp) + "\n");
			} catch (Exception e) {
				outText.appendText("REQUEST DATA " + moduleId + " to pdm " + pdmId + ": failure\n");
				e.printStackTrace();
			}
		default:
			break;
		}
	}

	@FXML
	private void setSwitch() {
		pdmId = Byte.parseByte(pdmIdText.getText());
		this.createSwitchTable();
		try {
			boolean result = pdm.setSwitch(pdmId, switchTable);
			outText.appendText("SET SWITCH to pdm " + pdmId + ": " + result + "\n");
		} catch (Exception e) {
			outText.appendText("SET SWITCH to pdm " + pdmId + ": failure\n");
			e.printStackTrace();
		}
	}

	@FXML
	private void perform_C11_C12() {
		try {
			c11_12_FileIndex+=1;
			String filename = "c11_12_" + c11_12_FileIndex + ".txt";
			File file = new File("./output/" + filename);
			PrintWriter out = new PrintWriter(file);

			int thresholdStart = Integer.parseInt(startThreshText.getText());
			int thresholdEnd = Integer.parseInt(stopThreshText.getText());
			int thresholdDelta = Integer.parseInt(deltaThreshText.getText());

			createFpgaTable();
			createSwitchTable();

			boolean res;

			res = pdm.execTable(pdmId, PdmConstants.ARG_FPGA_TABLE, fpgaTable);
			outText.appendText("EXEC FPGA TABLE: " + res + "\n");

			res = pdm.setSwitch(pdmId, switchTable);
			outText.appendText("SET SWITCH: " + res + "\n");

			int time = PdmUtils.convertTimeWnd(fpgaTableData.getTime_window());
			double time_sec = ((double) time) / 1000;
			
			int threshold = thresholdStart;
			
			res = pdm.resetModule(pdmId, PdmConstants.ARG_TRIGGER_MODULE);
			outText.appendText("RESET TRIGGER MODULE: " + res + "\n");
			
			while (threshold <= thresholdEnd) {
				createAsicTable();
				asicTableData.setThreshold((short) threshold);
				asicTable = pdm.createAsicTable(asicTableData);

				res = pdm.execTable(pdmId, PdmConstants.ARG_ASIC_TABLE, asicTable);
				outText.appendText("EXEC ASIC TABLE: " + res + "\n");

				res = pdm.loadTable(pdmId, PdmConstants.ARG_ASIC_TABLE);
				outText.appendText("LOAD ASIC TABLE: " + res + "\n");
								
				// REQUEST DATA
				
				if (pdm.startCount(pdmId)) {
					
					Thread.sleep(PdmUtils.convertTimeWnd(fpgaTableData.getTime_window()) + 10);
					
					byte[] resp = pdm.requestData(pdmId, PdmConstants.ARG_TRIGGER_MODULE);
					
					outText.appendText("RESULT at THRESHOLD " + threshold + ": \n" + PrintUtils.byteArrayToHexString(resp) + "\n");
					
					int[] result = PdmUtils.convertStairsData(asicTableData.getThreshold(), resp, time_sec);
					
					String output = "";
					for (int i = 0; i < result.length; i++) {
						output += result[i] + " ";
					}
					out.println(output);
					out.flush();
					
					threshold += thresholdDelta;
				
				}
			}
			
			res = pdm.resetModule(pdmId, PdmConstants.ARG_TRIGGER_MODULE);
			outText.appendText("RESET TRIGGER MODULE: " + res + "\n");
			
		} catch (Exception e) {
			outText.appendText("PERFORM C11_C12: failure\n");
			e.printStackTrace();
		}
	}
	
	@FXML
	private void start_C13_C14() {
		try {
			c13_14Flag = true;
			c13_14_FileIndex+=1;
			String filename = "c13_14_" + c13_14_FileIndex + ".txt";
			File file = new File("./output/" + filename);
			PrintWriter out = new PrintWriter(file);
			
			Map<Integer, Integer>[] hglgMap = new TreeMap[128];
			for (int i = 0; i < hglgMap.length; i++) {
				hglgMap[i] = new TreeMap<Integer, Integer>();
			}
	
			int threshold = Integer.valueOf(c13_14ThreshText.getText());
			int iterations = Integer.valueOf(c13_14IterText.getText());
					
			createFpgaTable();
			createSwitchTable();

			boolean res;

			res = pdm.execTable(pdmId, PdmConstants.ARG_FPGA_TABLE, fpgaTable);
			outText.appendText("EXEC FPGA TABLE: " + res + "\n");

			res = pdm.setSwitch(pdmId, switchTable);
			outText.appendText("SET SWITCH: " + res + "\n");

			createAsicTable();
			
			asicTableData.setThreshold((short) threshold);
			asicTable = pdm.createAsicTable(asicTableData);

			res = pdm.execTable(pdmId, PdmConstants.ARG_ASIC_TABLE, asicTable);
			outText.appendText("EXEC ASIC TABLE: " + res + "\n");

			res = pdm.loadTable(pdmId, PdmConstants.ARG_ASIC_TABLE);
			outText.appendText("LOAD ASIC TABLE: " + res + "\n");
			
			res = pdm.resetModule(pdmId, PdmConstants.ARG_ACQUIRE_MODULE);
			outText.appendText("RESET ACQUIRE MODULE: " + res + "\n");
			
//			while (c13_14Flag) {
			for (int i = 0; i < iterations; i++) {
				
				// REQUEST DATA
				byte[] resp = pdm.requestData(pdmId, PdmConstants.ARG_ACQUIRE_MODULE);

				outText.appendText("RESULT at ITERATION " + i + ": \n" + PrintUtils.byteArrayToHexString(resp) + "\n");
				
				int[] result = PdmUtils.convertDistributionData(resp);
				
				//scan hg_lg
				for (int j = 0; j < 128; j++) {
					if (hglgMap[j].containsKey(result[j])) {
			            Integer count = hglgMap[j].get(result[j]);
			            if (count == null)
			                count = 0;
			            count++;
			            hglgMap[j].put(result[j], count);
					} else
						hglgMap[j].put(result[j], 1);
				}				
			}
			
			res = pdm.resetModule(pdmId, PdmConstants.ARG_ACQUIRE_MODULE);
			outText.appendText("RESET ACQUIRE MODULE: " + res + "\n");
			
			//scrittura su file degli istogrammi
			Iterator[] it = new Iterator[128];

			int max = 0;
			int it_big = 0;
			
			for (int i = 0; i < 128; i++) {
				it[i] = hglgMap[i].keySet().iterator();
				int size = hglgMap[i].size();
				if (size > max) {
					max = size;
					it_big = i;
				}
			}

			String header = "";
			for (int i = 1; i < 65; i++) {
				header += "HGpix_" + i + "\t" + "cts" + "\t" + "LGpix_" + i + "\t" + "cts" + "\t";
			}
			out.println(header);
			
			while (it[it_big].hasNext()) {
				String line = "";
				String hgValue = "";
				String lgValue = "";
				for (int i = 0; i < 64; i++) {
					try {
						int value_hg = (int) it[i].next();
						if (value_hg == 0)
							hgValue = 0 + "\t" + 0;
						else
							hgValue = value_hg + "\t" + hglgMap[i].get(value_hg);
					} catch (NoSuchElementException e) {
						hgValue = 0 + "\t" + 0;
					}
					try {
						int value_lg = (int) it[i+64].next();
						if (value_lg == 0)
							lgValue = 0 + "\t" + 0;
						else
							lgValue = value_lg + "\t" + hglgMap[i+64].get(value_lg);
					} catch (NoSuchElementException e) {
						lgValue = 0 + "\t" + 0;
					}
										
					line += hgValue + "\t" + lgValue + "\t";
				}				
				out.println(line);
			}
		
			out.flush();
			out.close();

		} catch (Exception e) {
			outText.appendText("PERFORM C13_C14: failure\n");
			e.printStackTrace();
		}
	}

	@FXML
	private void stop_C13_C14() {
		c13_14Flag = false;
	}
	
	@FXML
	private void clearOutText() {
		outText.clear();
	}

	@FXML
	private void clearOutChart() {
		outChart.getData().get(0).getData().clear();
	}

//	private void saveOutToFile(int moduleId, byte[] data) {
//		String filename = "";
//		switch (moduleId) {
//		case PdmConstants.ARG_ACQUIRE_MODULE:
//			acqFileIndex += 1;
//			filename = "acquire_" + acqFileIndex + ".txt";
//			break;
//		case PdmConstants.ARG_VARIANCE_MODULE:
//			varFileIndex += 1;
//			filename = "var_" + varFileIndex + ".txt";
//			break;
//		case PdmConstants.ARG_TRIGGER_MODULE:
//			trigFileIndex += 1;
//			filename = "trig_" + trigFileIndex + ".txt";
//			break;
//		case PdmConstants.ARG_HK_MODULE:
//			hkFileIndex += 1;
//			filename = "hk_" + hkFileIndex + ".txt";
//			break;
//		default:
//			break;
//		}
//		try {
//			File file = new File("./output/" + filename);
//			if (file != null) {
//				PrintWriter out = new PrintWriter(file);
//				for (int i = 0; i < data.length; i++) {
//					String thisByte = String.format("%02x", data[i]).toUpperCase();
//					out.println(thisByte);
//				}
//				out.close();
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

	private void saveOutToFile(String filename, int[] data) {
		try {
			File file = new File("./output/" + filename);
			if (file != null) {
				PrintWriter out = new PrintWriter(file);
				String output = "";
				for (int i = 0; i < data.length; i++) {
					output += data[i] + " ";
					
				}
				out.println(output);
				out.flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	private void openDefaultConfig() {
		loadFpgaTable(new File("./tables/fpga_config.txt"));
		loadAsicTable(new File("./tables/asic_config.txt"));
		loadSwitchTable(new File("./tables/switch_config.txt"));
	}

}
