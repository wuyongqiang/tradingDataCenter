package grant.wu.trading;

import grant.wu.migration.Lock;

import java.util.ArrayList;
import java.util.Date;

public class VmMarket extends Thread {

	
	private static final String PM_AGENT_LIST_LOCK = "PmAgentListLock";
	private static final int STATUS_COLLECTING_GOODS = 0;
	private static final int STATUS_TRADING_GOODS = 1;
	private static final int STATUS_OVER = 2;
	private ArrayList<PmAgent> pmAgentList = new ArrayList<PmAgent>(); 
	private int _status = -1;
	private int curTradeNo = -1;
	private int tradeNumber = 150;
	private Callback tradeCallback;
	private  ArrayList<Goods> listGoods = new ArrayList<Goods>();
	private int migration = 0;
	private int tradingInterval = 400;
	private boolean bClosePmAgent;
	
	public VmMarket( Callback callback){
		tradeCallback = callback;
		bClosePmAgent = true;
	}
	
	public synchronized void addToGoodsList(Goods goods){
		listGoods.add(goods);
	}
	
	@Override
	public void run() {
		getLock().lock();
		for ( PmAgent agent : pmAgentList){
			agent.start();
		}
		getLock().unlock();
		int i=0;
		print("Market begin");
		while(i++< getTradeNumber()){
			collectGoods();
			tradeGoods();
			tradeCallback.callback();
		}
		if (isbClosePmAgent()){
			getLock().lock();
			stopAllPmAgent();
			getLock().unlock();		
			waitFinishPmAgent();
		}
		_status = STATUS_OVER;
		print("Market exit");
	}

	private void stopAllPmAgent() {
		for (PmAgent agent : pmAgentList) {
			agent.setStopRun(true);
		}
	}
	
	private void waitFinishPmAgent() {
		for ( PmAgent agent : pmAgentList){
			try {
				agent.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void tradeGoods() {
		//select the Vm with the highest priority
		//then pick up the VMs with lowest efficiency		
		Goods goods = pickUpGoods();
		if (goods!=null){
			//inviting bid
			double highestBidPrice = Double.MIN_VALUE;
			PmAgent winAgent = null;
			getLock().lock();
			for (PmAgent agent : pmAgentList){			
				double bidPrice = agent.bid(curTradeNo, goods.getVm());
				if (bidPrice > highestBidPrice && bidPrice > goods.getReservedPrice()){
					winAgent = agent;
					highestBidPrice = bidPrice;
				}
			}
			for (PmAgent agent : pmAgentList)
				agent.notifyBidResult(curTradeNo,goods.getVm(), winAgent);
			getLock().unlock();
			if (winAgent!=null){
				setMigration(getMigration() + 1);
				print("migrate to " + winAgent.getPm().getId());
			}
				
		}
	}

	private Goods pickUpGoods() {
		double efficiency = Double.MAX_VALUE;
		long auctionTime = Long.MAX_VALUE;
		Goods resultGoods = null;
		Lock listGoodsLock = Lock.getLock("listGoodsLock");
		listGoodsLock.lock();
		for(Goods goods:listGoods){
			if ( goods.getVm().getLastAuctionTime() < auctionTime) {
				auctionTime = goods.getVm().getLastAuctionTime();
				efficiency = Double.MAX_VALUE;
				resultGoods = goods;
			}else if( goods.getVm().getLastAuctionTime() == auctionTime && goods.getEfficiency() < efficiency ){
				efficiency = goods.getEfficiency();
				resultGoods = goods;
			}
		}
		listGoodsLock.unlock();
		if (resultGoods!=null){
			Date now = new Date();
			resultGoods.getVm().setLastAuctionTime(now.getTime());
			print("picked up: " + resultGoods.getVm().getName());
		}
		return resultGoods;
	}

	private void collectGoods() {
		curTradeNo++;
		listGoods.clear();
		setStatus( STATUS_COLLECTING_GOODS );
		mySleep(getTradingInterval() );
		setStatus ( STATUS_TRADING_GOODS );
		print("collected Goods:" + listGoods.size());
	}

	private void mySleep(int i) {
		try {
			sleep(i);
		} catch (InterruptedException e) {			
		}
		
	}

	public void registerPmAgent(PmAgent pmAgent) {
		getLock().lock();
		if (pmAgentList.indexOf(pmAgent)<0){
			pmAgentList.add(pmAgent);	
			print("PM registered: "+pmAgent.getPm().getId());
		}
		getLock().unlock();
	}
	
	public boolean unRegisterPmAgent(PmAgent pmAgent) {
		int tryTime = 0;
		if (pmAgentList.indexOf(pmAgent)>=0){
			//waiting for collectingGoods status
			while( !(isCollectingGoods()||_status==STATUS_OVER)){
				mySleep(5);
				if (tryTime++>10) return false;
				if (isCollectingGoods()||_status==STATUS_OVER) break;
			}
			getLock().lock();
			pmAgentList.remove(pmAgent);	
			getLock().unlock();
			print("PM unregistered: "+pmAgent.getPm().getId());
		}
		return true;
	}
	
	private Lock getLock() {
		
		return Lock.getLock(PM_AGENT_LIST_LOCK+this.getName());
	}

	private  void print(String s){
		System.out.println(s);
	}

	public synchronized boolean isCollectingGoods() {		
		return _status == STATUS_COLLECTING_GOODS;
	}
	
	public synchronized boolean isOver() {		
		return _status == STATUS_OVER;
	}
	
	protected synchronized void setStatus(int status){
		this._status = status;
	}

	public synchronized int  addGoods(Goods vmToSell) {
		Lock listGoodsLock = Lock.getLock("listGoodsLock");
		listGoodsLock.lock();
		listGoods.add(vmToSell);
		listGoodsLock.unlock();
		return curTradeNo;		
	}

	public int getTradeNumber() {
		return tradeNumber;
	}

	public void setTradeNumber(int tradeNumber) {
		this.tradeNumber = tradeNumber;
	}

	public int getMigration() {
		return migration;
	}

	public void setMigration(int migration) {
		this.migration = migration;
	}

	public int getTradingInterval() {
		return tradingInterval;
	}

	public void setTradingInterval(int tradingInterval) {
		this.tradingInterval = tradingInterval;
	}

	public boolean isbClosePmAgent() {
		return bClosePmAgent;
	}

	public void setbClosePmAgent(boolean bClosePmAgent) {
		this.bClosePmAgent = bClosePmAgent;
	}

}
