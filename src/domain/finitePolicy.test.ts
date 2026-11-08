import {DEFAULT_SCHEDULE, isScheduleActive, normalizeSchedule, normalizeTrackedApps, selectedTrackedApps} from './finitePolicy';
import {dashboardHistory, destinationAfterAppsSaved} from './flowPolicy';

describe('FINITE schedule policy', () => {
  test('keeps an overnight window attached to its start day', () => {
    const fridayLate = new Date(2026, 10, 6, 23, 0);
    const saturdayEarly = new Date(2026, 10, 7, 1, 0);
    const saturdayDay = new Date(2026, 10, 7, 12, 0);
    const window = {enabled: true, days: [5], startMinute: 22 * 60, endMinute: 2 * 60};
    expect(isScheduleActive(fridayLate, window)).toBe(true);
    expect(isScheduleActive(saturdayEarly, window)).toBe(true);
    expect(isScheduleActive(saturdayDay, window)).toBe(false);
  });

  test('only exposes apps with implemented detectors', () => {
    expect(selectedTrackedApps(['instagram', 'tiktok', 'youtube', 'snapchat'])).toEqual(['instagram', 'youtube']);
  });

  test('falls back to both apps for missing, empty, legacy, and labeled values', () => {
    expect(normalizeTrackedApps(undefined)).toEqual(['instagram', 'youtube']);
    expect(normalizeTrackedApps([])).toEqual(['instagram', 'youtube']);
    expect(normalizeTrackedApps(['old-instagram-id'])).toEqual(['instagram', 'youtube']);
    expect(normalizeTrackedApps(['com.instagram.android', 'YouTube Shorts'])).toEqual(['instagram', 'youtube']);
  });

  test('falls back to an always-active schedule when saved data is absent or invalid', () => {
    expect(normalizeSchedule(undefined)).toEqual(DEFAULT_SCHEDULE);
    expect(DEFAULT_SCHEDULE.enabled).toBe(false);
    expect(normalizeSchedule({id: 'exam_week', days: [], startMinute: 480, endMinute: 1320})).toEqual(DEFAULT_SCHEDULE);
    expect(isScheduleActive(new Date(2026, 10, 8, 3, 0), normalizeSchedule(undefined))).toBe(true);
  });

  test('disabled schedule ignores a restrictive saved window', () => {
    const disabled = normalizeSchedule({enabled: false, days: [1], startMinute: 8 * 60, endMinute: 9 * 60});
    expect(isScheduleActive(new Date(2026, 10, 8, 3, 0), disabled)).toBe(true);
  });

  test('keeps an explicit weekday schedule restricted', () => {
    const weekdays = normalizeSchedule({enabled: true, days: [1, 2, 3, 4, 5], startMinute: 8 * 60, endMinute: 22 * 60});
    expect(isScheduleActive(new Date(2026, 10, 6, 12, 0), weekdays)).toBe(true);
    expect(isScheduleActive(new Date(2026, 10, 7, 12, 0), weekdays)).toBe(false);
  });

  test('editor navigation returns to dashboard without replaying onboarding', () => {
    expect(destinationAfterAppsSaved(true)).toBe('review');
    expect(destinationAfterAppsSaved(false)).toBe('choose-schedule');
    expect(dashboardHistory()).toEqual([]);
  });

  test('migrated schedule normalizes duplicate days without changing its policy', () => {
    expect(normalizeSchedule({enabled: true, id: 'weekdays', days: [5, 1, 1], startMinute: 480, endMinute: 1320})).toEqual({enabled: true, days: [1, 5], startMinute: 480, endMinute: 1320});
  });

  test('enabling a migrated window preserves its saved days and times', () => {
    const saved = normalizeSchedule({enabled: false, days: [1, 5], startMinute: 480, endMinute: 1320});
    expect({...saved, enabled: true}).toEqual({enabled: true, days: [1, 5], startMinute: 480, endMinute: 1320});
  });
});
