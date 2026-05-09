package com.example.demo.services.alert;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.dtos.alert.AlertContext;
import com.example.demo.models.Alert;
import com.example.demo.models.AlertStatus;
import com.example.demo.models.AlertType;
import com.example.demo.models.Article;
import com.example.demo.models.StockEntry;
import com.example.demo.models.Supplier;
import com.example.demo.repositories.AlertRepository;
import com.example.demo.repositories.ArticleRepository;
import com.example.demo.repositories.StockEntryRepository;

@Service
public class AlertService implements IAlertService {

    private final AlertRepository alertRepository;
    private final ArticleRepository articleRepository;
    private final StockEntryRepository stockEntryRepository;
    private final AlertEvaluator alertEvaluator;
    private final int lowStockThreshold;

    public AlertService(
            AlertRepository alertRepository,
            ArticleRepository articleRepository,
            StockEntryRepository stockEntryRepository,
            AlertEvaluator alertEvaluator,
            @Value("${inventory.alert.low-stock-threshold:10}") int lowStockThreshold) {
        this.alertRepository = alertRepository;
        this.articleRepository = articleRepository;
        this.stockEntryRepository = stockEntryRepository;
        this.alertEvaluator = alertEvaluator;
        this.lowStockThreshold = lowStockThreshold;
    }

    @Override
    public List<Alert> getOpenAlerts() {
        return alertRepository.findByStatusOrderByCreatedAtDesc(AlertStatus.OPEN);
    }

    @Override
    public List<Alert> getAlertsByStatus(AlertStatus status) {
        return alertRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Override
    public Alert getAlertById(Long id) {
        return alertRepository.findById(id).orElseThrow();
    }

    @Override
    public Alert acknowledgeAlert(Long id) {
        Alert alert = alertRepository.findById(id).orElseThrow();
        if (alert.getStatus() == AlertStatus.RESOLVED) {
            return alert;
        }
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        return alertRepository.save(alert);
    }

    @Override
    public Alert resolveAlert(Long id) {
        Alert alert = alertRepository.findById(id).orElseThrow();
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        return alertRepository.save(alert);
    }

    @Override
    public void evaluateAndCreateForArticle(Article article) {
        if (article == null) {
            return;
        }

        AlertContext context = buildContext(article);
        createAlertsForContext(context, null);
        autoResolveLowStockAlertWhenRecovered(article);
    }

    @Override
    public void evaluateAndCreateForLowStockSweep() {
        List<Article> lowStockArticles = articleRepository.findByQuantityLessThanEqual(lowStockThreshold);
        for (Article article : lowStockArticles) {
            createAlertsForContext(buildContext(article), AlertType.LOW_STOCK);
        }
    }

    @Override
    public void evaluateAndCreateForSupplyChainSweep() {
        List<Article> lowStockArticles = articleRepository.findByQuantityLessThanEqual(lowStockThreshold);
        for (Article article : lowStockArticles) {
            createAlertsForContext(buildContext(article), AlertType.SUPPLY_CHAIN_DELAY);
        }
    }

    private AlertContext buildContext(Article article) {
        Supplier supplier = article.getSupplier();
        LocalDate lastRestockDate = null;

        if (supplier != null) {
            StockEntry latestEntry = stockEntryRepository.findTopByArticleAndSupplierOrderByDateDesc(article, supplier);
            if (latestEntry != null && latestEntry.getDate() != null) {
                lastRestockDate = latestEntry.getDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
            }
        }

        return new AlertContext(article, supplier, lastRestockDate);
    }

    private void createAlertsForContext(AlertContext context, AlertType filterType) {
        List<Alert> candidates = alertEvaluator.evaluateAll(context);
        for (Alert candidate : candidates) {
            if (filterType != null && candidate.getType() != filterType) {
                continue;
            }

            boolean alreadyExists = alertRepository.existsByFingerprintAndStatusIn(
                    candidate.getFingerprint(),
                    List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED));
            if (!alreadyExists) {
                candidate.setStatus(AlertStatus.OPEN);
                alertRepository.save(candidate);
            }
        }
    }

    private void autoResolveLowStockAlertWhenRecovered(Article article) {
        if (article.getQuantity() <= lowStockThreshold) {
            return;
        }

        String fingerprint = "LOW_STOCK:" + article.getId();
        List<Alert> activeAlerts = alertRepository.findByFingerprintAndStatusIn(
                fingerprint,
                List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED));

        for (Alert alert : activeAlerts) {
            alert.setStatus(AlertStatus.RESOLVED);
            alert.setResolvedAt(LocalDateTime.now());
            alertRepository.save(alert);
        }
    }
}
