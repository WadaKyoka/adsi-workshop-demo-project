package com.example.attendance.correction.controller;

import com.example.attendance.common.config.CorsConfig;
import com.example.attendance.common.config.SecurityConfig;
import com.example.attendance.correction.dto.CorrectionResponse;
import com.example.attendance.correction.dto.PendingCorrectionResponse;
import com.example.attendance.correction.entity.CorrectionStatus;
import com.example.attendance.correction.service.CorrectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = CorrectionController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {SecurityConfig.class, CorsConfig.class}
    )
)
@Import(CorrectionControllerTest.TestSecurityConfig.class)
@ActiveProfiles("test")
class CorrectionControllerTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CorrectionService correctionService;

    private static final UUID EMPLOYEE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MANAGER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CORRECTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Test
    @DisplayName("POST /api/corrections は201を返す")
    void create_validRequest_returns201() throws Exception {
        // Arrange
        var response = new CorrectionResponse(
                CORRECTION_ID, null, EMPLOYEE_ID, "田中太郎", null, null,
                LocalDate.of(2025, 1, 15),
                Instant.parse("2025-01-14T23:00:00Z"),
                Instant.parse("2025-01-15T08:00:00Z"),
                "打刻忘れ", null, CorrectionStatus.PENDING, null, 0L,
                Instant.parse("2025-01-16T00:00:00Z")
        );
        when(correctionService.create(eq(EMPLOYEE_ID), any())).thenReturn(response);

        var body = """
                {
                    "targetDate": "2025-01-15",
                    "correctedClockIn": "2025-01-14T23:00:00Z",
                    "correctedClockOut": "2025-01-15T08:00:00Z",
                    "reason": "打刻忘れ"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/corrections")
                        .param("requesterId", EMPLOYEE_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.reason").value("打刻忘れ"));
    }

    @Test
    @DisplayName("GET /api/corrections は200と申請一覧を返す")
    void findByRequester_returns200() throws Exception {
        // Arrange
        var response = new CorrectionResponse(
                CORRECTION_ID, null, EMPLOYEE_ID, "田中太郎", null, null,
                LocalDate.of(2025, 1, 15),
                Instant.parse("2025-01-14T23:00:00Z"),
                Instant.parse("2025-01-15T08:00:00Z"),
                "打刻忘れ", null, CorrectionStatus.PENDING, null, 0L,
                Instant.parse("2025-01-16T00:00:00Z")
        );
        when(correctionService.findByRequester(eq(EMPLOYEE_ID), any())).thenReturn(List.of(response));

        // Act & Assert
        mockMvc.perform(get("/api/corrections")
                        .param("requesterId", EMPLOYEE_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/corrections/pending は200を返す")
    void findPending_returns200() throws Exception {
        // Arrange
        var response = new PendingCorrectionResponse(
                CORRECTION_ID, null, EMPLOYEE_ID, "田中太郎",
                LocalDate.of(2025, 1, 15),
                Instant.parse("2025-01-14T23:00:00Z"),
                Instant.parse("2025-01-15T08:00:00Z"),
                "打刻忘れ", null, 0L,
                Instant.parse("2025-01-16T00:00:00Z")
        );
        when(correctionService.findPending(MANAGER_ID)).thenReturn(List.of(response));

        // Act & Assert
        mockMvc.perform(get("/api/corrections/pending")
                        .param("managerId", MANAGER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requesterName").value("田中太郎"));
    }

    @Test
    @DisplayName("PATCH /api/corrections/{id}/approve は200を返す")
    void approve_returns200() throws Exception {
        // Arrange
        var response = new CorrectionResponse(
                CORRECTION_ID, null, EMPLOYEE_ID, "田中太郎",
                MANAGER_ID, "佐藤次郎",
                LocalDate.of(2025, 1, 15),
                Instant.parse("2025-01-14T23:00:00Z"),
                Instant.parse("2025-01-15T08:00:00Z"),
                "打刻忘れ", null, CorrectionStatus.APPROVED, null, 1L,
                Instant.parse("2025-01-16T00:00:00Z")
        );
        when(correctionService.approve(CORRECTION_ID, MANAGER_ID, 0L)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch("/api/corrections/{id}/approve", CORRECTION_ID)
                        .param("approverId", MANAGER_ID.toString())
                        .param("version", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("PATCH /api/corrections/{id}/reject は200を返す")
    void reject_returns200() throws Exception {
        // Arrange
        var response = new CorrectionResponse(
                CORRECTION_ID, null, EMPLOYEE_ID, "田中太郎",
                MANAGER_ID, "佐藤次郎",
                LocalDate.of(2025, 1, 15),
                Instant.parse("2025-01-14T23:00:00Z"),
                Instant.parse("2025-01-15T08:00:00Z"),
                "打刻忘れ", null, CorrectionStatus.REJECTED, "不備あり", 1L,
                Instant.parse("2025-01-16T00:00:00Z")
        );
        when(correctionService.reject(eq(CORRECTION_ID), eq(MANAGER_ID), eq("不備あり"), eq(0L)))
                .thenReturn(response);

        var body = """
                {
                    "reason": "不備あり",
                    "version": 0
                }
                """;

        // Act & Assert
        mockMvc.perform(patch("/api/corrections/{id}/reject", CORRECTION_ID)
                        .param("approverId", MANAGER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectReason").value("不備あり"));
    }
}
