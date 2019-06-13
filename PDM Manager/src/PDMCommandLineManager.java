import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import it.inaf.iasfpa.astri.camera.pdm.AsicProbeTableData;
import it.inaf.iasfpa.astri.camera.pdm.AsicTableData;
import it.inaf.iasfpa.astri.camera.pdm.FpgaTableData;
import it.inaf.iasfpa.astri.camera.pdm.Pdm;
import it.inaf.iasfpa.astri.camera.pdm.PdmConstants;
import it.inaf.iasfpa.astri.camera.pdm.PdmUtils;
import it.inaf.iasfpa.astri.camera.pdm.SwitchTableData;
import it.inaf.iasfpa.astri.camera.pdm.data.HkData;
import it.inaf.iasfpa.astri.camera.utils.PrintUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Region;

public class PDMCommandLineManager {

	Pdm pdm = new Pdm();

	private byte[] asicTable = new byte[286];
	private byte[] switchTable = new byte[2];
	private byte[] fpgaTable = new byte[38];

	private double[] sipm_Temp_M, sipm_Temp_Q;

	AsicTableData asicTableData = new AsicTableData();
	AsicProbeTableData probeTableData = new AsicProbeTableData();
	FpgaTableData fpgaTableData = new FpgaTableData();
	SwitchTableData switchTableData = new SwitchTableData();

	static byte pdmId;
	int thresholdStart, thresholdEnd, thresholdDelta;
	int thresholdDistribution, thresholdIterations;
	int hkIterations, hkInterval;
	int varIterations, varInterval;

	boolean c13_14Flag, isFreeRunning;

	private BufferedReader br;

	DateFormat df = new SimpleDateFormat("[yyyy_MM_dd_HH_mm_ss_SSS]");
	DecimalFormat decF = new DecimalFormat("#.00");

	public static void main(String[] args) {
		PDMCommandLineManager test = new PDMCommandLineManager();

		test.br = new BufferedReader(new InputStreamReader(System.in));
		String input;

		// Scelgo su quale PDM operare (bypasso app.properties, aggiunto per
		// test dispatcher singolo connettore)
		System.out.println("Enter PDM ID to Manage");
		System.out.print("> ");
		try {
			input = test.br.readLine();
			test.executeCommand(input);
			pdmId = Byte.parseByte(input);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// stabilisco la connessione
		boolean result = test.pdm.connect();
		if (result) {
			System.out.println("Port Connection OK");
		} else
			System.out.println("Port Connection Failed");

		while (true) {
			System.out.println("Enter commands, or 'exit' to quit");
			System.out.print("> ");
			try {
				input = test.br.readLine();
				test.executeCommand(input);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void performBroadcastTest() {
		// chiamata di tutti i comandi di cui ha senso fare il broadcast
		try {
			// carico i parametri di configurazione dell'app
			readAppProperties();

			// carico le tabelle in memoria
			openDefaultConfig();

			fpgaTableData.setPdmId(pdmId);
			fpgaTable = pdm.createFpgaTable(fpgaTableData);
			asicTable = pdm.createAsicTable(asicTableData);
			switchTable = pdm.createSwitchTable(switchTableData);

			boolean res;

			// init
			res = pdm.init((byte) 0, pdmId);
			System.out.println("INIT PDM: " + res + "\n");

			// fpga table
			res = pdm.sendTable(pdmId, PdmConstants.ARG_FPGA_TABLE, fpgaTable);
			System.out.println("SEND FPGA TABLE: " + res + "\n");

			res = pdm.writeTable((byte) 0, PdmConstants.ARG_FPGA_TABLE);
			System.out.println("[BROADCAST] WRITE FPGA TABLE: " + res + "\n");

			// asic table
			res = pdm.sendTable(pdmId, PdmConstants.ARG_ASIC_TABLE, asicTable);
			System.out.println("SEND ASIC TABLE: " + res + "\n");

			res = pdm.writeTable((byte) 0, PdmConstants.ARG_ASIC_TABLE);
			System.out.println("[BROADCAST] WRITE ASIC TABLE: " + res + "\n");

			// set switch
			res = pdm.setSwitch((byte) 0, switchTable);
			System.out.println("[BROADCAST] SET SWITCH: " + res + "\n");

			// reset modules
			res = pdm.resetModule((byte) 0, PdmConstants.ARG_TRIGGER_MODULE);
			System.out.println("[BROADCAST] RESET TRIGGER MODULE: " + res + "\n");

			res = pdm.resetModule((byte) 0, PdmConstants.ARG_ACQUIRE_MODULE);
			System.out.println("[BROADCAST] RESET ACQUIRE MODULE: " + res + "\n");

			res = pdm.resetModule((byte) 0, PdmConstants.ARG_VARIANCE_MODULE);
			System.out.println("[BROADCAST] RESET VARIANCE MODULE: " + res + "\n");

			if (pdm.startCount((byte) 0)) {
				int threshold = thresholdStart;
				long startStairs = System.currentTimeMillis();
				byte[] resp = pdm.requestData(pdmId, PdmConstants.ARG_TRIGGER_MODULE);
				long stopStairs = System.currentTimeMillis();
				System.out.println(
						"Elapsed time per request data trigger: " + (stopStairs - startStairs) + " milliseconds");
				System.out.println(
						"RESULT at THRESHOLD " + threshold + ": \n" + PrintUtils.byteArrayToHexString(resp) + "\n");

				System.out.println("PERFORM BROADCAST TEST SU 1 PDM: END\n");
			}
		} catch (Exception e) {
			System.out.println("PERFORM BROADCAST TEST SU 1 PDM: FAILURE\n");
			e.printStackTrace();
		}
	}

	private void performTest() {
		try {
			// carico i parametri di configurazione dell'app
			readAppProperties();

			// carico le tabelle in memoria
			openDefaultConfig();

			fpgaTableData.setPdmId(pdmId);
			fpgaTable = pdm.createFpgaTable(fpgaTableData);
			asicTable = pdm.createAsicTable(asicTableData);
			switchTable = pdm.createSwitchTable(switchTableData);

			boolean res;

			// init
			res = pdm.init(pdmId, pdmId);
			System.out.println("INIT PDM: " + res + "\n");

			res = pdm.sendTable(pdmId, PdmConstants.ARG_FPGA_TABLE, fpgaTable);
			System.out.println("SEND FPGA TABLE: " + res + "\n");

			res = pdm.writeTable(pdmId, PdmConstants.ARG_FPGA_TABLE);
			System.out.println("WRITE FPGA TABLE: " + res + "\n");

			System.out.println("PERFORM TEST: END\n");

		} catch (Exception e) {
			System.out.println("PERFORM TEST: FAILURE\n");
			e.printStackTrace();
		}
	}

	private void performInitialization() {
		try {
			// carico i parametri di configurazione dell'app
			readAppProperties();

			// carico le tabelle in memoria
			openDefaultConfig();

			fpgaTableData.setPdmId(pdmId);
			fpgaTable = pdm.createFpgaTable(fpgaTableData);
			asicTable = pdm.createAsicTable(asicTableData);
			switchTable = pdm.createSwitchTable(switchTableData);

			boolean res;

			// init
			res = pdm.init(pdmId, pdmId);
			// res = pdm.init((byte) 0, pdmId);
			System.out.println("INIT PDM: " + res + "\n");

			res = pdm.sendTable(pdmId, PdmConstants.ARG_FPGA_TABLE, fpgaTable);
			System.out.println("SEND FPGA TABLE: " + res + "\n");

			res = pdm.writeTable(pdmId, PdmConstants.ARG_FPGA_TABLE);
			System.out.println("WRITE FPGA TABLE: " + res + "\n");

//			for (int i = 0; i < 100; i++) {
				res = pdm.sendTable(pdmId, PdmConstants.ARG_ASIC_TABLE, asicTable);
				System.out.println("SEND ASIC TABLE: " + res + "\n");
//			}

			res = pdm.writeTable(pdmId, PdmConstants.ARG_ASIC_TABLE);
			System.out.println("WRITE ASIC TABLE: " + res + "\n");

			res = pdm.loadTable(pdmId, PdmConstants.ARG_ASIC_TABLE);
			System.out.println("LOAD ASIC TABLE: " + res + "\n");

			// set switch
			res = pdm.setSwitch(pdmId, switchTable);
			System.out.println("SET SWITCH: " + res + "\n");

			// reset modules
			res = pdm.resetModule(pdmId, PdmConstants.ARG_TRIGGER_MODULE);
			System.out.println("RESET TRIGGER MODULE: " + res + "\n");

			res = pdm.resetModule(pdmId, PdmConstants.ARG_ACQUIRE_MODULE);
			System.out.println("RESET ACQUIRE MODULE: " + res + "\n");

			res = pdm.resetModule(pdmId, PdmConstants.ARG_VARIANCE_MODULE);
			System.out.println("RESET VARIANCE MODULE: " + res + "\n");

			System.out.println("PERFORM FULL INITIALIZATION: END\n");

		} catch (Exception e) {
			System.out.println("PERFORM FULL INITIALIZATION: FAILURE\n");
			e.printStackTrace();
		}
	}

	private void performStairs() {
		try {
			long start = System.currentTimeMillis();
			boolean res;

			performInitialization();

			// file di output
			File file = new File("./output/C11_C12_" + pdmId + "_" + df.format(new Date()) + ".txt");
			PrintWriter out = new PrintWriter(new FileWriter(file), true);

			// intestazione del file
			String header = "DAC ";
			for (int i = 0; i < 64; i++) {
				header += "CH" + i + " ";
			}
			out.println(header);

			int time = PdmUtils.convertTimeWnd(fpgaTableData.getTime_window());
			double time_sec = ((double) time) / 1000;

			int threshold = thresholdStart;

			while (threshold <= thresholdEnd) {
				asicTableData.setThreshold((short) threshold);
				asicTable = pdm.createAsicTable(asicTableData);

				res = pdm.sendTable(pdmId, PdmConstants.ARG_ASIC_TABLE, asicTable);
				System.out.println("SEND ASIC TABLE: " + res + "\n");

				res = pdm.writeTable(pdmId, PdmConstants.ARG_ASIC_TABLE);
				System.out.println("WRITE ASIC TABLE: " + res + "\n");

				res = pdm.loadTable(pdmId, PdmConstants.ARG_ASIC_TABLE);
				System.out.println("LOAD ASIC TABLE: " + res + "\n");

				// REQUEST DATA

				if (pdm.startCount(pdmId)) {
					long startStairs = System.currentTimeMillis();

					byte[] resp = pdm.requestData(pdmId, PdmConstants.ARG_TRIGGER_MODULE);

					long stopStairs = System.currentTimeMillis();

					System.out.println(
							"Elapsed time per request data trigger: " + (stopStairs - startStairs) + " milliseconds");

					System.out.println(
							"RESULT at THRESHOLD " + threshold + ": \n" + PrintUtils.byteArrayToHexString(resp) + "\n");

					int[] result = PdmUtils.convertStairsData(asicTableData.getThreshold(), resp, time_sec);

					String output = "";
					for (int i = 0; i < result.length; i++) {
						output += result[i] + " ";
					}
					out.println(output);

					threshold += thresholdDelta;
				}
			}
			out.close();

			res = pdm.resetModule(pdmId, PdmConstants.ARG_TRIGGER_MODULE);
			System.out.println("RESET TRIGGER MODULE: " + res + "\n");

			long stop = System.currentTimeMillis();
			System.out.println("C11_C12 Elapsed in " + ((stop - start) / 1000) + " seconds");

		} catch (Exception e) {
			System.out.println("PERFORM C11_C12: FAILURE\n");
			e.printStackTrace();
		}
	}

	private void performDistributions() {
		try {
			long start = System.currentTimeMillis();
			c13_14Flag = true;
			boolean res;

			performInitialization();

			// file di output
			File file = new File("./output/C13_C14_" + pdmId + "_" + df.format(new Date()) + ".txt");
			PrintWriter out = new PrintWriter(new FileWriter(file), true);

			// struttura dati per output
			Map<Integer, Integer>[] hglgMap = new TreeMap[128];
			for (int i = 0; i < hglgMap.length; i++) {
				hglgMap[i] = new TreeMap<Integer, Integer>();
			}

			asicTableData.setThreshold((short) thresholdDistribution);
			asicTable = pdm.createAsicTable(asicTableData);

			res = pdm.sendTable(pdmId, PdmConstants.ARG_ASIC_TABLE, asicTable);
			System.out.println("SEND ASIC TABLE: " + res + "\n");

			res = pdm.writeTable(pdmId, PdmConstants.ARG_ASIC_TABLE);
			System.out.println("WRITE ASIC TABLE: " + res + "\n");

			res = pdm.loadTable(pdmId, PdmConstants.ARG_ASIC_TABLE);
			System.out.println("LOAD ASIC TABLE: " + res + "\n");

			// while (c13_14Flag) {
			for (int i = 0; i < thresholdIterations; i++) {

				// REQUEST DATA
				try {
					// System.out.println("RUNNING ITERATION " + i + "\n");
					long startDistr = System.currentTimeMillis();
					byte[] resp = pdm.requestData(pdmId, PdmConstants.ARG_ACQUIRE_MODULE);
					long stopDistr = System.currentTimeMillis();
					System.out.println(
							"Elapsed time per request data trigger: " + (stopDistr - startDistr) + " milliseconds");
					// System.out.println("RESULT at ITERATION " + i + ": \n" +
					// PrintUtils.byteArrayToHexString(resp) + "\n");

					int[] result = PdmUtils.convertDistributionData(resp);

					// scan hg_lg
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

			// scrittura su file degli istogrammi
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
						int value_lg = (int) it[i + 64].next();
						if (value_lg == 0)
							lgValue = 0 + "\t" + 0;
						else
							lgValue = value_lg + "\t" + hglgMap[i + 64].get(value_lg);
					} catch (NoSuchElementException e) {
						lgValue = 0 + "\t" + 0;
					}

					line += hgValue + "\t" + lgValue + "\t";
				}
				out.println(line);
			}

			out.close();

			long stop = System.currentTimeMillis();
			System.out.println("C13_C14 Elapsed in " + ((stop - start) / 1000) + " seconds");

		} catch (Exception e) {
			System.out.println("PERFORM C13_C14: FAILURE\n");
			e.printStackTrace();
		}
	}

	private void performHk() {
		try {
			long start = System.currentTimeMillis();
			boolean res;

			performInitialization();

			// file di output
			File file = new File("./output/HK_" + pdmId + "_" + df.format(new Date()) + "_HK.txt");
			PrintWriter out = new PrintWriter(new FileWriter(file), true);

			// intestazione del file
			String header = "";
			for (int i = 1; i < 20; i++) {
				header += "ADC" + i + " ";
			}
			for (int i = 1; i < 20; i++) {
				header += "T" + i + " ";
			}
			out.println(header);

			for (int k = 0; k < hkIterations; k++) {
				long startHk = System.currentTimeMillis();

				byte[] resp = pdm.requestData(pdmId, PdmConstants.ARG_HK_MODULE);

				long stopHk = System.currentTimeMillis();
				System.out.println("Elapsed time per request data hk: " + (stopHk - startHk) + " milliseconds");

				int[] adcValues = PdmUtils.convertHkData(resp);

				HkData hkData = PdmUtils.parseHkData((byte) pdmId, adcValues, sipm_Temp_M, sipm_Temp_Q);

				String output = "";
				for (int i = 0; i < adcValues.length; i++) {
					output += adcValues[i] + " ";
				}

				double[] sipmTemp = hkData.getTemperature();
				for (int i = 0; i < sipmTemp.length; i++) {
					output += decF.format(sipmTemp[i]) + " ";
				}
				output += decF.format(hkData.getCitirocTemp()) + " ";
				output += decF.format(hkData.getCitirocCurr()[0]) + " ";
				output += decF.format(hkData.getCitirocCurr()[1]) + " ";
				output += decF.format(hkData.getCitirocCurr()[2]) + " ";
				output += "0.00 ";
				output += "0.00 ";
				output += decF.format(hkData.getFpgaTemp()) + " ";
				output += "0.00 ";
				output += decF.format(hkData.getFpgaCurr()) + " ";

				out.println(output);

				Thread.sleep(hkInterval);
			}
			out.close();

			long stop = System.currentTimeMillis();
			System.out.println("HK Elapsed in " + ((stop - start)) + " milliseconds");

		} catch (Exception e) {
			System.out.println("PERFORM HK: FAILURE\n");
			e.printStackTrace();
		}
	}

	private void performVar() {
		try {
			long start = System.currentTimeMillis();
			boolean res;

			performInitialization();

			// file di output
			File file = new File("./output/VAR_" + pdmId + "_" + df.format(new Date()) + ".txt");
			PrintWriter out = new PrintWriter(new FileWriter(file), true);

			// intestazione del file
			String header = "";
			for (int i = 1; i < 65; i++) {
				header += "Mean_pix_" + i + " ";
			}
			for (int i = 1; i < 65; i++) {
				header += "Sum_pix_" + i + " ";
			}
			for (int i = 1; i < 65; i++) {
				header += "Sum_Squares_pix_" + i + " ";
			}
			out.println(header);
			for (int k = 0; k < varIterations; k++) {
				// REQUEST DATA
				if (pdm.startCount(pdmId)) {

					long startVar = System.currentTimeMillis();

					byte[] resp = pdm.requestData(pdmId, PdmConstants.ARG_VARIANCE_MODULE);

					long stopVar = System.currentTimeMillis();

					System.out.println(
							"Elapsed time per request data variance: " + (stopVar - startVar) + " milliseconds");

					double[] result = PdmUtils.convertVarData(resp);

					String output = "";
					for (int i = 0; i < result.length; i++) {
						output += result[i] + " ";
					}
					out.println(output);

					Thread.sleep(varInterval);
				}
			}
			out.close();

			res = pdm.resetModule(pdmId, PdmConstants.ARG_VARIANCE_MODULE);
			System.out.println("RESET VARIANCE MODULE: " + res + "\n");

			long stop = System.currentTimeMillis();
			System.out.println("VARIANCE Elapsed in " + ((stop - start)) + " milliseconds");

		} catch (Exception e) {
			System.out.println("PERFORM VARIANCE: FAILURE\n");
			e.printStackTrace();
		}
	}

	private void performFreeRunning() {
		try {
			performInitialization();

			// file di output
			File fileHK = new File("./output/HK_" + pdmId + "_" + df.format(new Date()) + ".txt");
			PrintWriter outHK = new PrintWriter(new FileWriter(fileHK), true);

			File fileVAR = new File("./output/VAR_" + pdmId + "_" + df.format(new Date()) + ".txt");
			PrintWriter outVAR = new PrintWriter(new FileWriter(fileVAR), true);

			// intestazione del file HK
			String header = "";
			for (int i = 1; i < 20; i++) {
				header += "ADC" + i + " ";
			}
			for (int i = 1; i < 20; i++) {
				header += "T" + i + " ";
			}
			outHK.println(header);

			// intestazione del file VAR
			header = "";
			for (int i = 1; i < 65; i++) {
				header += "Mean_pix_" + i + " ";
			}
			for (int i = 1; i < 65; i++) {
				header += "Sum_pix_" + i + " ";
			}
			for (int i = 1; i < 65; i++) {
				header += "Sum_Squares_pix_" + i + " ";
			}
			outVAR.println(header);

			ScheduledFuture threadHK = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					try {
						System.out.println("faccio hk del free running");
						byte[] resp = pdm.requestData(pdmId, PdmConstants.ARG_HK_MODULE);

						int[] adcValues = PdmUtils.convertHkData(resp);
						HkData hkData = PdmUtils.parseHkData((byte) pdmId, adcValues, sipm_Temp_M, sipm_Temp_Q);

						String output = "";
						for (int i = 0; i < adcValues.length; i++) {
							output += adcValues[i] + " ";
						}

						double[] sipmTemp = hkData.getTemperature();
						for (int i = 0; i < sipmTemp.length; i++) {
							output += decF.format(sipmTemp[i]) + " ";
						}
						output += decF.format(hkData.getCitirocTemp()) + " ";
						output += decF.format(hkData.getCitirocCurr()[0]) + " ";
						output += decF.format(hkData.getCitirocCurr()[1]) + " ";
						output += decF.format(hkData.getCitirocCurr()[2]) + " ";
						output += "0.00 ";
						output += "0.00 ";
						output += decF.format(hkData.getFpgaTemp()) + " ";
						output += "0.00 ";
						output += decF.format(hkData.getFpgaCurr()) + " ";

						outHK.println(output);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}, 0, 10, TimeUnit.SECONDS);

			ScheduledFuture threadVAR = Executors.newSingleThreadScheduledExecutor()
					.scheduleAtFixedRate(new Runnable() {
						@Override
						public void run() {
							try {
								System.out.println("faccio var del free running");
								byte[] resp = pdm.requestData(pdmId, PdmConstants.ARG_VARIANCE_MODULE);
								double[] result = PdmUtils.convertVarData(resp);
								String output = "";
								for (int i = 0; i < result.length; i++) {
									output += result[i] + " ";
								}
								outVAR.println(output);
								pdm.resetModule(pdmId, PdmConstants.ARG_VARIANCE_MODULE);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}, 0, 1, TimeUnit.SECONDS);

		} catch (Exception e) {
			System.out.println("PERFORM FREE RUNNING: FAILURE\n");
			e.printStackTrace();
		}
	}

	public boolean areAllTrue(boolean[] array) {
		for (boolean b : array)
			if (!b)
				return false;
		return true;
	}

	public void startCountTest() throws Exception {
		System.out.println("START COUNT TEST");
		pdm.startCount(pdmId);
	}

	private void temperatureCalibration() {
		double[] m_def = new double[] { 6.4500, 6.4500, 6.4500, 6.4500, 6.4500, 6.4500, 6.4500, 6.4500, 6.4500 };
		double[] qb = new double[] { 514.4616, 514.4616, 514.4616, 514.4616, 514.4616, 514.4616, 514.4616, 514.4616,
				514.4616 };

		int nSample = 100;
		double tref = 15.0;

		boolean[] calibrated = new boolean[9];
		double[][] temperature = new double[100][9];

		double[] tMedia = new double[9];

		double start = System.currentTimeMillis();

		while (!areAllTrue(calibrated)) {
			// colleziono 100 campioni
			for (int k = 0; k < nSample; k++) {
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

			// faccio la media dei 100 campioni
			for (int i = 0; i < 9; i++) {
				double somma = 0.0;
				for (int k = 0; k < nSample; k++) {
					somma += temperature[k][i];
					tMedia[i] = somma / nSample;
				}

				if (!calibrated[i]) {
					if ((tMedia[i] > (tref + 0.05)) || (tMedia[i] < (tref - 0.05))) {
						double delta = (tref - tMedia[i]);

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

						if (tMedia[i] >= (tref + 0.05))
							qb[i] = qb[i] + mu * Math.abs(delta);
						if (tMedia[i] < (tref - 0.05))
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
			File file = new File("./output/TEMP_CALIB/PDM_" + pdmId + "_temperature_sensors.txt");
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

	private void temperatureCalibration2() {
		double[] sipm_M = new double[9];
		double[] sipm_Q = new double[9];
		double tRef = 20.0;

		try {
			// apro file con q temporaneo
			Scanner scan = new Scanner(new File("./output/TEMP_CALIB/PDM_" + pdmId + "_temperature_sensors.txt"));
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
			File file = new File("./output/TEMP_CALIB/20°_PDM_" + pdmId + "_15°.txt");
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

			for (int k = 0; k < 2000; k++) {
				byte[] resp = pdm.requestData(pdmId, PdmConstants.ARG_HK_MODULE);
				int[] adcValues = PdmUtils.convertHkData(resp);
				HkData hkData = PdmUtils.parseHkData((byte) pdmId, adcValues, sipm_M, sipm_Q);

				String output = tRef + " ";
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

	private void selectBoot(byte boot) {
		boolean result;
		try {
			result = pdm.setBoot(pdmId, boot);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
		}
		System.out.println("Select boot number " + boot + " result:" + result + "\n");
	}

	private void flash(int sector, int page, int fw) {
		try {
			byte[] flashData;
			int pagesNum, sectorsNum;
			String fileName = "";
			if (fw == 1) {
				fileName = "./flash/fw.bit";
			} else if (fw == 2) {
				fileName = "./flash/fw2.bit";
			}
			File file = new File(fileName);
			if (file != null) {
				Path path = file.toPath();
				flashData = Files.readAllBytes(path);

				pagesNum = flashData.length / 256;
				if ((flashData.length % 256) != 0)
					pagesNum++;

				sectorsNum = pagesNum / 256;
				if ((pagesNum % 256) != 0)
					sectorsNum++;

				System.out.println("File size = " + flashData.length + " bytes. " + sectorsNum + " sectors for "
						+ pagesNum + " pages of 256 bytes needed.");

				boolean flashResult = true;
				for (int i = 0; i < sectorsNum; i++) {
					if (flashResult) {
						for (int j = 0; j < 256; j++) {
							if (flashResult) {
								if ((i * 256) + (j) < pagesNum) {
									// System.out.println("write page " + j + " in
									// sector " + sector);
									int startBytes = ((i * 256) + (j)) * 256;
									int stopBytes = startBytes + 256;
									if (stopBytes >= flashData.length)
										stopBytes = flashData.length;
									// System.out.println("from byte " + startBytes + "
									// to byte " + stopBytes);
		
									// set settore e pagina
									byte[] address = new byte[38];
									address[0] = (byte) ((sector >> 8) & 0xFF);
									address[1] = (byte) (sector & 0xFF);
									address[2] = (byte) j;
									boolean result;
									try {
										result = pdm.sendTable(pdmId, PdmConstants.ARG_FPGA_TABLE, address);
										flashResult &= result;
									} catch (Exception e) {
										e.printStackTrace();
										result = false;
										flashResult &= result;
									}
									System.out.println("Set Sector " + sector + " - Page " + j + " result: " + result + "\n");
		
									// send data
									byte[] data = Arrays.copyOfRange(flashData, startBytes, stopBytes);

									try {
										result = pdm.sendTable(pdmId, PdmConstants.ARG_ASIC_TABLE, data);
									} catch (Exception e) {
										e.printStackTrace();
										result = false;
										flashResult &= result;
									}
									System.out.println(
											"Send data to Sector " + sector + " - Page " + j + " result: " + result + "\n");
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
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void flashFixedSize(int sector, int page, int fw) {
		try {
			byte[] flashData;
			int pagesNum, sectorsNum;
			String fileName = "";
			if (fw == 1) {
				fileName = "./flash/fw.bit";
			} else if (fw == 2) {
				fileName = "./flash/fw2.bit";
			}
			File file = new File(fileName);
			if (file != null) {
				Path path = file.toPath();
				flashData = Files.readAllBytes(path);

				pagesNum = flashData.length / 256;
				if ((flashData.length % 256) != 0)
					pagesNum++;

				sectorsNum = pagesNum / 256;
				if ((pagesNum % 256) != 0)
					sectorsNum++;

				System.out.println("File size = " + flashData.length + " bytes. " + sectorsNum + " sectors for "
						+ pagesNum + " pages of 256 bytes needed.");

				boolean flashResult = true;
				for (int i = 0; i < sectorsNum; i++) {
					if (flashResult) {
						for (int j = 0; j < 256; j++) {
							if (flashResult) {
								if ((i * 256) + (j) < pagesNum) {
									// System.out.println("write page " + j + " in
									// sector " + sector);
									int startBytes = ((i * 256) + (j)) * 256;
									int stopBytes = startBytes + 256;
									if (stopBytes >= flashData.length)
										stopBytes = flashData.length;
									// System.out.println("from byte " + startBytes + "
									// to byte " + stopBytes);
		
									// set settore e pagina
									byte[] address = new byte[38];
									address[0] = (byte) ((sector >> 8) & 0xFF);
									address[1] = (byte) (sector & 0xFF);
									address[2] = (byte) j;
									boolean result;
									try {
										result = pdm.sendTable(pdmId, PdmConstants.ARG_FPGA_TABLE, address);
										flashResult &= result;
									} catch (Exception e) {
										e.printStackTrace();
										result = false;
										flashResult &= result;
									}
									System.out.println("Set Sector " + sector + " - Page " + j + " result: " + result + "\n");
		
									// send data
									byte[] data = Arrays.copyOfRange(flashData, startBytes, stopBytes);
									
									if (data.length < 256) {
										byte[] data2 = new byte[256];
										System.arraycopy(data, 0, data2, 0, data.length);
										data = data2;
									}
									
									try {
										result = pdm.sendTable(pdmId, PdmConstants.ARG_ASIC_TABLE, data);
									} catch (Exception e) {
										e.printStackTrace();
										result = false;
										flashResult &= result;
									}
									System.out.println(
											"Send data to Sector " + sector + " - Page " + j + " result: " + result + "\n");
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
				System.out.println("FLASH DONE!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void testEchoFlash(int sector, int page) {
		try {
			
			long start = System.currentTimeMillis();
			
			byte[] flashData;
			int pagesNum, sectorsNum;
			String fileName = "./flash/fw.bit";

			File file = new File(fileName);
			if (file != null) {
				Path path = file.toPath();
				flashData = Files.readAllBytes(path);

				for (int i = 0; i < flashData.length; i++) {
					flashData[i] = (byte) 0;
				}

				pagesNum = flashData.length / 256;
				if ((flashData.length % 256) != 0)
					pagesNum++;

				sectorsNum = pagesNum / 256;
				if ((pagesNum % 256) != 0)
					sectorsNum++;

				System.out.println("File size = " + flashData.length + " bytes. " + sectorsNum + " sectors for "
						+ pagesNum + " pages of 256 bytes needed.");

				boolean flashResult = true;
				for (int i = 0; i < sectorsNum; i++) {
					if (flashResult) {
						for (int j = 0; j < 256; j++) {
							if (flashResult) {
								if ((i * 256) + (j) < pagesNum) {
									// System.out.println("write page " + j + " in
									// sector " + sector);
									int startBytes = ((i * 256) + (j)) * 256;
									int stopBytes = startBytes + 256;
									if (stopBytes >= flashData.length)
										stopBytes = flashData.length;
									// System.out.println("from byte " + startBytes + "
									// to byte " + stopBytes);
		
									// set settore e pagina
									byte[] address = new byte[38];
									address[0] = (byte) ((sector >> 8) & 0xFF);
									address[1] = (byte) (sector & 0xFF);
									address[2] = (byte) j;
									boolean result;
									try {
										result = pdm.sendTable(pdmId, PdmConstants.ARG_FPGA_TABLE, address);
										flashResult &= result;
									} catch (Exception e) {
										e.printStackTrace();
										result = false;
										flashResult &= result;
									}
									System.out.println("Set Sector " + sector + " - Page " + j + " result: " + result + "\n");
		
									// send data
									byte[] data = Arrays.copyOfRange(flashData, startBytes, stopBytes);
									
									if (data.length < 256) {
										byte[] data2 = new byte[256];
										System.arraycopy(data, 0, data2, 0, data.length);
										data = data2;
									}
									
									try {
										result = pdm.sendTable(pdmId, PdmConstants.ARG_ASIC_TABLE, data);
									} catch (Exception e) {
										e.printStackTrace();
										result = false;
										flashResult &= result;
									}
									System.out.println("Send data to Sector " + sector + " - Page " + j + " result: " + result + "\n");
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
				long stop = System.currentTimeMillis();				
				long time = (stop-start) / 1000;

				System.out.println("FLASH DONE in " + time + " seconds");
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void checkFirmwareVersion() {
		performInitialization();
		try {
			byte[] table = pdm.requestTable(pdmId, PdmConstants.ARG_FPGA_TABLE);

			System.out.println("FW TYPE: " + table[10]);
			System.out.println("FW VERSION: " + table[11]);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void checkConnection() {
		boolean res;
		try {
			// init
			res = pdm.init(pdmId, pdmId);
			// res = pdm.init((byte) 0, pdmId);
			System.out.println("INIT PDM: " + res + "\n");
			
			//request data trigger count
			byte[] resp = pdm.requestData(pdmId, PdmConstants.ARG_TRIGGER_MODULE);
			System.out.println("RESPONSE: " + PrintUtils.byteArrayToHexString(resp));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void reset() {
		boolean res;
		try {
			// reset modules
			res = pdm.resetModule(pdmId, PdmConstants.ARG_TRIGGER_MODULE);
			System.out.println("RESET TRIGGER MODULE: " + res + "\n");

			res = pdm.resetModule(pdmId, PdmConstants.ARG_ACQUIRE_MODULE);
			System.out.println("RESET ACQUIRE MODULE: " + res + "\n");

			res = pdm.resetModule(pdmId, PdmConstants.ARG_VARIANCE_MODULE);
			System.out.println("RESET VARIANCE MODULE: " + res + "\n");

			System.out.println("PERFORM FULL RESET: END\n");
		} catch (Exception e) {
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
			while (scan.hasNextLine()) {
				switchTable[i] = (byte) Short.parseShort(scan.nextLine(), 16);
				i++;
			}
			switchTableData = pdm.parseSwitchTable(switchTable);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void openDefaultConfig() {
		loadFpgaTable(new File("./tables/fpga_config.txt"));
		loadAsicTable(new File("./tables/asic_config.txt"));
		loadSwitchTable(new File("./tables/switch_config.txt"));

		loadPdmProperties(new File("./tables/pdm_config.txt"));
	}

	private void loadPdmProperties(File file) {
		try {
			Properties devProps = new Properties();
			FileInputStream in = new FileInputStream(file);
			devProps.load(in);
			in.close();

			String[] mList = devProps.getProperty("SIPM_TEMP_M").split(", ");
			String[] qList = devProps.getProperty("SIPM_TEMP_Q").split(", ");
			sipm_Temp_M = new double[9];
			sipm_Temp_Q = new double[9];
			for (int i = 0; i < 9; i++) {
				sipm_Temp_M[i] = Double.valueOf(mList[i]);
				sipm_Temp_Q[i] = Double.valueOf(qList[i]);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void readAppProperties() {
		try {
			// file properties dei parametri
			Properties devProps = new Properties();
			FileInputStream in = new FileInputStream("./application.properties");
			devProps.load(in);
			in.close();

			thresholdStart = Integer.parseInt(devProps.getProperty("START_THRESHOLD"));
			thresholdEnd = Integer.parseInt(devProps.getProperty("END_THRESHOLD"));
			thresholdDelta = Integer.parseInt(devProps.getProperty("DELTA_THRESHOLD"));

			thresholdDistribution = Integer.parseInt(devProps.getProperty("DISTRIBUTIONS_THRESHOLD"));
			thresholdIterations = Integer.parseInt(devProps.getProperty("DISTRIBUTIONS_ITERATIONS"));

			hkIterations = Integer.parseInt(devProps.getProperty("HK_ITERATIONS"));
			hkInterval = Integer.parseInt(devProps.getProperty("HK_INTERVAL"));

			varIterations = Integer.parseInt(devProps.getProperty("VAR_ITERATIONS"));
			varInterval = Integer.parseInt(devProps.getProperty("VAR_INTERVAL"));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void txtest() {
		try {
			byte[] data = new byte[23]; 
			boolean result = pdm.sendTable(pdmId, PdmConstants.ARG_ASIC_TABLE, data);
			
			System.out.println("end");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void executeCommand(String cmd) throws Exception {
		switch (cmd) {
		case "broadcast":
			performBroadcastTest();
		case "test":
			performTest();
			break;
		case "init":
			performInitialization();
			break;
		case "stairs":
			performStairs();
			break;
		case "distributions":
			performDistributions();
			break;
		case "scientific":
			// TODO
			break;
		case "hk":
			performHk();
			break;
		case "var":
			performVar();
			break;
		case "free running":
			performFreeRunning();
			break;
		case "temp calib 1":
			temperatureCalibration();
			break;
		case "temp calib 2":
			temperatureCalibration2();
			break;
		case "select boot 1":
			selectBoot((byte) 1);
			break;
		case "select boot 2":
			selectBoot((byte) 2);
			break;
		case "reset":
			reset();
			break;
		case "flash":
			flash(64, 0, 1);
			break;
		case "flash 2":
			flash(128, 0, 2);
			break;
		case "flash fixed size":
			flashFixedSize(64, 0, 1);
			break;
		case "flash fixed size 2":
			flashFixedSize(128, 0, 2);
			break;
		case "check firmware":
			checkFirmwareVersion();
			break;
		case "check connection":
			checkConnection();
			break;
		case "start count":
			startCountTest();
			break;
		case "echotestflash":
			testEchoFlash(64, 0);
			break;
		case "txtest":
			txtest();
			break;
		case "exit":
			pdm.releaseDevice();
			System.exit(0);
			break;
		default:
			break;
		}
	}

}
