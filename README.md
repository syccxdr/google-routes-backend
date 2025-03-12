# Google Routes Backend

A Java backend application for efficient routing using Google Maps API.

## Overview

This service provides optimized route planning with dynamic transportation mode selection, combining Google Maps API for transit routes and OpenTripPlanner for driving directions.

## Key Features

- Multi-modal route planning (transit, walking, driving)
- Smart decision engine to determine when to use taxis vs. public transit
- High-performance caching with Redis
- Asynchronous processing for improved response times

## API Endpoints

- `POST /api/routes/calculate` - Calculate optimized routes between locations
- `POST /api/routes/sorted` - Get routes sorted by criteria (duration, cost, etc.)

## Technology Stack

- Java Spring Boot
- Redis for caching
- OpenTripPlanner for driving routes
- Google Maps Routes API

## Getting Started

Configure your API keys, set up Redis and OpenTripPlanner, then build and run:

```bash
./mvnw clean package
java -jar target/google-routes-backend.jar
```

This lightweight service enhances standard Google routing with intelligent transportation mode decisions and performance optimizations.
