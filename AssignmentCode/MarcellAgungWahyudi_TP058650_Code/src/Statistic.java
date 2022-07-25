/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Maxine
 */
public class Statistic {
    private long arriveTime;
    private long departTime;
    private AtomicInteger rejectedPassengerCount=new AtomicInteger(0);
    private AtomicInteger embarkedPassengerCount=new AtomicInteger(0);
    private AtomicInteger disembarkedPassengerCount=new AtomicInteger(0);
    private boolean isServed=false;


    public void updateAircraftStats(long arriveTime, long departTime){
        this.arriveTime=arriveTime;
        this.departTime=departTime;
        this.isServed=true;
    }

    
    public int addRejectedPassengerCount() {
        synchronized(rejectedPassengerCount){
                return rejectedPassengerCount.incrementAndGet();
        }
    }
    
    public int addEmbarkedPassengerCount() {
        synchronized(embarkedPassengerCount){
                return embarkedPassengerCount.incrementAndGet();
        }
    }
    
     public int addDisembarkedPassengerCount() {
        synchronized(disembarkedPassengerCount){
                return disembarkedPassengerCount.incrementAndGet();
        }
    }

    public long getArriveTime() {
        return arriveTime;
    }


    public long getDepartTime() {
        return departTime;
    }

    public AtomicInteger getRejectedPassengerCount() {
        return rejectedPassengerCount;
    }

    public AtomicInteger getEmbarkedPassengerCount() {
        return embarkedPassengerCount;
    }

    public AtomicInteger getDisembarkedPassengerCount() {
        return disembarkedPassengerCount;
    }

    public boolean isIsServed() {
        return isServed;
    }

    public void setIsServed(boolean isServed) {
        this.isServed = isServed;
    }

    @Override
    public String toString() {
        return "Statistic{" + "arriveTime=" + arriveTime + ", departTime=" + departTime + ", rejectedPassengerCount=" + rejectedPassengerCount + ", embarkedPassengerCount=" + embarkedPassengerCount + ", disembarkedPassengerCount=" + disembarkedPassengerCount + ", isServed=" + isServed + '}';
    }
    
    
     
     
}
