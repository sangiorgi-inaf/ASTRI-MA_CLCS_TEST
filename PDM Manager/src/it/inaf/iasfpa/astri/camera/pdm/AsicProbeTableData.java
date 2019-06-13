package it.inaf.iasfpa.astri.camera.pdm;

public class AsicProbeTableData {
	
	public enum AnalogModule {
		Out_fs, Out_ssh_LG, Out_ssh_HG, Out_PA_LG, Out_PA_HG
	}
	
	public enum DigitalModule {
		PeakSensing_modeb_LG, PeakSensing_modeb_HG
	}
	
	private byte analogCh, digitalCh;
	private AnalogModule analogMod;
	private DigitalModule digitalMod;
	
	public byte getAnalogCh() {
		return analogCh;
	}
	
	public void setAnalogCh(byte analogCh) {
		this.analogCh = analogCh;
	}
	
	public byte getDigitalCh() {
		return digitalCh;
	}
	
	public void setDigitalCh(byte digitalCh) {
		this.digitalCh = digitalCh;
	}
	
	public AnalogModule getAnalogMod() {
		return analogMod;
	}
	
	public void setAnalogMod(AnalogModule analogMod) {
		this.analogMod = analogMod;
	}
	
	public DigitalModule getDigitalMod() {
		return digitalMod;
	}
	
	public void setDigitalMod(DigitalModule digitalMod) {
		this.digitalMod = digitalMod;
	}

}
