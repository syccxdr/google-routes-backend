package com.example.google_backend.model;

import lombok.Data;

@Data
public class BusEta {

    private String eta;           // 预计到达时间
    private String stopId;        // 站点ID
    private String routeNumber;   // 路线号
    private String company;       // 巴士公司

}
