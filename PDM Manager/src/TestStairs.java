import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.Properties;
import java.util.Scanner;

import it.inaf.iasfpa.astri.camera.pdm.AsicProbeTableData;
import it.inaf.iasfpa.astri.camera.pdm.AsicTableData;
import it.inaf.iasfpa.astri.camera.pdm.FpgaTableData;
import it.inaf.iasfpa.astri.camera.pdm.Pdm;
import it.inaf.iasfpa.astri.camera.pdm.PdmConstants;
import it.inaf.iasfpa.astri.camera.pdm.PdmUtils;
import it.inaf.iasfpa.astri.camera.pdm.SwitchTableData;
import it.inaf.iasfpa.astri.camera.utils.PrintUtils;

public class TestStairs {
	
	Pdm pdm = new Pdm();
	
	private byte[] asicTable = new byte[286];
	private byte[] switchTable = new byte[2];
	private byte[] fpgaTable = new byte[38];

	AsicTableData asicTableData = new AsicTableData();
	AsicProbeTableData probeTableData = new AsicProbeTableData();
	FpgaTableData fpgaTableData = new FpgaTableData();
	SwitchTableData switchTableData = new SwitchTableData();

	byte pdmId;
	int thresholdStart, thresholdEnd, thresholdDelta;

	public static void main(String[] args) {
		TestStairs test = new TestStairs();
		
		//carico i parametri di configurazione dell'app
		test.readAppProperties();
		
		//carico le tabelle in memoria
		test.openDefaultConfig();
		
		//stabilisco la connessione
		boolean result = test.pdm.connect();
		if (result) {
			System.out.println("Connection OK");
		} else
			System.out.println("Connection Failed");
		
		//faccio la stairs
		test.performStairs();
	}
	
	private void openDefaultConfig() {
		loadFpgaTable(new File("./tables/fpga_config.txt"));
		loadAsicTable(new File("./tables/asic_config.txt"));
		loadSwitchTable(new File("./tables/switch_config.txt"));
	}
	
	private void performStairs() {
		try {
			String filename = "C11_12_PDM_" + pdmId + ".txt";
			File file = new File("./output/" + filename);
			PrintWriter out = new PrintWriter(file);

			fpgaTable = pdm.createFpgaTable(fpgaTableData);
			switchTable = pdm.createSwitchTable(switchTableData);

			boolean res;
			
			res = pdm.init((byte) 0, pdmId);
			System.out.println("INIT PDM: " + res + "\n");

			res = pdm.execTable(pdmId, PdmConstants.ARG_FPGA_TABLE, fpgaTable);
			System.out.println("EXEC FPGA TABLE: " + res + "\n");

			res = pdm.setSwitch(pdmId, switchTable);
			System.out.println("SET SWITCH: " + res + "\n");

			int time = PdmUtils.convertTimeWnd(fpgaTableData.getTime_window());
			double time_sec = ((double) time) / 1000;
			
			int threshold = thresholdStart;
			
			res = pdm.resetModule(pdmId, PdmConstants.ARG_TRIGGER_MODULE);
			System.out.println("RESET TRIGGER MODULE: " + res + "\n");
			
			String header = "DAC ";
			for (int i = 0; i < 64; i++) {
				header += "CH" + i + " ";
			}
			out.println(header);
			
			while (threshold <= thresholdEnd) {
				asicTable = pdm.createAsicTable(asicTableData);
				asicTableData.setThreshold((short) threshold);
				asicTable = pdm.createAsicTable(asicTableData);

				res = pdm.execTable(pdmId, PdmConstants.ARG_ASIC_TABLE, asicTable);
				System.out.println("EXEC ASIC TABLE: " + res + "\n");

				res = pdm.loadTable(pdmId, PdmConstants.ARG_ASIC_TABLE);
				System.out.println("LOAD ASIC TABLE: " + res + "\n");
				
				// REQUEST DATA
				byte[] resp = pdm.requestData(pdmId, PdmConstants.ARG_TRIGGER_MODULE);

				System.out.println("RESULT at THRESHOLD " + threshold + ": \n" + PrintUtils.byteArrayToHexString(resp) + "\n");
				
				int[] result = PdmUtils.convertStairsData(asicTableData.getThreshold(), resp, time_sec);
				
				String output = "";
				for (int i = 0; i < result.length; i++) {
					output += result[i] + " ";
				}
				out.println(output);
				out.flush();
				
				threshold += thresholdDelta;
			}
			
			res = pdm.resetModule(pdmId, PdmConstants.ARG_TRIGGER_MODULE);
			System.out.println("RESET TRIGGER MODULE: " + res + "\n");
			
			pdm.releaseDevice();
			
			System.exit(0);
			
		} catch (Exception e) {
			System.out.println("PERFORM C11_C12: failure\n");
			e.printStackTrace();
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

		} catch (Exception e) {
			e.printStackTrace();
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

		} catch (Exception e) {
			e.printStackTrace();
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
    		
     	} catch(Exception e) {
    		e.printStackTrace();
    	}
	}
	
	private void readAppProperties() {
		try {
			//file properties dei parametri
			Properties devProps = new Properties();
			FileInputStream in = new FileInputStream("./application.properties");
			devProps.load(in);
			in.close();
			pdmId = Byte.parseByte(devProps.getProperty("PDM_ID"));
			thresholdStart = Integer.parseInt(devProps.getProperty("START_THRESHOLD"));
			thresholdEnd = Integer.parseInt(devProps.getProperty("END_THRESHOLD"));
			thresholdDelta = Integer.parseInt(devProps.getProperty("DELTA_THRESHOLD"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
