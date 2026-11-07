import React, {useState} from 'react';
import {registerRootComponent} from 'expo';
import {Text, View} from 'react-native';
import SignUpScreen from './android/app/src/screens/SignUpScreen';

function SignUpPreview() {
  const [message, setMessage] = useState('');

  return (
    <View>
      <SignUpScreen
        onSignUp={() =>
          setMessage('Form valid. Account creation is not connected yet.')
        }
        onSignIn={() => setMessage('The sign-in page is not connected yet.')}
      />
      {message ? <Text accessibilityLiveRegion="polite">{message}</Text> : null}
    </View>
  );
}

registerRootComponent(SignUpPreview);