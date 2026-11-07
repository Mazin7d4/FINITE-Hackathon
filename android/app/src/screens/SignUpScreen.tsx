import React, {useState} from 'react';
import {Button, ScrollView, Text, TextInput} from 'react-native';

type Props = {
  onSignUp: (name: string, email: string, password: string) => void;
  onSignIn: () => void;
};

export default function SignUpScreen({onSignUp, onSignIn}: Props) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');

  function handleSignUp() {
    const trimmedName = name.trim();
    const trimmedEmail = email.trim();

    if (!trimmedName || !trimmedEmail || !password || !confirmPassword) {
      setError('Please fill in all fields.');
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail)) {
      setError('Enter a valid email address.');
      return;
    }

    if (password.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setError('');
    onSignUp(trimmedName, trimmedEmail, password);
  }

  return (
    <ScrollView keyboardShouldPersistTaps="handled">
      <Text>Create an account</Text>

      <Text>Name</Text>
      <TextInput
        accessibilityLabel="Name"
        placeholder="Your name"
        value={name}
        onChangeText={setName}
        autoComplete="name"
        autoCapitalize="words"
      />

      <Text>Email</Text>
      <TextInput
        accessibilityLabel="Email"
        placeholder="you@example.com"
        value={email}
        onChangeText={setEmail}
        keyboardType="email-address"
        autoCapitalize="none"
        autoCorrect={false}
        autoComplete="email"
      />

      <Text>Password</Text>
      <TextInput
        accessibilityLabel="Password"
        placeholder="At least 8 characters"
        value={password}
        onChangeText={setPassword}
        secureTextEntry
        autoCapitalize="none"
        autoCorrect={false}
        autoComplete="new-password"
      />

      <Text>Confirm password</Text>
      <TextInput
        accessibilityLabel="Confirm password"
        placeholder="Re-enter your password"
        value={confirmPassword}
        onChangeText={setConfirmPassword}
        secureTextEntry
        autoCapitalize="none"
        autoCorrect={false}
        returnKeyType="done"
        onSubmitEditing={handleSignUp}
      />

      {error ? (
        <Text accessibilityLiveRegion="polite">{error}</Text>
      ) : null}

      <Button title="Create account" onPress={handleSignUp} />

      <Text>Already have an account?</Text>
      <Button title="Sign in" onPress={onSignIn} />
    </ScrollView>
  );
}
