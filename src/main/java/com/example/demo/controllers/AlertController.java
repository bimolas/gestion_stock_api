package com.example.demo.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.models.Alert;
import com.example.demo.models.AlertStatus;
import com.example.demo.services.alert.IAlertService;

@RestController
@RequestMapping("/Api/Alert/")
public class AlertController {

    private final IAlertService alertService;

    public AlertController(IAlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("GetOpenAlerts")
    public ResponseEntity<List<Alert>> getOpenAlerts() {
        return ResponseEntity.ok(alertService.getOpenAlerts());
    }

    @GetMapping("GetAlertsByStatus/{status}")
    public ResponseEntity<List<Alert>> getAlertsByStatus(@PathVariable("status") AlertStatus status) {
        return ResponseEntity.ok(alertService.getAlertsByStatus(status));
    }

    @GetMapping("GetAlertById/{id}")
    public ResponseEntity<Alert> getAlertById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(alertService.getAlertById(id));
    }

    @PutMapping("Acknowledge/{id}")
    public ResponseEntity<Alert> acknowledge(@PathVariable("id") Long id) {
        return ResponseEntity.ok(alertService.acknowledgeAlert(id));
    }

    @PutMapping("Resolve/{id}")
    public ResponseEntity<Alert> resolve(@PathVariable("id") Long id) {
        return ResponseEntity.ok(alertService.resolveAlert(id));
    }
}
