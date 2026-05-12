package com.example.demo.services.analytics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.models.Article;
import com.example.demo.models.StockEntry;
import com.example.demo.models.StockExit;
import com.example.demo.repositories.ArticleRepository;
import com.example.demo.repositories.StockEntryRepository;
import com.example.demo.repositories.StockExitRepository;

@Service
public class AnalyticsService {

    private final ArticleRepository articleRepository;
    private final StockEntryRepository stockEntryRepository;
    private final StockExitRepository stockExitRepository;

    public AnalyticsService(
            ArticleRepository articleRepository,
            StockEntryRepository stockEntryRepository,
            StockExitRepository stockExitRepository) {
        this.articleRepository = articleRepository;
        this.stockEntryRepository = stockEntryRepository;
        this.stockExitRepository = stockExitRepository;
    }

    @Transactional(readOnly = true)
    public List<Object> getArticleHistory(Long id) {
        Article article = articleRepository.findById(id).orElseThrow();

        List<StockEntry> entries = stockEntryRepository.findByArticle(article);
        List<StockExit> exits = stockExitRepository.findByArticle(article);

        List<Object> history = new ArrayList<>();
        history.addAll(entries);
        history.addAll(exits);

        history.sort(Comparator.comparing(this::extractDate, Comparator.nullsLast(Date::compareTo)).reversed());
        return history;
    }

    private Date extractDate(Object movement) {
        if (movement instanceof StockEntry) {
            return ((StockEntry) movement).getDate();
        }

        if (movement instanceof StockExit) {
            return ((StockExit) movement).getDate();
        }

        return null;
    }
}
