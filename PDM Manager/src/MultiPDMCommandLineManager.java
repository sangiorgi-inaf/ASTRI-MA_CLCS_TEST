import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
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
import it.inaf.iasfpa.astri.camera.pdm.data.PdmConfigData;
import it.inaf.iasfpa.astri.camera.utils.PrintUtils;

public class MultiPDMCommandLineManager {

	Pdm pdm = new Pdm();

	private byte[] switchTable = new byte[2];
	private byte[] fpgaTable = new byte[38];

	private byte[] pdmIdList;
	private int nPdm;
	private HashMap<Byte, PdmConfigData> pdmConfigDataList;

	FpgaTableData fpgaTableData = new FpgaTableData();
	SwitchTableData switchTableData = new SwitchTableData();

	int stairsThresholdStart, stairsThresholdEnd, stairsThresholdDelta;
	int distributionThreshold, distributionIterations;
	int sciIterations, sciInterval;
	double sciThreshold;
	boolean sciOneShot;

	int varThreshold, varIterations, varInterval;
	int hkIterations, hkInterval;

	boolean c13_14Flag, isFreeRunning;

	private BufferedReader br;

	DateFormat df = new SimpleDateFormat("[yyyy_MM_dd_HH_mm_ss_SSS]");
	DecimalFormat decF = new DecimalFormat("#.00");

	public static void main(String[] args) {
		MultiPDMCommandLineManager test = new MultiPDMCommandLineManager();

		test.br = new BufferedReader(new InputStreamReader(System.in));
		String input;

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

	private void performInitialization() {
		try {
			boolean res;

			// carico i parametri di configurazione dell'app
			readAppProperties();

			// carico le tabelle in memoria
			openDefaultConfig();

			fpgaTable = pdm.createFpgaTable(fpgaTableData);
			switchTable = pdm.createSwitchTable(switchTableData);

			fpgaTableData.setPdmId((byte) 0);
			fpgaTable = pdm.createFpgaTable(fpgaTableData);

			// comandi su singola pdm
			for (byte pdmId : pdmIdList) {
				res = pdm.init(pdmId, pdmId);
				System.out.println("INIT PDM: " + res + "\n");

				byte[] asicTable = pdm.createAsicTable(pdmConfigDataList.get(pdmId).getAsicTableData());
				res = pdm.sendTable(pdmId, PdmConstants.ARG_ASIC_TABLE, asicTable);
				System.out.println("SEND ASIC TABLE: " + res + "\n");
			}

			// comandi broadcastabili
			res = pdm.sendTable((byte) 0, PdmConstants.ARG_FPGA_TABLE, fpgaTable);
			System.out.println("SEND FPGA TABLE: " + res + "\n");

			res = pdm.writeTable((byte) 0, PdmConstants.ARG_FPGA_TABLE);
			System.out.println("WRITE FPGA TABLE: " + res + "\n");

			res = pdm.writeTable((byte) 0, PdmConstants.ARG_ASIC_TABLE);
			System.out.println("WRITE ASIC TABLE: " + res + "\n");
			
			res = pdm.loadTable((byte) 0, PdmConstants.ARG_ASIC_TABLE);
			System.out.println("LOAD ASIC TABLE: " + res + "\n");

			// set switch
			res = pdm.setSwitch((byte) 0, switchTable);
			System.out.println("SET SWITCH: " + res + "\n");

			// reset modules
			res = pdm.resetModule((byte) 0, PdmConstants.ARG_TRIGGER_MODULE);
			System.out.println("RESET TRIGGER MODULE: " + res + "\n");

			res = pdm.resetModule((byte) 0, PdmConstants.ARG_ACQUIRE_MODULE);
			System.out.println("RESET ACQUIRE MODULE: " + res + "\n");

			res = pdm.resetModule((byte) 0, PdmConstants.ARG_VARIANCE_MODULE);
			System.out.println("RESET VARIANCE MODULE: " + res + "\n");

			System.out.println("PERFORM FULL INITIALIZATION: END\n");

		} catch (Exception e) {
			System.out.println("PERFORM FULL INITIALIZATION: FAILURE\n");
			e.printStackTrace();
		}
	}

	private void performHk() {
		try {
			long start = System.currentTimeMillis();
			boolean res;

			// preparo i files di output
			File[] files = new File[nPdm];
			PrintWriter[] outs = new PrintWriter[nPdm];
			int j = 0;
			String startTime = df.format(new Date());
			for (byte pdmId : pdmIdList) {
				files[j] = new File("./output/HK/" + startTime + "_" + pdmId + ".txt");
				outs[j] = new PrintWriter(new FileWriter(files[j]), true);

				// intestazione del file
				String header = "TIMETAG ";
				for (int i = 1; i < 20; i++) {
					header += "ADC" + i + " ";
				}
				for (int i = 1; i < 20; i++) {
					header += "T" + i + " ";
				}
				outs[j].println(header);
				j++;
			}

			// itero per numero shot di HK
			for (int k = 0; k < hkIterations; k++) {

				// effettuo richieste a tutte le pdm e colleziono i dati
				String[] dates = new String[nPdm];
				ArrayList<byte[]> resps = new ArrayList<byte[]>();
				j = 0;
				for (byte pdmId : pdmIdList) {
					long startHk = System.currentTimeMillis();
					dates[j] = df.format(new Date()) + " ";
					resps.add(pdm.requestData(pdmId, PdmConstants.ARG_HK_MODULE));
					long stopHk = System.currentTimeMillis();
					System.out.println("Elapsed time per request data hk: " + (stopHk - startHk) + " milliseconds");
					j++;
				}

				// effettuo conversione hk e scrivo su relativo file
				j = 0;
				for (byte pdmId : pdmIdList) {
					int[] adcValues = PdmUtils.convertHkData(resps.get(j));
					HkData hkData = PdmUtils.parseHkData(pdmId, adcValues, pdmConfigDataList.get(pdmId).getSipmTemp_m(),
							pdmConfigDataList.get(pdmId).getSipmTemp_q());

					String output = dates[j];
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

					outs[j].println(output);
					j++;
				}

				Thread.sleep(hkInterval);
			}
			// chiudo tutti i file
			for (int i = 0; i < outs.length; i++) {
				outs[i].close();
			}

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

			// setto la threshold per VAR
			for (byte pdmId : pdmIdList) {
				AsicTableData asicTemp = pdmConfigDataList.get(pdmId).getAsicTableData();
				asicTemp.setThreshold((short) varThreshold);
				byte[] asicTable = pdm.createAsicTable(asicTemp);
				res = pdm.sendTable(pdmId, PdmConstants.ARG_ASIC_TABLE, asicTable);
				System.out.println("SEND ASIC TABLE: " + res + "\n");
			}
			res = pdm.writeTable((byte) 0, PdmConstants.ARG_ASIC_TABLE);
			System.out.println("WRITE ASIC TABLE: " + res + "\n");

			// preparo i files di output
			File[] files = new File[nPdm];
			PrintWriter[] outs = new PrintWriter[nPdm];
			int j = 0;
			String startTime = df.format(new Date());
			for (byte pdmId : pdmIdList) {
				files[j] = new File("./output/VAR/" + startTime + "_" + pdmId + ".txt");
				outs[j] = new PrintWriter(new FileWriter(files[j]), true);

				// intestazione dei file
				String header = "DAC: " + varThreshold + " SAMPLES: " + fpgaTableData.getAcq_cycles() + " OFFSET: "
						+ fpgaTableData.getData_offs() + "\n" + "TimeTag ";
				for (int i = 1; i < 65; i++) {
					header += "Sum_pix_" + i + " ";
				}
				for (int i = 1; i < 65; i++) {
					header += "Sum_Squares_pix_" + i + " ";
				}
				outs[j].println(header);
				j++;
			}

			// itero per numero shot di VAR
			for (int k = 0; k < varIterations; k++) {

				// Avvio i conteggi a tutte le pdm
				if (pdm.startCount((byte) 0)) {
					String date = df.format(new Date()) + " ";

					// richiedo i dati ad ogni pdm e li scrivo su file
					// TODO: check se si ottimizzano i tempi collezionando i
					// dati e scrivendoli dopo
					j = 0;
					for (byte pdmId : pdmIdList) {
						long startVar = System.currentTimeMillis();
						byte[] resp = pdm.requestData(pdmId, PdmConstants.ARG_VARIANCE_MODULE);
						double[] result = PdmUtils.convertVarData(resp);

						// scrivo risultato su file
						String output = date;
						for (int i = 0; i < result.length; i++) {
							output += result[i] + " ";
						}
						outs[j].println(output);
						j++;
						long stopVar = System.currentTimeMillis();
						System.out.println("Elapsed time per request data variance (e scrittura su file): "
								+ (stopVar - startVar) + " milliseconds");
					}
				} else {
					System.out.println("START COUNT FAILED");
				}
				Thread.sleep(varInterval);
			}

			// chiudo tutti i file
			for (int i = 0; i < outs.length; i++) {
				outs[i].close();
			}

			// resetto il modulo a tutte le pdm
			res = pdm.resetModule((byte) 0, PdmConstants.ARG_VARIANCE_MODULE);
			System.out.println("RESET VARIANCE MODULE: " + res + "\n");

			long stop = System.currentTimeMillis();
			System.out.println("VARIANCE Elapsed in " + ((stop - start)) + " milliseconds");

		} catch (Exception e) {
			System.out.println("PERFORM VARIANCE: FAILURE\n");
			e.printStackTrace();
		}
	}

	private void performStairs() {
		try {
			long start = System.currentTimeMillis();
			boolean res;

			int time = PdmUtils.convertTimeWnd(fpgaTableData.getTime_window());
			double time_sec = ((double) time) / 1000;

			int threshold = stairsThresholdStart;

			// preparo i files di output
			File[] files = new File[nPdm];
			PrintWriter[] outs = new PrintWriter[nPdm];
			int j = 0;
			String startTime = df.format(new Date());
			for (byte pdmId : pdmIdList) {
				files[j] = new File("./output/C11_C12/" + startTime + "_" + pdmId + ".txt");
				outs[j] = new PrintWriter(new FileWriter(files[j]), true);

				// intestazione dei file
				String header = "TIMETAG " + "DAC ";
				for (int i = 0; i < 64; i++) {
					header += "CH" + i + " ";
				}
				outs[j].println(header);
				j++;
			}

			// itero finchè non faccio tutte le soglie
			while (threshold <= stairsThresholdEnd) {

				// setto la threshold ad ogni pdm
				for (byte pdmId : pdmIdList) {
					AsicTableData asicTemp = pdmConfigDataList.get(pdmId).getAsicTableData();
					asicTemp.setThreshold((short) threshold);
					byte[] asicTable = pdm.createAsicTable(asicTemp);
					res = pdm.sendTable(pdmId, PdmConstants.ARG_ASIC_TABLE, asicTable);
					System.out.println("SEND ASIC TABLE: " + res + "\n");
				}

				res = pdm.writeTable((byte) 0, PdmConstants.ARG_ASIC_TABLE);
				System.out.println("WRITE ASIC TABLE: " + res + "\n");

				res = pdm.loadTable((byte) 0, PdmConstants.ARG_ASIC_TABLE);
				System.out.println("LOAD ASIC TABLE: " + res + "\n");

				// REQUEST DATA

				if (pdm.startCount((byte) 0)) {

					String date = df.format(new Date()) + " ";

					// richiedo i dati ad ogni pdm e li scrivo su file
					// TODO: check se si ottimizzano i tempi collezionando i
					// dati e scrivendoli dopo
					j = 0;
					for (byte pdmId : pdmIdList) {
						long startStairs = System.currentTimeMillis();
						byte[] resp = pdm.requestData(pdmId, PdmConstants.ARG_TRIGGER_MODULE);
						long stopStairs = System.currentTimeMillis();

						int[] result = PdmUtils.convertStairsData(threshold, resp, time_sec);

						// scrivo risultato su file
						String output = date;
						for (int i = 0; i < result.length; i++) {
							output += result[i] + " ";
						}
						outs[j].println(output);
						j++;

						System.out.println("Elapsed time per request data (e scrittura su file): "
								+ (stopStairs - startStairs) + " milliseconds");
					}
					threshold += stairsThresholdDelta;
				}
			}

			// chiudo tutti i file
			for (int i = 0; i < outs.length; i++) {
				outs[i].close();
			}

			res = pdm.resetModule((byte) 0, PdmConstants.ARG_TRIGGER_MODULE);
			System.out.println("RESET TRIGGER MODULE: " + res + "\n");

			long stop = System.currentTimeMillis();
			System.out.println("C11_C12 Elapsed in " + ((stop - start) / 1000) + " seconds");

		} catch (Exception e) {
			System.out.println("PERFORM C11_C12: FAILURE\n");
			e.printStackTrace();
		}
	}

	private void performScientific() {
		try {
			long start = System.currentTimeMillis();
			boolean res;

			// setto la threshold per gli scientifici
			int j = 0;
			int[] thresholds = new int[nPdm];
			for (byte pdmId : pdmIdList) {
				AsicTableData asicTemp = pdmConfigDataList.get(pdmId).getAsicTableData();
				int[] peToDac = pdmConfigDataList.get(pdmId).getPeToDac();
				thresholds[j] = (int) Math.round((peToDac[1] * (sciThreshold - 1) + peToDac[0]));
				asicTemp.setThreshold((short) thresholds[j]);
				byte[] asicTable = pdm.createAsicTable(asicTemp);

				System.out.println("DAC scritto: " + asicTemp.getThreshold());

				res = pdm.sendTable(pdmId, PdmConstants.ARG_ASIC_TABLE, asicTable);
				System.out.println("SEND ASIC TABLE: " + res + "\n");
				j++;
			}
			res = pdm.writeTable((byte) 0, PdmConstants.ARG_ASIC_TABLE);
			System.out.println("WRITE ASIC TABLE: " + res + "\n");

			res = pdm.loadTable((byte) 0, PdmConstants.ARG_ASIC_TABLE);
			System.out.println("LOAD ASIC TABLE: " + res + "\n");

			// preparo i files di output
			File[] files = new File[nPdm];
			PrintWriter[] outs = new PrintWriter[nPdm];
			j = 0;
			String startTime = df.format(new Date());
			for (byte pdmId : pdmIdList) {
				files[j] = new File("./output/SCI/" + startTime + "_" + pdmId + ".txt");
				outs[j] = new PrintWriter(new FileWriter(files[j]), true);

				// intestazione dei file
				String header = "DAC: " + thresholds[j] + "\n" + "TIMETAG ";
				for (int i = 1; i < 65; i++) {
					header += "HGpix_" + i + " ";
				}
				for (int i = 1; i < 65; i++) {
					header += "LGpix_" + i + " ";
				}
				outs[j].println(header);
				j++;
			}

			for (int i = 0; i < sciIterations; i++) {
				long startSci = 0, stopSci = 0;

				// effettuo richieste a tutte le pdm e colleziono i dati
				String[] dates = new String[nPdm];
				ArrayList<byte[]> resps = new ArrayList<byte[]>();
				j = 0;
				// dates[0] = df.format(new Date());
				for (byte pdmId : pdmIdList) {
					try {
						startSci = System.currentTimeMillis();
						dates[j] = df.format(new Date());
						resps.add(pdm.requestData(pdmId, PdmConstants.ARG_ACQUIRE_MODULE));
						stopSci = System.currentTimeMillis();
						j++;
						System.out.println("Elapsed time per request data: " + (stopSci - startSci) + " milliseconds");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				// effettuo conversione sci e scrivo su relativo file
				j = 0;
				for (byte pdmId : pdmIdList) {
					if ((!allEmpty(resps.get(j))) || ((stopSci - startSci) > 19000)) {
						int[] result = PdmUtils.convertDistributionData(resps.get(j));
						String output = dates[0] + " ";
						for (int k = 0; k < result.length; k++) {
							output += result[k] + " ";
						}
						outs[j].println(output);
						j++;
					}
				}

				Thread.sleep(sciInterval);

				if (sciOneShot) {
					// effettuo il riarmo del trigger
					// One shot OFF
					for (byte pdmId : pdmIdList) {
						fpgaTableData.setPdmId(pdmId);
						fpgaTableData.setAcq_hold_en(false);
						fpgaTable = pdm.createFpgaTable(fpgaTableData);

						res = pdm.sendTable(pdmId, PdmConstants.ARG_FPGA_TABLE, fpgaTable);
						System.out.println("SEND FPGA TABLE: " + res + "\n");
					}
					res = pdm.writeTable((byte) 0, PdmConstants.ARG_FPGA_TABLE);
					System.out.println("WRITE FPGA TABLE: " + res + "\n");

					// One shot ON
					for (byte pdmId : pdmIdList) {
						fpgaTableData.setPdmId(pdmId);
						fpgaTableData.setAcq_hold_en(true);
						fpgaTable = pdm.createFpgaTable(fpgaTableData);

						res = pdm.sendTable(pdmId, PdmConstants.ARG_FPGA_TABLE, fpgaTable);
						System.out.println("SEND FPGA TABLE: " + res + "\n");
					}
					res = pdm.writeTable((byte) 0, PdmConstants.ARG_FPGA_TABLE);
					System.out.println("WRITE FPGA TABLE: " + res + "\n");

					res = pdm.resetModule((byte) 0, PdmConstants.ARG_ACQUIRE_MODULE);
					System.out.println("RESET ACQUIRE MODULE: " + res + "\n");
				}

			}

			// chiudo tutti i file
			for (int i = 0; i < outs.length; i++) {
				outs[i].close();
			}

			res = pdm.resetModule((byte) 0, PdmConstants.ARG_ACQUIRE_MODULE);
			System.out.println("RESET ACQUIRE MODULE: " + res + "\n");

			long stop = System.currentTimeMillis();
			System.out.println("SCI Elapsed in " + ((stop - start) / 1000) + " seconds");

		} catch (Exception e) {
			System.out.println("PERFORM SCI: FAILURE\n");
			e.printStackTrace();
		}
	}

	private boolean allEmpty(byte[] data) {
		boolean result = true;
		for (byte b : data) {
			if (b == 0)
				result &= true;
			else
				result &= false;
		}
		return result;
	}

	// TODO: MULTIMOD
	/*
	 * private void performDistributions() { try { long start =
	 * System.currentTimeMillis(); c13_14Flag = true; boolean res;
	 * 
	 * performInitialization();
	 * 
	 * //file di output File file = new File("./output/C13_C14_" + pdmId + "_" +
	 * df.format(new Date())+".txt"); PrintWriter out = new PrintWriter(new
	 * FileWriter(file), true);
	 * 
	 * //struttura dati per output Map<Integer, Integer>[] hglgMap = new
	 * TreeMap[128]; for (int i = 0; i < hglgMap.length; i++) { hglgMap[i] = new
	 * TreeMap<Integer, Integer>(); }
	 * 
	 * asicTableData.setThreshold((short) thresholdDistribution); asicTable =
	 * pdm.createAsicTable(asicTableData);
	 * 
	 * res = pdm.sendTable(pdmId, PdmConstants.ARG_ASIC_TABLE, asicTable);
	 * System.out.println("SEND ASIC TABLE: " + res + "\n");
	 * 
	 * res = pdm.writeTable(pdmId, PdmConstants.ARG_ASIC_TABLE);
	 * System.out.println("WRITE ASIC TABLE: " + res + "\n");
	 * 
	 * res = pdm.loadTable(pdmId, PdmConstants.ARG_ASIC_TABLE);
	 * System.out.println("LOAD ASIC TABLE: " + res + "\n");
	 * 
	 * // while (c13_14Flag) { for (int i = 0; i < thresholdIterations; i++) {
	 * 
	 * // REQUEST DATA try { // System.out.println("RUNNING ITERATION " + i +
	 * "\n"); long startDistr = System.currentTimeMillis(); byte[] resp =
	 * pdm.requestData(pdmId, PdmConstants.ARG_ACQUIRE_MODULE); long stopDistr =
	 * System.currentTimeMillis();
	 * System.out.println("Elapsed time per request data: " +
	 * (stopDistr-startDistr) + " milliseconds"); //
	 * System.out.println("RESULT at ITERATION " + i + ": \n" +
	 * PrintUtils.byteArrayToHexString(resp) + "\n");
	 * 
	 * int[] result = PdmUtils.convertDistributionData(resp);
	 * 
	 * //scan hg_lg for (int j = 0; j < 128; j++) { if
	 * (hglgMap[j].containsKey(result[j])) { Integer count =
	 * hglgMap[j].get(result[j]); if (count == null) count = 0; count++;
	 * hglgMap[j].put(result[j], count); } else hglgMap[j].put(result[j], 1); }
	 * } catch (Exception e) { e.printStackTrace(); } }
	 * 
	 * res = pdm.resetModule(pdmId, PdmConstants.ARG_ACQUIRE_MODULE);
	 * System.out.println("RESET ACQUIRE MODULE: " + res + "\n");
	 * 
	 * //scrittura su file degli istogrammi Iterator[] it = new Iterator[128];
	 * 
	 * int max = 0; int it_big = 0;
	 * 
	 * for (int i = 0; i < 128; i++) { it[i] = hglgMap[i].keySet().iterator();
	 * int size = hglgMap[i].size(); if (size > max) { max = size; it_big = i; }
	 * }
	 * 
	 * String header = ""; for (int i = 1; i < 65; i++) { header += "HGpix_" + i
	 * + "\t" + "cts" + "\t" + "LGpix_" + i + "\t" + "cts" + "\t"; }
	 * out.println(header);
	 * 
	 * while (it[it_big].hasNext()) { String line = ""; String hgValue = "";
	 * String lgValue = ""; for (int i = 0; i < 64; i++) { try { int value_hg =
	 * (int) it[i].next(); if (value_hg == 0) hgValue = 0 + "\t" + 0; else
	 * hgValue = value_hg + "\t" + hglgMap[i].get(value_hg); } catch
	 * (NoSuchElementException e) { hgValue = 0 + "\t" + 0; } try { int value_lg
	 * = (int) it[i+64].next(); if (value_lg == 0) lgValue = 0 + "\t" + 0; else
	 * lgValue = value_lg + "\t" + hglgMap[i+64].get(value_lg); } catch
	 * (NoSuchElementException e) { lgValue = 0 + "\t" + 0; }
	 * 
	 * line += hgValue + "\t" + lgValue + "\t"; } out.println(line); }
	 * 
	 * out.close();
	 * 
	 * long stop = System.currentTimeMillis();
	 * System.out.println("C13_C14 Elapsed in " + ((stop - start)/1000) +
	 * " seconds");
	 * 
	 * } catch (Exception e) { System.out.println("PERFORM C13_C14: FAILURE\n");
	 * e.printStackTrace(); } }
	 */

	// TODO: MULTIMOD
	/*
	 * private void performFreeRunning() { try { performInitialization();
	 * 
	 * //file di output File fileHK = new File("./output/HK_" + pdmId + "_" +
	 * df.format(new Date())+".txt"); PrintWriter outHK = new PrintWriter(new
	 * FileWriter(fileHK), true);
	 * 
	 * File fileVAR = new File("./output/VAR_" + pdmId + "_" + df.format(new
	 * Date())+".txt"); PrintWriter outVAR = new PrintWriter(new
	 * FileWriter(fileVAR), true);
	 * 
	 * //intestazione del file HK String header = ""; for (int i = 1; i < 20;
	 * i++) { header += "ADC" + i + " "; } for (int i = 1; i < 20; i++) { header
	 * += "T" + i + " "; } outHK.println(header);
	 * 
	 * //intestazione del file VAR header = ""; for (int i = 1; i < 65; i++) {
	 * header += "Mean_pix_" + i + " "; } for (int i = 1; i < 65; i++) { header
	 * += "Sum_pix_" + i + " "; } for (int i = 1; i < 65; i++) { header +=
	 * "Sum_Squares_pix_" + i + " "; } outVAR.println(header);
	 * 
	 * ScheduledFuture threadHK =
	 * Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new
	 * Runnable() {
	 * 
	 * @Override public void run() { try {
	 * System.out.println("faccio hk del free running"); byte[] resp =
	 * pdm.requestData(pdmId, PdmConstants.ARG_HK_MODULE);
	 * 
	 * int[] adcValues = PdmUtils.convertHkData(resp); HkData hkData =
	 * PdmUtils.parseHkData((byte) pdmId, adcValues, sipm_Temp_M, sipm_Temp_Q);
	 * 
	 * String output = ""; for (int i = 0; i < adcValues.length; i++) { output
	 * += adcValues[i] + " "; }
	 * 
	 * double[] sipmTemp = hkData.getTemperature(); for (int i = 0; i <
	 * sipmTemp.length; i++) { output += decF.format(sipmTemp[i]) + " "; }
	 * output += decF.format(hkData.getCitirocTemp()) + " "; output +=
	 * decF.format(hkData.getCitirocCurr()[0]) + " "; output +=
	 * decF.format(hkData.getCitirocCurr()[1]) + " "; output +=
	 * decF.format(hkData.getCitirocCurr()[2]) + " "; output += "0.00 "; output
	 * += "0.00 "; output += decF.format(hkData.getFpgaTemp()) + " "; output +=
	 * "0.00 "; output += decF.format(hkData.getFpgaCurr()) + " ";
	 * 
	 * outHK.println(output); } catch (Exception e) { // TODO Auto-generated
	 * catch block e.printStackTrace(); } } }, 0, 10, TimeUnit.SECONDS);
	 * 
	 * ScheduledFuture threadVAR =
	 * Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new
	 * Runnable() {
	 * 
	 * @Override public void run() { try {
	 * System.out.println("faccio var del free running"); byte[] resp =
	 * pdm.requestData(pdmId, PdmConstants.ARG_VARIANCE_MODULE); double[] result
	 * = PdmUtils.convertVarData(resp, fpgaTableData.getAcq_cycles(),
	 * fpgaTableData.getData_offs()); String output = ""; for (int i = 0; i <
	 * result.length; i++) { output += result[i] + " "; }
	 * outVAR.println(output); pdm.resetModule(pdmId,
	 * PdmConstants.ARG_VARIANCE_MODULE); } catch (Exception e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); } } }, 0, 1,
	 * TimeUnit.SECONDS);
	 * 
	 * } catch (Exception e) {
	 * System.out.println("PERFORM FREE RUNNING: FAILURE\n");
	 * e.printStackTrace(); } }
	 */

	private void reset() {
		boolean res;
		try {
			// reset modules
			res = pdm.resetModule((byte) 0, PdmConstants.ARG_TRIGGER_MODULE);
			System.out.println("RESET TRIGGER MODULE: " + res + "\n");

			res = pdm.resetModule((byte) 0, PdmConstants.ARG_ACQUIRE_MODULE);
			System.out.println("RESET ACQUIRE MODULE: " + res + "\n");

			res = pdm.resetModule((byte) 0, PdmConstants.ARG_VARIANCE_MODULE);
			System.out.println("RESET VARIANCE MODULE: " + res + "\n");

			System.out.println("PERFORM FULL RESET: END\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void hardReset() {
		pdm.releaseDevice();
		if (pdm.connect())
			System.out.println("### Disconnected and Reconnected to UART");
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

	private AsicTableData loadAsicTable(File file) {
		try {
			Scanner scan = new Scanner(file);
			int i = 0;
			byte[] asicTable = new byte[286];
			while (scan.hasNextLine()) {
				asicTable[i] = (byte) Short.parseShort(scan.nextLine(), 16);
				i++;
			}
			return pdm.parseAsicTable(asicTable);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
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
		// FPGA e SWITCH comuni
		loadFpgaTable(new File("./pdm_config/0/fpga_config.txt"));
		loadSwitchTable(new File("./pdm_config/0/switch_config.txt"));

		// parametri specifici per pdm
		pdmConfigDataList = new HashMap<Byte, PdmConfigData>();
		for (byte pdmId : pdmIdList) {
			PdmConfigData cfg = loadPdmConfig(new File("./pdm_config/" + pdmId + "/pdm_config.txt"));
			AsicTableData asicData = new AsicTableData();
			asicData = loadAsicTable(new File("./pdm_config/" + pdmId + "/asic_config.txt"));
			cfg.setAsicTableData(asicData);
			pdmConfigDataList.put(pdmId, cfg);
		}
	}

	private PdmConfigData loadPdmConfig(File file) {
		try {
			Properties devProps = new Properties();
			FileInputStream in = new FileInputStream(file);
			devProps.load(in);
			in.close();

			PdmConfigData config = new PdmConfigData();
			config.setHvValue(Integer.parseInt(devProps.getProperty("HV_VALUE")));
			config.setTempRef(Double.parseDouble(devProps.getProperty("T_REF")));
			String[] mList = devProps.getProperty("SIPM_TEMP_M").split(", ");
			String[] qList = devProps.getProperty("SIPM_TEMP_Q").split(", ");
			double[] sipm_Temp_M = new double[9];
			double[] sipm_Temp_Q = new double[9];
			for (int i = 0; i < 9; i++) {
				sipm_Temp_M[i] = Double.valueOf(mList[i]);
				sipm_Temp_Q[i] = Double.valueOf(qList[i]);
			}
			config.setSipmTemp_m(sipm_Temp_M);
			config.setSipmTemp_q(sipm_Temp_Q);

			String[] peDacList = devProps.getProperty("PE_TO_DAC").split(", ");
			int[] peToDac = new int[2];
			peToDac[0] = Integer.valueOf(peDacList[0]);
			peToDac[1] = Integer.valueOf(peDacList[1]);

			config.setPeToDac(peToDac);

			return config;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public boolean areAllTrue(boolean[] array) {
		for (boolean b : array)
			if (!b)
				return false;
		return true;
	}
	
	private void temperatureCalibration(double tref) {
		double[] m_def = new double[] { 6.4500, 6.4500, 6.4500, 6.4500, 6.4500, 6.4500, 6.4500, 6.4500, 6.4500 };
		double[] qb = new double[] { 514.4616, 514.4616, 514.4616, 514.4616, 514.4616, 514.4616, 514.4616, 514.4616,
				514.4616 };

		int nSample = 100;

		for (byte pdmId : pdmIdList) {
		
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
	}

	private void temperatureCalibration2(double tRef) {
		for (byte pdmId : pdmIdList) {
			double[] sipm_M = new double[9];
			double[] sipm_Q = new double[9];
	
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
				File file = new File("./output/TEMP_CALIB/" + tRef + "_PDM_" + pdmId + "_old_tref.txt");
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
	}

	private void readAppProperties() {
		try {
			// file properties dei parametri
			Properties devProps = new Properties();
			FileInputStream in = new FileInputStream("./application.properties");
			devProps.load(in);
			in.close();

			String[] pdmList = devProps.getProperty("PDM_ID").split(",");
			nPdm = pdmList.length;
			pdmIdList = new byte[nPdm];
			for (int i = 0; i < nPdm; i++) {
				byte id = Byte.valueOf(pdmList[i]);
				pdmIdList[i] = id;
			}

			stairsThresholdStart = Integer.parseInt(devProps.getProperty("START_THRESHOLD"));
			stairsThresholdEnd = Integer.parseInt(devProps.getProperty("END_THRESHOLD"));
			stairsThresholdDelta = Integer.parseInt(devProps.getProperty("DELTA_THRESHOLD"));

			distributionThreshold = Integer.parseInt(devProps.getProperty("DISTRIBUTIONS_THRESHOLD"));
			distributionIterations = Integer.parseInt(devProps.getProperty("DISTRIBUTIONS_ITERATIONS"));

			sciThreshold = Double.parseDouble(devProps.getProperty("SCIENTIFIC_PE_THRESHOLD"));
			sciIterations = Integer.parseInt(devProps.getProperty("SCIENTIFIC_ITERATIONS"));
			sciInterval = Integer.parseInt(devProps.getProperty("SCIENTIFIC_INTERVAL"));
			sciOneShot = Boolean.valueOf(devProps.getProperty("SCIENTIFIC_ONE_SHOT_TRICK"));

			hkIterations = Integer.parseInt(devProps.getProperty("HK_ITERATIONS"));
			hkInterval = Integer.parseInt(devProps.getProperty("HK_INTERVAL"));

			varThreshold = Integer.parseInt(devProps.getProperty("VAR_THRESHOLD"));
			varIterations = Integer.parseInt(devProps.getProperty("VAR_ITERATIONS"));
			varInterval = Integer.parseInt(devProps.getProperty("VAR_INTERVAL"));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void executeCommand(String cmd) throws Exception {
		switch (cmd) {
		case "init":
			performInitialization();
			break;
		case "stairs":
			performStairs();
			break;
		case "distributions":
			// performDistributions();
			break;
		case "scientific":
			performScientific();
			break;
		case "hk":
			performHk();
			break;
		case "var":
			performVar();
			break;
		case "free running":
			// performFreeRunning();
			break;
		case "reset":
			reset();
			break;
		case "hard reset":
			hardReset();
			break;
		case "temp calib 1":
			System.out.println("Enter T REF");
			System.out.print("> ");
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				String input = br.readLine();
				double tRef = Double.parseDouble(input);
				temperatureCalibration(tRef);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case "temp calib 2":
			System.out.println("Enter T REF");
			System.out.print("> ");
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				String input = br.readLine();
				double tRef = Double.parseDouble(input);
				temperatureCalibration2(tRef);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
