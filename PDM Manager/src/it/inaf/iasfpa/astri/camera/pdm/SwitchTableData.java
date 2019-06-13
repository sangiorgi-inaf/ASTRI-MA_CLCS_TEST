package it.inaf.iasfpa.astri.camera.pdm;

public class SwitchTableData {
	
	private boolean acq_en, acq_var_en, acq_enable, acq_clear;
	private byte trig_sel, wind_mon;
	
	public boolean isAcq_en() {
		return acq_en;
	}
	
	public void setAcq_en(boolean acq_en) {
		this.acq_en = acq_en;
	}
	
	public boolean isAcq_var_en() {
		return acq_var_en;
	}
	
	public void setAcq_var_en(boolean acq_var_en) {
		this.acq_var_en = acq_var_en;
	}
	
	public boolean isAcq_enable() {
		return acq_enable;
	}
	
	public void setAcq_enable(boolean acq_enable) {
		this.acq_enable = acq_enable;
	}
	
	public boolean isAcq_clear() {
		return acq_clear;
	}
	
	public void setAcq_clear(boolean acq_clear) {
		this.acq_clear = acq_clear;
	}
	
	public byte getTrig_sel() {
		return trig_sel;
	}
	
	public void setTrig_sel(byte trig_sel) {
		this.trig_sel = trig_sel;
	}

	public byte getWind_mon() {
		return wind_mon;
	}

	public void setWind_mon(byte wind_mon) {
		this.wind_mon = wind_mon;
	}

}
