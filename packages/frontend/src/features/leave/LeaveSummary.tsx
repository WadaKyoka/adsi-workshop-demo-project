"use client";

import { useState } from "react";
import { type Column, DataTable } from "@/components/DataTable";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import type { LeaveBalanceResponse } from "@/lib/leave-api";
import { useAllBalances } from "./useLeaves";

function currentFiscalYear(): number {
  const now = new Date();
  return now.getMonth() >= 3 ? now.getFullYear() : now.getFullYear() - 1;
}

const columns: Column<LeaveBalanceResponse>[] = [
  { key: "employeeName", header: "社員名", render: (item) => item.employeeName },
  { key: "departmentName", header: "部署", render: (item) => item.departmentName },
  { key: "grantedDays", header: "付与", render: (item) => `${item.grantedDays}日` },
  { key: "carriedDays", header: "繰越", render: (item) => `${item.carriedDays}日` },
  { key: "usedDays", header: "使用済", render: (item) => `${item.usedDays}日` },
  {
    key: "remainingDays",
    header: "残日数",
    render: (item) => <span className="font-bold">{item.remainingDays}日</span>,
  },
];

export function LeaveSummary() {
  const [fiscalYear, setFiscalYear] = useState(currentFiscalYear());
  const { data, isLoading } = useAllBalances(fiscalYear);

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <Label htmlFor="fiscalYear">年度</Label>
        <Input
          id="fiscalYear"
          type="number"
          className="w-24"
          value={fiscalYear}
          onChange={(e) => setFiscalYear(Number(e.target.value))}
        />
      </div>

      {isLoading ? (
        <div className="space-y-2">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </div>
      ) : (
        <DataTable<LeaveBalanceResponse & Record<string, unknown>>
          columns={columns as Column<LeaveBalanceResponse & Record<string, unknown>>[]}
          data={(data ?? []) as (LeaveBalanceResponse & Record<string, unknown>)[]}
          rowKey={(item) => item.employeeId}
          emptyMessage="データがありません"
        />
      )}
    </div>
  );
}
