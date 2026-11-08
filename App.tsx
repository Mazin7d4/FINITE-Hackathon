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
import PersonalizeGoal from './src/screens/PersonalizeGoal';
import type {PersonalGoal} from './src/screens/PersonalizeGoal';
import SaveGoal from './src/screens/SaveGoal';
import UsageReview from './src/screens/UsageReview';
import Reminder from './src/screens/Reminder';
import { finiteGradientColors } from './src/screens/theme';

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
} as const;

type ScheduleId = 'exam_week' | 'weekdays' | 'busy_week';

export default function App(): React.JSX.Element {
  const [screen, setScreen] = useState<Screen>('login');
  const [history, setHistory] = useState<Screen[]>([]);
  const [selectedApps, setSelectedApps] = useState<string[]>(['instagram', 'youtube']);
  const [goal, setGoal] = useState<PersonalGoal | undefined>();
  const [schedule, setSchedule] = useState<ScheduleId>('exam_week');
  const [storageLoaded, setStorageLoaded] = useState(false);

  useEffect(() => {
    let mounted = true;

    const loadSavedState = async () => {
      try {
        const [savedGoal, savedApps, savedSchedule] = await Promise.all([
          AsyncStorage.getItem(STORAGE_KEYS.goal),
          AsyncStorage.getItem(STORAGE_KEYS.selectedApps),
          AsyncStorage.getItem(STORAGE_KEYS.schedule),
        ]);

        if (!mounted) return;
        if (savedGoal) setGoal(JSON.parse(savedGoal) as PersonalGoal);
        if (savedApps) setSelectedApps(JSON.parse(savedApps) as string[]);
        if (savedSchedule) setSchedule(JSON.parse(savedSchedule) as ScheduleId);
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
    setSelectedApps(apps);
    void AsyncStorage.setItem(STORAGE_KEYS.selectedApps, JSON.stringify(apps)).catch(error => {
      console.warn('Unable to save FINITE app selection', error);
    });
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
    void AsyncStorage.setItem(STORAGE_KEYS.goal, JSON.stringify(nextGoal)).catch(error => {
      console.warn('Unable to save FINITE goal', error);
    });
    navigate('saved');
  };

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
      return <ChooseSchedule initialSchedule={schedule} onBack={goBack} onContinue={nextSchedule => {
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
        scheduleId={schedule}
        scheduleLabel={schedule === 'weekdays' ? 'Weekdays' : schedule === 'busy_week' ? 'Busy week' : 'Exam week'}
        onBack={goBack}
        onSave={persistGoal}
      />
      );
    }

    if (screen === 'saved') {
      return (
      <SaveGoal
        goal={goal}
        scheduleLabel={schedule === 'weekdays' ? 'Weekdays' : schedule === 'busy_week' ? 'Busy week' : 'Exam week'}
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
        onAddReminder={() => navigate('reminder')}
      />
      );
    }

    if (screen === 'reminder') {
      return <Reminder onBack={goBack} onClose={goBack} onSave={goBack} />;
    }

    return <FiniteScreen initialLimit={goal?.dailyLimit} onBack={goBack} />;
  })();

  return <SafeAreaProvider>{content}</SafeAreaProvider>;
}
