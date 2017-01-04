package grant.wu.migration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PM{	
	Integer number;
    double cpu;
	double ePM;
	double mem;
	private List<VM> assignedVmList;
	private Map<Integer,VM> assignedVmMap;
	
	private double idleEnergyRatio = 0.7;
	private double targetUtilization = 1.0;
	
	public PM(int pmNumber,double cpu, double maxEnergy){
		this();		
		number = pmNumber;
		this.cpu = cpu;
		ePM = maxEnergy;
		mem = cpu;
	}
	
	public PM(int pmNumber,double cpu,double mem, double maxEnergy){
		this();		
		number = pmNumber;
		this.cpu = cpu;
		ePM = maxEnergy;
		this.mem = mem;
	}
	
	public void setTargetUtilization(double v){
		this.targetUtilization = v;
	}
	
	public double getTargetUtilization(){
		return this.targetUtilization;
	}
	
	public PM(){		
		assignedVmList = new ArrayList<VM>();
		assignedVmMap = new HashMap<Integer, VM>();
	}
	
	public double getUtilizationCPU(){
		double utilization;
		double usedCpu = 0;
		//getLock().lock();
		for (int i= 0; i<assignedVmList.size();i++){
			usedCpu += assignedVmList.get(i).getMips();
		}
		//getLock().unlock();
		utilization = usedCpu / this.cpu;
		return utilization;
	}
	
	public double getUtilizationMem(){
		double utilization;
		double usedMem = 0;
		//getLock().lock();
		for (int i= 0; i<assignedVmList.size();i++){
			usedMem += assignedVmList.get(i).getMem();
		}
		//getLock().unlock();
		utilization = usedMem / this.mem;
		return utilization;
	}
	
	public double getCPU(){
		return this.cpu + getId();
	}
	
	public double getMem(){
		return this.mem;
	}
	
	public boolean canAccept(VM vm){
		double finalUitlization = getUtilizationCPU();
		finalUitlization += vm.getMips() / this.cpu;		
		return finalUitlization <= this.targetUtilization;
	}
	
	public double getEnergy(){
		double u = getUtilizationCPU();
		if (u < 0.0000001) 
			return 0;
		else
			return ePM * idleEnergyRatio + (1-idleEnergyRatio)* ePM * getUtilizationCPU();
	}
	
	public double getMaxEnergy(){		
			return ePM;
	}
	
	public int getVmCount(){
		return assignedVmList.size();
	}
	
	public String getName(){
		return number.toString();
	}
	
	public int getId(){
		return number;
	}
	
	public VM getVm(int index){
		if (index<assignedVmList.size())
			return assignedVmList.get(index);
		else
			return null;
	}
	
	public VM getVmByNumer(Integer vmNumber){
		return assignedVmMap.get(vmNumber);
	}
	
	public boolean hasVM(VM vm){
		return assignedVmMap.containsKey(vm.number);
	}
	
	public Lock getLock(){
		Lock pmLock = Lock.getLock("PM"+getName());
		return pmLock;
	}
	
	public synchronized void addVm(VM vm){		
		getLock().lock();
		addVmWithoutLock(vm);
		getLock().unlock();
	}
	
	public synchronized void addVmWithoutLock(VM vm){		
		if (!assignedVmMap.containsKey(vm.number)){
			assignedVmMap.put(vm.number, vm);
			int i =0;
			while (i<assignedVmList.size()){
				if (assignedVmList.get(i).requestedMips < vm.requestedMips)
					break;
				i++;
			}
			assignedVmList.add(i, vm);
			vm.setPM(this);
		}
		
	}
	
	public synchronized void removeVm(VM vm){
		getLock().lock();
		assignedVmList.remove(vm);
		assignedVmMap.remove(vm.number);
		getLock().unlock();
	}
	
	public synchronized void removeVmWithoutLock(VM vm){		
		assignedVmList.remove(vm);
		assignedVmMap.remove(vm.number);		
	}
	
	public VM getSmallestVm(){
		getLock().lock();
		int idx = assignedVmList.size() -1;
		VM result = null;
		if (idx>=0 ) {
			result = assignedVmList.get(0);
			if (result.isInMigration()) result = null;
		}
		getLock().unlock();
		return result;
	}
	
	
	public PM clone(){
		PM newPm = new PM();
		newPm.cpu = cpu;
		newPm.number = number;
		newPm.ePM = ePM;
		newPm.mem = mem;
		newPm.targetUtilization = targetUtilization;
		
		for (int i=0;i<assignedVmList.size();i++){			
			newPm.addVm(getVm(i).clone());
		}
		return newPm;
	}
	
	public String getPmInfo(){
		String reslt = "";
		
		reslt = String.format("%3d\t%.2f\t%.2f%%\t(m)%.2f%%", this.number,this.cpu,this.getUtilizationCPU()*100, this.getUtilizationMem()*100);
		for (VM vm : assignedVmList){
			reslt += "\r\n\t" + vm.getVmInfo() ;
		}
		return reslt;
	}
}