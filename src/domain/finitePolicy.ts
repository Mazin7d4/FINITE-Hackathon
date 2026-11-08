export type ScheduleWindow = {enabled: boolean; days: number[]; startMinute: number; endMinute: number};

export const DEFAULT_TRACKED_APPS = ['instagram', 'youtube'] as const;
export const DEFAULT_SCHEDULE: ScheduleWindow = {
  enabled: false,
  days: [0, 1, 2, 3, 4, 5, 6],
  startMinute: 0,
  endMinute: 0,
};

const APP_ALIASES: Record<string, 'instagram' | 'youtube'> = {
  instagram: 'instagram',
  'instagram reels': 'instagram',
  reels: 'instagram',
  'com.instagram.android': 'instagram',
  youtube: 'youtube',
  'youtube shorts': 'youtube',
  shorts: 'youtube',
  'com.google.android.youtube': 'youtube',
};

export function normalizeTrackedApps(value: unknown): string[] {
  const values = Array.isArray(value) ? value : typeof value === 'string' ? [value] : [];
  const normalized = values
    .filter((app): app is string => typeof app === 'string')
    .map(app => APP_ALIASES[app.trim().toLowerCase()])
    .filter((app): app is 'instagram' | 'youtube' => Boolean(app));
  const unique = [...new Set(normalized)];
  return unique.length > 0
    ? unique
    : [...DEFAULT_TRACKED_APPS];
}

export function isValidSchedule(value: unknown): value is ScheduleWindow {
  if (!value || typeof value !== 'object') return false;
  const candidate = value as Partial<ScheduleWindow>;
  return Array.isArray(candidate.days) && candidate.days.length > 0 &&
    candidate.days.every(day => Number.isInteger(day) && day >= 0 && day <= 6) &&
    typeof candidate.startMinute === 'number' && Number.isInteger(candidate.startMinute) && candidate.startMinute >= 0 && candidate.startMinute < 24 * 60 &&
    typeof candidate.endMinute === 'number' && Number.isInteger(candidate.endMinute) && candidate.endMinute >= 0 && candidate.endMinute < 24 * 60;
}

export function normalizeSchedule(value: unknown): ScheduleWindow {
  if (!isValidSchedule(value)) return {...DEFAULT_SCHEDULE};
  return {
    enabled: (value as Partial<ScheduleWindow>).enabled === true,
    days: [...new Set(value.days)].sort((a, b) => a - b),
    startMinute: value.startMinute,
    endMinute: value.endMinute,
  };
}

/** Mirrors the native policy's local-clock rules for previews and deterministic UI tests. */
export function isScheduleActive(date: Date, window: ScheduleWindow): boolean {
  if (!window.enabled) return true;
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
  return normalizeTrackedApps(apps);
}
