package com.example.attendance.leave.service;

import com.example.attendance.leave.dto.LeaveBalanceResponse;
import com.example.attendance.leave.dto.LeaveCreateRequest;
import com.example.attendance.leave.dto.LeaveResponse;

import java.util.List;
import java.util.UUID;

public interface LeaveService {

    LeaveResponse create(LeaveCreateRequest request);

    List<LeaveResponse> findByRequester(UUID requesterId);

    LeaveResponse cancel(UUID leaveId, Long version);

    List<LeaveResponse> findPending(UUID managerId);

    LeaveResponse approve(UUID leaveId, UUID approverId, Long version);

    LeaveResponse reject(UUID leaveId, UUID approverId, String rejectReason, Long version);

    LeaveBalanceResponse getBalance(UUID employeeId);

    List<LeaveBalanceResponse> getAllBalances(int fiscalYear);
}
