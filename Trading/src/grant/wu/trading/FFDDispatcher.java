package grant.wu.trading;

import grant.wu.migration.PM;
import grant.wu.migration.VM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*iFFd,

Sorting accoding to the efficiency, decreasingly
target util rate 1.0

modified best fit
90%, 

select the VM from the lowest utilized servers and find a feasible server for each VM by FFD

multi-thread :

PM thread can be still usable

there is no market thread, but there is a centrol dispatcher

PMAgent is the PMs that can be handled

VM numbers can be changed with time
*/
public class FFDDispatcher extends Thread {
	//the PMs in the order of decreasing efficiency
	private List<PmAgent> pmList = new ArrayList<PmAgent>();
	
	private PM pmDispatcher;
	//vms to be re-assigned
	private List<VM> vmList = new ArrayList<VM>();
	
	private Callback _callback;
	
	private int tradeNumber;

	private int tradingInterval;

	private int migrations = 0;
	
	private HashMap<Integer,ArrayList<VM>> migrationPlan = new HashMap<Integer, ArrayList<VM>>();

	
	public FFDDispatcher(Callback callback){
		_callback = callback;		
	}
	
	private void optimize(){
		vmList.clear();
		//add the new created VMs in the head of vmlist
		addNewVMs();
				
		//pick the vms from the least efficient PMs, with the bigger size in the front for the same PM
		addExistingVMs();
		
		//apply the first fit algorithm to assign the VMs
		ffdAssign();
		
	}
	@Override
	public void run(){
		for ( PmAgent agent : pmList){
			agent.start();
		}
		int i=0;
		mySleep(500);
		print("ffd begin");
		while(i++ < getTradeNumber()){
			optimize();
			_callback.callback();
			mySleep(tradingInterval);
		}
		stopAllPmAgent();
		waitFinishPmAgent();
		print("ffd exit");
	}
	
	private void mySleep(int i) {
		try {
			sleep(i);
		} catch (InterruptedException e) {			
		}
		
	}
	
	private int getTradeNumber() {
		
		return tradeNumber;
	}

	private void stopAllPmAgent() {
		for (PmAgent agent : pmList) {
			agent.setStopRun(true);
		}
	}
	
	private void waitFinishPmAgent() {
		for ( PmAgent agent : pmList){
			try {
				agent.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private  void print(String s){
		System.out.println(s);
	}
	
/*
	private void ffdAssign() {
		if (vmList.size()==0 || pmList.size()==0) return;
		
		if (!isMigrationOver()) return;
	
		
		
		for( int i=0; i< vmList.size();i++){
			VM vm = vmList.get(i);
			vmSizes[i] = vm.getMips();
			vminds[i] = i;			
		}		
		
		for( int j=0; j< pmList.size(); j++){
			PmAgent agent = pmList.get(j); 
			double leftCPU = agent.getPm().getCPU() * agent.getPm().getUtilizationCPU();
			double leftMem = agent.getPm().getMem() * agent.getPm().getUtilizationMem();
			if (agent.canHold(vm)){
				agent.startMigrateIn(vm);
				migrations++;
				break;
			}				
		}
	}
	*/
	
	private void ffdAssign() {
		if (vmList.size()==0 || pmList.size()==0) return;
		
		for( int i=0; i< vmList.size();i++){
			VM vm = vmList.get(i);
			if (vm.isInMigration()) continue;
 			for( int j=0; j< pmList.size(); j++){
				PmAgent agent = pmList.get(j); 				
				PM oldPm = vm.getPM();
				if (oldPm.getId()!= agent.getPm().getId() 
						&& (oldPm.getCPU() < agent.getPm().getCPU() || oldPm.getId()==PmAgent.DISPATCHER_PM_NUM )
						&& agent.getStatus() != PmAgent.STATUS_MIGRATING_IN
						&& agent.canHold(vm) 
						 ){
					agent.startMigrateIn(vm);
					migrations++;
					break;
				}				
			}			
		}					
	}
	
	private boolean isMigrationOver() {
		return migrationPlan.size() > 0;		
	}
	
	private void addExistingVMs() {
		for(int i=pmList.size()-1; i>=0;i--){
			//addInOrderedVmList(pmList.get(i).getVMs());
			vmList.addAll(pmList.get(i).getVMs());
		}		
	}

	private void addInOrderedVmList(List<VM> vMs) {
		for(int i=0; i<vMs.size()-1;i++){
			addInOrderedVmList(vMs.get(i));
		}
		
	}

	private void addNewVMs() {
		int newVmCount = pmDispatcher.getVmCount();
		for (int i= 0;i<newVmCount;i++){
			//addInOrderedVmList(pmDispatcher.getVm(i));
			vmList.add(pmDispatcher.getVm(i));
		}
	}

	private void addInOrderedVmList(VM vm) {
		double cpu = vm.getMips();
		for(int i=0;i<vmList.size();i++){
			if(cpu > vmList.get(i).getMips()){
				vmList.add(i, vm);
				return;
			}
		}
		//add to the tail
		vmList.add(pmList.size(), vm);
		
	}

	public PM getPmDispatcher() {
		return pmDispatcher;
	}

	public void setPmDispatcher(PM pmDispatcher) {
		this.pmDispatcher = pmDispatcher;
	}
	
	public void addPmAgent(PmAgent agent){
		double agentEfficiency = agent.getPmPotentEfficiency();
		for(int i=0;i<pmList.size();i++){
			if(agentEfficiency > pmList.get(i).getPmPotentEfficiency()){
				pmList.add(i, agent);
				return;
			}
		}
		//add to the tail
		pmList.add(pmList.size(), agent);
	}

	public void setTradeNumber(int tradeNumber) {
		this.tradeNumber = tradeNumber;
	}

	public void setTradingInterval(int i) {
		tradingInterval = i;
		
	}

	public int getMigration() {		
		return migrations;
	}
}
