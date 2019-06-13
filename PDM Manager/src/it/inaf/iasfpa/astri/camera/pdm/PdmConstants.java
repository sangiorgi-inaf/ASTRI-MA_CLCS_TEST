package it.inaf.iasfpa.astri.camera.pdm;

/**
 * @author P. Sangiorgi, IASFPA, sangiorgi@ifc.inaf.it
 *
 */
public class PdmConstants {
	
	public static final String PORT_OWNER = "ASTRI_CAMERA_PDM";

	public static final byte ACK = 0x59;
	public static final byte NACK = 0x4E;
	
	public static final byte SOH = 0x0A;
	public static final byte EOH = 0x0D;

	public static final byte CODE_PDM_INIT = 1;
	public static final byte CODE_RESET_MODULE = 2;
	public static final byte CODE_CLOCK_SELECT = 3;
	public static final byte CODE_START_COUNT = 4;
	public static final byte CODE_SEND_TABLE = 5;
	public static final byte CODE_WRITE_TABLE = 6;
	public static final byte CODE_LOAD_TABLE = 7;
	public static final byte CODE_EXEC_TABLE = 8;
	public static final byte CODE_REQUEST_TABLE = 9;
	public static final byte CODE_REQUEST_DATA = 10;
	public static final byte CODE_SET_SWITCH = 11;
	
	public static final byte ARG_FPGA_TABLE = 1;
	public static final byte ARG_ASIC_TABLE = 2;
	public static final byte ARG_ASIC_PROBE_TABLE = 3;
	//solo per config manager, no protocollo
	public static final byte ARG_SWITCH_TABLE = 4;
	
	public static final byte ARG_ACQUIRE_MODULE = 1;
	public static final byte ARG_VARIANCE_MODULE = 2;
	public static final byte ARG_TRIGGER_MODULE = 3;
	public static final byte ARG_HK_MODULE = 4;
	
	public static final byte CODE_SET_BOOT = 0x0E;
		
}
