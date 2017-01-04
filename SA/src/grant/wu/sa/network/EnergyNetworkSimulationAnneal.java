package grant.wu.sa.network;

import grant.wu.network.NetworkCostCalculator;

import java.util.List;

import grant.wu.network.FatTreeTopologicalNode;


public class EnergyNetworkSimulationAnneal extends SimulationAnneal {
	
	private NetworkCostCalculator networkCalculator = new NetworkCostCalculator();

	public EnergyNetworkSimulationAnneal(double[] pCPU, double[] pVM, int[] vAssignOld,
			int oldPMInUse, int newPMInUse, double targetUtilization,
			String[] vmNames) {
		super(pCPU, pVM, vAssignOld, oldPMInUse, newPMInUse, targetUtilization, vmNames);
	}
	
	public void setNetworkRootNode(FatTreeTopologicalNode node){
		networkCalculator.setNetworkRootNode(node);
	}
	
	public void setVmTraffic(int[] trafficMap, int vNum){
		networkCalculator.setVmTraffic(trafficMap, vNum);		
	}
		
	public int getTotalNetworkCost(int[] assignment){
		
		return networkCalculator.getTotalNetworkCost(assignment);
	}
	
	public double getServerEnergy(int[] assignment){
		
		return serverEnergy(assignment);
	}
	
	public EnergyNetworkSimulationAnneal() {
		super();
		migrationCost = 1;
	}	
	
	public EnergyNetworkSimulationAnneal(int timeLimit) {
		super();
		this.annealTimeLimit = timeLimit;
		migrationCost = 1;
	}
	
	public void setAnnealTime(int v){
		this.annealTimeLimit = v;
	}

	@Override
	protected double dievationEnergy() {
		double largestPMEnergy = 0;
		for (int i = 0;i< pNum; i++){			
			largestPMEnergy +=  ePM[i];			
		}
		double energy = largestPMEnergy * temperature / initialTemperature + 0.1 ;
		return energy;
	}
	
	@Override
	protected double stateEnergy(int[] assignment) {
		double totalWeight = serverEnergy(assignment) + getTotalNetworkCost(assignment);
		return totalWeight;
	}
	
	double serverEnergy(int[] assignment) {
		double energy = 0;
		double[] uPM = new double[pNum];
		double[] usedMEM = new double[pNum];
		for (int i=0;i<assignment.length;i++)
		{
			int iPM = assignment[i];
			if ( iPM >= pNum || iPM <0 ){
				print("illegal assignment "+vmNames[i] +" to " + iPM);
				return Double.MAX_VALUE;
			}
			
			uPM[iPM] += vCPU[i] / pCPU[iPM];
			usedMEM[iPM] += vMEM[i]/ pMEM[iPM];
		}
		
		for (int i = 0;i< pNum; i++){
			if (uPM[i]>1 || usedMEM[i]>1 ){
				return Double.MAX_VALUE;
			}
			double energyPM = 0;
			if (uPM[i] > 0.001)
				energyPM = uPM[i] * (1- idleEnergyRatio ) * ePM[i] + idleEnergyRatio * ePM[i];
			energy += energyPM;
			
			saveUtilization(uPM,usedMEM);
		}
		
		return energy;
	}

}
