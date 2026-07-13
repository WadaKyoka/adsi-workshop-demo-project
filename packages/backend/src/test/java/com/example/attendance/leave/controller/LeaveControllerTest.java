package com.example.attendance.leave.controller;

import com.example.attendance.leave.dto.LeaveBalanceResponse;
import com.example.attendance.leave.dto.LeaveResponse;
import com.example.attendance.leave.service.LeaveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaveController.class)
@AutoConfigureMockMvc(addFilters = false)
class LeaveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LeaveService leaveService;

    @Test
    @DisplayName("POST /api/leaves → 201")
    void create_validRequest_returns201() throws Exception {
        var response = new LeaveResponse(
                UUID.randomUUID(), UUID.randomUUID(), "田中太郎",
                LocalDate.of(2026, 7, 20), "FULL", "家庭の事情",
                "PENDING", null, 0L, Instant.now());

        when(leaveService.create(any())).thenReturn(response);

        var body = """
                {
                  "requesterId": "%s",
                  "leaveDate": "2026-07-20",
                  "leaveType": "FULL",
                  "reason": "家庭の事情"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.leaveType").value("FULL"));
    }

    @Test
    @DisplayName("GET /api/leaves?requesterId=... → 200")
    void findByRequester_returns200() throws Exception {
        var requesterId = UUID.randomUUID();
        var response = new LeaveResponse(
                UUID.randomUUID(), requesterId, "田中太郎",
                LocalDate.of(2026, 7, 20), "FULL", null,
                "PENDING", null, 0L, Instant.now());

        when(leaveService.findByRequester(requesterId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/leaves")
                        .param("requesterId", requesterId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requesterName").value("田中太郎"));
    }

    @Test
    @DisplayName("PATCH /api/leaves/{id}/cancel → 200")
    void cancel_returns200() throws Exception {
        var leaveId = UUID.randomUUID();
        var response = new LeaveResponse(
                leaveId, UUID.randomUUID(), "田中太郎",
                LocalDate.of(2026, 7, 20), "FULL", null,
                "CANCELLED", null, 1L, Instant.now());

        when(leaveService.cancel(leaveId, 0L)).thenReturn(response);

        mockMvc.perform(patch("/api/leaves/{id}/cancel", leaveId)
                        .param("version", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("PATCH /api/leaves/{id}/approve → 200")
    void approve_returns200() throws Exception {
        var leaveId = UUID.randomUUID();
        var approverId = UUID.randomUUID();
        var response = new LeaveResponse(
                leaveId, UUID.randomUUID(), "田中太郎",
                LocalDate.of(2026, 7, 20), "FULL", null,
                "APPROVED", null, 1L, Instant.now());

        when(leaveService.approve(leaveId, approverId, 0L)).thenReturn(response);

        var body = """
                {
                  "approverId": "%s",
                  "version": 0
                }
                """.formatted(approverId);

        mockMvc.perform(patch("/api/leaves/{id}/approve", leaveId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("PATCH /api/leaves/{id}/reject → 200")
    void reject_returns200() throws Exception {
        var leaveId = UUID.randomUUID();
        var approverId = UUID.randomUUID();
        var response = new LeaveResponse(
                leaveId, UUID.randomUUID(), "田中太郎",
                LocalDate.of(2026, 7, 20), "FULL", null,
                "REJECTED", "日程調整をお願いします", 1L, Instant.now());

        when(leaveService.reject(leaveId, approverId, "日程調整をお願いします", 0L))
                .thenReturn(response);

        var body = """
                {
                  "approverId": "%s",
                  "rejectReason": "日程調整をお願いします",
                  "version": 0
                }
                """.formatted(approverId);

        mockMvc.perform(patch("/api/leaves/{id}/reject", leaveId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("GET /api/leaves/balance?employeeId=... → 200")
    void getBalance_returns200() throws Exception {
        var employeeId = UUID.randomUUID();
        var response = new LeaveBalanceResponse(
                employeeId, "田中太郎", "開発部", 2026,
                new BigDecimal("20.0"), new BigDecimal("5.0"),
                new BigDecimal("3.5"), new BigDecimal("21.5"));

        when(leaveService.getBalance(employeeId)).thenReturn(response);

        mockMvc.perform(get("/api/leaves/balance")
                        .param("employeeId", employeeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingDays").value(21.5));
    }

    @Test
    @DisplayName("GET /api/leaves/balance/all?fiscalYear=2026 → 200")
    void getAllBalances_returns200() throws Exception {
        var response = new LeaveBalanceResponse(
                UUID.randomUUID(), "田中太郎", "開発部", 2026,
                new BigDecimal("20.0"), new BigDecimal("5.0"),
                new BigDecimal("3.5"), new BigDecimal("21.5"));

        when(leaveService.getAllBalances(2026)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/leaves/balance/all")
                        .param("fiscalYear", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeName").value("田中太郎"));
    }

    @Test
    @DisplayName("POST /api/leaves バリデーションエラー → 400")
    void create_invalidRequest_returns400() throws Exception {
        var body = """
                {
                  "leaveDate": "2026-07-20",
                  "leaveType": "FULL"
                }
                """;

        mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
