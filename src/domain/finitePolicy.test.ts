import {isScheduleActive, selectedTrackedApps} from './finitePolicy';

describe('FINITE schedule policy', () => {
  test('keeps an overnight window attached to its start day', () => {
    const fridayLate = new Date(2026, 10, 6, 23, 0);
    const saturdayEarly = new Date(2026, 10, 7, 1, 0);
    const saturdayDay = new Date(2026, 10, 7, 12, 0);
    const window = {days: [5], startMinute: 22 * 60, endMinute: 2 * 60};
    expect(isScheduleActive(fridayLate, window)).toBe(true);
    expect(isScheduleActive(saturdayEarly, window)).toBe(true);
    expect(isScheduleActive(saturdayDay, window)).toBe(false);
  });

  test('only exposes apps with implemented detectors', () => {
    expect(selectedTrackedApps(['instagram', 'tiktok', 'youtube', 'snapchat'])).toEqual(['instagram', 'youtube']);
  });
});
