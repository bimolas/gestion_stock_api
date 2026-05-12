package com.example.demo.services.stockwatchdog;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.models.Alert;
import com.example.demo.models.AlertSeverity;
import com.example.demo.models.AlertStatus;
import com.example.demo.models.AlertType;
import com.example.demo.models.Article;
import com.example.demo.repositories.AlertRepository;
import com.example.demo.repositories.ArticleRepository;

@Service
public class StockWatchdogService {

    private static final Logger logger = LoggerFactory.getLogger(StockWatchdogService.class);

    private final ArticleRepository articleRepository;
    private final AlertRepository alertRepository;
    private final int lowStockThreshold;

    public StockWatchdogService(
            ArticleRepository articleRepository,
            AlertRepository alertRepository,
            @Value("${inventory.alert.low-stock-threshold:10}") int lowStockThreshold) {
        this.articleRepository = articleRepository;
        this.alertRepository = alertRepository;
        this.lowStockThreshold = lowStockThreshold;
    }

    @Transactional
    public void checkLevels(Long articleId) {
        try {
            if (articleId == null) {
                return;
            }

            Article article = articleRepository.findById(articleId).orElseThrow();

            if (article.getQuantity() > lowStockThreshold) {
                return;
            }

            String fingerprint = "LOW_STOCK:" + article.getId();
            boolean alreadyOpen = alertRepository.existsByFingerprintAndStatusIn(
                    fingerprint,
                    List.of(AlertStatus.OPEN));

            if (alreadyOpen) {
                return;
            }

            Alert alert = new Alert();
            alert.setType(AlertType.LOW_STOCK);
            alert.setSeverity(AlertSeverity.HIGH);
            alert.setStatus(AlertStatus.OPEN);
            alert.setArticleId(article.getId());
            alert.setFingerprint(fingerprint);
            alert.setTitle("Low stock alert");
            alert.setContent("Article " + article.getId() + " is low on stock. Current quantity: " + article.getQuantity());

            alertRepository.save(alert);
        } catch (Exception ex) {
            logger.warn("Watchdog check failed for articleId={}", articleId, ex);
        }
    }
}
