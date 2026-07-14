"use client";

import { LeaveForm } from "@/features/leave/LeaveForm";

export default function NewLeavePage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">有給休暇 - 新規申請</h1>
      <LeaveForm />
    </div>
  );
}
