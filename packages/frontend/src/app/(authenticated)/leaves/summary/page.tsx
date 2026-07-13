"use client";

import { LeaveSummary } from "@/features/leave/LeaveSummary";

export default function LeaveSummaryPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">有給取得状況</h1>
      <LeaveSummary />
    </div>
  );
}
