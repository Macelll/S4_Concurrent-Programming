/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author Maxine
 */
class Gate implements Runnable {

    private char name;
    private Airport airport;
    private volatile AtomicReference<Aircraft> currAircraft;

    public Gate(char name, Airport airport) {
        this.name = name;
        this.airport = airport;
        currAircraft = new AtomicReference<Aircraft>();
    }

    @Override
    public void run() {
        try {
            while (airport.getIsOperate().get()) {
                currAircraft.set(airport.getUrgentQueue().poll());
                if (currAircraft.get() != null) {


                } else {
                    currAircraft.set(airport.getNormalQueue().poll());
                }
                if (currAircraft.get() != null) {
                    //Ensure that the aircraft does not left the airport
                    taken();
                }
                Thread.sleep(100);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void taken() {
        try {
            synchronized (this) {
                if (currAircraft.get().getStatus().compareAndSet(Status.QUEUE, Status.LANDING) ||
                        currAircraft.get().getStatus().compareAndSet(Status.URGENT, Status.LANDING)) {
                    System.out.println(this.getGateCodeName() + " is assigned to " + currAircraft.get().getAircraftCodeName() + ".");
                    currAircraft.get().getAssignedGate().set(this);
                    this.wait();
                    //Will be notified by aircraft
                    currAircraft.set(null);
                    System.out.println(this.getGateCodeName() + " is available.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public char getName() {
        return name;
    }

    public void setName(char name) {
        this.name = name;
    }

    public String getGateCodeName() {
        return "Gate " + name;
    }

    public AtomicReference<Aircraft> getCurrAircraft() {
        return currAircraft;
    }

    public void setCurrAircraft(AtomicReference<Aircraft> currAircraft) {
        this.currAircraft = currAircraft;
    }

}
