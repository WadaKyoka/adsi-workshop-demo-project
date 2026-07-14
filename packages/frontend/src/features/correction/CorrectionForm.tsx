"use client";

import { useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useCreateCorrection } from "./useCorrections";

function toIsoInstant(date: string, time: string): string {
  return new Date(`${date}T${time}:00+09:00`).toISOString();
}

export function CorrectionForm() {
  const router = useRouter();
  const createMutation = useCreateCorrection();

  const [targetDate, setTargetDate] = useState("");
  const [clockIn, setClockIn] = useState("");
  const [clockOut, setClockOut] = useState("");
  const [reason, setReason] = useState("");
  const [memo, setMemo] = useState("");

  const isValid = targetDate && clockIn && clockOut && reason.trim().length > 0;

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!isValid) return;

    createMutation.mutate(
      {
        targetDate,
        correctedClockIn: toIsoInstant(targetDate, clockIn),
        correctedClockOut: toIsoInstant(targetDate, clockOut),
        reason: reason.trim(),
        memo: memo.trim() || undefined,
      },
      {
        onSuccess: () => {
          router.push("/corrections");
        },
      },
    );
  }

  return (
    <form onSubmit={handleSubmit} className="max-w-md space-y-6">
      <div className="space-y-2">
        <Label htmlFor="targetDate">対象日</Label>
        <Input
          id="targetDate"
          type="date"
          value={targetDate}
          onChange={(e) => setTargetDate(e.target.value)}
          required
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="clockIn">修正出勤時刻</Label>
        <Input
          id="clockIn"
          type="time"
          value={clockIn}
          onChange={(e) => setClockIn(e.target.value)}
          required
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="clockOut">修正退勤時刻</Label>
        <Input
          id="clockOut"
          type="time"
          value={clockOut}
          onChange={(e) => setClockOut(e.target.value)}
          required
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="memo">メモ</Label>
        <Input
          id="memo"
          type="text"
          value={memo}
          onChange={(e) => setMemo(e.target.value)}
          maxLength={100}
          placeholder="客先直行、午後有給 等（任意）"
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="reason">修正理由</Label>
        <textarea
          id="reason"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          required
          maxLength={500}
          rows={3}
          className="w-full min-w-0 rounded-lg border border-input bg-transparent px-2.5 py-2 text-base transition-colors outline-none placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50 md:text-sm dark:bg-input/30"
          placeholder="打刻忘れ、時刻誤り等の理由を入力してください"
        />
      </div>

      <div className="flex gap-2">
        <Button type="submit" disabled={!isValid || createMutation.isPending}>
          {createMutation.isPending ? "送信中..." : "申請する"}
        </Button>
        <Button type="button" variant="outline" onClick={() => router.push("/corrections")}>
          キャンセル
        </Button>
      </div>
    </form>
  );
}
