"use client";

import { useState } from "react";
import { type Column, DataTable } from "@/components/DataTable";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import type { LeaveResponse } from "@/lib/leave-api";
import { useApproveLeave, usePendingLeaves, useRejectLeave } from "./useLeaves";

const LEAVE_TYPE_LABEL: Record<string, string> = {
  FULL: "全日",
  AM: "午前休",
  PM: "午後休",
};

export function PendingLeaveList() {
  const { data: pending, isLoading } = usePendingLeaves();
  const approveMutation = useApproveLeave();
  const rejectMutation = useRejectLeave();
  const [rejectReasons, setRejectReasons] = useState<Record<string, string>>({});

  const columns: Column<LeaveResponse>[] = [
    {
      key: "requesterName",
      header: "申請者",
      render: (item) => item.requesterName,
    },
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
      key: "actions",
      header: "操作",
      render: (item) => (
        <div className="flex items-center gap-2">
          <Button
            size="sm"
            disabled={approveMutation.isPending}
            onClick={() => approveMutation.mutate({ id: item.id, version: item.version })}
          >
            承認
          </Button>
          <Input
            placeholder="却下理由"
            className="w-40 h-8 text-sm"
            value={rejectReasons[item.id] ?? ""}
            onChange={(e) => setRejectReasons((prev) => ({ ...prev, [item.id]: e.target.value }))}
          />
          <Button
            size="sm"
            variant="destructive"
            disabled={rejectMutation.isPending}
            onClick={() =>
              rejectMutation.mutate({
                id: item.id,
                reason: rejectReasons[item.id] ?? "",
                version: item.version,
              })
            }
          >
            却下
          </Button>
        </div>
      ),
    },
  ];

  if (isLoading) {
    return (
      <div className="space-y-2">
        {[1, 2, 3].map((i) => (
          <Skeleton key={i} className="h-10 w-full" />
        ))}
      </div>
    );
  }

  return (
    <DataTable<LeaveResponse & Record<string, unknown>>
      columns={columns as Column<LeaveResponse & Record<string, unknown>>[]}
      data={(pending ?? []) as (LeaveResponse & Record<string, unknown>)[]}
      rowKey={(item) => item.id}
      emptyMessage="承認待ちの有給申請はありません"
    />
  );
}
