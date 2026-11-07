import React, { useState } from 'react';
import LoginScreen from './screens/login';
import FiniteScreen from './screens/FiniteScreen';
import SignUpScreen from './screens/SignUpScreen';

type Screen = 'login' | 'signup' | 'home';

export default function App(): React.JSX.Element {
  const [screen, setScreen] = useState<Screen>('login');

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
        onSignUp={() => setScreen('home')}
        onSignIn={() => setScreen('login')}
      />
    );
  }

  return <FiniteScreen />;
}
