package grant.wu.trading;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public class Lock {
	
	static synchronized Lock getLock(String name){
		Lock l = lockList.get(name);
		if (l==null){
			l = new Lock(); 
			lockList.put(name, l);			
		}
		
		return l;
	}
	
	private static HashMap<String,Lock> lockList = new HashMap<String,Lock>();
	
	
	int userCount;
	
	LinkedList<Object> queue = new LinkedList<Object>();
	public Lock(){
		userCount = 0;
		
	}
	
	public int getUserCount(){
		return userCount;
	}

	public void lock() {
		Object lockObj = null;
		synchronized(this){
			if (++userCount > 1) {
				lockObj = new Object();				
					queue.addLast(lockObj);					
			}
			
		}
		try {
			
			if (lockObj!=null) 
				synchronized(lockObj){
				lockObj.wait(5000);
				}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	public synchronized void unlock_old(){
		synchronized(this){
		if (--userCount>0){
		  int loop= 0;	
		  while(true){	
			  synchronized(queue){  
			if (queue.size()>0){
				Object lockObj = queue.removeFirst();
				synchronized(lockObj){
					lockObj.notify();
				}
				break;
			}else if(loop<100){
				loop ++;
				try {
					Thread.sleep(2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}else{
				break;
			}
			  }
		  }
		  }
		}
	}
	
		
	public void unlock() {		
		synchronized(this){
			if (--userCount > 0) {
				Object lockObj = queue.removeFirst();
				synchronized(lockObj){
				lockObj.notify();
				}
			}	
		}
	}
}
