package grant.wu.trading;

import grant.wu.migration.VM;

public class Goods {
	private VM vm;
	private double reservedPrice;
	private double efficiency;
	
	public Goods(){
		
	}

	public VM getVm() {
		return vm;
	}

	public void setVm(VM vm) {
		this.vm = vm;
	}

	public double getReservedPrice() {
		return reservedPrice;
	}

	public void setReservedPrice(double reservedPrice) {
		this.reservedPrice = reservedPrice;
	}

	public double getEfficiency() {
		return efficiency;
	}

	public void setEfficiency(double efficiency) {
		this.efficiency = efficiency;
	}

}
