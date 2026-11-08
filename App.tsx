import React, { useState } from 'react';
import LoginScreen from './src/screens/login';
import FiniteScreen from './src/screens/FiniteScreen';
import SignUpScreen from './src/screens/SignUpScreen';

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
