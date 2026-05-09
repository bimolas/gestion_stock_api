package com.example.demo.services.scheduledtask;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.demo.services.alert.IAlertService;

@Service
public class ScheduledTaskService implements IScheduledTaskService {

    private final IAlertService alertService;

    public ScheduledTaskService(IAlertService alertService) {
        this.alertService = alertService;
    }

    @Override
    @Scheduled(cron = "0 0 8 * * *")
    public void checkDepletedStock() {
        alertService.evaluateAndCreateForLowStockSweep();
        alertService.evaluateAndCreateForSupplyChainSweep();
    }
}
