package com.example.stataggregation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.stataggregation.service.StatAggregationService;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@RestController 
public class StatController {

    @Autowired
    private StatAggregationService service;

    @GetMapping ("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

}