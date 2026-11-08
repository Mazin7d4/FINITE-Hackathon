/** The only credential fields that may be persisted for a local profile. */
export type PasswordVerifier = {passwordSalt: string; passwordHash: string; passwordIterations: number};

/** Rejects legacy records that contain the removed plaintext password field. */
export function isHashOnlyProfile(value: unknown): value is PasswordVerifier {
  if (!value || typeof value !== 'object') return false;
  const item = value as Partial<PasswordVerifier> & {password?: unknown};
  const iterations = item.passwordIterations;
  return typeof item.passwordSalt === 'string' && typeof item.passwordHash === 'string' && typeof iterations === 'number' && Number.isInteger(iterations) && iterations >= 100000 && item.password === undefined;
}

/** Returns only future reminders eligible for native restoration. */
export function futureReminders<T extends {timestamp: number}>(reminders: T[], now: number): T[] {
  return reminders.filter(reminder => Number.isFinite(reminder.timestamp) && reminder.timestamp > now);
}

type RepeatReminder = {timestamp: number; repeat: string};

/** Advances a missed recurring reminder using local wall-clock calendar dates. */
export function restoreRecurringReminders<T extends RepeatReminder>(reminders: T[], now: number): T[] {
  return reminders.flatMap(reminder => {
    if (!Number.isFinite(reminder.timestamp)) return [];
    if (reminder.timestamp > now) return [reminder];
    if (reminder.repeat !== 'Every day' && reminder.repeat !== 'Every week') return [];
    const next = new Date(reminder.timestamp);
    const days = reminder.repeat === 'Every day' ? 1 : 7;
    do next.setDate(next.getDate() + days); while (next.getTime() <= now);
    return [{...reminder, timestamp: next.getTime()}];
  });
}
