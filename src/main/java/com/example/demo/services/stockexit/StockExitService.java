package com.example.demo.services.stockexit;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dtos.stockexit.CreateStockExitDto;
import com.example.demo.dtos.stockexit.UpdateStockExitDto;
import com.example.demo.models.Article;
import com.example.demo.models.StockExit;
import com.example.demo.repositories.ArticleRepository;
import com.example.demo.repositories.StockExitRepository;
import com.example.demo.services.alert.IAlertService;

@Service
public class StockExitService implements IStockExitService {

    private final StockExitRepository stockExitRepository;
    private final ArticleRepository articleRepository;
    private final IAlertService alertService;

    public StockExitService(
            StockExitRepository stockExitRepository,
            ArticleRepository articleRepository,
            IAlertService alertService) {
        this.stockExitRepository = stockExitRepository;
        this.articleRepository = articleRepository;
        this.alertService = alertService;
    }

    @Override
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
        alertService.evaluateAndCreateForArticle(article);
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
}
