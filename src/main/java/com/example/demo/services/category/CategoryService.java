package com.example.demo.services.category;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dtos.category.CreateCategoryDto;
import com.example.demo.dtos.category.UpdateCategoryDto;
import com.example.demo.models.Category;
import com.example.demo.repositories.CategoryRepository;
import com.example.demo.services.notification.NotificationService;
import com.example.demo.models.Message;

@Service
public class CategoryService implements ICategoryService {

    @Autowired
    private final CategoryRepository categoryRepository;
    private final NotificationService notificationService;

    public CategoryService(CategoryRepository categoryRepository, NotificationService notificationService) {
        this.categoryRepository = categoryRepository;
        this.notificationService = notificationService;
    }

    @Override
    public Category createCategory(CreateCategoryDto createCategoryDto) {
        Category category = new Category();
        category.setName(createCategoryDto.getName());
        category.setDescription(createCategoryDto.getDescription());
        Category saved = categoryRepository.save(category);
        try {
            notificationService.logAction("Category Created", "Category '" + saved.getName() + "' was created (ID: " + saved.getId() + ").");
        } catch (Exception ignored) {}
        return saved;
    }

    @Override
    public Category updateCategory(UpdateCategoryDto updateCategoryDto, Long id) {
        Category category = categoryRepository.findById(id).orElseThrow();
        category.setName(updateCategoryDto.getName());
        category.setDescription(updateCategoryDto.getDescription());
        Category saved = categoryRepository.save(category);
        try {
            notificationService.logAction("Category Updated", "Category '" + saved.getName() + "' was updated (ID: " + saved.getId() + ").");
        } catch (Exception ignored) {}
        return saved;
    }

    @Override
    public List<Category> getAllCategorys() {
        return categoryRepository.findAll();
    }

    @Override
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id).orElseThrow();
    }
}
