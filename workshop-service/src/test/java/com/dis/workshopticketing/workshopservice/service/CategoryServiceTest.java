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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private WorkshopRepository workshopRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void createSavesActiveCategoryWhenNameIsAvailable() {
        CreateCategoryRequest request = new CreateCategoryRequest("Painting", "Creative painting workshops");
        when(categoryRepository.existsByNameIgnoreCase("Painting")).thenReturn(false);
        when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(1L);
            return category;
        });

        CategoryResponse response = categoryService.create(request);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).saveAndFlush(captor.capture());
        Category savedCategory = captor.getValue();

        assertThat(savedCategory.getName()).isEqualTo("Painting");
        assertThat(savedCategory.getDescription()).isEqualTo("Creative painting workshops");
        assertThat(savedCategory.isActive()).isTrue();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Painting");
    }

    @Test
    void createThrowsDuplicateCategoryExceptionWhenNameAlreadyExists() {
        when(categoryRepository.existsByNameIgnoreCase("Painting")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(new CreateCategoryRequest("Painting", "Duplicate")))
                .isInstanceOf(DuplicateCategoryException.class)
                .hasMessageContaining("Painting");
    }

    @Test
    void getAllReturnsActiveAndInactiveCategories() {
        Category activeCategory = category(1L, "Painting", true);
        Category inactiveCategory = category(2L, "Ceramics", false);
        when(categoryRepository.findAll()).thenReturn(List.of(activeCategory, inactiveCategory));

        List<CategoryResponse> responses = categoryService.getAll();

        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(CategoryResponse::active)
                .containsExactly(true, false);
    }

    @Test
    void getReturnsActiveCategory() {
        Category category = category(1L, "Painting", true);
        when(categoryRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(category));

        CategoryResponse response = categoryService.get(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Painting");
        assertThat(response.active()).isTrue();
    }

    @Test
    void getThrowsResourceNotFoundExceptionWhenCategoryDoesNotExistOrIsInactive() {
        when(categoryRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.get(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found: 1");
    }

    @Test
    void updateChangesNameAndDescriptionWhenNewNameIsAvailable() {
        Category category = category(1L, "Painting", true);
        UpdateCategoryRequest request = new UpdateCategoryRequest("Watercolor", "Updated description");

        when(categoryRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCase("Watercolor")).thenReturn(false);
        when(categoryRepository.saveAndFlush(category)).thenReturn(category);

        CategoryResponse response = categoryService.update(1L, request);

        assertThat(category.getName()).isEqualTo("Watercolor");
        assertThat(category.getDescription()).isEqualTo("Updated description");
        assertThat(response.name()).isEqualTo("Watercolor");
    }

    @Test
    void updateDoesNotCheckDuplicateWhenNameIsSameIgnoringCase() {
        Category category = category(1L, "Painting", true);
        UpdateCategoryRequest request = new UpdateCategoryRequest("painting", "Updated description");

        when(categoryRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.saveAndFlush(category)).thenReturn(category);

        categoryService.update(1L, request);

        verify(categoryRepository, never()).existsByNameIgnoreCase(any());
        assertThat(category.getName()).isEqualTo("Painting");
        assertThat(category.getDescription()).isEqualTo("Updated description");
    }

    @Test
    void deleteSetsActiveToFalseWhenCategoryHasNoActiveWorkshops() {
        Category category = category(1L, "Painting", true);
        when(categoryRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(category));
        when(workshopRepository.existsByCategoryIdAndActiveTrue(1L)).thenReturn(false);

        categoryService.delete(1L);

        assertThat(category.isActive()).isFalse();
    }

    @Test
    void deleteThrowsBadRequestExceptionWhenCategoryHasActiveWorkshops() {
        Category category = category(1L, "Painting", true);
        when(categoryRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(category));
        when(workshopRepository.existsByCategoryIdAndActiveTrue(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("active workshops");

        assertThat(category.isActive()).isTrue();
    }

    private Category category(Long id, String name, boolean active) {
        return Category.builder()
                .id(id)
                .name(name)
                .description(name + " description")
                .active(active)
                .build();
    }
}
