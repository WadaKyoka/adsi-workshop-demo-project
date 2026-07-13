"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/components/Toast";
import { useAuth } from "@/features/auth/useAuth";
import { type LeaveCreateRequest, leaveApi } from "@/lib/leave-api";

const LEAVES_KEY = ["leaves"] as const;
const PENDING_LEAVES_KEY = ["leaves", "pending"] as const;
const BALANCE_KEY = ["leaves", "balance"] as const;

export function useLeaves() {
  const { user } = useAuth();

  return useQuery({
    queryKey: [...LEAVES_KEY, user?.id],
    queryFn: () => {
      if (!user) throw new Error("User not authenticated");
      return leaveApi.findByRequester(user.id);
    },
    enabled: !!user?.id,
  });
}

export function useLeaveBalance() {
  const { user } = useAuth();

  return useQuery({
    queryKey: [...BALANCE_KEY, user?.id],
    queryFn: () => {
      if (!user) throw new Error("User not authenticated");
      return leaveApi.getBalance(user.id);
    },
    enabled: !!user?.id,
  });
}

export function useCreateLeave() {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: Omit<LeaveCreateRequest, "requesterId">) => {
      if (!user) throw new Error("User not authenticated");
      return leaveApi.create({ ...request, requesterId: user.id });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: LEAVES_KEY });
      queryClient.invalidateQueries({ queryKey: BALANCE_KEY });
      toast.success("有給休暇を申請しました");
    },
    onError: () => {
      toast.error("有給申請に失敗しました");
    },
  });
}

export function useCancelLeave() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, version }: { id: string; version: number }) => leaveApi.cancel(id, version),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: LEAVES_KEY });
      queryClient.invalidateQueries({ queryKey: BALANCE_KEY });
      toast.success("申請を取り下げました");
    },
    onError: () => {
      toast.error("取り下げに失敗しました");
    },
  });
}

export function usePendingLeaves() {
  const { user } = useAuth();

  return useQuery({
    queryKey: [...PENDING_LEAVES_KEY, user?.id],
    queryFn: () => {
      if (!user) throw new Error("User not authenticated");
      return leaveApi.findPending(user.id);
    },
    enabled: !!user?.isManager,
  });
}

export function useApproveLeave() {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, version }: { id: string; version: number }) => {
      if (!user) throw new Error("User not authenticated");
      return leaveApi.approve(id, user.id, version);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: PENDING_LEAVES_KEY });
      queryClient.invalidateQueries({ queryKey: LEAVES_KEY });
      queryClient.invalidateQueries({ queryKey: BALANCE_KEY });
      toast.success("有給申請を承認しました");
    },
    onError: () => {
      toast.error("承認に失敗しました");
    },
  });
}

export function useRejectLeave() {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, reason, version }: { id: string; reason: string; version: number }) => {
      if (!user) throw new Error("User not authenticated");
      return leaveApi.reject(id, user.id, reason, version);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: PENDING_LEAVES_KEY });
      queryClient.invalidateQueries({ queryKey: LEAVES_KEY });
      toast.success("有給申請を却下しました");
    },
    onError: () => {
      toast.error("却下に失敗しました");
    },
  });
}

export function useAllBalances(fiscalYear: number) {
  return useQuery({
    queryKey: [...BALANCE_KEY, "all", fiscalYear],
    queryFn: () => leaveApi.getAllBalances(fiscalYear),
  });
}
