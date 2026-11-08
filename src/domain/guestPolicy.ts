import {DEFAULT_SCHEDULE, DEFAULT_TRACKED_APPS} from './finitePolicy';

export const GUEST_SESSION = 'guest' as const;
export const GUEST_STORAGE_PREFIX = '@finite/guest/';

export type SessionIdentity = typeof GUEST_SESSION | {kind: 'account'; email: string};

export function guestStorageKey(name: string): string {
  return `${GUEST_STORAGE_PREFIX}${name}`;
}

export function sessionStorageKey(identity: SessionIdentity, name: string): string {
  return identity === GUEST_SESSION
    ? guestStorageKey(name)
    : `@finite/profile/${encodeURIComponent(identity.email.toLowerCase())}/${name}`;
}

export function sessionDestination(hasGoal: boolean): 'review' | 'choose-apps' {
  return hasGoal ? 'review' : 'choose-apps';
}

export type NativeReminderRecord = {id: string; videoCount: number};

/** Legacy alarm records are deliberately not compatible with milestone records. */
export function readNativeReminders(value: unknown): NativeReminderRecord[] {
  if (!Array.isArray(value)) return [];
  return value.filter(item => {
    if (!item || typeof item !== 'object') return false;
    const record = item as Record<string, unknown>;
    return typeof record.id === 'string' && typeof record.videoCount === 'number' && Number.isInteger(record.videoCount) && record.videoCount > 0 &&
      !('timestamp' in record) && !('repeat' in record);
  }) as NativeReminderRecord[];
}

export function hasLegacyAlarmReminder(value: unknown): boolean {
  return Array.isArray(value) && value.some(item => item && typeof item === 'object' && ('timestamp' in item || 'repeat' in item));
}

export function guestDefaults(): {selectedApps: string[]; schedule: typeof DEFAULT_SCHEDULE; reminders: never[]} {
  return {
    selectedApps: [...DEFAULT_TRACKED_APPS],
    schedule: {...DEFAULT_SCHEDULE},
    reminders: [],
  };
}
