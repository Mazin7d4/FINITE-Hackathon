export type ScheduleWindow = {days: number[]; startMinute: number; endMinute: number};

/** Mirrors the native policy's local-clock rules for previews and deterministic UI tests. */
export function isScheduleActive(date: Date, window: ScheduleWindow): boolean {
  const minute = date.getHours() * 60 + date.getMinutes();
  const overnight = window.startMinute > window.endMinute;
  const scheduleDay = overnight && minute < window.endMinute
    ? (date.getDay() + 6) % 7
    : date.getDay();
  if (!window.days.includes(scheduleDay)) return false;
  if (window.startMinute === window.endMinute) return true;
  return overnight
    ? minute >= window.startMinute || minute < window.endMinute
    : minute >= window.startMinute && minute < window.endMinute;
}

export function selectedTrackedApps(apps: string[]): string[] {
  return apps.filter(app => app === 'instagram' || app === 'youtube');
}
