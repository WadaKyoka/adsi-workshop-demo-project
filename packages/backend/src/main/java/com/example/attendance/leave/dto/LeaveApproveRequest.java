package com.example.attendance.leave.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LeaveApproveRequest(
        @NotNull UUID approverId,
        @NotNull Long version
) {}
