package grant.wu.migration;

import java.util.List;


public class VM{
	Integer number;
	String name;
	double requestedMips;
	double requestedMem;
	private PM pm;
	private long lastMigrationTime;
	private long lastAuctionTime;
	private boolean inMigration;
	
	public VM(int vmNumber,String name, double requestedMips){
		this.number = vmNumber;
		this.name = name;
		this.requestedMips = requestedMips;
	}
	
	public VM clone(){
		VM newVm = new VM(number,name,requestedMips);	
		newVm.setMem(this.requestedMem);
		return newVm;
	}
	
	public double getMips(){
		return requestedMips;
	}
	
	public double getMem(){
		return requestedMem;
	}
	
	public void setMem(double v){
		this.requestedMem = v;
	}
	
	public String getName(){
		return name;
	}
	
	public void setPM(PM pm){
		this.pm = pm;
	}
	
	public PM getPM(){
		return pm;
	}
	
	public String getVmInfo(){
		String  reslt = String.format("%3d\t%s\t%.2f\t%.2f", this.number,this.name, this.requestedMips, this.requestedMem);
		return reslt;
	}

	public long getLastMigrationTime() {
		return lastMigrationTime;
	}

	public void setLastMigrationTime(long lastMigrationTime) {
		this.lastMigrationTime = lastMigrationTime;
	}

	public long getLastAuctionTime() {
		return lastAuctionTime;
	}

	public void setLastAuctionTime(long lastAuctionTime) {
		this.lastAuctionTime = lastAuctionTime;
	}

	public boolean isInMigration() {
		return inMigration;
	}

	public void setInMigration(boolean inMigration) {
		this.inMigration = inMigration;
	}

	public int getId() {
		
		return number;
	}
}

