package it.inaf.iasfpa.astri.camera.pdm.data;

import it.inaf.iasfpa.astri.camera.pdm.AsicTableData;

public class PdmConfigData {

	private int hvValue;
	private double tempRef;
	private double[] sipmTemp_m = new double[9];
	private double[] sipmTemp_q = new double[9];
	private int[] peToDac = new int[2];
	
	private AsicTableData asicTableData = new AsicTableData();
	
	public int getHvValue() {
		return hvValue;
	}
	
	public void setHvValue(int hvValue) {
		this.hvValue = hvValue;
	}
	
	public double getTempRef() {
		return tempRef;
	}

	public void setTempRef(double tempRef) {
		this.tempRef = tempRef;
	}

	public double[] getSipmTemp_m() {
		return sipmTemp_m;
	}
	
	public void setSipmTemp_m(double[] sipmTemp_m) {
		this.sipmTemp_m = sipmTemp_m;
	}
	
	public double[] getSipmTemp_q() {
		return sipmTemp_q;
	}
	
	public void setSipmTemp_q(double[] sipmTemp_q) {
		this.sipmTemp_q = sipmTemp_q;
	}

	public AsicTableData getAsicTableData() {
		return asicTableData;
	}

	public void setAsicTableData(AsicTableData asicTableData) {
		this.asicTableData = asicTableData;
	}

	public int[] getPeToDac() {
		return peToDac;
	}

	public void setPeToDac(int[] peToDac) {
		this.peToDac = peToDac;
	}
	
}
