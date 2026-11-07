import React, { useState } from 'react';
import LoginScreen from './src/screens/login';
import FiniteScreen from './src/screens/FiniteScreen';

export default function App(): React.JSX.Element {
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  if (!isLoggedIn) {
    return <LoginScreen onLogin={() => setIsLoggedIn(true)} />;
  }

  return <FiniteScreen />;
}
