package com.example.attendance.leave.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record LeaveCreateRequest(
        @NotNull UUID requesterId,
        @NotNull LocalDate leaveDate,
        @NotNull String leaveType,
        String reason
) {}
