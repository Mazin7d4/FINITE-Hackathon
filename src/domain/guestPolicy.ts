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

export function guestDefaults(): {selectedApps: string[]; schedule: typeof DEFAULT_SCHEDULE; reminders: never[]} {
  return {
    selectedApps: [...DEFAULT_TRACKED_APPS],
    schedule: {...DEFAULT_SCHEDULE},
    reminders: [],
  };
}
