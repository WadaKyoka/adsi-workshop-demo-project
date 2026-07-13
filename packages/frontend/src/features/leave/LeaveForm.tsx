"use client";

import { useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useCreateLeave, useLeaveBalance } from "./useLeaves";

export function LeaveForm() {
  const router = useRouter();
  const createMutation = useCreateLeave();
  const { data: balance } = useLeaveBalance();

  const [leaveDate, setLeaveDate] = useState("");
  const [leaveType, setLeaveType] = useState<"FULL" | "AM" | "PM">("FULL");
  const [reason, setReason] = useState("");

  const isValid = leaveDate.length > 0;

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!isValid) return;

    createMutation.mutate(
      {
        leaveDate,
        leaveType,
        reason: reason.trim() || undefined,
      },
      {
        onSuccess: () => {
          router.push("/leaves");
        },
      },
    );
  }

  return (
    <form onSubmit={handleSubmit} className="max-w-md space-y-6">
      {balance && (
        <div className="rounded-lg border p-3 bg-muted/50 text-sm">
          有給残日数: <span className="font-bold">{balance.remainingDays} 日</span>
        </div>
      )}

      <div className="space-y-2">
        <Label htmlFor="leaveDate">取得日</Label>
        <Input
          id="leaveDate"
          type="date"
          value={leaveDate}
          onChange={(e) => setLeaveDate(e.target.value)}
          required
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="leaveType">種別</Label>
        <Select value={leaveType} onValueChange={(v) => setLeaveType(v as "FULL" | "AM" | "PM")}>
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="FULL">全日</SelectItem>
            <SelectItem value="AM">午前休</SelectItem>
            <SelectItem value="PM">午後休</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-2">
        <Label htmlFor="reason">理由（任意）</Label>
        <textarea
          id="reason"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          maxLength={500}
          rows={3}
          className="w-full min-w-0 rounded-lg border border-input bg-transparent px-2.5 py-2 text-base transition-colors outline-none placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50 md:text-sm dark:bg-input/30"
          placeholder="理由があれば入力してください"
        />
      </div>

      <div className="flex gap-2">
        <Button type="submit" disabled={!isValid || createMutation.isPending}>
          {createMutation.isPending ? "送信中..." : "申請する"}
        </Button>
        <Button type="button" variant="outline" onClick={() => router.push("/leaves")}>
          キャンセル
        </Button>
      </div>
    </form>
  );
}
