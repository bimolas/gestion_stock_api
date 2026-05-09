package com.example.demo.services.alert.rules;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.demo.dtos.alert.AlertContext;
import com.example.demo.models.Alert;
import com.example.demo.models.AlertSeverity;
import com.example.demo.models.AlertType;

@Component
public class LowStockRule implements AlertRule {

    private final int lowStockThreshold;

    public LowStockRule(@Value("${inventory.alert.low-stock-threshold:10}") int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    @Override
    public boolean supports(AlertContext context) {
        return context.getArticle() != null;
    }

    @Override
    public Optional<Alert> evaluate(AlertContext context) {
        if (!supports(context)) {
            return Optional.empty();
        }

        int quantity = context.getArticle().getQuantity();
        if (quantity > lowStockThreshold) {
            return Optional.empty();
        }

        Alert alert = new Alert();
        alert.setType(AlertType.LOW_STOCK);
        alert.setSeverity(quantity == 0 ? AlertSeverity.CRITICAL : AlertSeverity.HIGH);
        alert.setArticleId(context.getArticle().getId());
        if (context.getSupplier() != null) {
            alert.setSupplierId(context.getSupplier().getId());
        }
        alert.setTitle("Low stock: " + context.getArticle().getName());
        alert.setContent("Article '" + context.getArticle().getName() + "' is at " + quantity
                + " units (threshold: " + lowStockThreshold + ").");
        alert.setFingerprint("LOW_STOCK:" + context.getArticle().getId());
        return Optional.of(alert);
    }
}
