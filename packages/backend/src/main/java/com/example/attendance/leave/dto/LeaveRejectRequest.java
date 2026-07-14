package com.example.attendance.leave.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LeaveRejectRequest(
        @NotNull UUID approverId,
        String rejectReason,
        @NotNull Long version
) {}
