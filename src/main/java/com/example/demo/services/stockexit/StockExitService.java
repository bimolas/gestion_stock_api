package com.example.demo.services.stockexit;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dtos.stockexit.CreateStockExitDto;
import com.example.demo.dtos.stockexit.UpdateStockExitDto;
import com.example.demo.models.Article;
import com.example.demo.models.StockExit;
import com.example.demo.repositories.ArticleRepository;
import com.example.demo.repositories.StockExitRepository;
import com.example.demo.services.alert.IAlertService;
import com.example.demo.services.notification.NotificationService;
import com.example.demo.services.stockwatchdog.StockWatchdogService;

@Service
public class StockExitService implements IStockExitService {

    private static final Logger logger = LoggerFactory.getLogger(StockExitService.class);

    private final StockExitRepository stockExitRepository;
    private final ArticleRepository articleRepository;
    private final IAlertService alertService;
    private final StockWatchdogService stockWatchdogService;
    private final NotificationService notificationService;

    public StockExitService(
            StockExitRepository stockExitRepository,
            ArticleRepository articleRepository,
            IAlertService alertService,
            StockWatchdogService stockWatchdogService,
            NotificationService notificationService) {
        this.stockExitRepository = stockExitRepository;
        this.articleRepository = articleRepository;
        this.alertService = alertService;
        this.stockWatchdogService = stockWatchdogService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public StockExit createStockExit(CreateStockExitDto createStockExitDto) {
        StockExit stockExit = new StockExit();
        stockExit.setDate(createStockExitDto.getDate());
        stockExit.setQuantity(createStockExitDto.getQuantity());
        stockExit.setDestination(createStockExitDto.getDestination());

        Article article = articleRepository.findById(createStockExitDto.getArticleId()).orElseThrow();
        int newQuantity = article.getQuantity() - createStockExitDto.getQuantity();

        if (newQuantity < 0) {
            throw new RuntimeException("Insufficient stock! Article quantity cannot be less than 0.");
        }

        article.setQuantity(newQuantity);
        articleRepository.save(article);

        stockExit.setArticle(article);
        StockExit savedStockExit = stockExitRepository.save(stockExit);
        runPostSaveHooks(article.getId(), createStockExitDto.getQuantity());
        return savedStockExit;
    }

    @Override
    public StockExit getStockExitById(Long id) {
        return stockExitRepository.findById(id).orElseThrow();
    }

    @Override
    public List<StockExit> getStockExitByArticle(Long id) {
        Article article = articleRepository.findById(id).orElseThrow();
        return stockExitRepository.findByArticle(article);
    }

    @Override
    public List<StockExit> getAllStockExit() {
        return stockExitRepository.findAll();
    }

    @Override
    public StockExit updateStockExit(UpdateStockExitDto updateStockExitDto, Long id) {
        StockExit stockExit = stockExitRepository.findById(id).orElseThrow();
        stockExit.setDate(updateStockExitDto.getDate());
        stockExit.setDestination(updateStockExitDto.getDestination());

        Article article = stockExit.getArticle();
        article.setQuantity(article.getQuantity() + stockExit.getQuantity());

        int newQuantity = article.getQuantity() - updateStockExitDto.getQuantity();
        if (newQuantity < 0) {
            throw new RuntimeException("Insufficient stock! Article quantity cannot be less than 0.");
        }

        stockExit.setQuantity(updateStockExitDto.getQuantity());
        article.setQuantity(newQuantity);
        articleRepository.save(article);

        StockExit savedStockExit = stockExitRepository.save(stockExit);
        alertService.evaluateAndCreateForArticle(article);
        return savedStockExit;
    }

    private void runPostSaveHooks(Long articleId, int quantity) {
        try {
            stockWatchdogService.checkLevels(articleId);
        } catch (Exception ex) {
            logger.warn("Watchdog alert creation failed for articleId={}", articleId, ex);
        }

        try {
            notificationService.logStockMovement(articleId, quantity, "EXIT");
        } catch (Exception ex) {
            logger.warn("Traceability message creation failed for articleId={}", articleId, ex);
        }
    }
}
