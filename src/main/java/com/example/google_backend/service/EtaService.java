package com.example.google_backend.service;

import java.util.List;

public interface EtaService {

    /**
     * Retrieves the estimated time of arrival for a bus at a given stop.
     *
     * @param stopId The ID of the bus stop.
     * @param routeId The ID of the bus route.
     * @param company The bus company.
     * @return A list of estimated times of arrival.
     */
    List<String> getBusEta(String stopName, String routeId, String company);
}
