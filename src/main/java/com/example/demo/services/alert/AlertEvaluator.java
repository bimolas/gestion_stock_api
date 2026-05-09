package com.example.demo.services.alert;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dtos.alert.AlertContext;
import com.example.demo.models.Alert;
import com.example.demo.services.alert.rules.AlertRule;

@Service
public class AlertEvaluator {

    private final List<AlertRule> rules;

    public AlertEvaluator(List<AlertRule> rules) {
        this.rules = rules;
    }

    public List<Alert> evaluateAll(AlertContext context) {
        List<Alert> alerts = new ArrayList<>();
        for (AlertRule rule : rules) {
            if (rule.supports(context)) {
                rule.evaluate(context).ifPresent(alerts::add);
            }
        }
        return alerts;
    }
}
