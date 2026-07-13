package com.example.attendance.leave.dto;

import com.example.attendance.leave.entity.LeaveRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveResponse(
        UUID id,
        UUID requesterId,
        String requesterName,
        LocalDate leaveDate,
        String leaveType,
        String reason,
        String status,
        String rejectReason,
        Long version,
        Instant createdAt
) {
    public static LeaveResponse from(LeaveRequest request) {
        return new LeaveResponse(
                request.getId(),
                request.getRequester().getId(),
                request.getRequester().getName(),
                request.getLeaveDate(),
                request.getLeaveType().name(),
                request.getReason(),
                request.getStatus().name(),
                request.getRejectReason(),
                request.getVersion(),
                request.getCreatedAt()
        );
    }
}
