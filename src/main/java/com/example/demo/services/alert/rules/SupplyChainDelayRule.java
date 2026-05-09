package com.example.demo.services.alert.rules;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.demo.dtos.alert.AlertContext;
import com.example.demo.models.Alert;
import com.example.demo.models.AlertSeverity;
import com.example.demo.models.AlertType;

@Component
public class SupplyChainDelayRule implements AlertRule {

    private final int lowStockThreshold;
    private final int maxDelayDays;

    public SupplyChainDelayRule(
            @Value("${inventory.alert.low-stock-threshold:10}") int lowStockThreshold,
            @Value("${inventory.alert.supply-chain-max-delay-days:14}") int maxDelayDays) {
        this.lowStockThreshold = lowStockThreshold;
        this.maxDelayDays = maxDelayDays;
    }

    @Override
    public boolean supports(AlertContext context) {
        return context.getArticle() != null && context.getSupplier() != null;
    }

    @Override
    public Optional<Alert> evaluate(AlertContext context) {
        if (!supports(context)) {
            return Optional.empty();
        }

        if (context.getArticle().getQuantity() > lowStockThreshold) {
            return Optional.empty();
        }

        LocalDate lastRestock = context.getLastSupplierRestockDate();
        if (lastRestock == null) {
            Alert alert = new Alert();
            alert.setType(AlertType.SUPPLY_CHAIN_DELAY);
            alert.setSeverity(AlertSeverity.HIGH);
            alert.setArticleId(context.getArticle().getId());
            alert.setSupplierId(context.getSupplier().getId());
            alert.setTitle("Supply chain risk: " + context.getArticle().getName());
            alert.setContent("No supplier restock history found for article '" + context.getArticle().getName()
                    + "' while stock is low.");
            alert.setFingerprint("SUPPLY_DELAY:" + context.getArticle().getId() + ":" + context.getSupplier().getId());
            return Optional.of(alert);
        }

        long delay = ChronoUnit.DAYS.between(lastRestock, LocalDate.now());
        if (delay <= maxDelayDays) {
            return Optional.empty();
        }

        Alert alert = new Alert();
        alert.setType(AlertType.SUPPLY_CHAIN_DELAY);
        alert.setSeverity(AlertSeverity.MEDIUM);
        alert.setArticleId(context.getArticle().getId());
        alert.setSupplierId(context.getSupplier().getId());
        alert.setTitle("Supplier delay: " + context.getArticle().getName());
        alert.setContent("Supplier restock delay detected for article '" + context.getArticle().getName()
                + "'. Last restock was " + delay + " days ago (limit: " + maxDelayDays + ").");
        alert.setFingerprint("SUPPLY_DELAY:" + context.getArticle().getId() + ":" + context.getSupplier().getId());
        return Optional.of(alert);
    }
}
