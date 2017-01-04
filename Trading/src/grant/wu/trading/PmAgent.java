package grant.wu.trading;

import grant.wu.migration.Lock;
import grant.wu.migration.PM;
import grant.wu.migration.VM;
import grant.wu.network.NetworkCostCalculator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;


/*
 * This class define the behavior of PM in a trading market
 * It has it's own thread to do the following things cyclically
 * 1) check resource usage; 
 * 2) sell or bid; 
 * 3) migrate 
 */
public class PmAgent extends Thread {
	public static final int DISPATCHER_PM_NUM = 10000000;
	public static List vmRemoveList = new ArrayList<VM>();
	private static final int STATUS_IDLE = 0;
	private static final int STATUS_BIDDING = 1;
	private static final int STATUS_SELLING = 2;
	static final int STATUS_MIGRATING_IN = 3;
	private static final int STATUS_MIGRATING_OUT = 4;
	
	public static int vmMigrationDuration = 400;
	private boolean stopRun = false;
	
	private PM pm;
	
	private VmMarket market; 
	
	private int currentTradeNo;
	private int localTradeNo;
	private  int _status;
	private VM vmInMigration;
	private Date migrationStart;
	
	private boolean usePotentialEfficieny = true;
	private boolean networkEnergyAware = false;
	private grant.wu.network.NetworkCostCalculator netCostCalc;
	private int vmAssign[];
	private boolean networkAware;
	private boolean createNewVMs;
	
	public PmAgent(boolean networkAware){
		setStatus(STATUS_IDLE);
		this.networkAware = networkAware;
	}

	public PM getPm() {
		return pm;
	}

	public void setPm(PM pm) {
		this.pm = pm;
	}

	public VmMarket getMarket() {
		return market;
	}

	public void setMarket(VmMarket market) {
		if (this.market != null){
			if (!market.unRegisterPmAgent(this))
				return;
		}
		this.market = market;
		market.registerPmAgent(this);
	}
	
	private void chooseMarket(){
		if (this.markets!=null && this.markets.size()>0){			
			if (localTradeNo%100==99){
				int chosenInx = new Random(new Date().getTime()).nextInt(this.markets.size());
				if (markets.get(chosenInx)!=market && !markets.get(chosenInx).isOver()){
					setMarket(markets.get(chosenInx));
				}
			}
		}
	}
	
	public void notifyBidResult(int tradeNo, VM vm, PmAgent target){
		
		if (tradeNo!=currentTradeNo || target != this){
			clearStatus();
		}
		else if (vm.getPM().getId() != pm.getId()){			
			print("notify " + vm.getName() + " to moved to "+pm.getId());
			vmInMigration = vm;
			migrationStart = new Date();		
			setStatus( STATUS_MIGRATING_IN );
			vm.setInMigration(true);
		}
	}
	
	public void startMigrateIn(VM vm){			
		if (vm.getPM().getId() != pm.getId()){			
			print("start to migrate " + vm.getName() + " to "+pm.getId());
			vmInMigration = vm;
			migrationStart = new Date();	
			setStatus( STATUS_MIGRATING_IN );
			vm.setInMigration(true);
		}
	}
	
	public void setVmAssign(int assign[]){
		vmAssign = assign;
	}
		
	private void clearStatus() {
		if (vmInMigration==null){
			setStatus( STATUS_IDLE );
			currentTradeNo = -1;
		}
		
	}

	@Override
	public void run() {
		print("PM agent begin:" + pm.getId());
		int i=0;
		while(!isStopRun()){
			localTradeNo++;
			checkMigration();
			chooseMarket();
			if (market!=null)
				sell();
			mySleep(10);
		}
		print("PM agent exit:" + pm.getId());
	}
	
	
	private void print(String s){
		System.out.println(s);
	}

	

	private void mySleep(int i) {
		try {
			sleep(i);
		} catch (InterruptedException e) {			
		}
		
	}

	private void checkMigration() {
		if (vmInMigration!=null && migrationStart!=null){
			int curVmMigrationDuration = vmMigrationDuration;
			int fromPM = vmInMigration.getPM().getId();
			if (vmMigrationDuration==-1){
				curVmMigrationDuration = (int) (vmInMigration.getMem()/4.0);
			}
			if (fromPM == DISPATCHER_PM_NUM && isCreateNewVMs())
				curVmMigrationDuration = 10;
			//print("check migration " + pm.getId());
			Date now = new Date();			
			
			if (pm.getId()==DISPATCHER_PM_NUM){
				vmInMigration.getPM().removeVm(vmInMigration);
				print("remove vm " + vmInMigration.getName());
				changeVmLocation(vmInMigration.getId(),-1);
				vmInMigration.setInMigration(false);
				vmInMigration = null;
				migrationStart = null;
				setStatus ( STATUS_IDLE );
				
			}
			else if ( now.getTime() - migrationStart.getTime() > curVmMigrationDuration ){ 
					
					vmInMigration.getPM().removeVm(vmInMigration);				
					pm.addVm(vmInMigration);
					vmInMigration.setInMigration(false);
					print("migration " + vmInMigration.getName()+" from "+fromPM + " to " + pm.getId());					
					changeVmLocation(vmInMigration.getId(),pm.getId());
					vmInMigration = null;
					migrationStart = null;
					setStatus ( STATUS_IDLE );
					
			}
			
		}
	}
	
	private void changeVmLocation(int vm, int pm){
		//getVmAssignLock().lock();
		vmAssign[vm] = pm;
		//getVmAssignLock().unlock();
	}

	private int lastSellIndex = 0;
	private boolean currentCostAware = true;
	private List<VmMarket> markets = new ArrayList<VmMarket>();
	private void sell() {
				
		if (pm.getId()==DISPATCHER_PM_NUM){
			Goods vmToSell = new Goods();
									
			VM selVM = pm.getSmallestVm();
			if (selVM!=null){
				vmToSell.setVm(selVM);
				vmToSell.setEfficiency(0);
				vmToSell.setReservedPrice(Double.MIN_VALUE);
				this.currentTradeNo = market.addGoods(vmToSell);
			}
		}
		if (getStatus() == STATUS_IDLE && market.isCollectingGoods()) {
			Goods vmToSell = new Goods();
			
			int inx = lastSellIndex;
			lastSellIndex++;
			if (lastSellIndex >= pm.getVmCount())
				lastSellIndex = 0;
		
			VM selVM = pm.getVm(inx);
			
			if (selVM!=null && !selVM.isInMigration()){
				vmToSell.setVm(selVM);
				vmToSell.setEfficiency(getPmPotentEfficiency());
				vmToSell.setReservedPrice(getReservedPrice(selVM));
				this.currentTradeNo = market.addGoods(vmToSell);
				setStatus ( STATUS_SELLING );
			}
		}
		
	}
	
	
	
	public double bid(int tradeNo, VM vm){
		if (pm.getId()==DISPATCHER_PM_NUM){
			if(vmRemoveList.indexOf(vm) >= 0){
				this.currentTradeNo = tradeNo;
				return Double.MAX_VALUE;
			}else
				return Double.MIN_VALUE;
		}
		if(getStatus() !=STATUS_MIGRATING_IN && vm.getPM().getId()!=pm.getId() && canHold(vm)){			
			
			
			pm.getLock().lock();
			double beforeEnergy = pm.getEnergy();
			PM oldPm = vm.getPM();
			pm.addVmWithoutLock(vm);
			double afterEnergy = pm.getEnergy();
			pm.removeVmWithoutLock(vm);
			pm.getLock().unlock();
			vm.setPM(oldPm);
			getVmAssignLock().lock();
			vmAssign[vm.getId()] = pm.getId();
			double networkCost = netCostCalc.getTotalNetworkCost(vmAssign, vm.getId());
			vmAssign[vm.getId()] = oldPm.getId();
			getVmAssignLock().unlock();
			double cost = afterEnergy - beforeEnergy;
			double revenue = vm.getMem() + vm.getMips();
			this.currentTradeNo = tradeNo;
			cost = cost + revenue/getPmPotentEfficiency() ;
			if (networkAware) cost += networkCost ;
			//cost =  networkCost;
			if (currentCostAware) cost += getCurrentCost(revenue);
			return revenue - cost;
		}
		return Double.MIN_VALUE;
	}
	
	private Lock getVmAssignLock(){
		return Lock.getLock("vmAssign");
	}

	public boolean canHold(VM vm) {
		if (getStatus() == STATUS_MIGRATING_IN) return false;
		pm.getLock().lock();
		double availCpu = pm.getCPU() * ( 1- pm.getUtilizationCPU());
		double availMem = pm.getMem() * ( 1- pm.getUtilizationMem());
		pm.getLock().unlock();
		return (availCpu >= vm.getMips() && availMem >= vm.getMem());	
		
	}
		
	private double getReservedPrice(VM selVM) {
		double revenue = selVM.getMem() + selVM.getMips();
		//double cost = revenue/getPmRevenue() * pm.getEnergy();
		pm.getLock().lock();
		double curCost = pm.getEnergy();
		pm.removeVmWithoutLock(selVM);
		double remCost = pm.getEnergy();
		pm.addVmWithoutLock(selVM);
		pm.getLock().unlock();
		double networkCost = netCostCalc.getTotalNetworkCost(vmAssign, selVM.getId());
		double cost = (curCost - remCost) + revenue/getPmPotentEfficiency() ;
		if (networkAware) cost += networkCost;
		//double cost =  networkCost;
		if (currentCostAware) cost += getCurrentCost(revenue);
		return revenue - cost;
	}

	private double getPmEfficiency() {		
		double energy = this.pm.getEnergy();//current energy
		double revenue = getPmRevenue(); //current efficiency
		return revenue/energy;
	}
	
	private double getCurrentCost(double revenue){
		return ( revenue/getPmEfficiency() ) / 10.0;
	}
	
	public double getPmPotentEfficiency() {
		if (pm.getId()== DISPATCHER_PM_NUM)
			return 0.0000001;
		double energy = pm.getMaxEnergy();
		double revenue = getPmMaxRevenue(); 
		return revenue/energy;
	}

	private double getPmRevenue() {
		pm.getLock().lock();
		double r =  this.pm.getCPU() * this.pm.getUtilizationCPU()
				+ this.pm.getMem() * this.pm.getUtilizationMem();
		pm.getLock().unlock();
		return r;
	}
	
	private double getPmMaxRevenue() {
		return this.pm.getCPU() + this.pm.getMem();
	}

	protected synchronized int getStatus() {
		return _status;
	}

	protected synchronized void setStatus(int _status) {
		this._status = _status;
	}

	public boolean isStopRun() {
		return stopRun;
	}

	public synchronized void setStopRun(boolean stopRun) {
		this.stopRun = stopRun;
	}

	public boolean isNetworkEnergyAware() {
		return netCostCalc != null;
	}	

	public NetworkCostCalculator getNetCostCalc() {
		return netCostCalc;
	}

	public void setNetCostCalc(NetworkCostCalculator netCostCalc) {
		this.netCostCalc = netCostCalc;
	}
	
	public List<VM> getVMs(){
		ArrayList<VM> vms = new ArrayList<VM>();
		for(int i=0;i<pm.getVmCount();i++){
			vms.add(pm.getVm(i));
		}
		return vms;
	}

	public boolean isCreateNewVMs() {
		return createNewVMs;
	}

	public void setCreateNewVMs(boolean createNewVMs) {
		this.createNewVMs = createNewVMs;
	}

	public void addMarket(VmMarket market) {
		if (markets.indexOf(market)<0)
			markets.add(market);		
	}
	
}
