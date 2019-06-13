package it.inaf.iasfpa.astri.camera.pdm;

import java.util.Arrays;
import java.util.BitSet;

import it.inaf.iasfpa.astri.camera.pdm.data.HkData;
import it.inaf.iasfpa.astri.camera.pdm.data.VarianceData;
import it.inaf.iasfpa.astri.camera.utils.PrintUtils;

public class Pdm {
	
	private byte[] asicTable = new byte[286];
	private byte[] asicProbeTable = new byte[56];
	private byte[] fpgaTable = new byte[38];
	private byte[] switchTable = new byte[2];
	
	private AsicTableData asicTableData = new AsicTableData();
	private AsicProbeTableData asicProbeTableData = new AsicProbeTableData();
	private FpgaTableData fpgaTableData = new FpgaTableData();
	private SwitchTableData switchTableData = new SwitchTableData();
	
	private HkData hkData = new HkData();
	private VarianceData varData = new VarianceData();
	
	private PdmControl pdmControl = new PdmControl();
	
	public boolean connect(String port) {
		try {
			pdmControl.connect(port);
//				if (configController()) {
//					System.out.println("PDM Connection and Configuration OK");
					return true;
//				} else {
//					System.out.println("PDM Configuration FAILED");
//					releaseDevice();
//					return false;
//				}
		} catch (Exception e) {
			e.printStackTrace();
			releaseDevice();
			return false;
		}
	}
	
	public boolean connect() {
		try {
			pdmControl.connect();
//				if (configController()) {
//					System.out.println("PDM Connection and Configuration OK");
					return true;
//				} else {
//					System.out.println("PDM Configuration FAILED");
//					releaseDevice();
//					return false;
//				}
		} catch (Exception e) {
			e.printStackTrace();
			releaseDevice();
			return false;
		}
	}
	
	public void releaseDevice() {
		pdmControl.disconnect();
	}
	
	public boolean init(byte pdmId, byte id) throws Exception {
		byte[] result = pdmControl.sendMsg(pdmId, PdmConstants.CODE_PDM_INIT, id);
//		System.out.println("CLEAN RESPONSE: " + PrintUtils.byteArrayToHexString(result));
		return true;
	}
	
	public boolean clockSelect(byte pdmId, byte argTable) throws Exception {
		byte[] result = pdmControl.sendMsg(pdmId, PdmConstants.CODE_CLOCK_SELECT, argTable);
//		System.out.println("CLEAN RESPONSE: " + PrintUtils.byteArrayToHexString(result));
		return true;
	}
	
	public boolean resetModule(byte pdmId, byte moduleId) throws Exception {
		byte[] result = pdmControl.sendMsg(pdmId, PdmConstants.CODE_RESET_MODULE, moduleId);
//		System.out.println("CLEAN RESPONSE: " + PrintUtils.byteArrayToHexString(result));
		return true;
	}
	
	public boolean startCount(byte pdmId) throws Exception {
		byte[] result = pdmControl.sendMsg(pdmId, PdmConstants.CODE_START_COUNT);
//		System.out.println("CLEAN RESPONSE: " + PrintUtils.byteArrayToHexString(result));
		return true;
	}
	
	public boolean sendTable(byte pdmId, byte argTable, byte[] table) throws Exception {
		byte[] result = pdmControl.sendMsg(pdmId, PdmConstants.CODE_SEND_TABLE, argTable, table);
//		System.out.println("CLEAN RESPONSE: " + PrintUtils.byteArrayToHexString(result));
		return true;
	}
	
	public boolean writeTable(byte pdmId, byte argTable) throws Exception {
		byte[] result = pdmControl.sendMsg(pdmId, PdmConstants.CODE_WRITE_TABLE, argTable);
//		System.out.println("CLEAN RESPONSE: " + PrintUtils.byteArrayToHexString(result));
		return true;
	}
	
	public boolean loadTable(byte pdmId, byte argTable) throws Exception {
		byte[] result = pdmControl.sendMsg(pdmId, PdmConstants.CODE_LOAD_TABLE, argTable);
//		System.out.println("CLEAN RESPONSE: " + PrintUtils.byteArrayToHexString(result));
		return true;
	}
	
	public boolean execTable(byte pdmId, byte argTable, byte[] table) throws Exception {
		byte[] result = pdmControl.sendMsg(pdmId, PdmConstants.CODE_EXEC_TABLE, argTable, table);
//		System.out.println("CLEAN RESPONSE: " + PrintUtils.byteArrayToHexString(result));
		return true;
	}
	
	public byte[] requestTable(byte pdmId, byte argTable) throws Exception {
		int prefSize = -1;
		switch (argTable) {
		case PdmConstants.ARG_FPGA_TABLE:
			prefSize = 48;
			break;
		case PdmConstants.ARG_ASIC_TABLE:
			prefSize = 296;
			break;
		case PdmConstants.ARG_ASIC_PROBE_TABLE:
			prefSize = 56;
			break;
		default:
			break;
		}
		byte[] resp =  pdmControl.sendMsg(pdmId, PdmConstants.CODE_REQUEST_TABLE, argTable, prefSize);
		//clean response
		byte[] result = Arrays.copyOfRange(resp, 6, resp.length);
//		System.out.println("CLEAN RESPONSE: " + PrintUtils.byteArrayToHexString(result));
		return result;
	}
	
	//TODO: check se size corretti
	public byte[] requestData(byte pdmId, byte argModule) throws Exception {
		int prefSize = -1;
		switch (argModule) {
		case PdmConstants.ARG_ACQUIRE_MODULE:
			prefSize = 266;
			break;
		case PdmConstants.ARG_VARIANCE_MODULE:
			prefSize = 522;
			break;
		case PdmConstants.ARG_TRIGGER_MODULE:
			prefSize = 266;
			break;
		case PdmConstants.ARG_HK_MODULE:
			prefSize = 48;
			break;
		default:
			break;
		}
		byte[] resp = pdmControl.sendMsg(pdmId, PdmConstants.CODE_REQUEST_DATA, argModule, prefSize);
		//clean response
		byte[] result = Arrays.copyOfRange(resp, 6, resp.length);
//		System.out.println("CLEAN RESPONSE: " + PrintUtils.byteArrayToHexString(result));
		
		return result;
	}
	
	public boolean setSwitch(byte pdmId, byte[] switchTable) throws Exception {
		byte[] result = pdmControl.sendMsg(pdmId, PdmConstants.CODE_SET_SWITCH, (byte) 0, switchTable);
//		System.out.println("CLEAN RESPONSE: " + PrintUtils.byteArrayToHexString(result));
		return true;
	}
	
	public boolean setBoot(byte pdmId, byte boot) throws Exception {
		byte[] result = pdmControl.sendMsg(pdmId, PdmConstants.CODE_SET_BOOT, boot);
		return true;
	}
	
	public AsicTableData parseAsicTable(byte[] asicTable) {
		this.asicTable = asicTable;
				
		//creo la stringa di bit
		String tableString = "";
		for (int i=asicTable.length-1; i>=0; i--) {
			tableString+= String.format("%8s", Integer.toBinaryString((short) asicTable[i] & 0xFF)).replace(' ', '0');
		}
		
		//PeakDet SCA
		String pdScaHGString = tableString.substring(306, 307);
//		String pdScaLGString = tableString.substring(307, 308);
		boolean pd = (pdScaHGString.equals("1")? false : true); // && (pdScaLGString.equals("1")? true : false);
		asicTableData.setPeakDetector(pd);
		
		//Slow shaper flag
		String lgSSFlagString = tableString.substring(314, 315);
//		String hgSSFlagString = tableString.substring(319, 320);		
		boolean slowShaper = (lgSSFlagString.equals("1")? true : false); // && (hgSSFlagString.equals("1")? true : false);
		asicTableData.setSlowShaper(slowShaper);
		
		//Shaping time
		String shapingTimeLGString = tableString.substring(315, 318);
//		String shapingTimeHGString = tableString.substring(320, 323);
		
		//reverse
		shapingTimeLGString = new StringBuilder(shapingTimeLGString).reverse().toString();
		byte shapingTime = Byte.parseByte(shapingTimeLGString, 2);
		
		asicTableData.setShapingTime(shapingTime);
		
		//PreAmp output to send to Fast Shaper flag
		String preAmpOutString = tableString.substring(328, 329);		
		boolean preAmpOut = (preAmpOutString.equals("1")? true : false);
		asicTableData.setPreampOut(preAmpOut);
		
		//Dac flag e Dac value
		boolean[] dacFlagArray = new boolean[64];
		short[] dacValueArray = new short[64];
		
		//citiroc 2 (leggendo da sotto trovo prima il citiroc 2)
		for (int i = 0; i < 32; i++) {
			String dacValueString = tableString.substring(331+(i*9), 339+(i*9));
			String dacFlagString = tableString.substring(339+(i*9), 340+(i*9));
			
			dacFlagArray[i+32] = (dacFlagString.equals("1")? true : false);
			dacValueArray[i+32] = Short.parseShort(dacValueString, 2);	
		}
		
		//citiroc 1 (leggendo da sotto trovo dopo il citiroc 1)
		for (int i = 0; i < 32; i++) {
			String dacValueString = tableString.substring(1475+(i*9), 1483+(i*9));
			String dacFlagString = tableString.substring(1483+(i*9), 1484+(i*9));
			dacFlagArray[i] = (dacFlagString.equals("1")? true : false);
			dacValueArray[i] = Short.parseShort(dacValueString, 2);	
		}
		asicTableData.setDacInputFlag(dacFlagArray);
		asicTableData.setDacInputValue(dacValueArray);
		
		//Threshold
		String thresholdString = tableString.substring(1117, 1127);
		short threshold = Short.parseShort(thresholdString, 2);
		asicTableData.setThreshold(threshold);
		
		return asicTableData;
	}
	
	public byte[] createAsicTable(AsicTableData asicTableData) {
		
		//creo la stringa di bit
		String tableString = "";
		for (int i=asicTable.length-1; i>=0; i--) {
			tableString+= String.format("%8s", Integer.toBinaryString((short) asicTable[i] & 0xFF)).replace(' ', '0');
		}
		
		//PeakDet SCA
		String newPdScaString = asicTableData.isPeakDetector()? "00" : "11";	
		tableString = tableString.substring(0, 306) + newPdScaString + tableString.substring(308, 1450) + 
				newPdScaString + tableString.substring(1452, 2288);

		//Slow Shaper flag e Shaping time (con reverse)
		String newShaperString = (asicTableData.isSlowShaper()? "1" : "0");
		
		String shapingTimeLGString = String.format("%3s", Integer.toBinaryString(asicTableData.getShapingTime())).replace(' ', '0');
		shapingTimeLGString = new StringBuilder(shapingTimeLGString).reverse().toString();
		newShaperString += shapingTimeLGString;
				
		tableString = tableString.substring(0, 314) + newShaperString + tableString.substring(318, 319) + newShaperString +
					tableString.substring(323, 1458) + newShaperString + tableString.substring(1462, 1463) + newShaperString +
					tableString.substring(1467, 2288);
		
		//PreAmp output to send to Fast Shaper
		String newPreampOut = (asicTableData.isPreampOut()? "1" : "0");
		tableString = tableString.substring(0, 328) + newPreampOut + tableString.substring(329, 1472) + newPreampOut + tableString.substring(1473, 2288);
		
		//Dac flag e Dac value
		String newDacString = "";
		for (int i = 0; i < 32; i++) {
			newDacString+= String.format("%8s", Integer.toBinaryString(asicTableData.getDacInputValue()[i])).replace(' ', '0') + 
					(asicTableData.getDacInputFlag()[i]? "1" : "0");
		}
//		tableString = tableString.substring(0, 331) + newDacString + tableString.substring(619, 2288);
		tableString = tableString.substring(0, 1475) + newDacString + tableString.substring(1763, 2288);
		
		newDacString = "";
		for (int i = 32; i < 64; i++) {
			newDacString+= String.format("%8s", Integer.toBinaryString(asicTableData.getDacInputValue()[i])).replace(' ', '0') + 
					(asicTableData.getDacInputFlag()[i]? "1" : "0");
					
		}
//		tableString = tableString.substring(0, 1475) + newDacString + tableString.substring(1763, 2288);
		tableString = tableString.substring(0, 331) + newDacString + tableString.substring(619, 2288);
		
		//Threshold
		String newThreshString = String.format("%10s", Integer.toBinaryString(asicTableData.getThreshold())).replace(' ', '0');
		tableString = tableString.substring(0, 1117) + newThreshString + tableString.substring(1127, 2261) + 
				newThreshString + tableString.substring(2271, 2288);

		//riformatto la configurazione in byte
		for (int i = 0; i < asicTable.length; i++) {
			String byteString = tableString.substring(i*8, (i+1)*8);
			short value = Short.parseShort(byteString, 2);
			asicTable[asicTable.length-1-i] = (byte) value;
		}
		
		return asicTable;
	}
	
	public AsicProbeTableData parseAsicProbeTable(byte[] asicProbeTable) {
		//TODO
		return asicProbeTableData;
	}
	
	public byte[] createAsicProbeTable(AsicProbeTableData asicProbeTableData) {
		//TODO (completare)
		asicProbeTable = new byte[56];
		
//		int anCh = asicProbeTableData.getAnalogCh();
//		int digCh = asicProbeTableData.getDigitalCh();
//		
//		BitSet anBit = new BitSet();
//		BitSet digBit = new BitSet();
//		
//		anBit.set(anCh);
//		digBit.set(digCh);
//				
//		long anReg = anBit.toLongArray()[0];
//		long digReg = digBit.toLongArray()[0];
//				
//		switch (asicProbeTableData.getAnalogMod()) {
//		case Out_fs:
//			
//			break;
//		case Out_ssh_LG:
//			
//			break;
//		case Out_ssh_HG:
//			
//			break;
//		case Out_PA_LG:
//			
//			break;
//		case Out_PA_HG:
//			
//			break;
//		default:
//			break;
//		}
//		
//		switch (asicProbeTableData.getDigitalMod()) {
//		case PeakSensing_modeb_LG:
//			
//			break;
//		case PeakSensing_modeb_HG:
//			
//			break;
//		default:
//			break;
//		}
		
		//TEST Sempre ON il canale x del modulo ssh hg
//		asicProbeTable[12] = (byte) 128;  //ch 0 - 7
//		asicProbeTable[13] = 0;  //ch 8 - 15
//		asicProbeTable[14] = 0;  //ch 16 - 23
//		asicProbeTable[15] = 0;  //ch 24 - 31
//		
//		asicProbeTable[40] = 0;  //ch 32 - 39
//		asicProbeTable[41] = 0;  //ch 40 - 47
//		asicProbeTable[42] = 0;  //ch 48 - 55
//		asicProbeTable[43] = 0;  //ch 56 - 63
				
		return asicProbeTable;
	}
	
	public FpgaTableData parseFpgaTable(byte[] fpgaTable) {
		fpgaTableData.setPdmId(fpgaTable[0]);
		fpgaTableData.setHk_en(fpgaTable[1] == 1 ? true : false);
		fpgaTableData.setAsic_table_verify(fpgaTable[2] == 1 ? true : false);
		fpgaTableData.setStand_alone(fpgaTable[3] == 1 ? true : false);
//		fpgaTableData.setHk_samp_period(fpgaTable[4]);
//		fpgaTableData.setExternalClock(fpgaTable[5] == 1 ? true : false);
		fpgaTableData.setRstb_psc((fpgaTable[7] & 0x01) == 1 ? true : false);
		fpgaTableData.setPs_global_trig((fpgaTable[7] & 0x02) == 2 ? true : false);
		fpgaTableData.setRaz_p((fpgaTable[7] & 0x04) == 4 ? true : false);
		fpgaTableData.setVal_evt((fpgaTable[7] & 0x08) == 8 ? true : false);
		fpgaTableData.setReset_pa((fpgaTable[7] & 0x10) == 0x10 ? true : false);
		fpgaTableData.setMajority_in(fpgaTable[8]);
		fpgaTableData.setFilter_out_en(fpgaTable[9] == 1 ? true : false);
		fpgaTableData.setTime_window(fpgaTable[12]);
		
		fpgaTableData.setAcq_valid_cycles(fpgaTable[16]);
		fpgaTableData.setAcq_mux_cycles(fpgaTable[17]);
		fpgaTableData.setAcq_conv_cycles(fpgaTable[18]);
		fpgaTableData.setVar_delay_cycles(fpgaTable[19]);
		fpgaTableData.setAcq_hold_en(fpgaTable[20] == 1 ? true : false);
				
		int tmp = ((int) (fpgaTable[24] & 0xFF) << 8) + (int) (fpgaTable[25] & 0xFF);
		fpgaTableData.setAcq_cycles(tmp);
		tmp = ((int) (fpgaTable[26] & 0xFF) << 8) + (int) (fpgaTable[27] & 0xFF);
		fpgaTableData.setData_offs(tmp);
				
		byte[] trMask = new byte[8];
		for (int i = 0; i < 8; i++) {
			trMask[i] = fpgaTable[30+i];
		}
		fpgaTableData.setTriggerMask(trMask);
		
		return fpgaTableData;
	}
	
	public byte[] createFpgaTable(FpgaTableData fpgaTableData) {
		fpgaTable[0] = fpgaTableData.getPdmId();
		fpgaTable[1] = (byte) (fpgaTableData.isHk_en() ? 1:0);
		fpgaTable[2] = (byte) (fpgaTableData.isAsic_table_verify() ? 1:0);
		fpgaTable[3] = (byte) (fpgaTableData.isStand_alone() ? 1:0);
//		fpgaTable[4] = fpgaTableData.getHk_samp_period();
//		fpgaTable[5] = (byte) (fpgaTableData.isExternalClock() ? 1:0);
		
		fpgaTable[7] = (byte) ((fpgaTableData.isRstb_psc() ? 1:0) + 
				((fpgaTableData.isPs_global_trig() ? 1:0) << 1) +
				((fpgaTableData.isRaz_p() ? 1:0) << 2) +
				((fpgaTableData.isVal_evt() ? 1:0) << 3) +
				((fpgaTableData.isReset_pa() ? 1:0) << 4));	
		
		fpgaTable[8] = fpgaTableData.getMajority_in();
		fpgaTable[9] = (byte) (fpgaTableData.isFilter_out_en()? 1:0);
		
		fpgaTable[12] = fpgaTableData.getTime_window();
		
		fpgaTable[16] = fpgaTableData.getAcq_valid_cycles();
		fpgaTable[17] = fpgaTableData.getAcq_mux_cycles();
		fpgaTable[18] = fpgaTableData.getAcq_conv_cycles();
		fpgaTable[19] = fpgaTableData.getVar_delay_cycles();
		fpgaTable[20] = (byte) (fpgaTableData.isAcq_hold_en()? 1:0);
				
		fpgaTable[24] = (byte) ((fpgaTableData.getAcq_cycles() >> 8) & 0xFF);
		fpgaTable[25] = (byte) (fpgaTableData.getAcq_cycles() & 0xFF);
		fpgaTable[26] = (byte) ((fpgaTableData.getData_offs() >> 8) & 0xFF);
		fpgaTable[27] = (byte) (fpgaTableData.getData_offs() & 0xFF);
		
		byte[] trMask = fpgaTableData.getTriggerMask();
		for (int i = 0; i < 8; i++) {
			fpgaTable[30+i] = trMask[i];	
		}
		
		return fpgaTable;
	}
	
	public SwitchTableData parseSwitchTable(byte[] switchTable) {		
		//acq_en
		boolean tmp = (switchTable[0] & 0x01) == 1 ? true : false;
		switchTableData.setAcq_en(tmp);

		//acq_var_en
		tmp = (switchTable[0] & 0x02) == 2 ? true : false;
		switchTableData.setAcq_var_en(tmp);

		//trig_sel
		byte temp = (byte) ((switchTable[0] & 0x30) >> 4);		
		switchTableData.setTrig_sel(temp);
		
		//acq_enable
		tmp = (switchTable[1] & 0x01) == 1 ? true : false;
		switchTableData.setAcq_enable(tmp);

		//acq_clear
		tmp = (switchTable[1] & 0x02) == 2 ? true : false;
		switchTableData.setAcq_clear(tmp);
		
		//trigger width
		temp = (byte) (switchTable[1] >> 2);
		switchTableData.setWind_mon(temp);

		return switchTableData;
	}
	
	public byte[] createSwitchTable(SwitchTableData switchTableData) {
		switchTable[0] = (byte) ((switchTableData.isAcq_en() ? 1:0) +
				((switchTableData.isAcq_var_en() ? 1:0) << 1) +
				(switchTableData.getTrig_sel() << 4));
		switchTable[1] = (byte) ((switchTableData.isAcq_enable() ? 1:0) +
				((switchTableData.isAcq_clear()? 1:0) << 1) +
				((switchTableData.getWind_mon()) << 2));
		
		return switchTable;
	}
		
}
