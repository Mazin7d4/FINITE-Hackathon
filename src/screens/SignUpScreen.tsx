import React, {useRef, useState} from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import {LinearGradient} from 'expo-linear-gradient';
import {SafeAreaView} from 'react-native-safe-area-context';
import {finiteColors, finiteGradientColors} from './theme';

type InputRef = React.ComponentRef<typeof TextInput>;

type Props = {
  onSignUp: (name: string, email: string, password: string) => void;
  onSignIn: () => void;
};

export default function SignUpScreen({onSignUp, onSignIn}: Props) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmation, setShowConfirmation] = useState(false);
  const emailRef = useRef<InputRef | null>(null);
  const passwordRef = useRef<InputRef | null>(null);
  const confirmationRef = useRef<InputRef | null>(null);
  type Field = 'name' | 'email' | 'password' | 'confirmPassword';
  const [errors, setErrors] = useState<Partial<Record<Field, string>>>({});

  function validate(values: Record<Field, string>) {
    const next: Partial<Record<Field, string>> = {};
    if (!values.name.trim()) {
      next.name = 'Enter your name.';
    }
    if (!values.email.trim()) {
      next.email = 'Enter your email address.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim())) {
      next.email = 'Enter a valid email address.';
    }
    if (!values.password) {
      next.password = 'Enter a password.';
    } else if (values.password.length < 8) {
      next.password = 'Use at least 8 characters.';
    }
    if (!values.confirmPassword) {
      next.confirmPassword = 'Confirm your password.';
    } else if (values.password !== values.confirmPassword) {
      next.confirmPassword = 'Passwords do not match.';
    }
    return next;
  }

  function updateField(field: Field, value: string) {
    const setters = {
      name: setName,
      email: setEmail,
      password: setPassword,
      confirmPassword: setConfirmPassword,
    };
    setters[field](value);
    const currentErrors = validate({
      name,
      email,
      password,
      confirmPassword,
      [field]: value,
    });
    // Recheck errors already shown, including confirmation when the password changes.
    setErrors(previous => {
      const next = {...previous};
      for (const key of Object.keys(previous) as Field[]) {
        next[key] = currentErrors[key];
      }
      return next;
    });
  }

  function fieldError(field: Field) {
    return errors[field] ? (
      <Text
        style={styles.error}
        accessibilityRole="alert"
        accessibilityLiveRegion="polite">
        {errors[field]}
      </Text>
    ) : null;
  }

  function handleSignUp() {
    const next = validate({name, email, password, confirmPassword});
    setErrors(next);
    if (Object.keys(next).length) {
      return;
    }
    onSignUp(name.trim(), email.trim(), password);
  }

  return (
    <LinearGradient
      colors={finiteGradientColors}
      locations={[0, 0.55, 1]}
      start={{x: 0, y: 0}}
      end={{x: 0, y: 1}}
      style={styles.background}>
      <StatusBar barStyle="dark-content" />
      <SafeAreaView style={styles.background} edges={['top', 'bottom']}>
        <KeyboardAvoidingView
          style={styles.background}
          behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
          <ScrollView
            contentContainerStyle={styles.page}
            keyboardShouldPersistTaps="handled">
            <View style={styles.header}>
              <View style={styles.brand}>
                <Text style={styles.clockMark}>◷</Text>
                <Text style={styles.brandName}>finite</Text>
              </View>
              <View style={styles.headerCircle} />
            </View>
            <Text style={styles.title}>Create an account.</Text>
            <View style={styles.form}>
              <View style={styles.field}>
                <Text style={styles.label}>Name</Text>
                <TextInput
                  style={styles.input}
                  placeholderTextColor={finiteColors.secondaryText}
                  accessibilityLabel="Name"
                  placeholder="Your name"
                  returnKeyType="next"
                  submitBehavior="submit"
                  onSubmitEditing={() => emailRef.current?.focus()}
                  value={name}
                  onChangeText={value => updateField('name', value)}
                  autoComplete="name"
                  autoCapitalize="words"
                />
                {fieldError('name')}
              </View>

              <View style={styles.field}>
                <Text style={styles.label}>Email</Text>
                <TextInput
                  style={styles.input}
                  placeholderTextColor={finiteColors.secondaryText}
                  ref={emailRef}
                  accessibilityLabel="Email"
                  placeholder="you@example.com"
                  returnKeyType="next"
                  submitBehavior="submit"
                  onSubmitEditing={() => passwordRef.current?.focus()}
                  value={email}
                  onChangeText={value => updateField('email', value)}
                  keyboardType="email-address"
                  autoCapitalize="none"
                  autoCorrect={false}
                  autoComplete="email"
                />
                {fieldError('email')}
              </View>

              <View style={styles.field}>
                <Text style={styles.label}>Password</Text>
                <View style={styles.passwordBox}>
                  <TextInput
                    style={[styles.input, styles.passwordInput]}
                    placeholderTextColor={finiteColors.secondaryText}
                    ref={passwordRef}
                    accessibilityLabel="Password"
                    placeholder="Create a password"
                    accessibilityHint="At least 8 characters"
                    returnKeyType="next"
                    submitBehavior="submit"
                    onSubmitEditing={() => confirmationRef.current?.focus()}
                    value={password}
                    onChangeText={value => updateField('password', value)}
                    secureTextEntry={!showPassword}
                    autoCapitalize="none"
                    autoCorrect={false}
                    autoComplete="new-password"
                  />
                  <Pressable
                    accessibilityRole="button"
                    accessibilityLabel={
                      showPassword ? 'Hide password' : 'Show password'
                    }
                    onPress={() => setShowPassword(visible => !visible)}
                    style={styles.visibilityButton}>
                    <Text style={styles.visibilityText}>
                      {showPassword ? 'Hide' : 'Show'}
                    </Text>
                  </Pressable>
                </View>
                <Text style={styles.guidance}>At least 8 characters</Text>
                {fieldError('password')}
              </View>

              <View style={styles.field}>
                <Text style={styles.label}>Confirm password</Text>
                <View style={styles.passwordBox}>
                  <TextInput
                    style={[styles.input, styles.passwordInput]}
                    placeholderTextColor={finiteColors.secondaryText}
                    ref={confirmationRef}
                    accessibilityLabel="Confirm password"
                    placeholder="Re-enter your password"
                    value={confirmPassword}
                    onChangeText={value => updateField('confirmPassword', value)}
                    secureTextEntry={!showConfirmation}
                    autoCapitalize="none"
                    autoCorrect={false}
                    returnKeyType="done"
                    onSubmitEditing={handleSignUp}
                  />
                  <Pressable
                    accessibilityRole="button"
                    accessibilityLabel={
                      showConfirmation
                        ? 'Hide confirm password'
                        : 'Show confirm password'
                    }
                    onPress={() => setShowConfirmation(visible => !visible)}
                    style={styles.visibilityButton}>
                    <Text style={styles.visibilityText}>
                      {showConfirmation ? 'Hide' : 'Show'}
                    </Text>
                  </Pressable>
                </View>
                {fieldError('confirmPassword')}
              </View>

              <Pressable
                accessibilityRole="button"
                onPress={handleSignUp}
                style={({pressed}) => [
                  styles.primaryButton,
                  pressed && styles.pressed,
                ]}>
                <Text style={styles.primaryText}>Create account</Text>
                <Text style={styles.arrow}>→</Text>
              </Pressable>
              <Text style={styles.timeNote}>
                Your time is yours. Let’s keep it that way.
              </Text>
            </View>
            <View style={styles.accountRow}>
              <Text style={styles.accountPrompt}>
                Already have an account?{' '}
              </Text>
              <Pressable
                accessibilityRole="button"
                onPress={onSignIn}
                hitSlop={12}
                style={({pressed}) => pressed && styles.pressed}>
                <Text style={styles.signIn}>Sign in</Text>
              </Pressable>
            </View>
          </ScrollView>
        </KeyboardAvoidingView>
      </SafeAreaView>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  background: {flex: 1},
  page: {
    flexGrow: 1,
    width: '100%',
    maxWidth: 430,
    alignSelf: 'center',
    paddingHorizontal: 20,
    paddingTop: 12,
    paddingBottom: 24,
  },
  header: {
    height: 48,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  brand: {flexDirection: 'row', alignItems: 'center'},
  clockMark: {color: finiteColors.text, fontSize: 25, marginRight: 5},
  brandName: {color: finiteColors.text, fontSize: 20},
  headerCircle: {
    width: 48,
    height: 48,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.12)',
  },
  title: {
    color: finiteColors.text,
    fontSize: 25,
    fontWeight: '600',
    marginTop: 38,
  },
  form: {marginTop: 25},
  label: {color: '#344957', fontSize: 14, marginBottom: 6},
  input: {
    minHeight: 48,
    backgroundColor: finiteColors.surface,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    color: finiteColors.text,
    fontSize: 16,
  },
  field: {marginBottom: 18},
  passwordBox: {position: 'relative'},
  passwordInput: {paddingRight: 76},
  visibilityButton: {
    position: 'absolute',
    right: 0,
    top: 0,
    bottom: 0,
    minWidth: 68,
    alignItems: 'center',
    justifyContent: 'center',
  },
  visibilityText: {color: finiteColors.text, fontSize: 14, fontWeight: '600'},
  guidance: {color: finiteColors.text, fontSize: 13, marginTop: 6},
  error: {color: '#842D31', fontSize: 13, lineHeight: 18, marginTop: 6},
  primaryButton: {
    minHeight: 48,
    backgroundColor: finiteColors.primary,
    borderRadius: 11,
    paddingHorizontal: 14,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  primaryText: {color: finiteColors.surface, fontSize: 16, fontWeight: '600'},
  arrow: {color: finiteColors.surface, fontSize: 20},
  timeNote: {
    color: finiteColors.secondaryText,
    fontSize: 13,
    textAlign: 'center',
    marginTop: 20,
  },
  accountRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 38,
  },
  accountPrompt: {color: finiteColors.secondaryText, fontSize: 14},
  signIn: {color: finiteColors.text, fontSize: 14, fontWeight: '600'},
  pressed: {opacity: 0.8},
});
