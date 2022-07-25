/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import static atc.Main.minutesToMilliseconds;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Maxine
 */
public class Airport {

    Comparator<Aircraft> fuelTimeSorter = Comparator.comparing(Aircraft::getNoFuelTime);

    class CompareOrder<T extends Aircraft> implements Comparator<T> {

        @Override
        public int compare(T left, T right) {
            if (left.getStatus().get() == Status.LANDING && right.getStatus().get() != Status.LANDING) {
                return -1; //push left true up the queue
            } else if (right.getStatus().get() == Status.LANDING && left.getStatus().get() != Status.LANDING) {
                return 1;  //push left false down the queue
            } else {
                return 0;  //they are the same..do nothing
            }
        }

    }
    public static int passengerCount = 0;

    private volatile AtomicBoolean isOperate;
    private Thread[] gateThreads;
    private Thread[] aircraftThreads;
    private Gate[] gates;
    private Aircraft[] aircrafts;

    private BlockingQueue<Aircraft> normalQueue;
    private PriorityBlockingQueue<Aircraft> urgentQueue;
    private ReentrantLock runway;
    private ReentrantLock intersection;
    private TrafficControl traffic;
    private Lounge lounge;
    int aircraftCounter = 0;
    int passengerCounter = 0;

    public Airport(int gatesCount, int roadLength, int standbyLength, int maxAircraftInQueue) {
        traffic = new TrafficControl();
        gateThreads = new Thread[gatesCount];
        gates = new Gate[gatesCount];

        traffic.setRoadToIntersectionFromGate(new Semaphore[gatesCount]);
        char gateName = 'A';
        for (int i = 0; i < gatesCount; i++, gateName++) {
            gateThreads[i] = new Thread(new Gate(gateName, this));
            if (i != gatesCount - 1) {
                traffic.setRoadToIntersectionFromAGate(i, new Semaphore(roadLength, true));
            }
        }

        aircraftThreads = new Thread[10];
        aircrafts = new Aircraft[10];

        isOperate = new AtomicBoolean(true);

        normalQueue = new LinkedBlockingQueue<>(maxAircraftInQueue);

        urgentQueue = new PriorityBlockingQueue<>(gatesCount, fuelTimeSorter);

        runway = new ReentrantLock(true);

        intersection = new ReentrantLock(true); //To avoid starvation

        traffic.setRoadToNearestGateFromRunway(new Semaphore(roadLength, true));

        traffic.setRoadToGateFromIntersection(new Semaphore(roadLength, true));

        traffic.setRoadToIntersectionFromRunway(new Semaphore(roadLength, true));

        traffic.setRoadToStandbyFromIntersection(new Semaphore(roadLength, true));

        traffic.setRoadToStandbyFromNearestGate(new Semaphore(roadLength, true));

        traffic.setStandbyTakeOff(new Semaphore(standbyLength, true));

        lounge = new Lounge();

    }

    public void operate() {
        //Gate is open
        for (Thread g : gateThreads) {
            g.start();
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Airport.class.getName()).log(Level.SEVERE, null, ex);
        }

        Thread aircraftGenerator = new Thread() {
            @Override
            public void run() {
                while (aircraftCounter < 10) {
                    aircraftCounter++;
                    Aircraft newAircraft = new Aircraft("" + aircraftCounter, Airport.this);

                    //Passengers generated before aircraft arrive
                    LinkedList<Passenger> embarkingPassengers = new LinkedList();
                    int passengerTotalCount = new Random().nextInt(60) + 1;
                    for (int i = 0; i < passengerTotalCount; i++) {
                        Passenger embarkingPassenger = new Passenger(newAircraft, 'E');
                        embarkingPassengers.add(embarkingPassenger);

                        Thread embarkingPassengerThread = new Thread(embarkingPassenger);
                        embarkingPassengerThread.start();
                    }
                    lounge.getWaitingPassengersTable().put("" + aircraftCounter, embarkingPassengers);
                    aircrafts[aircraftCounter - 1] = newAircraft;
                    aircraftThreads[aircraftCounter - 1] = new Thread(newAircraft);
                    aircraftThreads[aircraftCounter - 1].start();

                    try {
                        this.sleep(new Random().nextInt(4) * 1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        };

        aircraftGenerator.start();

        try {
            aircraftGenerator.join();
            int counter = 0;
            for (Thread aircraft : aircraftThreads) {
                counter++;
                aircraft.join();
                System.out.println("Aircraft " + counter + " has done execution");
            }
            isOperate.set(false);
            counter = 0;
            for (Thread g : gateThreads) {
                counter++;
                g.join();
                System.out.println("Gate " + counter + " has done execution");
            }

        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        printStatistics();
    }

    public void printStatistics() {
        int totalAircraftServed = 0;
        int totalAircraftFailed = 0;
        int totalPassengers = 0;
        int totalPassengersEmbarked = 0;
        int totalPassengersDisembarked = 0;
        int totalPassengersRejected = 0;
        long maxTurnaroundTime = 0;
        long minTurnaroundTime = 0;
        long avgTurnaroundTime = 0;
        try {
            Thread.sleep(1000);

        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        for (Aircraft aircraft : aircrafts) {
            if (aircraft.getStat().isIsServed()) {
                totalAircraftServed++;

                totalPassengersEmbarked += aircraft.getStat().getEmbarkedPassengerCount().get();
                totalPassengersDisembarked += aircraft.getStat().getDisembarkedPassengerCount().get();
                totalPassengersRejected += aircraft.getStat().getRejectedPassengerCount().get();
                long turnaroundTime = aircraft.getStat().getDepartTime() - aircraft.getStat().getArriveTime();
                if (turnaroundTime > maxTurnaroundTime) {
                    maxTurnaroundTime = turnaroundTime;
                }
                if (totalAircraftServed == 1) {
                    minTurnaroundTime = turnaroundTime;
                } else if (turnaroundTime <= minTurnaroundTime) {
                    minTurnaroundTime = turnaroundTime;
                }
                avgTurnaroundTime = (avgTurnaroundTime * (totalAircraftServed - 1) + (turnaroundTime)) / totalAircraftServed;
            } else {
                totalAircraftFailed++;
            }
        }
        totalPassengers = totalPassengersEmbarked + totalPassengersDisembarked + totalPassengersRejected;
        System.out.println("Total Aircrafts Served: " + totalAircraftServed);
        System.out.println("Total Aircrafts Failed: " + totalAircraftFailed);
        System.out.println("Total Passengers: " + totalPassengers);
        System.out.println("Total Passengers Embarked: " + totalPassengersEmbarked);
        System.out.println("Total Passengers Disembarked: " + totalPassengersDisembarked);
        System.out.println("Total Passengers Rejected: " + totalPassengersRejected);
        System.out.println("Maximum Turnaround Minutes: " + maxTurnaroundTime / minutesToMilliseconds);
        System.out.println("Maximum Turnaround Milliseconds: " + maxTurnaroundTime);

        System.out.println("Minimum Turnaround Minutes: " + minTurnaroundTime / minutesToMilliseconds);
        System.out.println("Minimum Turnaround Milliseconds: " + minTurnaroundTime);

        System.out.println("Average Turnaround Minutes: " + avgTurnaroundTime / minutesToMilliseconds);
        System.out.println("Average Turnaround Milliseconds: " + avgTurnaroundTime);

    }

    public AtomicBoolean getIsOperate() {
        return isOperate;
    }

    public void setIsOperate(AtomicBoolean isOperate) {
        this.isOperate = isOperate;
    }

    public Thread[] getGateThreads() {
        return gateThreads;
    }

    public void setGateThreads(Thread[] gateThreads) {
        this.gateThreads = gateThreads;
    }

    public Thread[] getAircraftThreads() {
        return aircraftThreads;
    }

    public void setAircraftThreads(Thread[] aircraftThreads) {
        this.aircraftThreads = aircraftThreads;
    }

    public BlockingQueue<Aircraft> getNormalQueue() {
        return normalQueue;
    }

    public void setNormalQueue(BlockingQueue<Aircraft> normalQueue) {
        this.normalQueue = normalQueue;
    }

    public PriorityBlockingQueue<Aircraft> getUrgentQueue() {
        return urgentQueue;
    }

    public void setUrgentQueue(PriorityBlockingQueue<Aircraft> urgentQueue) {
        this.urgentQueue = urgentQueue;
    }

    public ReentrantLock getRunway() {
        return runway;
    }

    public void setRunway(ReentrantLock runway) {
        this.runway = runway;
    }

    public ReentrantLock getIntersection() {
        return intersection;
    }

    public void setIntersection(ReentrantLock intersection) {
        this.intersection = intersection;
    }

    public Lounge getLounge() {
        return lounge;
    }

    public void setLounge(Lounge lounge) {
        this.lounge = lounge;
    }

    public TrafficControl getTraffic() {
        return traffic;
    }

    public void setTraffic(TrafficControl traffic) {
        this.traffic = traffic;
    }

}
