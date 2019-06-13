package it.inaf.iasfpa.astri.camera.pdm.data;

public class HkData {
	
	byte pdmId;
	double[] temperature = new double[10];
	double citirocTemp, fpgaTemp, fpgaCurr;
	double[] citirocCurr = new double[3];
	
	public byte getPdmId() {
		return pdmId;
	}
	
	public void setPdmId(byte pdmId) {
		this.pdmId = pdmId;
	}

	public double[] getTemperature() {
		return temperature;
	}

	public void setTemperature(double[] temperature) {
		this.temperature = temperature;
	}

	public double getCitirocTemp() {
		return citirocTemp;
	}

	public void setCitirocTemp(double citirocTemp) {
		this.citirocTemp = citirocTemp;
	}

	public double getFpgaTemp() {
		return fpgaTemp;
	}

	public void setFpgaTemp(double fpgaTemp) {
		this.fpgaTemp = fpgaTemp;
	}

	public double getFpgaCurr() {
		return fpgaCurr;
	}

	public void setFpgaCurr(double fpgaCurr) {
		this.fpgaCurr = fpgaCurr;
	}

	public double[] getCitirocCurr() {
		return citirocCurr;
	}

	public void setCitirocCurr(double[] citirocCurr) {
		this.citirocCurr = citirocCurr;
	}
	
}