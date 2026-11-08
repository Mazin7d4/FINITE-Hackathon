import React from 'react';
import LoginScreen from './login';

type Props = React.ComponentProps<typeof LoginScreen>;

export default function SignInScreen(props: Props): React.JSX.Element {
  return <LoginScreen {...props} />;
}
