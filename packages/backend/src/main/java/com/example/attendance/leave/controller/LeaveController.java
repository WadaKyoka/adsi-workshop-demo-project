package com.example.attendance.leave.controller;

import com.example.attendance.leave.dto.LeaveApproveRequest;
import com.example.attendance.leave.dto.LeaveBalanceResponse;
import com.example.attendance.leave.dto.LeaveCreateRequest;
import com.example.attendance.leave.dto.LeaveRejectRequest;
import com.example.attendance.leave.dto.LeaveResponse;
import com.example.attendance.leave.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    public LeaveResponse create(@Valid @RequestBody LeaveCreateRequest request) {
        return leaveService.create(request);
    }

    @GetMapping
    public List<LeaveResponse> findByRequester(@RequestParam UUID requesterId) {
        return leaveService.findByRequester(requesterId);
    }

    @PatchMapping("/{id}/cancel")
    public LeaveResponse cancel(@PathVariable UUID id, @RequestParam Long version) {
        return leaveService.cancel(id, version);
    }

    @GetMapping("/pending")
    public List<LeaveResponse> findPending(@RequestParam UUID managerId) {
        return leaveService.findPending(managerId);
    }

    @PatchMapping("/{id}/approve")
    public LeaveResponse approve(
            @PathVariable UUID id,
            @Valid @RequestBody LeaveApproveRequest request) {
        return leaveService.approve(id, request.approverId(), request.version());
    }

    @PatchMapping("/{id}/reject")
    public LeaveResponse reject(
            @PathVariable UUID id,
            @Valid @RequestBody LeaveRejectRequest request) {
        return leaveService.reject(id, request.approverId(), request.rejectReason(), request.version());
    }

    @GetMapping("/balance")
    public LeaveBalanceResponse getBalance(@RequestParam UUID employeeId) {
        return leaveService.getBalance(employeeId);
    }

    @GetMapping("/balance/all")
    public List<LeaveBalanceResponse> getAllBalances(@RequestParam int fiscalYear) {
        return leaveService.getAllBalances(fiscalYear);
    }
}
