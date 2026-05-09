package com.example.demo.dtos.alert;

import java.time.LocalDate;

import com.example.demo.models.Article;
import com.example.demo.models.Supplier;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertContext {
    private Article article;
    private Supplier supplier;
    private LocalDate lastSupplierRestockDate;
}
