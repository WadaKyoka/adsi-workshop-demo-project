package com.example.attendance.leave.controller;

import com.example.attendance.common.config.security.EmployeeUserDetails;
import com.example.attendance.leave.dto.LeaveApproveRequest;
import com.example.attendance.leave.dto.LeaveBalanceResponse;
import com.example.attendance.leave.dto.LeaveCreateRequest;
import com.example.attendance.leave.dto.LeaveRejectRequest;
import com.example.attendance.leave.dto.LeaveResponse;
import com.example.attendance.leave.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveResponse create(
            @AuthenticationPrincipal EmployeeUserDetails principal,
            @Valid @RequestBody LeaveCreateRequest request) {
        verifyOwnership(principal, request.requesterId());
        return leaveService.create(request);
    }

    @GetMapping
    public List<LeaveResponse> findByRequester(
            @AuthenticationPrincipal EmployeeUserDetails principal,
            @RequestParam UUID requesterId) {
        verifyOwnership(principal, requesterId);
        return leaveService.findByRequester(requesterId);
    }

    @PatchMapping("/{id}/cancel")
    public LeaveResponse cancel(
            @AuthenticationPrincipal EmployeeUserDetails principal,
            @PathVariable UUID id,
            @RequestParam Long version) {
        return leaveService.cancel(id, principal.getEmployeeId(), version);
    }

    @GetMapping("/pending")
    public List<LeaveResponse> findPending(
            @AuthenticationPrincipal EmployeeUserDetails principal,
            @RequestParam UUID managerId) {
        verifyOwnership(principal, managerId);
        return leaveService.findPending(managerId);
    }

    @PatchMapping("/{id}/approve")
    public LeaveResponse approve(
            @AuthenticationPrincipal EmployeeUserDetails principal,
            @PathVariable UUID id,
            @Valid @RequestBody LeaveApproveRequest request) {
        verifyOwnership(principal, request.approverId());
        return leaveService.approve(id, request.approverId(), request.version());
    }

    @PatchMapping("/{id}/reject")
    public LeaveResponse reject(
            @AuthenticationPrincipal EmployeeUserDetails principal,
            @PathVariable UUID id,
            @Valid @RequestBody LeaveRejectRequest request) {
        verifyOwnership(principal, request.approverId());
        return leaveService.reject(id, request.approverId(), request.rejectReason(), request.version());
    }

    @GetMapping("/balance")
    public LeaveBalanceResponse getBalance(
            @AuthenticationPrincipal EmployeeUserDetails principal,
            @RequestParam UUID employeeId) {
        verifyOwnership(principal, employeeId);
        return leaveService.getBalance(employeeId);
    }

    @GetMapping("/balance/all")
    public List<LeaveBalanceResponse> getAllBalances(@RequestParam int fiscalYear) {
        return leaveService.getAllBalances(fiscalYear);
    }

    private void verifyOwnership(EmployeeUserDetails principal, UUID targetId) {
        if (!principal.getEmployeeId().equals(targetId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "アクセス権限がありません");
        }
    }
}
