import React, {useEffect, useState} from 'react';
import {Alert, BackHandler} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import {LinearGradient} from 'expo-linear-gradient';
import {SafeAreaProvider} from 'react-native-safe-area-context';
import LoginScreen from './src/screens/login';
import FiniteScreen from './src/screens/FiniteScreen';
import SignUpScreen from './src/screens/SignUpScreen';
import ChooseApp from './src/screens/ChooseApp';
import ChooseSchedule from './src/screens/ChooseSchedule';
import type {ScheduleSettings} from './src/screens/ChooseSchedule';
import PersonalizeGoal from './src/screens/PersonalizeGoal';
import type {PersonalGoal} from './src/screens/PersonalizeGoal';
import UsageReview from './src/screens/UsageReview';
import Reminder, {type ReminderRecord} from './src/screens/Reminder';
import {finiteGradientColors} from './src/screens/theme';
import {FiniteNative} from './src/native/finite';
import {DEFAULT_SCHEDULE, normalizeSchedule, normalizeTrackedApps, isScheduleActive} from './src/domain/finitePolicy';
import {dashboardHistory, destinationAfterAppsSaved, destinationForSignedInProfile} from './src/domain/flowPolicy';
import {GUEST_SESSION, sessionDestination, sessionStorageKey, type SessionIdentity} from './src/domain/guestPolicy';
import {restoreRecurringReminders} from './src/domain/profileSecurity';

type Screen = 'login' | 'signup' | 'choose-apps' | 'choose-schedule' | 'personalize' | 'review' | 'reminder' | 'home';
type Profile = {name: string; email: string; passwordSalt: string; passwordHash: string; passwordIterations: number};
type StoredState = {goal?: PersonalGoal; selectedApps: string[]; schedule: ScheduleSettings; reminders: ReminderRecord[]};
const LEGACY = {goal: '@finite/personal-goal', selectedApps: '@finite/selected-apps', schedule: '@finite/schedule', reminders: '@finite/reminders'} as const;
const AUTH = {profiles: '@finite/local-profiles', signedIn: '@finite/signed-in', legacyMigrated: '@finite/local-profile-migration-v1'} as const;
const defaultState = (): StoredState => ({selectedApps: ['instagram', 'youtube'], schedule: {id: 'exam_week', ...DEFAULT_SCHEDULE}, reminders: []});

export default function App(): React.JSX.Element {
  const [screen, setScreen] = useState<Screen>('login');
  const [history, setHistory] = useState<Screen[]>([]);
  const [profile, setProfile] = useState<Profile>();
  const [identity, setIdentity] = useState<SessionIdentity>();
  const [profiles, setProfiles] = useState<Profile[]>([]);
  const [selectedApps, setSelectedApps] = useState<string[]>(defaultState().selectedApps);
  const [goal, setGoal] = useState<PersonalGoal>();
  const [schedule, setSchedule] = useState<ScheduleSettings>(defaultState().schedule);
  const [reminders, setReminders] = useState<ReminderRecord[]>([]);
  const [storageLoaded, setStorageLoaded] = useState(false);
  const [editing, setEditing] = useState(false);
  const [editingReminderId, setEditingReminderId] = useState<string>();

  const readState = async (session?: SessionIdentity): Promise<StoredState> => {
    const key = (name: keyof typeof LEGACY) => session ? sessionStorageKey(session, name) : LEGACY[name];
    const [savedGoal, savedApps, savedSchedule, savedReminders] = await Promise.all([AsyncStorage.getItem(key('goal')), AsyncStorage.getItem(key('selectedApps')), AsyncStorage.getItem(key('schedule')), AsyncStorage.getItem(key('reminders'))]);
    const next = defaultState();
    if (savedGoal) { try { next.goal = JSON.parse(savedGoal) as PersonalGoal; } catch {} }
    if (savedApps) { try { next.selectedApps = normalizeTrackedApps(JSON.parse(savedApps)); } catch {} }
    if (savedSchedule) { try { const parsed = JSON.parse(savedSchedule) as Partial<ScheduleSettings>; next.schedule = {id: parsed.id === 'weekdays' || parsed.id === 'busy_week' || parsed.id === 'exam_week' ? parsed.id : 'exam_week', ...normalizeSchedule(parsed)}; } catch {} }
    if (savedReminders) { try { const parsed = JSON.parse(savedReminders) as ReminderRecord[]; next.reminders = parsed.filter(item => item && typeof item.id === 'string' && item.timestamp); } catch {} }
    return next;
  };
  const applyState = (state: StoredState) => { setGoal(state.goal); setSelectedApps(state.selectedApps); setSchedule(state.schedule); setReminders(state.reminders); };

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const [rawProfiles, signedIn] = await Promise.all([AsyncStorage.getItem(AUTH.profiles), AsyncStorage.getItem(AUTH.signedIn)]);
        const parsedProfiles = rawProfiles ? JSON.parse(rawProfiles) as Array<Partial<Profile> & {password?: string}> : [];
        // Never retain the old plaintext credential field. Legacy profiles must sign up again.
        const savedProfiles = parsedProfiles
          .filter(item => typeof item.email === 'string' && typeof item.name === 'string' && typeof item.passwordHash === 'string' && typeof item.passwordSalt === 'string')
          .map(item => ({name: item.name!, email: item.email!, passwordHash: item.passwordHash!, passwordSalt: item.passwordSalt!, passwordIterations: Number(item.passwordIterations) || 120000}));
        if (rawProfiles && savedProfiles.length !== parsedProfiles.length) await AsyncStorage.setItem(AUTH.profiles, JSON.stringify(savedProfiles));
        const signedProfile = signedIn && signedIn !== GUEST_SESSION
          ? savedProfiles.find(item => item.email.toLowerCase() === signedIn.toLowerCase())
          : undefined;
        if (!mounted) return;
        setProfiles(savedProfiles);
        if (signedProfile) { const accountIdentity = {kind: 'account' as const, email: signedProfile.email}; const savedState = await readState(accountIdentity); setIdentity(accountIdentity); setProfile(signedProfile); applyState(savedState); await restoreReminders(savedState.reminders, accountIdentity); setScreen(destinationForSignedInProfile(Boolean(savedState.goal))); }
        else if (signedIn === GUEST_SESSION) { const savedState = await readState(GUEST_SESSION); setIdentity(GUEST_SESSION); applyState(savedState); await restoreReminders(savedState.reminders, GUEST_SESSION); setScreen(sessionDestination(Boolean(savedState.goal))); }
        else applyState(await readState());
      } catch (error) { console.warn('Unable to load saved FINITE setup', error); }
      finally { if (mounted) setStorageLoaded(true); }
    })();
    return () => { mounted = false; };
  }, []);

  const navigate = (next: Screen, replace = false) => { setHistory(current => replace ? [] : [...current, screen]); setScreen(next); };
  const goBack = () => { setHistory(current => { const previous = current[current.length - 1]; if (!previous) return current; setScreen(previous); return current.slice(0, -1); }); };
  const dashboard = () => { setEditing(false); setHistory(dashboardHistory()); setScreen('review'); };
  const openEditor = (next: Screen) => { setEditing(true); navigate(next); };
  const saveValueForCurrentProfile = async (name: keyof typeof LEGACY, value: unknown) => {
    const key = identity ? sessionStorageKey(identity, name) : LEGACY[name];
    await AsyncStorage.setItem(key, JSON.stringify(value));
  };
  const persistApps = async (apps: string[]) => { const supported = normalizeTrackedApps(apps); setSelectedApps(supported); try { await saveValueForCurrentProfile('selectedApps', supported); } catch { Alert.alert('Could not save apps', 'Your app choices were not saved. Please try again.'); throw new Error('apps-save-failed'); } };
  const persistSchedule = async (next: ScheduleSettings) => { setSchedule(next); try { await saveValueForCurrentProfile('schedule', next); } catch { Alert.alert('Could not save schedule', 'Your schedule was not saved. Please try again.'); throw new Error('schedule-save-failed'); } };
  const persistGoal = async (next: PersonalGoal) => {
    try {
      await saveValueForCurrentProfile('goal', next);
      await FiniteNative.configure(next.dailyLimit, selectedApps, schedule.enabled ? schedule.days : [0, 1, 2, 3, 4, 5, 6], schedule.enabled ? schedule.startMinute : 0, schedule.enabled ? schedule.endMinute : 0);
      setGoal(next); dashboard();
    } catch (error) { Alert.alert('Could not save goal', 'FINITE could not apply this goal. Check your settings and try again.'); console.warn('Unable to save/configure FINITE goal', error); }
  };
  const persistSessionLimit = async (dailyLimit: number) => {
    if (!goal) return;
    const next = {...goal, dailyLimit};
    await saveValueForCurrentProfile('goal', next);
    await FiniteNative.configure(next.dailyLimit, selectedApps, schedule.enabled ? schedule.days : [0, 1, 2, 3, 4, 5, 6], schedule.enabled ? schedule.startMinute : 0, schedule.enabled ? schedule.endMinute : 0);
    setGoal(next);
  };
  const persistReminders = async (next: ReminderRecord[]) => { setReminders(next); try { await saveValueForCurrentProfile('reminders', next); } catch { Alert.alert('Could not save reminder', 'Your reminder change was not saved. Please try again.'); throw new Error('reminder-save-failed'); } };

  const restoreReminders = async (saved: ReminderRecord[], session?: SessionIdentity) => {
    const restored = restoreRecurringReminders(saved, Date.now());
    if (session && JSON.stringify(restored) !== JSON.stringify(saved)) {
      await AsyncStorage.setItem(sessionStorageKey(session, 'reminders'), JSON.stringify(restored));
      setReminders(restored);
    }
    await Promise.all(restored.map(item => FiniteNative.scheduleReminder(item.id, item.timestamp, item.videoCount, item.repeat).catch(error => console.warn('Unable to restore reminder', item.id, error))));
  };

  useEffect(() => { if (storageLoaded && goal) void FiniteNative.configure(goal.dailyLimit, selectedApps, schedule.enabled ? schedule.days : [0, 1, 2, 3, 4, 5, 6], schedule.enabled ? schedule.startMinute : 0, schedule.enabled ? schedule.endMinute : 0).catch(error => console.warn('Unable to sync FINITE native policy', error)); }, [storageLoaded, goal, selectedApps, schedule]);
  useEffect(() => { if (!storageLoaded || history.length === 0) return; const subscription = BackHandler.addEventListener('hardwareBackPress', () => { goBack(); return true; }); return () => subscription.remove(); }, [history.length, storageLoaded]);

  const login = async (email: string, password: string) => {
    const found = profiles.find(item => item.email.toLowerCase() === email.trim().toLowerCase());
    if (!found || !await FiniteNative.verifyPassword(password, found.passwordSalt, found.passwordHash, found.passwordIterations)) { Alert.alert('Could not log in', 'This device has no matching local account. Create an account here first.'); return; }
    const accountIdentity = {kind: 'account' as const, email: found.email};
    const saved = await readState(accountIdentity); setIdentity(accountIdentity); setProfile(found); await AsyncStorage.setItem(AUTH.signedIn, found.email); applyState(saved); await restoreReminders(saved.reminders, accountIdentity); setHistory([]); setScreen(destinationForSignedInProfile(Boolean(saved.goal)));
  };
  const skip = async () => {
    const guestState = await readState(GUEST_SESSION);
    const hasGuestData = await AsyncStorage.getItem(sessionStorageKey(GUEST_SESSION, 'selectedApps'));
    const saved = hasGuestData ? guestState : await readState();
    if (!hasGuestData && (saved.goal || saved.reminders.length > 0)) {
      await Promise.all(Object.entries({goal: saved.goal, selectedApps: saved.selectedApps, schedule: saved.schedule, reminders: saved.reminders})
        .filter(([, value]) => value !== undefined)
        .map(([key, value]) => AsyncStorage.setItem(sessionStorageKey(GUEST_SESSION, key), JSON.stringify(value))));
    }
    setIdentity(GUEST_SESSION); setProfile(undefined); await AsyncStorage.setItem(AUTH.signedIn, GUEST_SESSION); applyState(saved); await restoreReminders(saved.reminders, GUEST_SESSION); setHistory([]); setScreen(sessionDestination(Boolean(saved.goal)));
  };
  const signUp = async (name: string, email: string, password: string) => {
    if (profiles.some(item => item.email.toLowerCase() === email.trim().toLowerCase())) { Alert.alert('Account already exists', 'Log in with this local profile instead.'); return; }
    try {
      const credential = await FiniteNative.hashPassword(password);
      const nextProfile = {name: name.trim(), email: email.trim(), ...{passwordSalt: credential.salt, passwordHash: credential.hash, passwordIterations: credential.iterations}};
      const accountIdentity = {kind: 'account' as const, email: nextProfile.email};
      const nextProfiles = [...profiles, nextProfile]; setProfiles(nextProfiles); setProfile(nextProfile);
      await AsyncStorage.setItem(AUTH.profiles, JSON.stringify(nextProfiles)); await AsyncStorage.setItem(AUTH.signedIn, nextProfile.email);
      const [savedGuestGoal, savedGuestApps] = await Promise.all([
        AsyncStorage.getItem(sessionStorageKey(GUEST_SESSION, 'goal')),
        AsyncStorage.getItem(sessionStorageKey(GUEST_SESSION, 'selectedApps')),
      ]);
      const sourceIdentity = identity === GUEST_SESSION || savedGuestGoal || savedGuestApps ? GUEST_SESSION : undefined;
      const sourceState = await readState(sourceIdentity);
      const migrated = await AsyncStorage.getItem(AUTH.legacyMigrated);
      const state = identity === GUEST_SESSION || !migrated ? sourceState : defaultState();
      for (const [key, value] of Object.entries({goal: state.goal, selectedApps: state.selectedApps, schedule: state.schedule, reminders: state.reminders})) if (value !== undefined) await AsyncStorage.setItem(sessionStorageKey(accountIdentity, key), JSON.stringify(value));
      if (identity !== GUEST_SESSION && !migrated) { await AsyncStorage.multiRemove(Object.values(LEGACY)); await AsyncStorage.setItem(AUTH.legacyMigrated, '1'); }
      setIdentity(accountIdentity); applyState(state); setHistory([]); setScreen(destinationForSignedInProfile(Boolean(state.goal)));
    } catch (error) { Alert.alert('Could not create local profile', 'Your profile could not be saved. Please try again.'); console.warn('Unable to create local profile', error); }
  };
  const clearLogout = async () => { try { await FiniteNative.endSession(); await FiniteNative.cancelAllReminders(); } catch (error) { Alert.alert('Could not finish logout', 'Your local profile was cleared from this screen, but native cleanup failed. Reopen FINITE and try again.'); console.warn('Unable to clean up native session', error); } finally { try { await AsyncStorage.removeItem(AUTH.signedIn); } catch (error) { console.warn('Unable to clear local sign-in state', error); } setIdentity(undefined); setProfile(undefined); setGoal(undefined); setReminders([]); setHistory([]); setScreen('login'); } };

  if (!storageLoaded) return <LinearGradient colors={finiteGradientColors} style={{flex: 1}} />;
  let content: React.ReactNode;
  if (screen === 'login') content = <LoginScreen onLogin={login} onSkip={() => { void skip(); }} onCreateAccount={() => navigate('signup')} onForgotPassword={() => Alert.alert('Local account', 'FINITE accounts are stored only on this device.')} onAppleSignIn={() => Alert.alert('Local account', 'Social sign-in is not connected. Use your local FINITE profile.')} onGoogleSignIn={() => Alert.alert('Local account', 'Social sign-in is not connected. Use your local FINITE profile.')} />;
  else if (screen === 'signup') content = <SignUpScreen onSignUp={signUp} onSignIn={goBack} />;
  else if (screen === 'choose-apps') content = <ChooseApp initialSelectedApps={selectedApps} onContinue={async apps => { try { await persistApps(apps); const next = destinationAfterAppsSaved(editing); if (next === 'review') dashboard(); else navigate(next); } catch {} }} />;
  else if (screen === 'choose-schedule') content = <ChooseSchedule initialSchedule={schedule.id} initialSettings={schedule} onBack={goBack} onContinue={async next => { try { await persistSchedule(next); if (editing) dashboard(); else navigate('personalize'); } catch {} }} />;
  else if (screen === 'personalize') content = <PersonalizeGoal initialGoal={goal} scheduleId={schedule.id} scheduleLabel={schedule.id === 'weekdays' ? 'Weekdays' : schedule.id === 'busy_week' ? 'Busy week' : 'Exam week'} onBack={goBack} onSave={persistGoal} />;
  else if (screen === 'reminder') content = <Reminder editingReminder={reminders.find(item => item.id === editingReminderId)} savedReminders={reminders} onEdit={id => setEditingReminderId(id)} onAddNew={() => setEditingReminderId(undefined)} onBack={goBack} onClose={goBack} onDelete={async id => { try { await FiniteNative.cancelReminder(id); await persistReminders(reminders.filter(item => item.id !== id)); if (editingReminderId === id) setEditingReminderId(undefined); } catch (error) { console.warn('Unable to delete reminder', error); } }} onSave={async settings => { try { const id = settings.id ?? `reminder-${Date.now()}`; await FiniteNative.scheduleReminder(id, settings.timestamp, settings.videoCount, settings.repeat); await persistReminders([...reminders.filter(item => item.id !== id), {...settings, id}]); Alert.alert('Reminder saved', 'FINITE will remind you on this device.', [{text: 'Done', onPress: dashboard}]); } catch (error) { if ((error as {code?: string}).code === 'INVALID_REMINDER_TIME') Alert.alert('Choose a future time', 'This reminder time has already passed. Pick a later date or time.'); else Alert.alert('Notifications are off', 'Allow FINITE notifications in Android settings, then save this reminder again.', [{text: 'Allow', onPress: () => { void FiniteNative.requestNotificationPermission(); }}, {text: 'Cancel', style: 'cancel'}]); } }} />;
  else if (screen === 'home') content = <FiniteScreen initialLimit={goal?.dailyLimit} onLimitSaved={persistSessionLimit} scheduleStatus={!schedule.enabled ? 'Schedule off · all day' : isScheduleActive(new Date(), schedule) ? 'Active window' : 'Outside active window'} onBack={goBack} />;
  else content = <UsageReview selectedApps={selectedApps} dailyLimit={goal?.dailyLimit ?? 7} schedule={schedule} onBack={() => BackHandler.exitApp()} onStartScrolling={() => navigate('home')} onAddReminder={() => { setEditingReminderId(undefined); openEditor('reminder'); }} onEditApps={() => openEditor('choose-apps')} onEditSchedule={() => openEditor('choose-schedule')} onEditGoal={() => openEditor('personalize')} onLogout={clearLogout} />;
  return <SafeAreaProvider>{content}</SafeAreaProvider>;
}
