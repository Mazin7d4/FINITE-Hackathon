import {NativeModules} from 'react-native';

export type FiniteUsage = {
  todayCount: number;
  platformCounts: Record<string, number>;
  history: Array<{date: string; count: number; instagram: number; youtube: number}>;
};

export type FiniteNativeApi = {
  configure(limit: number, selectedApps: string[], days: number[], startMinute: number, endMinute: number): Promise<unknown>;
  getUsage(): Promise<FiniteUsage>;
  scheduleReminder(id: string, videoCount: number): Promise<boolean>;
  cancelReminder(id: string): Promise<boolean>;
  pickTime(initialMinute: number): Promise<number | null>;
  endSession(): Promise<unknown>;
  cancelAllReminders(): Promise<boolean>;
  hashPassword(password: string): Promise<{salt: string; hash: string; iterations: number}>;
  verifyPassword(password: string, salt: string, hash: string, iterations: number): Promise<boolean>;
};

export const FiniteNative = NativeModules.FiniteNative as FiniteNativeApi;

export const supportedApps = ['instagram', 'youtube'] as const;
