"use client";

import { PendingLeaveList } from "@/features/leave/PendingLeaveList";

export default function PendingLeavesPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">有給休暇 - 承認待ち</h1>
      <PendingLeaveList />
    </div>
  );
}
