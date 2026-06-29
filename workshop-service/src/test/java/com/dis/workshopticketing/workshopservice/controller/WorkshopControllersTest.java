package com.dis.workshopticketing.workshopservice.controller;

import com.dis.workshopticketing.workshopservice.dto.CategoryResponse;
import com.dis.workshopticketing.workshopservice.dto.CreateCategoryRequest;
import com.dis.workshopticketing.workshopservice.dto.CreateWorkshopRequest;
import com.dis.workshopticketing.workshopservice.dto.CreateWorkshopSessionRequest;
import com.dis.workshopticketing.workshopservice.dto.ExistenceResponse;
import com.dis.workshopticketing.workshopservice.dto.UpdateWorkshopSessionRequest;
import com.dis.workshopticketing.workshopservice.dto.WorkshopResponse;
import com.dis.workshopticketing.workshopservice.dto.WorkshopSessionResponse;
import com.dis.workshopticketing.workshopservice.exception.ResourceNotFoundException;
import com.dis.workshopticketing.workshopservice.service.CategoryService;
import com.dis.workshopticketing.workshopservice.service.WorkshopService;
import com.dis.workshopticketing.workshopservice.service.WorkshopSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:workshop_controller_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class WorkshopControllersTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private WorkshopService workshopService;

    @MockitoBean
    private WorkshopSessionService workshopSessionService;

    @Test
    void createsCategory() throws Exception {
        when(categoryService.create(any(CreateCategoryRequest.class))).thenReturn(category());

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Backend\",\"description\":\"Server side\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Backend"));

        verify(categoryService).create(any(CreateCategoryRequest.class));
    }

    @Test
    void rejectsInvalidCategoryCreateRequest() throws Exception {
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Missing name\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("name: must not be blank"));

        verifyNoInteractions(categoryService);
    }

    @Test
    void listsCategories() throws Exception {
        when(categoryService.getAll()).thenReturn(List.of(category()));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Backend"));

        verify(categoryService).getAll();
    }

    @Test
    void mapsCategoryServiceException() throws Exception {
        when(categoryService.get(99L)).thenThrow(new ResourceNotFoundException("Category", 99L));

        mockMvc.perform(get("/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found: 99"));
    }

    @Test
    void deletesCategory() throws Exception {
        mockMvc.perform(delete("/categories/1"))
                .andExpect(status().isNoContent());

        verify(categoryService).delete(1L);
    }

    @Test
    void createsWorkshop() throws Exception {
        when(workshopService.create(any(CreateWorkshopRequest.class))).thenReturn(workshop());

        mockMvc.perform(post("/workshops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Spring Boot\",\"description\":\"Build services\",\"instructorId\":5,\"categoryId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Spring Boot"));

        verify(workshopService).create(any(CreateWorkshopRequest.class));
    }

    @Test
    void rejectsInvalidWorkshopCreateRequest() throws Exception {
        mockMvc.perform(post("/workshops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Build services\",\"instructorId\":5,\"categoryId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("title: must not be blank"));

        verifyNoInteractions(workshopService);
    }

    @Test
    void updatesWorkshop() throws Exception {
        when(workshopService.update(eq(10L), any())).thenReturn(workshop());

        mockMvc.perform(put("/workshops/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Spring Boot\",\"description\":\"Build services\",\"instructorId\":5,\"categoryId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));

        verify(workshopService).update(eq(10L), any());
    }

    @Test
    void createsWorkshopSession() throws Exception {
        when(workshopSessionService.create(eq(10L), any(CreateWorkshopSessionRequest.class))).thenReturn(session());

        mockMvc.perform(post("/workshops/10/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.workshopId").value(10));

        verify(workshopSessionService).create(eq(10L), any(CreateWorkshopSessionRequest.class));
    }

    @Test
    void listsWorkshopSessions() throws Exception {
        when(workshopSessionService.getAllByWorkshop(10L)).thenReturn(List.of(session()));

        mockMvc.perform(get("/workshops/10/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100));

        verify(workshopSessionService).getAllByWorkshop(10L);
    }

    @Test
    void rejectsInvalidWorkshopSessionUpdateRequest() throws Exception {
        mockMvc.perform(put("/sessions/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startsAt\":\"2026-07-01T10:00:00\",\"endsAt\":\"2026-07-01T12:00:00\",\"price\":50.00,\"capacity\":20}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("location: must not be blank"));

        verifyNoInteractions(workshopSessionService);
    }

    @Test
    void updatesWorkshopSession() throws Exception {
        when(workshopSessionService.update(eq(100L), any(UpdateWorkshopSessionRequest.class))).thenReturn(session());

        mockMvc.perform(put("/sessions/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.location").value("Room 1"));

        verify(workshopSessionService).update(eq(100L), any(UpdateWorkshopSessionRequest.class));
    }

    @Test
    void checksWorkshopSessionExistence() throws Exception {
        when(workshopSessionService.exists(100L)).thenReturn(new ExistenceResponse(100L, true));

        mockMvc.perform(get("/sessions/100/exists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.exists").value(true));

        verify(workshopSessionService).exists(100L);
    }

    @Test
    void deletesWorkshopSession() throws Exception {
        mockMvc.perform(delete("/sessions/100"))
                .andExpect(status().isNoContent());

        verify(workshopSessionService).delete(100L);
    }

    @Test
    void mapsWorkshopSessionServiceException() throws Exception {
        doThrow(new ResourceNotFoundException("Workshop session", 100L))
                .when(workshopSessionService).delete(100L);

        mockMvc.perform(delete("/sessions/100"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Workshop session not found: 100"));
    }

    private CategoryResponse category() {
        return new CategoryResponse(
                1L,
                "Backend",
                "Server side",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private WorkshopResponse workshop() {
        return new WorkshopResponse(
                10L,
                "Spring Boot",
                "Build services",
                5L,
                1L,
                "Backend",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private WorkshopSessionResponse session() {
        return new WorkshopSessionResponse(
                100L,
                10L,
                "Spring Boot",
                LocalDateTime.of(2026, 7, 1, 10, 0),
                LocalDateTime.of(2026, 7, 1, 12, 0),
                "Room 1",
                new BigDecimal("50.00"),
                20,
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private String sessionJson() {
        return """
                {
                  "startsAt": "2026-07-01T10:00:00",
                  "endsAt": "2026-07-01T12:00:00",
                  "location": "Room 1",
                  "price": 50.00,
                  "capacity": 20
                }
                """;
    }
}
