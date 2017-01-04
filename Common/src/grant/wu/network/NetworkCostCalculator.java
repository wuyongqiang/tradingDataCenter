package grant.wu.network;

import java.util.List;



public class NetworkCostCalculator{
	
	private static final int MAXPMNUM = 1000;
	private FatTreeTopologicalNode networkRootNode =null;
	private int[] trafficMap;
	
	private int vNum;
	
	private int []unitCostArray;

	public NetworkCostCalculator( ) {
		unitCostArray = new int[MAXPMNUM*MAXPMNUM];
		for (int i=0;i<MAXPMNUM*MAXPMNUM;i++)
			unitCostArray[i] = -1;
	}
	
	public void setNetworkRootNode(FatTreeTopologicalNode node){
		this.networkRootNode = node;
	}
	
	public void setVmTraffic(int[] trafficMap, int vNum){
		this.trafficMap = trafficMap;		
		this.vNum = vNum;
	}
	
	
	
	private int networkCost(int vm1, int vm2,int[] assignment){	
		int pm1 = assignment[vm1];
		int pm2 = assignment[vm2];
		
		if (pm1 > MAXPMNUM || pm2 > MAXPMNUM) 
			return 0;
		
		int traffic = getTaffic(vm1,vm2);
		int unitCost = 0;
		if (traffic>0){
			unitCost = unitCostArray[pm1*MAXPMNUM+pm2];
			if(unitCost==-1){
				unitCost = networkUnitCost(pm1,pm2);
				unitCostArray[pm1*MAXPMNUM+pm2] = unitCost;
			}
		}
		return unitCost * traffic;
	}
	
	public void killVm(int vm){
		for (int i=0;i<vNum;i++){
			trafficMap[vNum*vm+i]=0;
			trafficMap[vm+i*vNum]=0;
		}
	}
	
	private int getTaffic(int vm1, int vm2) {					
			int pos = vm1 * vNum + vm2;
			return trafficMap[pos];		
	}
	
	public int getUpperBound(){
		int total = 0;		
		for(int i=0;i<vNum;i++){			
			for(int j=0;j<vNum;j++){			
				total += getTaffic(i,j) * 4;
			}
		}		
		
		return total;
	}
	
	
	
	public int getTotalNetworkCost(int[] assignment){
		int total = 0;
		
		if(vNum > assignment.length){
			System.err.println( "vNum("+vNum+") != assignment.length("+assignment.length+")");
			//throw new RuntimeException("vNum != assignment.length");
			return 0;
		}
		for(int i=0;i<vNum;i++){			
			for(int j=0;j<vNum;j++){
				if (i==j) continue;
				if (assignment[i]==-1 || assignment[j]==-1) 
					continue;
				total += networkCost(i,j,assignment);
			}
		}		
		
		return total;
	}
	
	public int getTotalNetworkCost(int[] assignment, int vm){
		int total = 0;
		
		if(vNum > assignment.length){
			System.err.println( "vNum("+vNum+") != assignment.length("+assignment.length+")");
			//throw new RuntimeException("vNum != assignment.length");
			return 0;
		}
		
		int i = vm;			
		
		for(int j=0;j<vNum;j++){
			if (i==j) continue;
			if (assignment[i]==-1 || assignment[j]==-1) 
				continue;
			total += networkCost(i,j,assignment);
		}

		return total;
	}
	
	public int getTotalNetworkCost(int[] assignment, double[] vmWorkloads){
		int total = 0;
		
		if(vNum > assignment.length || vNum > vmWorkloads.length){
			System.err.println( "vNum("+vNum+") != assignment.length("+assignment.length+")");
			//throw new RuntimeException("vNum != assignment.length");
			return 0;
		}
		for(int i=0;i<vNum;i++){			
			for(int j=0;j<vNum;j++){
				if (i==j) continue;
				if (assignment[i]==-1 || assignment[j]==-1) 
					continue;
				total += networkCost(i,j,assignment) * (vmWorkloads[i]+ vmWorkloads[j])/2;
			}
		}		
		
		return total;
	}

	private int networkUnitCost(int pmNumber1, int pmNumber2){
		String pm1 = "pm"+pmNumber1;
		String pm2 = "pm"+pmNumber2;
		if (pm1.equals(pm2)) return 0;
		FatTreeTopologicalNode node1 = networkRootNode.findNodeByLabel(pm1);
		FatTreeTopologicalNode node2 = networkRootNode.findNodeByLabel(pm2);
		List<FatTreeTopologicalNode> list = node1.getNodesFrom(node2);
		if (list.size()==1)
			return 0;
		else if (list.size()==3)
			return 1;
		else if (list.size()==5)
			return 3;
		else if (list.size()==7)
			return 5;
		else{
			throw new RuntimeException("impossible network link " + list.size());
		}
	}
}
