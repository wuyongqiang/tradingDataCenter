package grant.wu.util;

import java.util.Date;
import java.util.Iterator;
import java.util.Random;

import grant.wu.network.FatTreeTopologicalNode;
import grant.wu.network.NetworkCostCalculator;
import grant.wu.network.TopologicalGraph;
import grant.wu.network.TopologicalNode;
import grant.wu.util.PrintUtil;

/**
 * Sample fitness function for the CoinsEnergy example. Adapted from
 * examples.MinimizingMakeChangeFitnessFunction
 * 
 * @author Klaus Meffert
 * @since 2.4
 */
public class ProblemGenerator {
	// private final double normalEnergyBound = 1000*1000*100;
	// private final double breachEnergy = normalEnergyBound * 100000;

	/** String containing the CVS revision. Read out via reflection! */
	private final String CVS_REVISION = "$Revision: 1.5 $";
	
	public static final int SERVER_HETEROGENEOUS = 0;
	public static final int SERVER_HOMOGENEOUS = 1;
	public static final int SERVER_BIGGERBETTER = 2;

	public int vNum;

	public double[] vCPU;

	public double[] vMEM;

	public int[] vAssign;
	public int pNum;

	public double[] pCPU;

	public double[] pMEM;

	public double[] ePM;

	private double idleEnergyRatio;

	private double[] pUtilization;

	private double pLeastTheoryEnergy;

	private double pLargestPMEnergy;

	private boolean inited = false;

	public NetworkCostCalculator networkCalc;
	public FatTreeTopologicalNode root;

	public int traffic[];

	private double networkWeight = 1;
	private double energyWeight = 1;

	public void generateProblem(int scale, int capacityIndexPM, boolean bSort, boolean createSpareVM,int problemType)
			throws Exception {

		if (inited)
			return;
		
		int ratio = createSpareVM?2:1;
		vNum = scale;
		vNum = vNum <= 0 ? 1 : vNum;
		vCPU = new double[vNum * ratio];
		vMEM = new double[vNum * ratio];
		vAssign = new int[vNum * ratio];
		
		pNum = scale * 2 / 2 / capacityIndexPM;
		pNum = pNum <= 0 ? 1 : pNum;
		pCPU = new double[pNum];
		pMEM = new double[pNum];
		ePM = new double[pNum];

		Random r = new Random();
		//r.setSeed(2000*scale);
		Date d = new Date();
		r.setSeed(2*d.getTime());
		Random rMem = new Random();
		rMem.setSeed(3*d.getTime());
		//rMem.setSeed(3000*scale);

		double totalRequirement = 0;
		int totalVNum = vNum * ratio;

		for (int i = 0; i < totalVNum; i++) {
			double randomRequirement = Math.abs( r.nextInt(31) ) * 100;
			vCPU[i] = randomRequirement;
			if (randomRequirement < 0.01)
				randomRequirement = 50; // minimum cpu to keep it alive
			vCPU[i] = randomRequirement;

			totalRequirement += vCPU[i];

			randomRequirement = Math.abs(rMem.nextInt(31)) * 100;
			if (randomRequirement < 0.01)
				randomRequirement = 300;
			vMEM[i] = randomRequirement;
		}
		
		if (bSort) sortVM();

		int capacity[] = { 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000, 5500};
		//int capacity[] = { 1000, 1200, 1500, 1800, 2000, 2300, 2400, 2500,2700, 3000 };
		// int capacity[] = {2000, 2000, 2000,2000,2000,2000, 2000,2000,2000,2000};

		for (int i = 0; i < pNum; i++) {
			if (problemType==SERVER_HOMOGENEOUS){
				pCPU[i] = 3000 * capacityIndexPM;
				pMEM[i] = 3000 * capacityIndexPM;
			}else{
			pCPU[i] = capacity[i % 10] * capacityIndexPM;
			pMEM[i] = pCPU[i];// 3000 * capacityIndexPM;
			}
		}
		if (bSort) sortPM();
		double maxPMCapacity = 0;
		for (int i = 0; i < pNum; i++) {

			double energyTimes = 1;
			Random r3 = new Random(3);
			if (pCPU[i] / 1000 < 100) {
				// 10 times capacity, 6 times of energy
				// 100 times capacity, 20 times of energy
				energyTimes = (1 - Math.log10(pCPU[i] / 1000) * 0.4);	
				if (problemType==SERVER_HETEROGENEOUS){
					if (i==5) energyTimes = energyTimes * 1.5;
					if (i==7) energyTimes = energyTimes * 1.5;
				}
			} else {
				println("wrong capacity: " + pCPU[i]);
				throw new Exception();
			}

			ePM[i] = (pCPU[i] / 10) * energyTimes; // 1000-> 100*(1-0), 3000->
													// 300 * (1-1/3)

			if (pCPU[i] > maxPMCapacity) {
				maxPMCapacity = pCPU[i];
				pLargestPMEnergy = ePM[i];
			}
		}

		idleEnergyRatio = 0.7;

		pLeastTheoryEnergy = (totalRequirement / maxPMCapacity)
				* (1 - idleEnergyRatio) * pLargestPMEnergy + idleEnergyRatio
				* pLargestPMEnergy;

		// printProblem();
		println("gnerate problem done");
		inited = true;

		generateNetworkConfig(pNum,totalVNum );

		printProblem();
	}

	private void generateNetworkConfig(int pmNumber, int vmNumber) {
		int childrenNumber = 5;
		for (int i = 2; i < 10; i++) {
			childrenNumber = i;
			if (Math.pow(i, 3) > pmNumber)
				break;
		}
		println("children number of network node is = " + childrenNumber);
		TopologicalGraph graph = FatTreeTopologicalNode.generateTree(pmNumber,
				childrenNumber);

		root = FatTreeTopologicalNode.orgnizeGraphToTree(graph);
		

		traffic = new int[vmNumber * vmNumber];

		resetArray(traffic);
		
		// networkPairs(vmNumber, traffic);
		networkRandomGrp(vmNumber, traffic);
		
		networkCalc = new NetworkCostCalculator();
		networkCalc.setNetworkRootNode(root);
		networkCalc.setVmTraffic(traffic, vmNumber);
	}

	private void networkPairs(int vmNumber, int traffic[]) {
		int vmHalfNumber = vmNumber / 2;
		for (int i = 0; i < vmHalfNumber; i++) {
			traffic[i * vmNumber + (vmHalfNumber + i)] = 10;
		}
	}

	private void networkRandom(int vmNumber, int traffic[]) {
		Random r = new Random(123456L);
		for (int i = 0; i < vmNumber; i++) {
			for (int j = 0; j < vmNumber; j++)
				if (i != j)
					traffic[i * vmNumber + j] = r.nextInt(10);
		}
	}

	private void networkRandomGrp(int vmNumber, int traffic[]) {
		Random r = new Random(123456L);
		int grpNum = vmNumber / 4;
		if (grpNum==0){
			System.err.println("warning: grpNum=0");
			return;
		}
		int vmGrp[] = new int[vmNumber];
		for (int i = 0; i < vmNumber; i++) {
			vmGrp[i] = r.nextInt(grpNum);
		}
		for (int i = 0; i < grpNum; i++) {
			String s = "Group" + i + " :";
			for (int j = 0; j < vmNumber; j++) {
				if (vmGrp[j] == i) {
					s += j + ",";
				}
			}
			println(s);
		}
		for (int i = 0; i < vmNumber; i++) {
			for (int j = 0; j < vmNumber; j++) {
				if (i != j && vmGrp[i] == vmGrp[j]) // in a grp
					traffic[i * vmNumber + j] = r.nextInt(10);
			}
		}
	}

	private void resetArray(int a[]) {
		for (int i = 0; i < a.length; i++) {
			a[i] = 0;
		}
	}

	public int[] firstFit() {

		double[] pLeftCPU = new double[pNum];
		double[] pLeftMEM = new double[pNum];
		for (int i = 0; i < pNum; i++) {
			pLeftCPU[i] = pCPU[i];
			pLeftMEM[i] = pMEM[i];
		}
		for (int i = 0; i < vNum; i++) {
			for (int j = 0; j < pNum; j++) {
				if (pLeftCPU[j] > vCPU[i] && pLeftMEM[j] > vMEM[i]) {
					vAssign[i] = j;
					pLeftCPU[j] -= vCPU[i];
					pLeftMEM[j] -= vMEM[i];
					break;
				}
			}
		}

		return vAssign;
	}
	
	public int[] initRandom() {

		double[] pLeftCPU = new double[pNum];
		double[] pLeftMEM = new double[pNum];
		for (int i = 0; i < pNum; i++) {
			pLeftCPU[i] = pCPU[i];
			pLeftMEM[i] = pMEM[i];
		}
		
		Random r = new Random(8888);
		for (int i = 0; i < vNum; i++) {
			while(true) {
				int j = r.nextInt(pNum);
				if (pLeftCPU[j] > vCPU[i] && pLeftMEM[j] > vMEM[i]) {
					vAssign[i] = j;
					pLeftCPU[j] -= vCPU[i];
					pLeftMEM[j] -= vMEM[i];
					break;
				}
			}
		}

		return vAssign;
	}

	private void sortVM() {
		for (int i = 0; i < vNum; i++) {
			double prevMax = vCPU[i];
			for (int j = i + 1; j < vNum; j++) {
				if (vCPU[j] > prevMax) {
					vCPU[i] = vCPU[j];
					vCPU[j] = prevMax;
					prevMax = vCPU[i];

					// swap the mem usage attribute
					double tmp = vMEM[i];
					vMEM[i] = vMEM[j];
					vMEM[j] = tmp;
				}
			}
		}
	}

	private void sortPM() {
		for (int i = 0; i < pNum; i++) {
			double prevMax = pCPU[i];
			for (int j = i + 1; j < pNum; j++) {
				if (pCPU[j] > prevMax) {
					pCPU[i] = pCPU[j];
					pCPU[j] = prevMax;
					prevMax = pCPU[i];

					// swap the mem usage attribute
					double tmp = pMEM[i];
					pMEM[i] = pMEM[j];
					pMEM[j] = tmp;
				}
			}
		}
	}

	private void printProblem() {
		String s = "VM:";
		double totalRequirement = 0;
		for (int i = 0; i < vNum; i++) {
			s += "(" + vCPU[i] + "," + vMEM[i] + "),";
			totalRequirement += vCPU[i];
		}

		println(s);
		println(String.format("total VM %d requirement %.2f", vNum,
				totalRequirement));
		s = "PM:";
		totalRequirement = 0;
		for (int i = 0; i < pNum; i++) {
			s += "(" + pCPU[i] + "," + pMEM[i] +"," + String.format("%.0f",ePM[i])+ "),";
			;
			totalRequirement += pCPU[i];
		}
		println(s);
		println(String.format("total PM %d capacity %.2f", pNum,
				totalRequirement));

		// network traffic info
		// s = "network traffic info\n";
		// for (int i=0;i<vNum; i++){
		// for (int j=0;j<vNum; j++){
		// s += String.format("%4d", traffic[i * vNum + j]);
		// }
		// s += "\n";
		// }
		println(s);
	}

	public ProblemGenerator(int scale, int capacityIndexPM)
			throws Exception {

		//generateProblem(scale, capacityIndexPM, true,false);

	}

	/**
	 * Determine the fitness of the given Chromosome instance. The higher the
	 * return value, the more fit the instance. This method should always return
	 * the same fitness value for two equivalent Chromosome instances.
	 * 
	 * @param a_subject
	 *            the Chromosome instance to evaluate
	 * 
	 * @return positive double reflecting the fitness rating of the given
	 *         Chromosome
	 * @since 2.0 (until 1.1: return type int)
	 * @author Neil Rotstan, Klaus Meffert, John Serri
	 */
	
	private double stateEnergy(int[] assignment) {
		double energy = 0;
		double[] uPM = new double[pNum];
		double[] usedMEM = new double[pNum];
		for (int i = 0; i < assignment.length; i++) {
			int iPM = assignment[i];
			if (iPM >= pNum || iPM < 0) {
				println("illegal assignment " + assignment.toString());
				return Double.MAX_VALUE;
			}

			uPM[iPM] += vCPU[i] / pCPU[iPM];
			usedMEM[iPM] += vMEM[i];
		}

		for (int i = 0; i < pNum; i++) {
			double energyPM = 0;

			if (uPM[i] > 1 || pMEM[i] < usedMEM[i]) {
				double uMem = usedMEM[i] / pMEM[i];
				double uE = uPM[i] > uMem ? uPM[i] : uMem;
				energyPM = pLargestPMEnergy * uE * 2;
			} else {
				if (uPM[i] > 0.001)
					energyPM = uPM[i] * (1 - idleEnergyRatio) * ePM[i]
							+ idleEnergyRatio * ePM[i];
			}
			energy += energyPM;

			saveUtilization(uPM);
		}

		return energy;
	}

	private void saveUtilization(double[] uPM) {
		if (pUtilization == null) {
			pUtilization = new double[pNum];
		}

		for (int i = 0; i < uPM.length; i++) {
			pUtilization[i] = uPM[i];
		}
	}

	private void println(String s) {
		PrintUtil.print(s);
	}

	/**
	 * 
	 * @param a_maxFitness
	 *            the maximum fitness value allowed
	 * @param a_weight
	 *            the coins weight of the current solution
	 * @return the penalty computed
	 * @author Klaus Meffert
	 * @since 2.4
	 */
	protected double computeWeightPenalty(double a_maxFitness, double a_weight) {
		if (a_weight <= 0) {
			// we know the solution cannot have less than one coin
			return 0;
		} else {
			// The more weight the more penalty, but not more than the maximum
			// fitness value possible. Let's avoid linear behavior and use
			// exponential penalty calculation instead
			return (Math.min(a_maxFitness, a_weight * a_weight));
		}
	}

	
	private void clearAllNetworkNodeAppData() {
		if (root != null) {
			Iterator<TopologicalNode> it = root.getGraph().getNodeIterator();
			while (it.hasNext()) {
				FatTreeTopologicalNode node = (FatTreeTopologicalNode) it
						.next();
				node.getAppData().clear();
			}
		}

	}

	@SuppressWarnings("unchecked")
	private void addToNetworkNode(Object appData, int iPM) {
		if (root != null) {
			FatTreeTopologicalNode node = FatTreeTopologicalNode
					.getNodeByIdInGraph(root.getGraph(), iPM);
			node.getAppData().add(appData);
		}

	}	
}
