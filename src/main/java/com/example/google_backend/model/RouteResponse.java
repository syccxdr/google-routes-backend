package com.example.google_backend.model;

import java.util.List;

public class RouteResponse {
    private List<RouteDetail> routes;

    // Getters and Setters

    public List<RouteDetail> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RouteDetail> routes) {
        this.routes = routes;
    }

    // 内部类表示单条路线的详细信息
    public static class RouteDetail {
        private String summary;
        private String distanceMeters;
        private String duration;
        private String polyline; // 可以用于在地图上绘制路线
        private List<LegDetail> legs;
        private String steps;

        // Constructors, Getters, and Setters

        public RouteDetail() {
        }

        public RouteDetail(String summary, String distanceMeters, String duration, String polyline) {
            this.summary = summary;
            this.distanceMeters = distanceMeters;
            this.duration = duration;
            this.polyline = polyline;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getDistanceMeters() {
            return distanceMeters;
        }

        public void setDistanceMeters(String distanceMeters) {
            this.distanceMeters = distanceMeters;
        }

        public String getDuration() {
            return duration;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }

        public String getPolyline() {
            return polyline;
        }

        public void setPolyline(String polyline) {
            this.polyline = polyline;
        }

        public List<LegDetail> getLegs() {
            return legs;
        }

        public void setLegs(List<LegDetail> legs) {
            this.legs = legs;
        }

        public String getSteps() {
            return steps;
        }

        public void setSteps(String steps) {
            this.steps = steps;
        }
    }

    public static class LegDetail {
        private String startLocation;
        private String endLocation;
        private String distance;
        private String duration;
        private List<StepDetail> steps;
        private String travelMode;

        // Getters and Setters

        public String getStartLocation() {
            return startLocation;
        }

        public void setStartLocation(String startLocation) {
            this.startLocation = startLocation;
        }

        public String getEndLocation() {
            return endLocation;
        }

        public void setEndLocation(String endLocation) {
            this.endLocation = endLocation;
        }

        public String getDistance() {
            return distance;
        }

        public void setDistance(String distance) {
            this.distance = distance;
        }

        public String getDuration() {
            return duration;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }

        public List<StepDetail> getSteps() {
            return steps;
        }

        public void setSteps(List<StepDetail> steps) {
            this.steps = steps;
        }

        public void setTravelMode(String travelMode) {
            this.travelMode = travelMode;
        }
    }

    public static class StepDetail {
        private String instruction;
        private long distance;
        private long duration;
        private String headsign;
        private TransitLine transitLine;
        private TransitDetails transitDetails;
        private int stopCount;
        private String polyline;
        private String travelMode;

        // Constructors, Getters, and Setters

        public StepDetail() {
        }

        public StepDetail(String instruction, long distance, long duration, String headsign, TransitLine transitLine) {
            this.instruction = instruction;
            this.distance = distance;
            this.duration = duration;
            this.headsign = headsign;
            this.transitLine = transitLine;
        }

        public String getInstruction() {
            return instruction;
        }

        public void setInstruction(String instruction) {
            this.instruction = instruction;
        }

        public long getDistance() {
            return distance;
        }

        public void setDistance(long distance) {
            this.distance = distance;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public String getHeadsign() {
            return headsign;
        }

        public void setHeadsign(String headsign) {
            this.headsign = headsign;
        }

        public TransitLine getTransitLine() {
            return transitLine;
        }

        public void setTransitLine(TransitLine transitLine) {
            this.transitLine = transitLine;
        }

        public TransitDetails getTransitDetails() {
            return transitDetails;
        }

        public void setTransitDetails(TransitDetails transitDetails) {
            this.transitDetails = transitDetails;
        }

        public int getStopCount() {
            return stopCount;
        }

        public void setStopCount(int stopCount) {
            this.stopCount = stopCount;
        }

        public String getPolyline() {
            return polyline;
        }

        public void setPolyline(String polyline) {
            this.polyline = polyline;
        }
        public void setTravelMode(String polyline) {
            this.travelMode=travelMode;
        }
        public String getTravelMode() {
            return this.travelMode;
        }

        // TransitDetails 类
        public static class TransitDetails {
            private long waitTimeSeconds;
            private StopDetails stopDetails;
            private TransitLine transitLine;
            private String headsign;
            private int stopCount;

            // Constructors, Getters, and Setters

            public TransitDetails() {
            }

            public TransitDetails(StopDetails stopDetails, TransitLine transitLine, String headsign, int stopCount,
                                  long waitTimeSeconds) {
                this.stopDetails = stopDetails;
                this.transitLine = transitLine;
                this.headsign = headsign;
                this.stopCount = stopCount;
                this.waitTimeSeconds = waitTimeSeconds;

            }

            public StopDetails getStopDetails() {
                return stopDetails;
            }

            public void setStopDetails(StopDetails stopDetails) {
                this.stopDetails = stopDetails;
            }

            public TransitLine getTransitLine() {
                return transitLine;
            }

            public void setTransitLine(TransitLine transitLine) {
                this.transitLine = transitLine;
            }

            public String getHeadsign() {
                return headsign;
            }

            public void setHeadsign(String headsign) {
                this.headsign = headsign;
            }

            public int getStopCount() {
                return stopCount;
            }

            public void setStopCount(int stopCount) {
                this.stopCount = stopCount;
            }

            // 新增的 getter 和 setter
            public long getWaitTimeSeconds() {
                return waitTimeSeconds;
            }

            public void setWaitTimeSeconds(long waitTimeSeconds) {
                this.waitTimeSeconds = waitTimeSeconds;
            }

            // StopDetails 类
            public static class StopDetails {
                private Stop arrivalStop;
                private Stop departureStop;
                private String arrivalTime;
                private String departureTime;

                // Constructors, Getters, and Setters

                public StopDetails() {
                }

                public StopDetails(Stop arrivalStop, Stop departureStop) {
                    this.arrivalStop = arrivalStop;
                    this.departureStop = departureStop;
                }

                public Stop getArrivalStop() {
                    return arrivalStop;
                }

                public void setArrivalStop(Stop arrivalStop) {
                    this.arrivalStop = arrivalStop;
                }

                public Stop getDepartureStop() {
                    return departureStop;
                }

                public void setDepartureStop(Stop departureStop) {
                    this.departureStop = departureStop;

                }

                public String getArrivalTime() {
                    return arrivalTime;
                }

                public void setArrivalTime(String arrivalTime) {
                    this.arrivalTime = arrivalTime;
                }

                public String getDepartureTime() {
                    return departureTime;
                }

                public void setDepartureTime(String departureTime) {
                    this.departureTime = departureTime;
                }

                // Stop 类
                public static class Stop {
                    private String name;
                    private String location;


                    // Constructors, Getters, and Setters

                    public Stop() {
                    }

                    public Stop(String name, String location) {
                        this.name = name;
                        this.location = location;
                    }

                    public String getName() {
                        return name;
                    }

                    public void setName(String name) {
                        this.name = name;
                    }

                    public String getLocation() {
                        return location;
                    }

                    public void setLocation(String location) {
                        this.location = location;
                    }




                }
            }

            // TransitLine 类
            public static class TransitLine {
                private List<Agency> agencies;
                private String name;
                private String color;
                private String nameShort;
                private String textColor;
                private Vehicle vehicle;

                // Constructors, Getters, and Setters

                public TransitLine() {
                }

                public TransitLine(List<Agency> agencies, String name, String color, String nameShort, String textColor, Vehicle vehicle) {
                    this.agencies = agencies;
                    this.name = name;
                    this.color = color;
                    this.nameShort = nameShort;
                    this.textColor = textColor;
                    this.vehicle = vehicle;
                }

                public List<Agency> getAgencies() {
                    return agencies;
                }

                public void setAgencies(List<Agency> agencies) {
                    this.agencies = agencies;
                }

                public String getName() {
                    return name;
                }

                public void setName(String name) {
                    this.name = name;
                }

                public String getColor() {
                    return color;
                }

                public void setColor(String color) {
                    this.color = color;
                }

                public String getNameShort() {
                    return nameShort;
                }

                public void setNameShort(String nameShort) {
                    this.nameShort = nameShort;
                }

                public String getTextColor() {
                    return textColor;
                }

                public void setTextColor(String textColor) {
                    this.textColor = textColor;
                }

                public Vehicle getVehicle() {
                    return vehicle;
                }

                public void setVehicle(Vehicle vehicle) {
                    this.vehicle = vehicle;
                }

                // Agency 类
                public static class Agency {
                    private String name;
                    private String phoneNumber;
                    private String uri;

                    // Constructors, Getters, and Setters

                    public Agency() {
                    }

                    public Agency(String name, String phoneNumber, String uri) {
                        this.name = name;
                        this.phoneNumber = phoneNumber;
                        this.uri = uri;
                    }

                    public String getName() {
                        return name;
                    }

                    public void setName(String name) {
                        this.name = name;
                    }

                    public String getPhoneNumber() {
                        return phoneNumber;
                    }

                    public void setPhoneNumber(String phoneNumber) {
                        this.phoneNumber = phoneNumber;
                    }

                    public String getUri() {
                        return uri;
                    }

                    public void setUri(String uri) {
                        this.uri = uri;
                    }
                }

                // Vehicle 类
                public static class Vehicle {
                    private Name name;
                    private String type;
                    private String iconUri;

                    // Constructors, Getters, and Setters

                    public Vehicle() {
                    }

                    public Vehicle(Name name, String type, String iconUri) {
                        this.name = name;
                        this.type = type;
                        this.iconUri = iconUri;
                    }

                    public Name getName() {
                        return name;
                    }

                    public void setName(Name name) {
                        this.name = name;
                    }

                    public String getType() {
                        return type;
                    }

                    public void setType(String type) {
                        this.type = type;
                    }

                    public String getIconUri() {
                        return iconUri;
                    }

                    public void setIconUri(String iconUri) {
                        this.iconUri = iconUri;
                    }

                    // Name 类
                    public static class Name {
                        private String text;

                        // Constructors, Getters, and Setters

                        public Name() {
                        }

                        public Name(String text) {
                            this.text = text;
                        }

                        public String getText() {
                            return text;
                        }

                        public void setText(String text) {
                            this.text = text;
                        }
                    }
                }
            }
        }
    }

    // TransitLine 类 (可选，如果在其他地方需要独立使用)
    public static class TransitLine {
        private List<Agency> agencies;
        private String name;
        private String color;
        private String nameShort;
        private String textColor;
        private Vehicle vehicle;

        // Getters and Setters
        public List<Agency> getAgencies() {
            return agencies;
        }

        public void setAgencies(List<Agency> agencies) {
            this.agencies = agencies;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public String getNameShort() {
            return nameShort;
        }

        public void setNameShort(String nameShort) {
            this.nameShort = nameShort;
        }

        public String getTextColor() {
            return textColor;
        }

        public void setTextColor(String textColor) {
            this.textColor = textColor;
        }

        public Vehicle getVehicle() {
            return vehicle;
        }

        public void setVehicle(Vehicle vehicle) {
            this.vehicle = vehicle;
        }

        // Agency 类
        public static class Agency {
            private String name;
            private String phoneNumber;
            private String uri;

            // Getters and Setters
            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getPhoneNumber() {
                return phoneNumber;
            }

            public void setPhoneNumber(String phoneNumber) {
                this.phoneNumber = phoneNumber;
            }

            public String getUri() {
                return uri;
            }

            public void setUri(String uri) {
                this.uri = uri;
            }
        }

        // Vehicle 类
        public static class Vehicle {
            private Name name;
            private String type;
            private String iconUri;

            // Getters and Setters
            public Name getName() {
                return name;
            }

            public void setName(Name name) {
                this.name = name;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getIconUri() {
                return iconUri;
            }

            public void setIconUri(String iconUri) {
                this.iconUri = iconUri;
            }

            // Name 类
            public static class Name {
                private String text;

                // Constructors, Getters, and Setters

                public Name() {
                }

                public Name(String text) {
                    this.text = text;
                }

                public String getText() {
                    return text;
                }

                public void setText(String text) {
                    this.text = text;
                }
            }
        }
    }

    // Agency 类 (可选，如果在其他地方需要独立使用)
    public static class Agency {
        private String name;
        private String phoneNumber;
        private String uri;

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }

    // Vehicle 类 (可选，如果在其他地方需要独立使用)
    public static class Vehicle {
        private Name name;
        private String type;
        private String iconUri;

        // Getters and Setters
        public Name getName() {
            return name;
        }

        public void setName(Name name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getIconUri() {
            return iconUri;
        }

        public void setIconUri(String iconUri) {
            this.iconUri = iconUri;
        }

        // Name 类
        public static class Name {
            private String text;

            // Getters and Setters
            public String getText() {
                return text;
            }

            public void setText(String text) {
                this.text = text;
            }
        }
    }
}