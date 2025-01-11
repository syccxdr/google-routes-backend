package com.example.google_backend.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class RouteRequestPayload {

    // Getters and Setters
    private Origin origin;
    private Destination destination;
    private String travelMode;
    private TransitPreferences transitPreferences;
    private boolean computeAlternativeRoutes;

    // Inner Classes
    public static class Origin {
        private Location location;

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }
    }

    public static class Destination {
        private Location location;

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }
    }

    public static class Location {
        private LatLng latLng;

        public Location() {}

        public Location(LatLng latLng) {
            this.latLng = latLng;
        }

        public LatLng getLatLng() {
            return latLng;
        }

        public void setLatLng(LatLng latLng) {
            this.latLng = latLng;
        }
    }

    public static class LatLng {
        private double latitude;
        private double longitude;

        public LatLng() {}

        public LatLng(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

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

    public static class TransitPreferences {
        private List<String> allowedTravelModes;

        public List<String> getAllowedTravelModes() {
            return allowedTravelModes;
        }

        public void setAllowedTravelModes(List<String> allowedTravelModes) {
            this.allowedTravelModes = allowedTravelModes;
        }
    }
}
