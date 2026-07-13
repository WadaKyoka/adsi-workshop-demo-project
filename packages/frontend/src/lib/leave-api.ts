import { apiClient } from "./api-client";

export interface LeaveResponse {
  id: string;
  requesterId: string;
  requesterName: string;
  leaveDate: string;
  leaveType: "FULL" | "AM" | "PM";
  reason: string | null;
  status: "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
  rejectReason: string | null;
  version: number;
  createdAt: string;
}

export interface LeaveBalanceResponse {
  employeeId: string;
  employeeName: string;
  departmentName: string;
  fiscalYear: number;
  grantedDays: number;
  carriedDays: number;
  usedDays: number;
  remainingDays: number;
}

export interface LeaveCreateRequest {
  requesterId: string;
  leaveDate: string;
  leaveType: "FULL" | "AM" | "PM";
  reason?: string;
}

export const leaveApi = {
  create(request: LeaveCreateRequest): Promise<LeaveResponse> {
    return apiClient.post("/api/leaves", request);
  },

  findByRequester(requesterId: string): Promise<LeaveResponse[]> {
    return apiClient.get(`/api/leaves?requesterId=${requesterId}`);
  },

  cancel(id: string, version: number): Promise<LeaveResponse> {
    return apiClient.patch(`/api/leaves/${id}/cancel?version=${version}`);
  },

  findPending(managerId: string): Promise<LeaveResponse[]> {
    return apiClient.get(`/api/leaves/pending?managerId=${managerId}`);
  },

  approve(id: string, approverId: string, version: number): Promise<LeaveResponse> {
    return apiClient.patch(`/api/leaves/${id}/approve`, { approverId, version });
  },

  reject(
    id: string,
    approverId: string,
    rejectReason: string,
    version: number,
  ): Promise<LeaveResponse> {
    return apiClient.patch(`/api/leaves/${id}/reject`, { approverId, rejectReason, version });
  },

  getBalance(employeeId: string): Promise<LeaveBalanceResponse> {
    return apiClient.get(`/api/leaves/balance?employeeId=${employeeId}`);
  },

  getAllBalances(fiscalYear: number): Promise<LeaveBalanceResponse[]> {
    return apiClient.get(`/api/leaves/balance/all?fiscalYear=${fiscalYear}`);
  },
};
