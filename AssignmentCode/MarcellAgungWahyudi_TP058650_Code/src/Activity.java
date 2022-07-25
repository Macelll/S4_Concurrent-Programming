/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import static atc.Main.minutesToMilliseconds;
import java.util.Random;


/**
 *
 * @author Maxine
 */
class Activity implements Runnable {

    private String name;
    private String aircraftCodeName;
    private int rangeInMinutes;
    private int minTimeInMinutes;
    private int duration = 0;

    public Activity(String aircraftCodeName,String name, int rangeInMinutes, int minTimeInMinutes) {
        this.name = name;
        this.aircraftCodeName = aircraftCodeName;
        this.rangeInMinutes = rangeInMinutes;
        this.minTimeInMinutes = minTimeInMinutes;
    }

    @Override
    public void run() {
        try {
            System.out.println(aircraftCodeName+" ongoing activity " + name);
            duration = new Random().nextInt(rangeInMinutes * minutesToMilliseconds) + minTimeInMinutes * minutesToMilliseconds;
            Thread.sleep(duration);
            System.out.println(aircraftCodeName+" completed activity " + name + " in " + duration + " milliseconds.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
