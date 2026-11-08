import {futureReminders, isHashOnlyProfile, restoreRecurringReminders} from './profileSecurity';

describe('local profile security boundaries', () => {
  test('accepts only salted derived credentials and never plaintext', () => {
    expect(isHashOnlyProfile({passwordSalt: 'salt', passwordHash: 'hash', passwordIterations: 120000})).toBe(true);
    expect(isHashOnlyProfile({password: 'secret', passwordSalt: 'salt', passwordHash: 'hash', passwordIterations: 120000})).toBe(false);
  });

  test('restores future reminders only', () => {
    expect(futureReminders([{timestamp: 99}, {timestamp: 100}, {timestamp: 101}], 100)).toEqual([{timestamp: 101}]);
  });

  test('advances missed daily and weekly reminders using local calendar dates', () => {
    const now = new Date(2026, 10, 10, 12, 0).getTime();
    const daily = new Date(2026, 10, 9, 9, 30).getTime();
    const weekly = new Date(2026, 9, 27, 17, 45).getTime();
    const restored = restoreRecurringReminders([
      {id: 'daily', timestamp: daily, repeat: 'Every day'},
      {id: 'weekly', timestamp: weekly, repeat: 'Every week'},
      {id: 'once', timestamp: daily, repeat: 'Never'},
    ], now);
    expect(restored.map(item => [item.id, new Date(item.timestamp).getDate()])).toEqual([
      ['daily', 11],
      ['weekly', 10],
    ]);
  });
});
