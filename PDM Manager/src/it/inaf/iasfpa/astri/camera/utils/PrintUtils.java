package it.inaf.iasfpa.astri.camera.utils;

import java.util.Formatter;

public class PrintUtils {

	public static String byteArrayToHexString(final byte[] array) {
        Formatter formatter = new Formatter();
        for (byte b : array)
        	formatter.format("%02x ", b);
        String result = formatter.toString().toUpperCase();
        formatter.close();
        return result;
    }
}
