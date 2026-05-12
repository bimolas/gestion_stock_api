package com.example.demo.services.stockentry;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dtos.stockentry.CreateStockEntryDto;
import com.example.demo.dtos.stockentry.UpdateStockEntryDto;
import com.example.demo.models.Article;
import com.example.demo.models.StockEntry;
import com.example.demo.models.Supplier;
import com.example.demo.repositories.ArticleRepository;
import com.example.demo.repositories.StockEntryRepository;
import com.example.demo.repositories.SupplierRepository;
import com.example.demo.services.alert.IAlertService;
import com.example.demo.services.notification.NotificationService;

@Service
public class StockEntryService implements IStockEntryService {

    private static final Logger logger = LoggerFactory.getLogger(StockEntryService.class);

    private final ArticleRepository articleRepository;
    private final StockEntryRepository stockEntryRepository;
    private final SupplierRepository supplierRepository;
    private final IAlertService alertService;
    private final NotificationService notificationService;

    public StockEntryService(
            ArticleRepository articleRepository,
            StockEntryRepository stockEntryRepository,
            SupplierRepository supplierRepository,
            IAlertService alertService,
            NotificationService notificationService) {
        this.articleRepository = articleRepository;
        this.stockEntryRepository = stockEntryRepository;
        this.supplierRepository = supplierRepository;
        this.alertService = alertService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public StockEntry createStockEntry(CreateStockEntryDto createDto) {
        StockEntry stockEntry = new StockEntry();
        stockEntry.setDate(createDto.getDate());
        stockEntry.setQuantity(createDto.getQuantity());

        Article article = articleRepository.findById(createDto.getArticleId()).orElseThrow();
        Supplier supplier = supplierRepository.findById(createDto.getSupplierId()).orElseThrow();

        article.setQuantity(article.getQuantity() + createDto.getQuantity());
        articleRepository.save(article);

        stockEntry.setArticle(article);
        stockEntry.setSupplier(supplier);

        StockEntry savedStockEntry = stockEntryRepository.save(stockEntry);
        try {
            alertService.evaluateAndCreateForArticle(article);
        } catch (Exception ex) {
            logger.warn("Alert evaluation failed for articleId={}", article.getId(), ex);
        }
        try {
            notificationService.logStockMovement(article.getId(), createDto.getQuantity(), "ENTRY");
        } catch (Exception ex) {
            logger.warn("Traceability message creation failed for articleId={}", article.getId(), ex);
        }
        return savedStockEntry;
    }

    @Override
    public StockEntry updateStockEntry(UpdateStockEntryDto updateDto, Long id) {
        StockEntry stockEntry = stockEntryRepository.findById(id).orElseThrow();
        Article article = stockEntry.getArticle();

        article.setQuantity(article.getQuantity() - stockEntry.getQuantity());
        stockEntry.setQuantity(updateDto.getQuantity());
        stockEntry.setDate(updateDto.getDate());
        article.setQuantity(article.getQuantity() + updateDto.getQuantity());

        articleRepository.save(article);
        StockEntry savedStockEntry = stockEntryRepository.save(stockEntry);
        alertService.evaluateAndCreateForArticle(article);
        return savedStockEntry;
    }

    @Override
    public StockEntry getStockEntryById(Long id) {
        return stockEntryRepository.findById(id).orElseThrow();
    }

    @Override
    public List<StockEntry> getStockEntriesByArticle(Long articleId) {
        Article article = articleRepository.findById(articleId).orElseThrow();
        return stockEntryRepository.findByArticle(article);
    }

    @Override
    public List<StockEntry> getStockEntriesBySupplier(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId).orElseThrow();
        return stockEntryRepository.findBySupplier(supplier);
    }

    @Override
    public List<StockEntry> getAllStockEntries() {
        return stockEntryRepository.findAll();
    }
}
