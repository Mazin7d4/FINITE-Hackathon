import {DEFAULT_SCHEDULE} from './finitePolicy';
import {GUEST_SESSION, guestDefaults, guestStorageKey, hasLegacyAlarmReminder, readNativeReminders, sessionDestination, sessionStorageKey} from './guestPolicy';

test('guest has a stable isolated storage namespace', () => {
  expect(GUEST_SESSION).toBe('guest');
  expect(guestStorageKey('goal')).toBe('@finite/guest/goal');
  expect(sessionStorageKey(GUEST_SESSION, 'selectedApps')).toBe('@finite/guest/selectedApps');
  expect(sessionStorageKey({kind: 'account', email: 'User@Example.com'}, 'goal')).toBe('@finite/profile/user%40example.com/goal');
});

test('does not reinterpret legacy timestamp/repeat alarms as milestones', () => {
  const legacy = [{id: 'old', timestamp: Date.now(), repeat: 'Every day', videoCount: 4}];
  expect(readNativeReminders(legacy)).toEqual([]);
  expect(hasLegacyAlarmReminder(legacy)).toBe(true);
});

test('accepts only native milestone reminder records', () => {
  expect(readNativeReminders([{id: 'milestone', videoCount: 4}, {id: 'bad', videoCount: 0}, {id: 'legacy', timestamp: 1, videoCount: 2}])).toEqual([
    {id: 'milestone', videoCount: 4},
  ]);
});

test('guest follows the same onboarding route as an account', () => {
  expect(sessionDestination(false)).toBe('choose-apps');
  expect(sessionDestination(true)).toBe('review');
});

test('guest defaults track both supported apps with schedule off', () => {
  const defaults = guestDefaults();
  expect(defaults.selectedApps).toEqual(['instagram', 'youtube']);
  expect(defaults.schedule).toEqual(DEFAULT_SCHEDULE);
  expect(defaults.schedule.enabled).toBe(false);
  expect(defaults.reminders).toEqual([]);
});
