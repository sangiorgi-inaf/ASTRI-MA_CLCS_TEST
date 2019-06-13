package it.inaf.iasfpa.astri.camera.pdm;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import it.inaf.iasfpa.astri.camera.interfaces.SerialPortHandler;
import it.inaf.iasfpa.astri.camera.utils.PrintUtils;

public class PdmControl {
	
	private SerialPortHandler sph;
	private int baudRate, dataBits, stopBits, parity, flowControl, waitTime;
	private int responseTimeout;
	private String portName;
	public static int monitorPeriod;

	public PdmControl() {
		readDeviceProperties();
	}
	
	public void connect(String port) throws Exception {
		sph = new SerialPortHandler(PdmConstants.PORT_OWNER, port, //portName,
				baudRate, dataBits, stopBits, parity, flowControl, SerialPortHandler.TRANSMIT_MODE_BYTE);
	}
	
	public void connect() throws Exception {
		connect(portName);
	}
	
	public void disconnect() {
		sph.close();
	}
	
	public byte[] sendMsg(byte pdmId, byte code) throws Exception {
		return sendMsg(pdmId, code, (byte) 0, new byte[] {}, -1);
	}
	
	public byte[] sendMsg(byte pdmId, byte code, byte arg) throws Exception {
		return sendMsg(pdmId, code, arg, new byte[] {}, -1);
	}
	
	public byte[] sendMsg(byte pdmId, byte code, byte arg, byte[] data) throws Exception {
		return sendMsg(pdmId, code, arg, data, -1);
	}
	
	public byte[] sendMsg(byte pdmId, byte code, byte arg, int fixed_resp_size) throws Exception {
		return sendMsg(pdmId, code, arg, new byte[] {}, fixed_resp_size);
	}
	
	public byte[] sendMsg(byte pdmId, byte code, byte arg, byte[] data, int fixed_resp_size) throws Exception {
		byte[] response;
		byte[] msg = composeMsg(pdmId, code, arg, data);
		
		System.out.println("REQUEST: " + PrintUtils.byteArrayToHexString(msg));
		
		int msgSize = msg.length;
		if ((waitTime > 0) && (msgSize > 16)) { //FRAMMENTAZIONE
			int intero = msgSize / 16;
			int resto = msgSize % 16;
			int last = 0;
			if (resto == 0)
				last = intero-1;
			else
				last = intero;
			for (int i = 0; i < last; i++) {
				sph.transmit(Arrays.copyOfRange(msg, 16*i, 16*(i+1)));
				Thread.sleep(waitTime);
			}
			response = sph.transmitAndReceive(Arrays.copyOfRange(msg, 16*last, msgSize), responseTimeout, fixed_resp_size, (byte) 0x0D);
		} else {
			response = sph.transmitAndReceive(msg, responseTimeout, fixed_resp_size, (byte) 0x0D);
		}
		
		System.out.println("RESPONSE RAW: " + PrintUtils.byteArrayToHexString(response));
		
		byte[] crc = crc16(Arrays.copyOfRange(response, 1, response.length-3));
		if ((crc[0] == response[response.length-3]) && (crc[1] == response[response.length-2])) {
			if (response[6] == PdmConstants.ACK)
				return Arrays.copyOfRange(response, 1, response.length-3);
			else
				throw new Exception("PDM ERROR, NACK RECEIVED");
		} else
			throw new Exception("PDM ERROR, CRC RESPONSE WRONG"); 
	}
	
	private byte[] composeMsg(byte pdmId, byte code, byte arg, byte[] data) {
		int dataSize = data.length;
		int msgSize = 9 + dataSize;
		byte[] msg = new byte[msgSize];
		//SOH
		msg[0] = PdmConstants.SOH;
		//PDM ID
		msg[1] = pdmId;
		//DATA LENGTH
		msg[2] = (byte) ((dataSize & 0x0000ff00) >>> 8);
		msg[3] = (byte) ((dataSize & 0x000000ff));
		//CODE
		msg[4] = code;
		//ARG
		msg[5] = arg;
		//DATA
		System.arraycopy(data, 0, msg, 6, data.length);
		//CRC		
		byte[] crc = crc16(Arrays.copyOfRange(msg, 1, msgSize-3));	    
//		msg[msgSize-3] = (byte) ((crc & 0x0000ff00) >>> 8);
//		msg[msgSize-2] = (byte) ((crc & 0x000000ff));
		msg[msgSize-3] = crc[0];
		msg[msgSize-2] = crc[1];
		//EOH
		msg[msgSize-1] = PdmConstants.EOH;
				
		return msg;
	}
	
	private byte[] crc16(byte[] data) {
		int current_crc_value = 0x0000;
		for (int i = 0; i < data.length; i++) {
			current_crc_value ^= data[i] & 0xFF;
			for (int j = 0; j < 8; j++) {
				if ((current_crc_value & 1) != 0) {
					current_crc_value = (current_crc_value >>> 1) ^ 0xA001;
				} else {
					current_crc_value = current_crc_value >>> 1;
				}
			}
		}
		return new byte[] {(byte) (current_crc_value >> 8), (byte) current_crc_value};
	}
	
	private void readDeviceProperties() {
		try {
			//file properties dei parametri
			Properties devProps = new Properties();
			FileInputStream in = new FileInputStream("./pdm.properties");
			devProps.load(in);
			in.close();
			portName = devProps.getProperty("PDM_PORT");
			baudRate = Integer.parseInt(devProps.getProperty("PDM_BAUD_RATE"));
			dataBits = Integer.parseInt(devProps.getProperty("PDM_DATABITS"));
			stopBits = Integer.parseInt(devProps.getProperty("PDM_STOPBITS"));
			parity = Integer.parseInt(devProps.getProperty("PDM_PARITY"));
			flowControl = Integer.parseInt(devProps.getProperty("PDM_FLOWCONTROL"));
			responseTimeout = Integer.parseInt(devProps.getProperty("PDM_RESP_TIMEOUT"));
//			monitorPeriod = Integer.parseInt(devProps.getProperty("PDM_MONITOR_PERIOD"));
			
			waitTime = Integer.parseInt(devProps.getProperty("PDM_WAIT_TIME"));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
