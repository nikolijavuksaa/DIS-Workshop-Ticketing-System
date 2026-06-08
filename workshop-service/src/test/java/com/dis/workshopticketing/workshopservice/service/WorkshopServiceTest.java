package com.dis.workshopticketing.workshopservice.service;

import com.dis.workshopticketing.workshopservice.dto.CreateWorkshopRequest;
import com.dis.workshopticketing.workshopservice.dto.UpdateWorkshopRequest;
import com.dis.workshopticketing.workshopservice.dto.WorkshopResponse;
import com.dis.workshopticketing.workshopservice.exception.ResourceNotFoundException;
import com.dis.workshopticketing.workshopservice.model.Category;
import com.dis.workshopticketing.workshopservice.model.Workshop;
import com.dis.workshopticketing.workshopservice.model.WorkshopSession;
import com.dis.workshopticketing.workshopservice.repository.WorkshopRepository;
import com.dis.workshopticketing.workshopservice.repository.WorkshopSessionRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkshopServiceTest {

    @Mock
    private WorkshopRepository workshopRepository;

    @Mock
    private WorkshopSessionRepository workshopSessionRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private InstructorValidationService instructorValidationService;

    @InjectMocks
    private WorkshopService workshopService;

    @Test
    void createValidatesInstructorFindsCategoryAndSavesActiveWorkshop() {
        Category category = category(1L, "Painting", true);
        CreateWorkshopRequest request = new CreateWorkshopRequest(
                "Beginner Watercolor",
                "Intro workshop",
                10L,
                1L
        );
        when(categoryService.findActiveCategory(1L)).thenReturn(category);
        when(workshopRepository.saveAndFlush(any(Workshop.class))).thenAnswer(invocation -> {
            Workshop workshop = invocation.getArgument(0);
            workshop.setId(1L);
            return workshop;
        });

        WorkshopResponse response = workshopService.create(request);

        verify(instructorValidationService).ensureInstructorExists(10L);
        verify(categoryService).findActiveCategory(1L);

        ArgumentCaptor<Workshop> captor = ArgumentCaptor.forClass(Workshop.class);
        verify(workshopRepository).saveAndFlush(captor.capture());
        Workshop savedWorkshop = captor.getValue();

        assertThat(savedWorkshop.getTitle()).isEqualTo("Beginner Watercolor");
        assertThat(savedWorkshop.getDescription()).isEqualTo("Intro workshop");
        assertThat(savedWorkshop.getInstructorId()).isEqualTo(10L);
        assertThat(savedWorkshop.getCategory()).isEqualTo(category);
        assertThat(savedWorkshop.isActive()).isTrue();

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.categoryId()).isEqualTo(1L);
    }

    @Test
    void getAllReturnsActiveAndInactiveWorkshops() {
        Category category = category(1L, "Painting", true);
        Workshop activeWorkshop = workshop(1L, "Active", 10L, category, true);
        Workshop inactiveWorkshop = workshop(2L, "Inactive", 10L, category, false);
        when(workshopRepository.findAll()).thenReturn(List.of(activeWorkshop, inactiveWorkshop));

        List<WorkshopResponse> responses = workshopService.getAll();

        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(WorkshopResponse::active)
                .containsExactly(true, false);
    }

    @Test
    void getReturnsActiveWorkshop() {
        Category category = category(1L, "Painting", true);
        Workshop workshop = workshop(1L, "Beginner Watercolor", 10L, category, true);
        when(workshopRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(workshop));

        WorkshopResponse response = workshopService.get(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Beginner Watercolor");
        assertThat(response.active()).isTrue();
    }

    @Test
    void getThrowsResourceNotFoundExceptionWhenWorkshopDoesNotExistOrIsInactive() {
        when(workshopRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workshopService.get(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Workshop not found: 1");
    }

    @Test
    void updateValidatesInstructorAndCategoryThenChangesWorkshop() {
        Category oldCategory = category(1L, "Painting", true);
        Category newCategory = category(2L, "Ceramics", true);
        Workshop workshop = workshop(1L, "Old title", 10L, oldCategory, true);
        UpdateWorkshopRequest request = new UpdateWorkshopRequest(
                "Updated title",
                "Updated description",
                20L,
                2L
        );

        when(workshopRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(workshop));
        when(categoryService.findActiveCategory(2L)).thenReturn(newCategory);
        when(workshopRepository.saveAndFlush(workshop)).thenReturn(workshop);

        WorkshopResponse response = workshopService.update(1L, request);

        verify(instructorValidationService).ensureInstructorExists(20L);
        verify(categoryService).findActiveCategory(2L);

        assertThat(workshop.getTitle()).isEqualTo("Updated title");
        assertThat(workshop.getDescription()).isEqualTo("Updated description");
        assertThat(workshop.getInstructorId()).isEqualTo(20L);
        assertThat(workshop.getCategory()).isEqualTo(newCategory);
        assertThat(response.title()).isEqualTo("Updated title");
        assertThat(response.categoryId()).isEqualTo(2L);
    }

    @Test
    void deleteSetsWorkshopInactiveAndSoftDeletesActiveSessions() {
        Category category = category(1L, "Painting", true);
        Workshop workshop = workshop(1L, "Beginner Watercolor", 10L, category, true);
        WorkshopSession firstSession = session(1L, workshop, true);
        WorkshopSession secondSession = session(2L, workshop, true);

        when(workshopRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(workshop));
        when(workshopSessionRepository.findAllByWorkshopIdAndActiveTrue(1L))
                .thenReturn(List.of(firstSession, secondSession));

        workshopService.delete(1L);

        assertThat(workshop.isActive()).isFalse();
        assertThat(firstSession.isActive()).isFalse();
        assertThat(secondSession.isActive()).isFalse();
    }

    private Category category(Long id, String name, boolean active) {
        return Category.builder()
                .id(id)
                .name(name)
                .description(name + " description")
                .active(active)
                .build();
    }

    private Workshop workshop(Long id, String title, Long instructorId, Category category, boolean active) {
        return Workshop.builder()
                .id(id)
                .title(title)
                .description(title + " description")
                .instructorId(instructorId)
                .category(category)
                .active(active)
                .build();
    }

    private WorkshopSession session(Long id, Workshop workshop, boolean active) {
        return WorkshopSession.builder()
                .id(id)
                .workshop(workshop)
                .active(active)
                .build();
    }
}
