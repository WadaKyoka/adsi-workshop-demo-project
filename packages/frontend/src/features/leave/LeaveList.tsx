"use client";

import Link from "next/link";
import { type Column, DataTable } from "@/components/DataTable";
import { StatusBadge } from "@/components/StatusBadge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import type { LeaveResponse } from "@/lib/leave-api";
import { useCancelLeave, useLeaveBalance, useLeaves } from "./useLeaves";

const STATUS_CONFIG_MAP: Record<
  string,
  { label: string; variant: "default" | "secondary" | "destructive" | "outline" }
> = {
  PENDING: { label: "申請中", variant: "secondary" },
  APPROVED: { label: "承認済", variant: "default" },
  REJECTED: { label: "却下", variant: "destructive" },
  CANCELLED: { label: "取下げ", variant: "outline" },
};

const LEAVE_TYPE_LABEL: Record<string, string> = {
  FULL: "全日",
  AM: "午前休",
  PM: "午後休",
};

export function LeaveList() {
  const { data: leaves, isLoading } = useLeaves();
  const { data: balance } = useLeaveBalance();
  const cancelMutation = useCancelLeave();

  const columns: Column<LeaveResponse>[] = [
    {
      key: "leaveDate",
      header: "取得日",
      render: (item) => item.leaveDate,
    },
    {
      key: "leaveType",
      header: "種別",
      render: (item) => LEAVE_TYPE_LABEL[item.leaveType] ?? item.leaveType,
    },
    {
      key: "reason",
      header: "理由",
      render: (item) => <span className="max-w-[200px] truncate block">{item.reason ?? "-"}</span>,
    },
    {
      key: "status",
      header: "ステータス",
      render: (item) => <StatusBadge status={item.status} configMap={STATUS_CONFIG_MAP} />,
    },
    {
      key: "actions",
      header: "",
      render: (item) =>
        item.status === "PENDING" ? (
          <Button
            variant="outline"
            size="sm"
            disabled={cancelMutation.isPending}
            onClick={() => cancelMutation.mutate({ id: item.id, version: item.version })}
          >
            取下げ
          </Button>
        ) : null,
    },
  ];

  return (
    <div className="space-y-4">
      {balance && (
        <div className="rounded-lg border p-4 bg-muted/50">
          <p className="text-sm text-muted-foreground">有給残日数</p>
          <p className="text-2xl font-bold">{balance.remainingDays} 日</p>
          <p className="text-xs text-muted-foreground mt-1">
            付与: {balance.grantedDays}日 / 繰越: {balance.carriedDays}日 / 使用済:{" "}
            {balance.usedDays}日
          </p>
        </div>
      )}

      <div className="flex items-center justify-end">
        <Button render={<Link href="/leaves/new" />}>新規申請</Button>
      </div>

      {isLoading ? (
        <div className="space-y-2">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </div>
      ) : (
        <DataTable<LeaveResponse & Record<string, unknown>>
          columns={columns as Column<LeaveResponse & Record<string, unknown>>[]}
          data={(leaves ?? []) as (LeaveResponse & Record<string, unknown>)[]}
          rowKey={(item) => item.id}
          emptyMessage="有給申請はありません"
        />
      )}
    </div>
  );
}
