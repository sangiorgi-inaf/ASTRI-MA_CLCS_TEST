package it.inaf.iasfpa.astri.camera.pdm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import it.inaf.iasfpa.astri.camera.pdm.data.HkData;
import it.inaf.iasfpa.astri.camera.pdm.data.VarianceData;

public class PdmUtils {
	
	private static final double q2 = 2639.8;
	private static final double m2 = -13.722;
	
	//MAPPE DI CORRISPONDENZA PIXEL - CANALE
	public static final byte[] PIXEL_FROM_CH = new byte[]{2, 4, 1, 12, 10, 11, 9, 3,
		19, 18, 20, 17, 27, 26, 28, 25,
		34, 35, 33, 36, 42, 43, 41, 44,
		60, 50, 59, 49, 52, 58, 51, 57,
		63, 53, 64, 54, 55, 61, 56, 62,
		46, 47, 45, 48, 38, 39, 37, 40,
		31, 30, 32, 29, 23, 22, 24, 21,
		5, 15, 6, 16, 13, 7, 14, 8};
	
	//a pixel 0 non corrisponde nessun canale
	public static final byte[] CH_FROM_PIXEL = new byte[]{-1, 
			2, 0, 7, 1, 56, 58, 61, 63,
			6, 4, 5, 3, 60, 62, 57, 59,
			11, 9, 8, 10, 55, 53, 52, 54,
			15, 13, 12, 14, 51, 49, 48, 50,
			18, 16, 17, 19, 46, 44, 45, 47,
			22, 20, 21, 23, 42, 40, 41, 43,
			27, 25, 30, 28, 33, 35, 36, 38,
			31, 29, 26, 24, 37, 39, 32, 34};
	
	public static int convertTimeWnd(int value) {
		int time = (int) (Math.pow(2, value)*125);
		return time;
	}
	
	public static int[] convertStairsData(int treshold, byte[] data, double time) {
		int[] result = new int[65];
		result[0] = treshold;
		for (int i = 0; i < data.length/4; i++) {
			int chValue =  (data[i*4] & 0xff) * 16777216 + 
					(data[(i*4)+1] & 0xff) * 65536 + 
					(data[(i*4)+2] & 0xff) * 256 + 
					(data[(i*4)+3] & 0xff);
								
			result[i+1] = (int) (chValue / time);
		}
		return result;
	}
	
	public static int[] convertDistributionData(byte[] data) {
		int[] result = new int[128];

		for (int i = 0; i < data.length/2; i++) {
			int chValue = (data[(i*2)] & 0xff) * 256 + 
					(data[(i*2)+1] & 0xff);
								
			result[i] = chValue;
		}
		return result;
	}
	
	public static int[] convertHkData(byte[] data) {
		int[] result = new int[19];

		for (int i = 0; i < data.length/2; i++) {
			int adcValue = (data[(i*2)] & 0xff) * 256 + 
					(data[(i*2)+1] & 0xff);
								
			result[i] = adcValue / 16;
		}
		result[18]*=16; 
		return result;
	}
	
	public static HkData parseHkData(byte pdmId, int[] adcValues, double[] m, double[] q) {
		HkData hkData = new HkData();
	
		//SiPM temperature
		int vref=1000;
		double[] sipmTemp = new double[10];
		for (int i = 0; i < 9; i++) {
			double tmp = (double) adcValues[i] * vref / 4096;
			sipmTemp[i] = (tmp - q[i]) / m[i];
		}
		hkData.setTemperature(sipmTemp);
		
		//citiroc board temperature
		double tmp = (double) adcValues[10] * 2500 / 4096;
		double temp = (tmp - q2) / m2;
		hkData.setCitirocTemp(temp);
		
		//citiroc currents
		double[] curr = new double[3]; 
		curr[0] = (double) adcValues[11] * 250 / 4096;
		curr[1] = (double) adcValues[12] * 250 / 4096;
		curr[2] = (double) adcValues[13] * 250 / 4096;
		hkData.setCitirocCurr(curr);
		
		//fpga temperature
		hkData.setFpgaTemp(((double) adcValues[16] * 503.975 / 4096) - 273.15);
		
		//fpga currents
		hkData.setFpgaCurr((double) adcValues[18] * 2500 / 4096);
		
		return hkData;
	}
	
	public static double[] convertVarData(byte[] data) {
		double[] result = new double[128];
		
		int[] rawValues = new int[128];
		for (int i = 0; i < data.length/4; i++) {
			rawValues[i] =  (data[i*4] & 0xff) * 16777216 + 
					(data[(i*4)+1] & 0xff) * 65536 + 
					(data[(i*4)+2] & 0xff) * 256 + 
					(data[(i*4)+3] & 0xff);
			
			result[i] = new BigDecimal(rawValues[i]).setScale(2, RoundingMode.HALF_UP).doubleValue();
		}
		return result;
	}
	
//	public static double[] convertVarData(byte[] data, int samples, int offset) {
//		double[] result = new double[192];
//		
//		int[] rawValues = new int[128];
//		for (int i = 0; i < data.length/4; i++) {
//			rawValues[i] =  (data[i*4] & 0xff) * 16777216 + 
//					(data[(i*4)+1] & 0xff) * 65536 + 
//					(data[(i*4)+2] & 0xff) * 256 + 
//					(data[(i*4)+3] & 0xff);
//		}
//		//MEDIE
//		for (int i = 0; i < 64; i++) {
//			double tmp = (double) rawValues[i] / samples + offset;
//			result[i] = new BigDecimal(tmp).setScale(2, RoundingMode.HALF_UP).doubleValue();
//		}
//		//SUM E SUMSQUARES
//		for (int i = 0; i < rawValues.length; i++) {
//			result[i+64] = new BigDecimal(rawValues[i]).setScale(2, RoundingMode.HALF_UP).doubleValue();
//			
//		}
//		return result;
//	}
	
//	public static VarianceData parseVARData(byte[] varRawData) {
//		VarianceData varData = new VarianceData();
//		return varData;
//	}
	
}
