/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.concurrent.Semaphore;

/**
 *
 * @author Maxine
 */
public class TrafficControl {
    private Semaphore roadToIntersectionFromRunway;
    private Semaphore roadToNearestGateFromRunway;
    private Semaphore roadToGateFromIntersection;
    private Semaphore[] roadToIntersectionFromGate;
    private Semaphore roadToStandbyFromIntersection;
    private Semaphore roadToStandbyFromNearestGate;
    private Semaphore standbyTakeOff;

    public Semaphore getRoadToIntersectionFromRunway() {
        return roadToIntersectionFromRunway;
    }

    public void setRoadToIntersectionFromRunway(Semaphore roadToIntersectionFromRunway) {
        this.roadToIntersectionFromRunway = roadToIntersectionFromRunway;
    }

    public Semaphore getRoadToNearestGateFromRunway() {
        return roadToNearestGateFromRunway;
    }

    public void setRoadToNearestGateFromRunway(Semaphore roadToNearestGateFromRunway) {
        this.roadToNearestGateFromRunway = roadToNearestGateFromRunway;
    }

    public Semaphore getRoadToGateFromIntersection() {
        return roadToGateFromIntersection;
    }

    public void setRoadToGateFromIntersection(Semaphore roadToGateFromIntersection) {
        this.roadToGateFromIntersection = roadToGateFromIntersection;
    }

    public Semaphore[] getRoadToIntersectionFromGate() {
        return roadToIntersectionFromGate;
    }

    public void setRoadToIntersectionFromGate(Semaphore[] roadToIntersectionFromGate) {
        this.roadToIntersectionFromGate = roadToIntersectionFromGate;
    }
            
    public void setRoadToIntersectionFromAGate(int i, Semaphore roadToIntersectionFromAGate) {
        roadToIntersectionFromGate[i] = roadToIntersectionFromAGate;
    }

    public Semaphore getRoadToStandbyFromIntersection() {
        return roadToStandbyFromIntersection;
    }

    public void setRoadToStandbyFromIntersection(Semaphore roadToStandbyFromIntersection) {
        this.roadToStandbyFromIntersection = roadToStandbyFromIntersection;
    }

    public Semaphore getRoadToStandbyFromNearestGate() {
        return roadToStandbyFromNearestGate;
    }

    public void setRoadToStandbyFromNearestGate(Semaphore roadToStandbyFromNearestGate) {
        this.roadToStandbyFromNearestGate = roadToStandbyFromNearestGate;
    }

    public Semaphore getStandbyTakeOff() {
        return standbyTakeOff;
    }

    public void setStandbyTakeOff(Semaphore standbyTakeOff) {
        this.standbyTakeOff = standbyTakeOff;
    }
    
    
}
