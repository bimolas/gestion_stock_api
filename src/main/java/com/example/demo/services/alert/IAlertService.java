package com.example.demo.services.alert;

import java.util.List;

import com.example.demo.models.Alert;
import com.example.demo.models.AlertStatus;
import com.example.demo.models.Article;

public interface IAlertService {
    List<Alert> getOpenAlerts();

    List<Alert> getAlertsByStatus(AlertStatus status);

    Alert getAlertById(Long id);

    Alert acknowledgeAlert(Long id);

    Alert resolveAlert(Long id);

    void evaluateAndCreateForArticle(Article article);

    void evaluateAndCreateForLowStockSweep();

    void evaluateAndCreateForSupplyChainSweep();
}
