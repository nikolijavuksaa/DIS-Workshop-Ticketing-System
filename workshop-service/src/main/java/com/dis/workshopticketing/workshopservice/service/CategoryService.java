package com.dis.workshopticketing.workshopservice.service;

import com.dis.workshopticketing.workshopservice.dto.CategoryResponse;
import com.dis.workshopticketing.workshopservice.dto.CreateCategoryRequest;
import com.dis.workshopticketing.workshopservice.dto.UpdateCategoryRequest;
import com.dis.workshopticketing.workshopservice.exception.BadRequestException;
import com.dis.workshopticketing.workshopservice.exception.DuplicateCategoryException;
import com.dis.workshopticketing.workshopservice.exception.ResourceNotFoundException;
import com.dis.workshopticketing.workshopservice.model.Category;
import com.dis.workshopticketing.workshopservice.repository.CategoryRepository;
import com.dis.workshopticketing.workshopservice.repository.WorkshopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final WorkshopRepository workshopRepository;

    public CategoryService(CategoryRepository categoryRepository, WorkshopRepository workshopRepository) {
        this.categoryRepository = categoryRepository;
        this.workshopRepository = workshopRepository;
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        ensureNameAvailable(request.name());

        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .active(true)
                .build();

        return CategoryResponse.from(categoryRepository.saveAndFlush(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(Long id) {
        return CategoryResponse.from(findActiveCategory(id));
    }

    @Transactional
    public CategoryResponse update(Long id, UpdateCategoryRequest request) {
        Category category = findActiveCategory(id);

        if (!request.name().equalsIgnoreCase(category.getName())) {
            ensureNameAvailable(request.name());
            category.setName(request.name());
        }

        category.setDescription(request.description());

        return CategoryResponse.from(categoryRepository.saveAndFlush(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = findActiveCategory(id);
        if (workshopRepository.existsByCategoryIdAndActiveTrue(id)) {
            throw new BadRequestException("Category has active workshops and cannot be deleted");
        }

        category.setActive(false);
    }

    Category findActiveCategory(Long id) {
        return categoryRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private void ensureNameAvailable(String name) {
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateCategoryException(name);
        }
    }
}
