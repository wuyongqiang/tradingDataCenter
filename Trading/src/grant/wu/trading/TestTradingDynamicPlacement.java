package grant.wu.trading;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import grant.wu.util.ProblemGenerator;
import grant.wu.migration.MigrationProblem;
import grant.wu.migration.PM;
import grant.wu.migration.VM;
import grant.wu.network.NetworkCostCalculator;
import grant.wu.util.PrintUtil;

public class TestTradingDynamicPlacement {	
	
	PM pmDispatcher;
	Map<Integer,VM> generatedVMs = new HashMap<Integer,VM>();
	Map<Integer,VM> spareVMs = new HashMap<Integer,VM>();
	NetworkCostCalculator netCostCalc;
	int vmAssign[];
	int problemType = ProblemGenerator.SERVER_HETEROGENEOUS;
	
	private MigrationProblem getMigrationProblem(int vmNum) throws Exception{
		ProblemGenerator pg = new ProblemGenerator(vmNum, 5);
		boolean createSpareVm = true;
		pg.generateProblem(vmNum,5,false,createSpareVm,problemType);
		pg.initRandom();
		MigrationProblem problem = new MigrationProblem();
		for (int i=0;i<pg.pNum;i++){
			PM pm = new PM(i,pg.pCPU[i],pg.ePM[i]);
			pm.setTargetUtilization(1);
			problem.addPM(pm);
		}
		
		addDispatcherPM(problem);
		if (!createSpareVm)
			vmAssign = new int[pg.vNum];
		else
			vmAssign = new int[pg.vNum*2];
		
		for (int i=0;i<pg.vNum;i++){
			VM vm = new VM(i,"vm"+i,pg.vCPU[i]);
			vm.setMem(pg.vMEM[i]);
			int iPM = pg.vAssign[i];
			problem.getPM(iPM).addVm(vm);
			generatedVMs.put(i,vm);
			vmAssign[i] = iPM;
		}
		if (createSpareVm){
			for (int i=pg.vNum;i<pg.vNum*2;i++){
				VM vm = new VM(i,"vm"+i,pg.vCPU[i]);
				vm.setMem(pg.vMEM[i]);
								
				generatedVMs.put(i,vm);
				spareVMs.put(i, vm);
				vmAssign[i] = -1;
			}
		}
		netCostCalc = pg.networkCalc;
		return problem;
	}
	
	private void addDispatcherPM(MigrationProblem problem){
		PM pm = new PM(PmAgent.DISPATCHER_PM_NUM,Double.MAX_VALUE,0);
		pm.setTargetUtilization(1);
		problem.addPM(pm);
		pmDispatcher = pm;
	}
	
	private Date begin ;
	@Before
	public void setBeginTime(){
		begin = new Date();
	}
	
	@After
	public void showDuration(){
		Date now = new Date();
		print("time lapsed:"+(now.getTime() - begin.getTime())/1000);
	}
	
	private void print(String s){
		PrintUtil.print(s);
	}
	
	private AtomicInteger _tradeCount = new AtomicInteger();
	private synchronized void incCount(){
		_tradeCount.incrementAndGet();
	}
	
	private synchronized int getTradeCount(){
		return _tradeCount.intValue();
	}
	
	private List<VM> generateAddtionalVMs(int scale,int vmNo) throws Exception{
		ProblemGenerator pg= new ProblemGenerator(scale, 5);
		pg.generateProblem(scale, 5, false,false,problemType);
		List<VM> vmlist = new ArrayList<VM>();
		for (int i=0;i<pg.vCPU.length;i++){
			VM vm = new VM(i,"vm"+(i+vmNo),pg.vCPU[i]);
			vm.setMem(pg.vMEM[i]);			
			vmlist.add(vm);
		}
		
		return vmlist;
	}
	
	private List<VM> getVmListToRemove(MigrationProblem problem, int scale, int vmCount){
		
		List<VM> vmlist = new ArrayList<VM>();	
		if (scale > problem.getTotalVmCount()) 
			return vmlist;
		
		int picked = 0;
			
		Random r = new Random(new Date().getTime());
		while(picked<scale){
			int inx = r.nextInt(vmCount);
			String name = "vm"+inx;
			VM vm = problem.getVmByName(name) ;
			if (vm!=null && vmlist.indexOf(vm)< 0 )
			{
				vmlist.add(vm);
				picked ++;
			}
		}
		PrintUtil.print("picked vms to remove "+vmlist.size());
		return vmlist;
	}
	
	private void removeVMs(MigrationProblem problem, int scale, int vmCount){
			
		if (scale > problem.getTotalVmCount()) 
			return;
		
		int picked = 0;
			
		Random r = new Random(new Date().getTime());
		while(picked<scale){
			int inx = r.nextInt(vmCount);
			
			VM vm = generatedVMs.get(inx) ;
			
			if (vm!=null && !vm.isInMigration() && vm.getPM()!=null )
			{
				vm.getPM().removeVm(vm);
				picked ++;
				int vmId= vm.getId();
				netCostCalc.killVm(vmId);
				PrintUtil.print("remove vm "+ vm.getName());
			}
		}
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testTradingPlacment() throws Exception{
		int problemSize = 10;
		boolean dynamic = false;
		boolean networkAware = false;
		int dynamicVmStartingNumber = 200;
		doTradingPlacment(problemSize, dynamic, networkAware, dynamicVmStartingNumber);
	}
	
	public void doTradingPlacment(int problemSize, boolean dynamic, boolean networkAware, int dynamicVmStartingNumber) throws Exception{	
		
		PrintUtil.setLogName("tradingPlacment-scale-"+String.format("%02d", problemSize)+"-networkAware-" + networkAware);
		int scale = 50 * problemSize;
		final MigrationProblem problem = getMigrationProblem(scale);
		print(problem.getProblemInfo());
		VmMarket market = new VmMarket(new Callback() {
			@Override
			public void callback() {				
				double totalEnergy = problem.getTotalEnergy();
				double netEnergy = netCostCalc.getTotalNetworkCost(vmAssign);
				Date now = new Date();
				PrintUtil.print(String.format("%d\t%.2f\t%.2f", now.getTime()/1000,totalEnergy, netEnergy));
				incCount();
			}
		});
		market.setTradingInterval(10);
		int tradeNo = 500 * problemSize;
		
		boolean createNewVMs = false;
		market.setTradeNumber(tradeNo);
		
		for (int i=0;i<problem.getPmCount();i++){
			PM pm = problem.getPM(i);
			PmAgent agent = new PmAgent(networkAware);
			agent.setPm(pm);
			agent.setMarket(market);
			agent.setNetCostCalc(netCostCalc);
			agent.setVmAssign(vmAssign);
			agent.setCreateNewVMs(createNewVMs);
		}
		market.start();
		int count = problem.getTotalVmCount();
		int doneCount  = -1;
		int newVMs = 0;
		
		
		while(true){
			
			if(dynamic && getTradeCount()> dynamicVmStartingNumber && getTradeCount()%5 == 0 && getTradeCount()!=doneCount){
				int dynNumber = 2;
				removeVMs(problem, dynNumber, scale);
				
				for(int i=0;i<dynNumber;i++){
					if(i+newVMs < scale)
					 pmDispatcher.addVm(spareVMs.get(i+newVMs+scale));
				}
				newVMs += dynNumber;
				doneCount = getTradeCount();
			}
			Thread.sleep(10);
			
			if (getTradeCount()>=tradeNo)
				break;
			
		}
		market.join();
		PrintUtil.print(problem.getProblemInfo());
		double netEnergy = netCostCalc.getTotalNetworkCost(vmAssign);
		PrintUtil.print( String.format("netEnergy: %.2f",netEnergy));
		PrintUtil.print("migrations: "+market.getMigration());
	}	
	
	
	@SuppressWarnings("unchecked")
	@Test
	public void testFFDPlacment() throws Exception{
		int problemSize = 1;
		boolean dynamic = true;
		int dynamicVmStartingNumber = 200;
		doFFDPlacment(problemSize, dynamic, dynamicVmStartingNumber);
	}
	
	public void doFFDPlacment(int problemSize, boolean dynamic, int dynamicVmStartingNumber) throws Exception{	
		PrintUtil.setLogName("ffdPlacment");
		PrintUtil.setLogName("ffdPlacment-scale-"+String.format("%02d", problemSize));
		int scale = 50 * problemSize;
		final MigrationProblem problem = getMigrationProblem(scale);
		print(problem.getProblemInfo());
		FFDDispatcher ffd = new FFDDispatcher(new Callback() {
			@Override
			public void callback() {				
				double totalEnergy = problem.getTotalEnergy();
				double netEnergy = netCostCalc.getTotalNetworkCost(vmAssign);
				Date now = new Date();
				PrintUtil.print(String.format("%d\t%.2f\t%.2f", now.getTime()/1000,totalEnergy, netEnergy));
				incCount();
			}
		});
		ffd.setTradingInterval(50);
		int tradeNo=600 * problemSize;
		boolean createNewVMs= false;
		ffd.setTradeNumber(tradeNo);
		boolean networkAware = false;
		for (int i=0;i<problem.getPmCount();i++){
			PM pm = problem.getPM(i);
			PmAgent agent = new PmAgent(networkAware);
			agent.setPm(pm);
			
			agent.setNetCostCalc(netCostCalc);
			agent.setVmAssign(vmAssign);
			agent.setCreateNewVMs(createNewVMs);
			if (pm.getId()!=PmAgent.DISPATCHER_PM_NUM)
				ffd.addPmAgent(agent);
			else
				ffd.setPmDispatcher(pmDispatcher);
		}
		ffd.start();
		int count = problem.getTotalVmCount();
		int doneCount  = -1;
		int newVMs = 0;
		
		while(true){
			
			if(dynamic && getTradeCount()> dynamicVmStartingNumber && getTradeCount()%5 == 0 && getTradeCount()!=doneCount){	
				removeVMs(problem, 2, scale);
				
				for(int i=0;i<2;i++){
					if(i+newVMs < scale)
					 pmDispatcher.addVm(spareVMs.get(i+newVMs+scale));
				}
				newVMs += 2;
				doneCount = getTradeCount();
			}
			Thread.sleep(10);
			
			if (getTradeCount()>=tradeNo)
				break;
			
		}
		ffd.join();
		PrintUtil.print(problem.getProblemInfo());
		double netEnergy = netCostCalc.getTotalNetworkCost(vmAssign);
		PrintUtil.print( String.format("netEnergy: %.2f",netEnergy));
		PrintUtil.print("migrations: "+ffd.getMigration());
	}	
	
	@SuppressWarnings("unchecked")
	@Test
	public void testTradingPlacmentAccrossGrps() throws Exception{
		int problemSize = 10;
		boolean dynamic = false;
		boolean networkAware = false;
		int dynamicVmStartingNumber = 200;
		doTradingPlacmentAccrossGrps(problemSize, dynamic, networkAware, dynamicVmStartingNumber);
	}
	
	public void doTradingPlacmentAccrossGrps(int problemSize, boolean dynamic, boolean networkAware, int dynamicVmStartingNumber) throws Exception{	
		
		ArrayList<PmAgent> pmList = new ArrayList<PmAgent>();
		PrintUtil.setLogName("tradingPlacmentAccossGrps-scale-"+String.format("%02d", problemSize)+"-networkAware-" + networkAware);
		int scale = 50 * problemSize;
		final MigrationProblem problem = getMigrationProblem(scale);
		print(problem.getProblemInfo());
		VmMarket market1 = new VmMarket(new Callback() {
			@Override
			public void callback() {				
				double totalEnergy = problem.getTotalEnergy();
				double netEnergy = netCostCalc.getTotalNetworkCost(vmAssign);
				Date now = new Date();
				PrintUtil.print(String.format("%d\t%.2f\t%.2f", now.getTime()/1000,totalEnergy, netEnergy));
				incCount();
			}
		});
		
		VmMarket market2 = new VmMarket(new Callback() {
			@Override
			public void callback() {							
				incCount();
			}
		});
		market1.setbClosePmAgent(false);
		market2.setbClosePmAgent(false);
		market1.setTradingInterval(10);
		market2.setTradingInterval(10);
		int tradeNo = 600 * problemSize;
		
		boolean createNewVMs = false;
		market1.setTradeNumber(tradeNo);
		market2.setTradeNumber(tradeNo);

		int halfPMCount = problem.getPmCount() / 2;
		for (int i=0;i<problem.getPmCount();i++){
			PM pm = problem.getPM(i);
			PmAgent agent = new PmAgent(networkAware);
			agent.setPm(pm);
			agent.addMarket(market1);
			agent.addMarket(market2);
			if (i< halfPMCount)
				agent.setMarket(market1);
			else
				agent.setMarket(market2);
			agent.setNetCostCalc(netCostCalc);
			agent.setVmAssign(vmAssign);
			agent.setCreateNewVMs(createNewVMs);
			pmList.add(agent);
		}
		market1.start();
		market2.start();
		int count = problem.getTotalVmCount();
		int doneCount  = -1;
		int newVMs = 0;
		
		while(true){
			
			if(dynamic && getTradeCount()>dynamicVmStartingNumber && getTradeCount()%5 == 0 && getTradeCount()!=doneCount){
				int dynNumber = 2;
				removeVMs(problem, dynNumber, scale);
				
				for(int i=0;i<dynNumber;i++){
					if(i+newVMs < scale)
					 pmDispatcher.addVm(spareVMs.get(i+newVMs+scale));
				}
				newVMs += dynNumber;
				doneCount = getTradeCount();
			}
			Thread.sleep(10);
			
			if (getTradeCount()>=tradeNo)
				break;
			
		}
		market1.join();
		market2.join();
		for (PmAgent agent : pmList){
			agent.setStopRun(true);			
		}
		for (PmAgent agent : pmList){
			if (agent.isAlive())
			agent.join(3000);			
		}		
		PrintUtil.print(problem.getProblemInfo());
		double netEnergy = netCostCalc.getTotalNetworkCost(vmAssign);
		PrintUtil.print( String.format("netEnergy: %.2f",netEnergy));
		PrintUtil.print("migrations: "+market1.getMigration());
		PrintUtil.print("migrations: "+market2.getMigration());
	}	
}
