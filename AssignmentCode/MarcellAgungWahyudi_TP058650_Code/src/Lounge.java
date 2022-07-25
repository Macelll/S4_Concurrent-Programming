/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import static atc.Main.minutesToMilliseconds;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Random;

/**
 *
 * @author Maxine
 */
public class Lounge {

    private Hashtable<String, LinkedList<Passenger>> waitingPassengersTable;
    private Hashtable<String, LinkedList<Thread>> waitingPassengersThreadsTable;

    public Lounge() {
        waitingPassengersTable = new Hashtable<String, LinkedList<Passenger>>();
        waitingPassengersThreadsTable = new Hashtable<String, LinkedList<Thread>>();

    }

    public int countOfPassengers(String id) {
        LinkedList flightWaitingPassengers = waitingPassengersTable.get(id);
        if (flightWaitingPassengers == null) {
            return 0;
        } else {
            return flightWaitingPassengers.size();
        }
    }

    public Hashtable<String, LinkedList<Passenger>> getWaitingPassengersTable() {
        return waitingPassengersTable;
    }

    public void setWaitingPassengersTable(Hashtable<String, LinkedList<Passenger>> waitingPassengersTable) {
        this.waitingPassengersTable = waitingPassengersTable;
    }

    public LinkedList<Passenger> getWaitingPassengers(String id) {
        return waitingPassengersTable.get(id);
    }

    public Hashtable<String, LinkedList<Thread>> getWaitingPassengersThreadsTable() {
        return waitingPassengersThreadsTable;
    }

    public void setWaitingPassengersThreadsTable(Hashtable<String, LinkedList<Thread>> waitingPassengersThreadsTable) {
        this.waitingPassengersThreadsTable = waitingPassengersThreadsTable;
    }
    
    public LinkedList<Thread> getWaitingPassengersThreads(String id) {
        return waitingPassengersThreadsTable.get(id);
    }
}
