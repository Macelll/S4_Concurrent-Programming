/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import static Main.minutesToMilliseconds;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Maxine
 */
public class Passenger implements Runnable {

    public int id;
    public Aircraft flight;
    public AtomicBoolean isDisembarked;
    public AtomicBoolean isEmbarked;
    public AtomicBoolean isRejected;

    public Passenger(Aircraft flight, char status) {
        flight.getAirport().passengerCount++;
        this.id = flight.getAirport().passengerCount;
        this.flight = flight;
        if (status == 'D') {
            //This passsenger is going to disembark from plane.
            isDisembarked = new AtomicBoolean(false);
            isEmbarked = new AtomicBoolean(true);
        } else if (status == 'E') {
            //This passsenger is going to embark to plane.
            isDisembarked = new AtomicBoolean(true);
            isEmbarked = new AtomicBoolean(false);
        }
        isRejected = new AtomicBoolean(false);
    }

    @Override
    public void run() {
        synchronized (this) {
            while (!isDisembarked.get()) {

                try {
                    this.wait();
                } catch (InterruptedException ex) {
                    System.err.println("Problem occurs when passsenger " + id + " is waiting to be embark to " + flight.getAircraftCodeName());
                }

                disembark();
            }
            while (!isEmbarked.get() && !isRejected.get()) {

                try {
                    this.wait();
                } catch (InterruptedException ex) {
                    System.err.println("Problem occurs when passsenger " + id + " is waiting to be embark to " + flight.getAircraftCodeName());
                }

                embark();
            }
        }

    }

    public synchronized void disembark() {
        try {
            //Two passengers are allowed to disembark at the same time
            flight.getDoor().acquire();
            Thread.sleep(new Random().nextInt(60) * minutesToMilliseconds / 60);
            System.out.println("Passenger " + id + " disembarked from " + flight.getAircraftCodeName() + ".");
            flight.getDoor().release();
            flight.getPassengersOnBoard().take();
            isDisembarked.set(true);
            flight.getStat().addDisembarkedPassengerCount();
        } catch (InterruptedException ex) {
            flight.getDoor().release();
            System.err.println("Problem occurs when passsenger " + id + " is disembarking from " + flight.getAircraftCodeName());
        }

    }

    public synchronized void embark() {
        try {
            //Two passengers are allowed to disembark at the same time
            flight.getDoor().acquire();
            Thread.sleep(new Random().nextInt(60) * minutesToMilliseconds / 60);
            System.out.println("Passenger " + id + " embarked to " + flight.getAircraftCodeName() + ".");

            if (flight.getPassengersOnBoard().offer(this)) {
                isEmbarked.set(true);
                flight.getStat().addEmbarkedPassengerCount();
            } else {
                isRejected.set(true);
                flight.getStat().addRejectedPassengerCount();
                System.err.println("Passenger " + id + " is kicked out from " + flight.getAircraftCodeName() + " because this aircraft is overbooked.");
            }
            flight.getDoor().release();
            flight.getAirport().getLounge().getWaitingPassengers(flight.getId()).remove(this);

        } catch (InterruptedException ex) {
            flight.getDoor().release();
            System.err.println("Problem occurs when passsenger " + id + " is embarking to " + flight.getAircraftCodeName());
        }

    }

}
