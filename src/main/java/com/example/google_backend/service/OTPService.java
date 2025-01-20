package com.example.google_backend.service;

import com.example.google_backend.model.RouteResponse;

import java.util.List;

public interface OTPService {
    List<RouteResponse.StepDetail> getDrivingRoute(RouteResponse.StepDetail.TransitDetails.StopDetails.Stop startLocation,
                                                   RouteResponse.StepDetail.TransitDetails.StopDetails.Stop endLocation) throws Exception;
}
