package it.inaf.iasfpa.astri.camera.pdm;

public class FpgaTableData {
	
	private boolean hk_en, asic_table_verify, stand_alone, rstb_psc, ps_global_trig, raz_p, val_evt, reset_pa, filter_out_en, acq_hold_en;
	private byte pdmId, majority_in, time_window, acq_valid_cycles, acq_mux_cycles, acq_conv_cycles, var_delay_cycles;
	private int acq_cycles, data_offs;
	
//	externalClock
//	hk_samp_period
	
	private byte[] triggerMask = new byte[8];
	
	public boolean isHk_en() {
		return hk_en;
	}
	
	public void setHk_en(boolean hk_en) {
		this.hk_en = hk_en;
	}
	
	public boolean isAsic_table_verify() {
		return asic_table_verify;
	}
	
	public void setAsic_table_verify(boolean asic_table_verify) {
		this.asic_table_verify = asic_table_verify;
	}
	
	public boolean isStand_alone() {
		return stand_alone;
	}
	
	public void setStand_alone(boolean stand_alone) {
		this.stand_alone = stand_alone;
	}
	
//	public boolean isExternalClock() {
//		return externalClock;
//	}
//	
//	public void setExternalClock(boolean externalClock) {
//		this.externalClock = externalClock;
//	}
	
	public boolean isRstb_psc() {
		return rstb_psc;
	}
	
	public void setRstb_psc(boolean rstb_psc) {
		this.rstb_psc = rstb_psc;
	}
	
	public boolean isPs_global_trig() {
		return ps_global_trig;
	}
	
	public void setPs_global_trig(boolean ps_global_trig) {
		this.ps_global_trig = ps_global_trig;
	}
	
	public boolean isRaz_p() {
		return raz_p;
	}
	
	public void setRaz_p(boolean raz_p) {
		this.raz_p = raz_p;
	}
	
	public boolean isVal_evt() {
		return val_evt;
	}
	
	public void setVal_evt(boolean val_evt) {
		this.val_evt = val_evt;
	}
	
	public boolean isReset_pa() {
		return reset_pa;
	}
	
	public void setReset_pa(boolean reset_pa) {
		this.reset_pa = reset_pa;
	}
	
	public boolean isFilter_out_en() {
		return filter_out_en;
	}
	
	public void setFilter_out_en(boolean filter_out_en) {
		this.filter_out_en = filter_out_en;
	}
	
	public boolean isAcq_hold_en() {
		return acq_hold_en;
	}
	
	public void setAcq_hold_en(boolean acq_hold_en) {
		this.acq_hold_en = acq_hold_en;
	}
	
	public byte getPdmId() {
		return pdmId;
	}
	
	public void setPdmId(byte pdmId) {
		this.pdmId = pdmId;
	}
	
//	public byte getHk_samp_period() {
//		return hk_samp_period;
//	}
//	
//	public void setHk_samp_period(byte hk_samp_period) {
//		this.hk_samp_period = hk_samp_period;
//	}
	
	public byte getMajority_in() {
		return majority_in;
	}
	
	public void setMajority_in(byte majority_in) {
		this.majority_in = majority_in;
	}
	
	public byte getTime_window() {
		return time_window;
	}
	
	public void setTime_window(byte time_window) {
		this.time_window = time_window;
	}
	
	public byte getAcq_valid_cycles() {
		return acq_valid_cycles;
	}
	
	public void setAcq_valid_cycles(byte acq_valid_cycles) {
		this.acq_valid_cycles = acq_valid_cycles;
	}
	
	public byte getAcq_mux_cycles() {
		return acq_mux_cycles;
	}
	
	public void setAcq_mux_cycles(byte acq_mux_cycles) {
		this.acq_mux_cycles = acq_mux_cycles;
	}
	
	public byte getAcq_conv_cycles() {
		return acq_conv_cycles;
	}
	
	public void setAcq_conv_cycles(byte acq_conv_cycles) {
		this.acq_conv_cycles = acq_conv_cycles;
	}
	
	public byte getVar_delay_cycles() {
		return var_delay_cycles;
	}
	
	public void setVar_delay_cycles(byte var_delay_cycles) {
		this.var_delay_cycles = var_delay_cycles;
	}
	
	public int getAcq_cycles() {
		return acq_cycles;
	}
	
	public void setAcq_cycles(int acq_cycles) {
		this.acq_cycles = acq_cycles;
	}
	
	public int getData_offs() {
		return data_offs;
	}
	
	public void setData_offs(int data_offs) {
		this.data_offs = data_offs;
	}

	public byte[] getTriggerMask() {
		return triggerMask;
	}

	public void setTriggerMask(byte[] triggerMask) {
		this.triggerMask = triggerMask;
	}
	
}
