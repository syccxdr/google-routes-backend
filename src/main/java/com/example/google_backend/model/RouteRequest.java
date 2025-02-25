package com.example.google_backend.model;

import java.io.Serializable;
import java.util.List;

public class RouteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Coordinates origin;
    private Coordinates destination;
    private String travelMode;
    private List<String> transitModes;

    // Getters and Setters

    public Coordinates getOrigin() {
        return origin;
    }

    public void setOrigin(Coordinates origin) {
        this.origin = origin;
    }

    public Coordinates getDestination() {
        return destination;
    }

    public void setDestination(Coordinates destination) {
        this.destination = destination;
    }

    public String getTravelMode() {
        return travelMode;
    }

    public void setTravelMode(String travelMode) {
        this.travelMode = travelMode;
    }

    public List<String> getTransitModes() {
        return transitModes;
    }

    public void setTransitModes(List<String> transitModes) {
        this.transitModes = transitModes;
    }

    // 内部类表示坐标
    public static class Coordinates {
        private double latitude;
        private double longitude;

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }
    }
}