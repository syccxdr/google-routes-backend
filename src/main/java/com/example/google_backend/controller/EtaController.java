package com.example.google_backend.controller;

import com.example.google_backend.service.EtaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eta")

public class EtaController {

    @Autowired
    private EtaService etaService;



    @GetMapping("/bus")
    public ResponseEntity<?> getBusEta(
            @RequestParam String stopName,
            @RequestParam String routeId,
            @RequestParam String company) {
        List<String> etaList = etaService.getBusEta(stopName, routeId, company);
        return ResponseEntity.ok(etaList);
    }




}
