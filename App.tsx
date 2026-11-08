import React, { useState } from 'react';
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

export default function App(): React.JSX.Element {
  const [screen, setScreen] = useState<Screen>('login');
  const [selectedApps, setSelectedApps] = useState<string[]>(['instagram', 'youtube']);
  const [goal, setGoal] = useState<PersonalGoal | undefined>();

  if (screen === 'login') {
    return (
      <LoginScreen
        onLogin={() => setScreen('home')}
        onCreateAccount={() => setScreen('signup')}
      />
    );
  }

  if (screen === 'signup') {
    return (
      <SignUpScreen
        onSignUp={() => setScreen('choose-apps')}
        onSignIn={() => setScreen('login')}
      />
    );
  }

  if (screen === 'choose-apps') {
    return <ChooseApp onContinue={apps => { setSelectedApps(apps); setScreen('choose-schedule'); }} />;
  }

  if (screen === 'choose-schedule') {
    return <ChooseSchedule onBack={() => setScreen('choose-apps')} onContinue={() => setScreen('personalize')} />;
  }

  if (screen === 'personalize') {
    return (
      <PersonalizeGoal
        onBack={() => setScreen('choose-schedule')}
        onSave={nextGoal => { setGoal(nextGoal); setScreen('saved'); }}
      />
    );
  }

  if (screen === 'saved') {
    return (
      <SaveGoal
        goal={goal}
        onBack={() => setScreen('personalize')}
        onEdit={() => setScreen('personalize')}
        onDone={() => setScreen('review')}
      />
    );
  }

  if (screen === 'review') {
    return (
      <UsageReview
        selectedApps={selectedApps}
        dailyLimit={goal?.dailyLimit ?? 7}
        onBack={() => setScreen('saved')}
        onStartScrolling={() => setScreen('home')}
        onAddReminder={() => setScreen('reminder')}
      />
    );
  }

  if (screen === 'reminder') {
    return <Reminder onBack={() => setScreen('review')} onClose={() => setScreen('review')} onSave={() => setScreen('review')} />;
  }

  return <FiniteScreen initialLimit={goal?.dailyLimit} />;
}
