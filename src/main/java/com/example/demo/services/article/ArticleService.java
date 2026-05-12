package com.example.demo.services.article;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dtos.article.CreateArticleDto;
import com.example.demo.dtos.article.UpdateArticleDto;
import com.example.demo.models.Article;
import com.example.demo.models.Category;
import com.example.demo.models.Supplier;
import com.example.demo.repositories.ArticleRepository;
import com.example.demo.repositories.CategoryRepository;
import com.example.demo.repositories.SupplierRepository;
import com.example.demo.services.alert.IAlertService;
import com.example.demo.services.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ArticleService implements IArticleService {

    @Autowired
    private final ArticleRepository articleRepository;
    @Autowired
    private final CategoryRepository categoryRepository;
    @Autowired
    private final SupplierRepository supplierRepository;
    private final IAlertService alertService;
    private final NotificationService notificationService;
    private static final Logger logger = LoggerFactory.getLogger(ArticleService.class);

    public ArticleService(SupplierRepository supplierRepository,
                          ArticleRepository articleRepository,
                          CategoryRepository categoryRepository,
                          IAlertService alertService,
                          NotificationService notificationService) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.alertService = alertService;
        this.notificationService = notificationService;
    }

    @Override
    public Article createArticle(CreateArticleDto createArticleDto) {
        Article article = new Article();
        article.setName(createArticleDto.getName());
        article.setDescription(createArticleDto.getDescription());
        article.setBarcode(createArticleDto.getBarcode());

        Category category = categoryRepository.findById(createArticleDto.getCategoryId()).orElseThrow();
        article.setCategory(category);

        Supplier supplier = supplierRepository.findById(createArticleDto.getSupplierId()).orElseThrow();
        article.setSupplier(supplier);

        article.setPrice(createArticleDto.getPrice());
        article.setQuantity(createArticleDto.getQuantity());

        Article savedArticle = articleRepository.save(article);
        
        try {
            alertService.evaluateAndCreateForArticle(savedArticle);
        } catch (Exception ex) {
            logger.warn("Alert evaluation failed for articleId={}", savedArticle.getId(), ex);
        }
        try {
            notificationService.logStockMovement(savedArticle.getId(), savedArticle.getQuantity(), "CREATION");
        } catch (Exception ex) {
            logger.warn("Traceability message creation failed for articleId={}", savedArticle.getId(), ex);
        }
        
        return savedArticle;
    }

    @Override
    public Article updateArticle(UpdateArticleDto updateArticleDto, Long id) {
        Article article = articleRepository.findById(id).orElseThrow();
        article.setName(updateArticleDto.getName());
        article.setDescription(updateArticleDto.getDescription());
        article.setPrice(updateArticleDto.getPrice());
        article.setQuantity(updateArticleDto.getQuantity());

        Article savedArticle = articleRepository.save(article);
        
        try {
            alertService.evaluateAndCreateForArticle(savedArticle);
        } catch (Exception ex) {
            logger.warn("Alert evaluation failed for articleId={}", savedArticle.getId(), ex);
        }
        try {
            notificationService.logStockMovement(savedArticle.getId(), savedArticle.getQuantity(), "UPDATE");
        } catch (Exception ex) {
            logger.warn("Traceability message creation failed for articleId={}", savedArticle.getId(), ex);
        }
        
        return savedArticle;
    }

    @Override
    public List<Article> getAllArticles() {
        return articleRepository.findAll();
    }

    @Override
    public List<Article> getAllArticlesByCategory(Long id) {
        Category category = categoryRepository.findById(id).orElseThrow();
        return articleRepository.findByCategory(category);
    }

    @Override
    public List<Article> getAllArticlesBySupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id).orElseThrow();
        return articleRepository.findBySupplier(supplier);
    }

    @Override
    public Article getArticleById(Long id) {
        return articleRepository.findById(id).orElseThrow();
    }
}
