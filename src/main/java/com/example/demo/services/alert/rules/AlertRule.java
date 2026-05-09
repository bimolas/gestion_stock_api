package com.example.demo.services.alert.rules;

import java.util.Optional;

import com.example.demo.dtos.alert.AlertContext;
import com.example.demo.models.Alert;

public interface AlertRule {
    boolean supports(AlertContext context);

    Optional<Alert> evaluate(AlertContext context);
}
