import React, { useEffect, useState } from 'react';
import { Alert, BackHandler } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { LinearGradient } from 'expo-linear-gradient';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import LoginScreen from './src/screens/login';
import FiniteScreen from './src/screens/FiniteScreen';
import SignUpScreen from './src/screens/SignUpScreen';
import ChooseApp from './src/screens/ChooseApp';
import ChooseSchedule from './src/screens/ChooseSchedule';
import type {ScheduleSettings} from './src/screens/ChooseSchedule';
import PersonalizeGoal from './src/screens/PersonalizeGoal';
import type {PersonalGoal} from './src/screens/PersonalizeGoal';
import SaveGoal from './src/screens/SaveGoal';
import UsageReview from './src/screens/UsageReview';
import Reminder, {type ReminderRecord} from './src/screens/Reminder';
import { finiteGradientColors } from './src/screens/theme';
import {FiniteNative} from './src/native/finite';
import {DEFAULT_SCHEDULE, normalizeSchedule, normalizeTrackedApps} from './src/domain/finitePolicy';

type Screen =
  | 'login'
  | 'signup'
  | 'choose-apps'
  | 'choose-schedule'
  | 'personalize'
  | 'saved'
  | 'review'
  | 'reminder'
  | 'home';

const STORAGE_KEYS = {
  goal: '@finite/personal-goal',
  selectedApps: '@finite/selected-apps',
  schedule: '@finite/schedule',
  reminders: '@finite/reminders',
} as const;

export default function App(): React.JSX.Element {
  const [screen, setScreen] = useState<Screen>('login');
  const [history, setHistory] = useState<Screen[]>([]);
  const [selectedApps, setSelectedApps] = useState<string[]>(['instagram', 'youtube']);
  const [goal, setGoal] = useState<PersonalGoal | undefined>();
  const [schedule, setSchedule] = useState<ScheduleSettings>({id: 'exam_week', ...DEFAULT_SCHEDULE});
  const [storageLoaded, setStorageLoaded] = useState(false);
  const [reminders, setReminders] = useState<ReminderRecord[]>([]);
  const [editingReminderId, setEditingReminderId] = useState<string | undefined>();

  useEffect(() => {
    let mounted = true;

    const loadSavedState = async () => {
      try {
        const [savedGoal, savedApps, savedSchedule, savedReminders] = await Promise.all([
          AsyncStorage.getItem(STORAGE_KEYS.goal),
          AsyncStorage.getItem(STORAGE_KEYS.selectedApps),
          AsyncStorage.getItem(STORAGE_KEYS.schedule),
          AsyncStorage.getItem(STORAGE_KEYS.reminders),
        ]);

        if (!mounted) return;
        if (savedGoal) setGoal(JSON.parse(savedGoal) as PersonalGoal);
        if (savedApps) setSelectedApps(normalizeTrackedApps(JSON.parse(savedApps)));
        if (savedSchedule) {
          const parsed = JSON.parse(savedSchedule) as Partial<ScheduleSettings>;
          const normalized = normalizeSchedule(parsed);
          setSchedule({id: parsed.id === 'weekdays' || parsed.id === 'busy_week' || parsed.id === 'exam_week' ? parsed.id : 'exam_week', ...normalized});
        }
        if (savedReminders) {
          const parsed = JSON.parse(savedReminders) as ReminderRecord[];
          setReminders(parsed.filter(item => item && typeof item.id === 'string' && item.timestamp));
        }
      } catch (error) {
        console.warn('Unable to load saved FINITE setup', error);
      } finally {
        if (mounted) setStorageLoaded(true);
      }
    };

    void loadSavedState();
    return () => {
      mounted = false;
    };
  }, []);

  const persistSelectedApps = (apps: string[]) => {
    const supported = normalizeTrackedApps(apps);
    setSelectedApps(supported);
    void AsyncStorage.setItem(STORAGE_KEYS.selectedApps, JSON.stringify(supported)).catch(error => {
      console.warn('Unable to save FINITE app selection', error);
    });
  };

  const persistReminders = (next: ReminderRecord[]) => {
    setReminders(next);
    void AsyncStorage.setItem(STORAGE_KEYS.reminders, JSON.stringify(next)).catch(error => console.warn('Unable to save FINITE reminders', error));
  };

  const navigate = (nextScreen: Screen) => {
    setHistory(current => [...current, screen]);
    setScreen(nextScreen);
  };

  const goBack = () => {
    setHistory(current => {
      const previous = current[current.length - 1];
      if (!previous) return current;
      setScreen(previous);
      return current.slice(0, -1);
    });
  };

  const persistGoal = (nextGoal: PersonalGoal) => {
    setGoal(nextGoal);
    void FiniteNative.configure(nextGoal.dailyLimit, selectedApps, schedule.days, schedule.startMinute, schedule.endMinute).catch(error => {
      console.warn('Unable to sync FINITE native policy', error);
    });
    void AsyncStorage.setItem(STORAGE_KEYS.goal, JSON.stringify(nextGoal)).catch(error => {
      console.warn('Unable to save FINITE goal', error);
    });
    navigate('saved');
  };

  useEffect(() => {
    if (!storageLoaded || !goal) return;
    void FiniteNative.configure(goal.dailyLimit, selectedApps, schedule.days, schedule.startMinute, schedule.endMinute).catch(error => {
      console.warn('Unable to restore FINITE native policy', error);
    });
  }, [storageLoaded, goal, selectedApps, schedule]);

  useEffect(() => {
    if (!storageLoaded || history.length === 0) return;
    const subscription = BackHandler.addEventListener('hardwareBackPress', () => {
      goBack();
      return true;
    });
    return () => subscription.remove();
  }, [history.length, storageLoaded]);

  const showAuthNotice = () => Alert.alert('FINITE', 'Authentication is not connected yet. You can continue setting up your goal.');

  if (!storageLoaded) {
    return <LinearGradient colors={finiteGradientColors} style={{ flex: 1 }} />;
  }

  const content = (() => {
    if (screen === 'login') {
      return (
      <LoginScreen
        onLogin={() => navigate(goal ? 'review' : 'choose-apps')}
        onCreateAccount={() => navigate('signup')}
        onForgotPassword={showAuthNotice}
        onAppleSignIn={showAuthNotice}
        onGoogleSignIn={showAuthNotice}
      />
      );
    }

    if (screen === 'signup') {
      return (
      <SignUpScreen
        onSignUp={() => navigate('choose-apps')}
        onSignIn={goBack}
      />
      );
    }

    if (screen === 'choose-apps') {
      return <ChooseApp initialSelectedApps={selectedApps} onContinue={apps => { persistSelectedApps(apps); navigate('choose-schedule'); }} />;
    }

    if (screen === 'choose-schedule') {
      return <ChooseSchedule initialSchedule={schedule.id} initialSettings={schedule} onBack={goBack} onContinue={nextSchedule => {
        setSchedule(nextSchedule);
        void AsyncStorage.setItem(STORAGE_KEYS.schedule, JSON.stringify(nextSchedule)).catch(error => {
          console.warn('Unable to save FINITE schedule', error);
        });
        navigate('personalize');
      }} />;
    }

    if (screen === 'personalize') {
      return (
      <PersonalizeGoal
        initialGoal={goal}
        scheduleId={schedule.id}
        scheduleLabel={schedule.id === 'weekdays' ? 'Weekdays' : schedule.id === 'busy_week' ? 'Busy week' : 'Exam week'}
        onBack={goBack}
        onSave={persistGoal}
      />
      );
    }

    if (screen === 'saved') {
      return (
      <SaveGoal
        goal={goal}
        scheduleLabel={schedule.id === 'weekdays' ? 'Weekdays' : schedule.id === 'busy_week' ? 'Busy week' : 'Exam week'}
        onBack={goBack}
        onEdit={goBack}
        onDone={() => navigate('review')}
      />
      );
    }

    if (screen === 'review') {
      return (
      <UsageReview
        selectedApps={selectedApps}
        dailyLimit={goal?.dailyLimit ?? 7}
        onBack={goBack}
        onStartScrolling={() => navigate('home')}
        onAddReminder={() => { setEditingReminderId(undefined); navigate('reminder'); }}
        onEditApps={() => navigate('choose-apps')}
        onEditSchedule={() => navigate('choose-schedule')}
      />
      );
    }

    if (screen === 'reminder') {
      return <Reminder editingReminder={reminders.find(item => item.id === editingReminderId)} savedReminders={reminders} onEdit={id => setEditingReminderId(id)} onAddNew={() => setEditingReminderId(undefined)} onBack={goBack} onClose={goBack} onDelete={async id => {
        await FiniteNative.cancelReminder(id);
        persistReminders(reminders.filter(item => item.id !== id));
        if (editingReminderId === id) setEditingReminderId(undefined);
      }} onSave={async settings => {
        try {
          const id = settings.id ?? `reminder-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
          await FiniteNative.scheduleReminder(id, settings.timestamp, settings.videoCount, settings.repeat);
          const next = {...settings, id};
          persistReminders([...reminders.filter(item => item.id !== id), next]);
          Alert.alert('Reminder saved', 'FINITE will remind you on this device.', [{text: 'Done', onPress: goBack}]);
        } catch (_error) {
          if ((_error as {code?: string}).code === 'INVALID_REMINDER_TIME') {
            Alert.alert('Choose a future time', 'This reminder time has already passed. Pick a later date or time.');
            return;
          }
          Alert.alert('Notifications are off', 'Allow FINITE notifications in Android settings, then save this reminder again.', [
            {text: 'Allow', onPress: () => { void FiniteNative.requestNotificationPermission(); }},
            {text: 'Cancel', style: 'cancel'},
          ]);
        }
      }} />;
    }

    return <FiniteScreen initialLimit={goal?.dailyLimit} onBack={goBack} />;
  })();

  return <SafeAreaProvider>{content}</SafeAreaProvider>;
}
