import {DEFAULT_SCHEDULE} from './finitePolicy';
import {GUEST_SESSION, guestDefaults, guestStorageKey, sessionDestination, sessionStorageKey} from './guestPolicy';

test('guest has a stable isolated storage namespace', () => {
  expect(GUEST_SESSION).toBe('guest');
  expect(guestStorageKey('goal')).toBe('@finite/guest/goal');
  expect(sessionStorageKey(GUEST_SESSION, 'selectedApps')).toBe('@finite/guest/selectedApps');
  expect(sessionStorageKey({kind: 'account', email: 'User@Example.com'}, 'goal')).toBe('@finite/profile/user%40example.com/goal');
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
