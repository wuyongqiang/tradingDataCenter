package grant.wu.sa.network;

import java.util.Date;

import grant.wu.util.ProblemGenerator;
import grant.wu.network.FatTreeTopologicalNode;
import grant.wu.util.PrintUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestNetworkCost {
	
	private Date startTime;
	@Before
	public void setUp(){
		
		PrintUtil.setLogName("EnergyNetwork");
	}
	
	@After
	public void tearDown(){
		
	}

	
	private void resetArray(int a[]){
		for (int i=0;i<a.length;i++){
			a[i] = 0;
		}
	}
	/*
	 * 
	 *                             core
	 *                       /            \
	 *                     a0              a1 
	 *              /             \         \ 
	 *             e0              e1       e2 
	 *          /     \         /     \      |
	 *        pm0      pm1     pm2     pm3  pm4
	 *  /   |  |   \   |     / |  \    |     |
	 * vm0 vm1 vm2 vm3 vm5 v4 vm6 vm7 vm8   vm9
	 */
	
	@Test
	public void test() throws Exception{
		for(int i=1; i<6; i++){
			testEnergyNetwork(100*i);
		}
	}
	private void testEnergyNetwork(int problemSize) throws Exception {
		startTime = new Date();
		ProblemGenerator gp = new grant.wu.util.ProblemGenerator(problemSize, 5); 
		
		double[] pCPU = gp.pCPU;
		double[] vCPU = gp.vCPU;
		
		int[] vAssignOldHere = gp.vAssign; 
		double[] vMem = gp.vMEM;
		int oldPMInUse = gp.pNum;
		int newPMInUse = gp.pNum;
		gp.firstFit();
		EnergyNetworkSimulationAnneal sa= new EnergyNetworkSimulationAnneal(pCPU, vCPU, vAssignOldHere, oldPMInUse, newPMInUse,
				1, null) ;
		sa.initMemory(vMem);
		
		sa.setNetworkRootNode(gp.root);
					
		sa.setVmTraffic(gp.traffic,gp.vNum);
		
		printCostInfo(sa, vAssignOldHere);
		
		sa.scheduleMigration();
		
		int assign[] = sa.getAssignment();		
		printCostInfo(sa,assign);
		
		setTreeAppData(gp, assign);
		FatTreeTopologicalNode.printTreeNode2(gp.root);
		println(FatTreeTopologicalNode.getTreeNode2StrBuilder().toString());
		
		Date endTime = new Date();
		double duration = (endTime.getTime() - startTime.getTime())/1000;
		println("duration="+String.format("%.2f", duration));
	}
	
	private void println(String message){
		PrintUtil.print(message);
	}
	
	private void setTreeAppData(ProblemGenerator gp, int[] assign){
		double[] rCPU = new double[assign.length];
		double[] rMem = new double[assign.length];
		for (int i=0; i< assign.length;i++){
			FatTreeTopologicalNode node = gp.root.getNodeByIdInGraph(gp.root.getGraph(), assign[i]);
			node.getAppData().add(i);
			int assignedPM = assign[i];
			rCPU[assignedPM] += gp.vCPU[i]/gp.pCPU[assignedPM]*100;
			rMem[assignedPM] += gp.vMEM[i]/gp.pMEM[assignedPM]*100;
		}
		
		for (int i=0; i<gp.pNum;i++){
			FatTreeTopologicalNode node = gp.root.getNodeByIdInGraph(gp.root.getGraph(),i);
			node.getAppData().add("cpu="+String.format("%.2f", rCPU[i]));
			node.getAppData().add("mem="+String.format("%.2f", rMem[i]));
		}
	}
	
	
	private void printCostInfo(EnergyNetworkSimulationAnneal sa, int[] assign){
		double networkCost = sa.getTotalNetworkCost(assign);
		double serverCost = sa.getServerEnergy(assign);
		println("serverCost = " + serverCost + " networkCost="+networkCost);
	}
}
