"use client";

import { ClockButtons } from "@/features/attendance/ClockButtons";
import { TodayRecords } from "@/features/attendance/TodayRecords";
import { useAuth } from "@/features/auth/useAuth";

export default function DashboardPage() {
  const { user } = useAuth();

  return (
    <div className="space-y-6 min-h-screen bg-green-50 -m-6 p-6">
      <div>
        <h1 className="text-2xl font-bold">ダッシュボード</h1>
        {user && (
          <p className="text-muted-foreground mt-1">
            {user.departmentName} / <span className="font-medium text-foreground">{user.name}</span>
          </p>
        )}
      </div>
      <ClockButtons />
      <TodayRecords />
    </div>
  );
}
