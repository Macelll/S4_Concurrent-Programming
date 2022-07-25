import

import static atc.Main.durationToAnotherAirport;
import static atc.Main.Main.gateCount;
import static atc.Main.minutesToMilliseconds;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;
import static atc.Main.chillingTime;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

enum Status {
    NEW(0), QUEUE(1), URGENT(1), LANDING(2), DOCKING(3), GATE(4), UNDOCKING(5), TAKEOFF(6), LEFT(7), ISSUE(-1);

    private int phase;

    private Status(int phase) {
        this.phase = phase;
    }

    public int getPhase() {
        return phase;
    }

}

class Aircraft implements Runnable {

    Random rnd = new Random();
    private String id;
    private int fuelTime;
    private int decisionTime;
    private long arriveTime;
    private long leftTime;
    private Statistic stat;
    private Airport airport;
    private AtomicReference<Gate> assignedGate;
    private ArrayBlockingQueue<Passenger> passengersOnBoard;
    private ArrayBlockingQueue<Thread> passengersThreadsOnBoard;

    private AtomicBoolean allowDisembark;
    private AtomicBoolean allowEmbark;
    private Semaphore doorCapactity;

    private volatile AtomicReference<Status> status;

    public Aircraft(String id, Airport airport) {
        this.id = id;
        this.airport = airport;
        this.stat = new Statistic();
        arriveTime = System.currentTimeMillis();
        decisionTime = rnd.nextInt(5 * minutesToMilliseconds) + 1 * minutesToMilliseconds;
        fuelTime = rnd.nextInt(chillingTime * 5) + 1 * minutesToMilliseconds + durationToAnotherAirport;
        status = new AtomicReference<Status>(Status.NEW);
        assignedGate = new AtomicReference<Gate>();
        passengersOnBoard = new ArrayBlockingQueue<Passenger>(50);
        passengersThreadsOnBoard = new ArrayBlockingQueue<Thread>(50);

        allowDisembark = new AtomicBoolean(false);
        allowEmbark = new AtomicBoolean(false);
        doorCapactity = new Semaphore(2);
        int currentNumberOfPassengers = rnd.nextInt(51);
        for (int i = 0; i < currentNumberOfPassengers; i++) {
            Passenger currentPassenger = new Passenger(this, 'D');
            passengersOnBoard.add(currentPassenger);

            Thread currentPassengerThread = new Thread(currentPassenger);
            passengersThreadsOnBoard.add(currentPassengerThread);
            currentPassengerThread.start();
        }

    }

    @Override
    public void run() {

        //The thread will be alive until the aircraft had taken off
        while (status.get() != Status.LEFT && status.get() != Status.ISSUE) {
            //Add new incoming aircraft into landing queue, there is a limited capacity for landing queue.
            if (status.get() == Status.NEW) {
                addToQueue();
            }

            //Add aircraft that is running out of fuel into priority queue.
            if (status.get() == Status.QUEUE) {
                //The aircraft will consider the extreme situation that the docked aircraft took the maximum time to undock
                addToUrgentQueue();

            }

            //Inform that the aircraft is landing and assign a duration for the aircraft to land
            if (status.get() == Status.LANDING) {
                landing();
            }

            if (arriveTime + fuelTime - System.currentTimeMillis() >= durationToAnotherAirport && arriveTime + fuelTime - System.currentTimeMillis() <= durationToAnotherAirport + decisionTime && status.get().getPhase() < 2) { //also larger than the time to another airport
                if (status.compareAndSet(Status.URGENT, Status.LEFT) || status.compareAndSet(Status.QUEUE, Status.LEFT) || status.compareAndSet(Status.LANDING, Status.LEFT)) {
                    airport.getNormalQueue().remove(this);
                    airport.getUrgentQueue().remove(this);
                    System.err.println(this.getAircraftCodeName() + " left for another airport because the airport is too busy and it is running out of fuel.");

                }
            }

            //Aircraft does not have enough fuel time to reach another airport, which is when the fuel time is less than 30 minutes.
            if (arriveTime + fuelTime - System.currentTimeMillis() < durationToAnotherAirport && status.get().getPhase() < 2) {
                //Should be leaving from urgent queue
                System.err.println(this.getAircraftCodeName() + " does not have the fuel to fly to another airport. The plane is landing in an abandoned airport just in case.");
                airport.getNormalQueue().remove(this);
                airport.getUrgentQueue().remove(this);
                status.set(Status.ISSUE);
            }

            //Aircraft crashes when the fuel time is less than 0. 
            //Starvation
            if (arriveTime + fuelTime - System.currentTimeMillis() < 0 && status.get().getPhase() <= 2) {
                airport.getNormalQueue().remove(this);
                airport.getUrgentQueue().remove(this);
                System.err.println("BBC News Report: " + this.getAircraftCodeName() + " had crashed in Asia Pacific Airport due to disastrous air traffic control system.");

                status.set(Status.ISSUE);
            }

            if (status.get() == Status.ISSUE) {
                System.err.println(this);
            }
        }
    }

    public synchronized void addToQueue() {
        try {
            if (arriveTime + fuelTime - System.currentTimeMillis() < durationToAnotherAirport + decisionTime) {
                if (status.compareAndSet(Status.NEW, Status.LEFT)) {
                    System.out.println(this.getAircraftCodeName() + " does not have enough fuel to wait, the pilot is flying to another airport.");
                    return;
                } else {
                    System.err.println(this.getAircraftCodeName() + "'s status is illegal!");
                    rescue();
                }
            } else {
                while (!airport.getNormalQueue().add(this)) {
                    System.err.println(this.getAircraftCodeName() + " is being added to normal queue");
                };
                System.out.println(this.getAircraftCodeName() + " is added to normal queue");

                if (status.compareAndSet(Status.NEW, Status.QUEUE)) {
                    System.out.println(this.getAircraftCodeName() + " is queueing with " + fuelTime + " milliseconds of fuel time remaining.");
                } else {
                    System.err.println(this.getAircraftCodeName() + "'s status is illegal!");
                    rescue();
                }
            }
            addToUrgentQueue();

        } catch (IllegalStateException e) {
            System.out.println("The airport is too packed. " + this.getAircraftCodeName() + " is flying to another airport.");
            status.set(Status.LEFT);
        }
    }

    public synchronized void addToUrgentQueue() {
        if (arriveTime + fuelTime - System.currentTimeMillis() < durationToAnotherAirport + chillingTime) {
            airport.getNormalQueue().remove(this);

            while (!airport.getUrgentQueue().add(this)) {
                System.err.println(this.getAircraftCodeName() + " is being added to urgent queue with "+(arriveTime + fuelTime - System.currentTimeMillis())+" milliseconds of fuel time left.");
            };
            System.out.println(this.getAircraftCodeName() + " is added to urgent queue");
            if (!(status.compareAndSet(Status.QUEUE, Status.URGENT) || status.get() == Status.LANDING)) {
                System.out.println(this.getAircraftCodeName() + "'s status is illegal!");
                rescue();
            }
            System.out.println(this.getAircraftCodeName() + " is running low on fuel and has been added to the emergency queue.");
        }
    }

    public synchronized void landing() {
        if (assignedGate.get() != null) {
            int landingTime = rnd.nextInt(3 * minutesToMilliseconds) + 1 * minutesToMilliseconds;
            try {
                synchronized (assignedGate.get()) {
                    if (assignedGate.get().getName() != 'A') {
                        //Another approach of 
                        if (!airport.getTraffic().getRoadToIntersectionFromRunway().tryAcquire(arriveTime + fuelTime - System.currentTimeMillis() - durationToAnotherAirport - decisionTime, TimeUnit.MILLISECONDS)) {
                            assignedGate.get().notify();
                            assignedGate.set(null);
                            return;
                        }
                    } else {
                        if (!airport.getTraffic().getRoadToNearestGateFromRunway().tryAcquire(arriveTime + fuelTime - System.currentTimeMillis() - durationToAnotherAirport - decisionTime, TimeUnit.MILLISECONDS)) {
                            assignedGate.get().notify();
                            assignedGate.set(null);
                            return;
                        }
                    }
                }
            } catch (InterruptedException ex) {
                rescue();
                ex.printStackTrace();
                return;
            }

            try {
                if (airport.getRunway().tryLock(arriveTime + fuelTime - System.currentTimeMillis() - durationToAnotherAirport - decisionTime, TimeUnit.MILLISECONDS)) {

                    System.out.println(this.getAircraftCodeName() + " is using the runway for landing after assigned to " + assignedGate.get().getGateCodeName() + ".");
                    Thread.sleep(landingTime);

                    System.out.println(this.getAircraftCodeName() + " has completed landing in " + landingTime + " milliseconds.");
                    if (!status.compareAndSet(Status.LANDING, Status.DOCKING)) {
                        System.out.println(this.getAircraftCodeName() + "'s status is illegal!");
                        rescue();
                        airport.getRunway().unlock();
                    }
                } else {
                    if (assignedGate.get() != null) {
                        assignedGate.get().notifyAll();
                    }
                    airport.getTraffic().getRoadToIntersectionFromRunway().release();
                    return;
                }
            } catch (InterruptedException ex) {
                rescue();
                airport.getTraffic().getRoadToIntersectionFromRunway().release();
                ex.printStackTrace();
                return;
            }
            docking();
        }

    }

    public synchronized void docking() {

        //Use intersection if not first gate
        if (assignedGate.get().getName() != 'A') {

            airport.getRunway().unlock();
            System.out.println(this.getAircraftCodeName() + " has freed the runway access after landing.");

            System.out.println(this.getAircraftCodeName() + " is on the road to intersection.");
            try {
                Thread.sleep(2 * minutesToMilliseconds);
            } catch (InterruptedException ex) {
                rescue();
                airport.getTraffic().getRoadToIntersectionFromRunway().release();
                ex.printStackTrace();
                return;
            }

            try {
                //Check if from intersection to gate is free before locking the intersection
                airport.getTraffic().getRoadToGateFromIntersection().acquire();
                System.out.println("Road to " + assignedGate.get().getGateCodeName() + " from intersection is free.");

            } catch (InterruptedException ex) {
                rescue();
                airport.getTraffic().getRoadToIntersectionFromRunway().release();
                ex.printStackTrace();
                return;
            }

            airport.getIntersection().lock();
            System.out.println(this.getAircraftCodeName() + " is using the intersection.");
            airport.getTraffic().getRoadToIntersectionFromRunway().release();

            try {
                Thread.sleep(1 * minutesToMilliseconds);
                System.out.println(this.getAircraftCodeName() + " is on the road to " + assignedGate.get().getGateCodeName() + ".");
            } catch (InterruptedException ex) {
                rescue();
                airport.getTraffic().getRoadToGateFromIntersection().release();
                ex.printStackTrace();
                return;
            } finally {
                airport.getIntersection().unlock();
            }

            try {
                Thread.sleep(2 * minutesToMilliseconds);
            } catch (InterruptedException ex) {
                rescue();
                ex.printStackTrace();
                return;
            } finally {
                airport.getTraffic().getRoadToGateFromIntersection().release();
            }

        } else {
            System.out.println(this.getAircraftCodeName() + " is on the road to " + assignedGate.get().getGateCodeName() + ".");
            System.out.println(this.getAircraftCodeName() + " has freed the runway access after landing.");
            airport.getRunway().unlock();

            try {
                Thread.sleep(2 * minutesToMilliseconds);
            } catch (InterruptedException ex) {
                rescue();
                ex.printStackTrace();
                return;
            } finally {
                airport.getTraffic().getRoadToNearestGateFromRunway().release();
            }
        }

        int dockingTime = rnd.nextInt(4 * minutesToMilliseconds);
        try {
            Thread.sleep(dockingTime);
        } catch (InterruptedException ex) {
            rescue();
            ex.printStackTrace();
            return;
        }
        System.out.println(this.getAircraftCodeName() + " has docked to " + assignedGate.get().getGateCodeName() + " in " + dockingTime + " milliseconds.");
        if (!status.compareAndSet(Status.DOCKING, Status.GATE)) {
            System.out.println(this.getAircraftCodeName() + "'s status is illegal!");
            rescue();

        }
        working();

    }

    public synchronized void working() {
        try {

            //Concurrent, will be working throughout the process
            Thread refillFuel = new Thread(new Activity(this.getAircraftCodeName(), "Refill fuel", 51, 10));
            refillFuel.start();

            //Concurrent
            //Allow passenger to disembark
            System.out.println(this.getAircraftCodeName() + " allows passenger to disembark. There are a total of " + passengersOnBoard.size() + " passengers on board.");
            if (allowDisembark.compareAndSet(false, true)) {

                for (Passenger p : passengersOnBoard) {
                    synchronized (p) {
                        p.notify();
                    }
                }
                //Wait until passengers are all disembarked
                while (passengersOnBoard.size() != 0) {

                }
                System.out.println(this.getAircraftCodeName() + " has all the passengers disembarked.");
            }
            //Sequential, starts right after all the passengers disembarked.
            Thread cleanCabin = new Thread(new Activity(this.getAircraftCodeName(), "Clean cabin", 21, 10));
            Thread refillSupplies = new Thread(new Activity(this.getAircraftCodeName(), "Refill supplies", 21, 10));
            cleanCabin.start();
            refillSupplies.start();

            //Sequential, starts right after all the passengers disembarked.
            cleanCabin.join();
            refillSupplies.join();

            //Sequential
            //Allow passenger to embark
            int counter = airport.getLounge().countOfPassengers(id);
            System.out.println(this.getAircraftCodeName() + " allows passenger to embark. There are a total of " + counter + " passengers waiting.");
            if (allowEmbark.compareAndSet(false, true)) {
                for (Passenger p : airport.getLounge().getWaitingPassengers(id)) {
                    synchronized (p) {
                        p.notify();
                    }
                }
            }

            //Wait until passengers are all embarked
            while (counter != passengersOnBoard.size() && passengersOnBoard.size() < 50) {

            }
            System.out.println(this.getAircraftCodeName() + " has a total of " + passengersOnBoard.size() + " passengers embarked.");

            //Wait for fuel to compl·ete refuel
            refillFuel.join();

            System.out.println(this.getAircraftCodeName() + " is ready to go! Undocking...");
            if (!status.compareAndSet(Status.GATE, Status.UNDOCKING)) {
                System.out.println(this.getAircraftCodeName() + "'s status is illegal!");
            }

        } catch (Exception e) {
            rescue();
            e.printStackTrace();
            return;
        }

        undocking();
    }

    public synchronized void undocking() {

        int undockingTime = (rnd.nextInt(4 * minutesToMilliseconds));

        if (assignedGate.get().getName() != 'A' + gateCount - 1) {
            synchronized (assignedGate.get()) {
                try {
                    airport.getTraffic().getRoadToIntersectionFromGate()[assignedGate.get().getName() - 'A'].acquire();
                    System.out.println("Road to intersection from " + assignedGate.get().getGateCodeName() + " is free.");
                } catch (InterruptedException ex) {
                    rescue();
                    ex.printStackTrace();
                }

                try {
                    //Aircraft is undocking
                    Thread.sleep(undockingTime);

                } catch (InterruptedException ex) {
                    rescue();
                    ex.printStackTrace();
                }

                System.out.println(this.getAircraftCodeName() + " has undocked from " + assignedGate.get().getGateCodeName() + " in " + undockingTime + " milliseconds.");
                try {
                    assignedGate.get().notify();
                } catch (Exception e) {
                    rescue();
                    e.printStackTrace();
                }
            }
            System.out.println(this.getAircraftCodeName() + " is on the road to intersection from " + assignedGate.get().getGateCodeName() + ".");
            try {
                Thread.sleep(2 * minutesToMilliseconds);
            } catch (InterruptedException ex) {
                rescue();
                ex.printStackTrace();
                return;
            }

            try {
                //Check if from intersection to standby is free before locking the intersection
                airport.getTraffic().getRoadToStandbyFromIntersection().acquire();
                System.out.println("Road to take-off standby from intersection is free.");

            } catch (InterruptedException ex) {
                rescue();
                ex.printStackTrace();
                return;
            }

            airport.getIntersection().lock();
            System.out.println(this.getAircraftCodeName() + " is using the intersection.");
            airport.getTraffic().getRoadToIntersectionFromGate()[assignedGate.get().getName() - 'A'].release();

            try {
                Thread.sleep(1 * minutesToMilliseconds); //Going through intersection
                System.out.println(this.getAircraftCodeName() + " is on the road to standby take-off.");
            } catch (InterruptedException ex) {
                rescue();
                airport.getTraffic().getRoadToStandbyFromIntersection().release();
                ex.printStackTrace();
                return;
            } finally {
                airport.getIntersection().unlock();
            }

            try {
                Thread.sleep(2 * minutesToMilliseconds);
                airport.getTraffic().getStandbyTakeOff().acquire();
                System.out.println(this.getAircraftCodeName() + " is in standby to take-off.");

            } catch (InterruptedException ex) {
                rescue();
                ex.printStackTrace();
                return;
            } finally {
                airport.getTraffic().getRoadToStandbyFromIntersection().release();
            }

        } else {

            synchronized (assignedGate.get()) {
                try {
                    airport.getTraffic().getRoadToStandbyFromNearestGate().acquire();
                    System.out.println("Road to standby from " + assignedGate.get().getGateCodeName() + " is free.");
                } catch (InterruptedException ex) {
                    rescue();
                    ex.printStackTrace();
                    return;
                }

                try {
                    //Aircraft is undocking
                    Thread.sleep(undockingTime);

                } catch (InterruptedException ex) {
                    rescue();
                    ex.printStackTrace();
                    return;
                }

                System.out.println(this.getAircraftCodeName() + " has undocked from " + assignedGate.get().getGateCodeName() + " in " + undockingTime + " milliseconds.");
                assignedGate.get().notify();
            }
            System.out.println(this.getAircraftCodeName() + " is on the road to standby from " + assignedGate.get().getGateCodeName() + ".");
            try {
                Thread.sleep(2 * minutesToMilliseconds);
                airport.getTraffic().getStandbyTakeOff().acquire();
                System.out.println(this.getAircraftCodeName() + " is in standby to take-off.");

            } catch (InterruptedException ex) {
                rescue();
                ex.printStackTrace();
                return;
            } finally {
                airport.getTraffic().getRoadToStandbyFromNearestGate().release();
            }

        }
        if (!status.compareAndSet(Status.UNDOCKING, Status.TAKEOFF)) {
            System.out.println(this.getAircraftCodeName() + "'s status is illegal!");
            rescue();
        }
        airport.getRunway().lock();
        airport.getTraffic().getStandbyTakeOff().release();
        System.out.println(this.getAircraftCodeName() + "'s turn to use the runway.");
        takingOff();
    }

    public synchronized void takingOff() {
        try {
            int takeOffTime = (rnd.nextInt(3 * minutesToMilliseconds) + 1 * minutesToMilliseconds);
            Thread.sleep(takeOffTime);
            System.out.println(this.getAircraftCodeName() + " took off in " + takeOffTime + " milliseconds. We wish them a safe flight.");
            stat.updateAircraftStats(arriveTime, System.currentTimeMillis());
            if (!status.compareAndSet(Status.TAKEOFF, Status.LEFT)) {
                System.out.println(this.getAircraftCodeName() + "'s status is illegal!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        } finally {
            System.out.println(this.getAircraftCodeName() + " has freed the runway access after taking off.");
            airport.getRunway().unlock();

        }
    }

    public void rescue() {
        try {
            Thread.sleep(3 * minutesToMilliseconds);
            System.out.println(this.getAircraftCodeName() + " had been removed from the path. We are still studying the problem.");
            status.set(Status.ISSUE);
            if (assignedGate.get() != null) {
                assignedGate.get().notifyAll();
            }

        } catch (Exception e) {
            while (true) {
                System.err.println("Rescue " + this.getAircraftCodeName() + " failed!");
            }
        }
    }

    public String getAircraftCodeName() {
        return "Aircraft No." + id;
    }

    public int getNoFuelTime() {
        return (int) (arriveTime + fuelTime);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getDecisionTime() {
        return decisionTime;
    }

    public void setDecisionTime(int decisionTime) {
        this.decisionTime = decisionTime;
    }

    public int getFuelTime() {
        return fuelTime;
    }

    public void setFuelTime(int fuelTime) {
        this.fuelTime = fuelTime;
    }

    public long getArriveTime() {
        return arriveTime;
    }

    public void setArriveTime(long arriveTime) {
        this.arriveTime = arriveTime;
    }

    public Airport getAirport() {
        return airport;
    }

    public void setAirport(Airport airport) {
        this.airport = airport;
    }

    public AtomicReference<Gate> getAssignedGate() {
        return assignedGate;
    }

    public void setAssignedGate(AtomicReference<Gate> assignedGate) {
        this.assignedGate = assignedGate;
    }

    public AtomicReference<Status> getStatus() {
        return status;
    }

    public void setStatus(AtomicReference<Status> status) {
        this.status = status;
    }

    public ArrayBlockingQueue<Passenger> getPassengersOnBoard() {
        return passengersOnBoard;
    }

    public void setPassengersOnBoard(ArrayBlockingQueue<Passenger> passengersOnBoard) {
        this.passengersOnBoard = passengersOnBoard;
    }

    public AtomicBoolean getAllowDisembark() {
        return allowDisembark;
    }

    public void setAllowDisembark(AtomicBoolean allowDisembark) {
        this.allowDisembark = allowDisembark;
    }

    public AtomicBoolean getAllowEmbark() {
        return allowEmbark;
    }

    public void setAllowEmbark(AtomicBoolean allowEmbark) {
        this.allowEmbark = allowEmbark;
    }

    public Semaphore getDoor() {
        return doorCapactity;
    }

    public void setDoorCapactity(Semaphore doorCapactity) {
        this.doorCapactity = doorCapactity;
    }

    public Statistic getStat() {
        return stat;
    }

    public void setStat(Statistic stat) {
        this.stat = stat;
    }

    @Override
    public String toString() {
        return "Aircraft{" + "rnd=" + rnd + ", id=" + id + ", fuelTime=" + fuelTime + ", arriveTime=" + arriveTime + ", airport=" + airport + ", assignedGate.get()=" + assignedGate.get() + ", status=" + status + '}';
    }

}
