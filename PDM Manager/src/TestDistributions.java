import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Scanner;
import java.util.TreeMap;

import it.inaf.iasfpa.astri.camera.pdm.AsicProbeTableData;
import it.inaf.iasfpa.astri.camera.pdm.AsicTableData;
import it.inaf.iasfpa.astri.camera.pdm.FpgaTableData;
import it.inaf.iasfpa.astri.camera.pdm.Pdm;
import it.inaf.iasfpa.astri.camera.pdm.PdmConstants;
import it.inaf.iasfpa.astri.camera.pdm.PdmUtils;
import it.inaf.iasfpa.astri.camera.pdm.SwitchTableData;
import it.inaf.iasfpa.astri.camera.utils.PrintUtils;

public class TestDistributions {
	
	int stairsFileIndex;
	
	Pdm pdm = new Pdm();
	
	private byte[] asicTable = new byte[286];
	private byte[] switchTable = new byte[2];
	private byte[] fpgaTable = new byte[38];

	AsicTableData asicTableData = new AsicTableData();
	AsicProbeTableData probeTableData = new AsicProbeTableData();
	FpgaTableData fpgaTableData = new FpgaTableData();
	SwitchTableData switchTableData = new SwitchTableData();

	byte pdmId;
	int thresholdDistribution, thresholdIterations;
	
	boolean c13_14Flag;
	
	public static void main(String[] args) {
		TestDistributions test = new TestDistributions();
		
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
		
		//faccio le distribuzioni
		test.performDistributions();
	}
	
	private void openDefaultConfig() {
		loadFpgaTable(new File("./tables/fpga_config.txt"));
		loadAsicTable(new File("./tables/asic_config.txt"));
		loadSwitchTable(new File("./tables/switch_config.txt"));
	}
	
	private void performDistributions() {
		long start = System.currentTimeMillis();
		try {
			c13_14Flag = true;
			String filename = "C13_14_PDM_" + pdmId + ".txt";
			File file = new File("./output/" + filename);
			PrintWriter out = new PrintWriter(file);
			
			Map<Integer, Integer>[] hglgMap = new TreeMap[128];
			for (int i = 0; i < hglgMap.length; i++) {
				hglgMap[i] = new TreeMap<Integer, Integer>();
			}
						
			fpgaTable = pdm.createFpgaTable(fpgaTableData);
			switchTable = pdm.createSwitchTable(switchTableData);

			boolean res;
			
			res = pdm.init((byte) 0, pdmId);
			System.out.println("INIT PDM: " + res + "\n");

			res = pdm.execTable(pdmId, PdmConstants.ARG_FPGA_TABLE, fpgaTable);
			System.out.println("EXEC FPGA TABLE: " + res + "\n");

			res = pdm.setSwitch(pdmId, switchTable);
			System.out.println("SET SWITCH: " + res + "\n");

			asicTable = pdm.createAsicTable(asicTableData);
			
			asicTableData.setThreshold((short) thresholdDistribution);
			asicTable = pdm.createAsicTable(asicTableData);

			res = pdm.execTable(pdmId, PdmConstants.ARG_ASIC_TABLE, asicTable);
			System.out.println("EXEC ASIC TABLE: " + res + "\n");

			res = pdm.loadTable(pdmId, PdmConstants.ARG_ASIC_TABLE);
			System.out.println("LOAD ASIC TABLE: " + res + "\n");
			
			res = pdm.resetModule(pdmId, PdmConstants.ARG_ACQUIRE_MODULE);
			System.out.println("RESET ACQUIRE MODULE: " + res + "\n");
			
//			while (c13_14Flag) {
			for (int i = 0; i < thresholdIterations; i++) {
				
				// REQUEST DATA
				try {
//					System.out.println("RUNNING ITERATION " + i + "\n");
					byte[] resp = pdm.requestData(pdmId, PdmConstants.ARG_ACQUIRE_MODULE);
//					System.out.println("RESULT at ITERATION " + i + ": \n" + PrintUtils.byteArrayToHexString(resp) + "\n");

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
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			res = pdm.resetModule(pdmId, PdmConstants.ARG_ACQUIRE_MODULE);
			System.out.println("RESET ACQUIRE MODULE: " + res + "\n");
			
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
			
			pdm.releaseDevice();
			
			long stop = System.currentTimeMillis();
			System.out.println("C13_C14 Elapsed in " + ((stop - start)/1000) + " seconds");
			
			System.exit(0);

		} catch (Exception e) {
			System.out.println("PERFORM C13_C14: failure\n");
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
			thresholdDistribution = Integer.parseInt(devProps.getProperty("DISTRIBUTIONS_THRESHOLD"));
			thresholdIterations = Integer.parseInt(devProps.getProperty("DISTRIBUTIONS_ITERATIONS"));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
