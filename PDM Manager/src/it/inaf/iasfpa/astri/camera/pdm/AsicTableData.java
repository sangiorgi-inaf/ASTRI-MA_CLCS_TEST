package it.inaf.iasfpa.astri.camera.pdm;

public class AsicTableData {
	
	private short threshold;
	private byte shapingTime;
	private boolean slowShaper, peakDetector, preampOut;
	private short[] dacInputValue = new short[64];
	private boolean[] dacInputFlag = new boolean[64];
	
	public short getThreshold() {
		return threshold;
	}
	
	public void setThreshold(short threshold) {
		this.threshold = threshold;
	}
	
	public byte getShapingTime() {
		return shapingTime;
	}
	
	public void setShapingTime(byte shapingTime) {
		this.shapingTime = shapingTime;
	}

	public boolean isSlowShaper() {
		return slowShaper;
	}

	public void setSlowShaper(boolean slowShaper) {
		this.slowShaper = slowShaper;
	}

	public boolean isPeakDetector() {
		return peakDetector;
	}
	
	public void setPeakDetector(boolean peakDetector) {
		this.peakDetector = peakDetector;
	}
	
	public short[] getDacInputValue() {
		return dacInputValue;
	}
	
	public void setDacInputValue(short[] dacInputValue) {
		this.dacInputValue = dacInputValue;
	}
	
	public boolean[] getDacInputFlag() {
		return dacInputFlag;
	}
	
	public void setDacInputFlag(boolean[] dacInputFlag) {
		this.dacInputFlag = dacInputFlag;
	}
	
	public boolean isPreampOut() {
		return preampOut;
	}

	public void setPreampOut(boolean preampOut) {
		this.preampOut = preampOut;
	}
	
}
