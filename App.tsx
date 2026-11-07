import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {
  Alert,
  AppState,
  NativeEventEmitter,
  NativeModules,
  Pressable,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

type SessionState = {
  limit: number;
  count: number;
  active: boolean;
  limitAcknowledged: boolean;
  detectorStatus: string;
  accessibilityEnabled: boolean;
};

type FiniteNativeModule = {
  getState(): Promise<SessionState>;
  startSession(limit: number): Promise<SessionState>;
  endSession(): Promise<SessionState>;
  openAccessibilitySettings(): Promise<void>;
  addListener(eventName: string): void;
  removeListeners(count: number): void;
};

const FiniteNative = NativeModules.FiniteNative as FiniteNativeModule;
const stateEmitter = new NativeEventEmitter(NativeModules.FiniteNative);
const initialState: SessionState = {
  limit: 5,
  count: 0,
  active: false,
  limitAcknowledged: false,
  detectorStatus: 'Waiting for a session',
  accessibilityEnabled: false,
};

function FiniteApp(): React.JSX.Element {
  const [session, setSession] = useState(initialState);
  const [limitText, setLimitText] = useState('5');
  const limit = Number(limitText);
  const validLimit = Number.isInteger(limit) && limit >= 1 && limit <= 100;

  const refreshState = useCallback(async () => {
    try {
      const next = await FiniteNative.getState();
      setSession(next);
      if (!next.active) setLimitText(String(next.limit));
    } catch (error) {
      console.warn('Unable to read FINITE state', error);
    }
  }, []);

  useEffect(() => {
    refreshState();
    const stateSubscription = stateEmitter.addListener(
      'FiniteSessionChanged',
      (...args: readonly Object[]) => setSession(args[0] as SessionState),
    );
    const appSubscription = AppState.addEventListener('change', nextState => {
      if (nextState === 'active') refreshState();
    });
    return () => {
      stateSubscription.remove();
      appSubscription.remove();
    };
  }, [refreshState]);

  const sessionCaption = useMemo(() => {
    if (session.count === 0) return 'Open Reels or Shorts and start scrolling';
    if (session.count < session.limit) {
      return `${session.limit - session.count} left in your plan`;
    }
    if (session.limitAcknowledged) return 'Limit acknowledged · still counting';
    return 'You reached your plan';
  }, [session]);

  const openAccessibilitySettings = useCallback(async () => {
    try {
      await FiniteNative.openAccessibilitySettings();
    } catch (error) {
      Alert.alert(
        'Could not open Settings',
        'Open Android Settings, choose Accessibility, then enable FINITE Reels and Shorts Counter.',
      );
      console.warn('Unable to open accessibility settings', error);
    }
  }, []);

  const startSession = async () => {
    if (!validLimit) return;
    if (!session.accessibilityEnabled) {
      Alert.alert(
        'Enable accessibility first',
        'FINITE needs accessibility access to identify Reels and Shorts on-device.',
        [
          {text: 'Cancel', style: 'cancel'},
          {text: 'Open Settings', onPress: openAccessibilitySettings},
        ],
      );
      return;
    }
    try {
      setSession(await FiniteNative.startSession(limit));
    } catch (error) {
      Alert.alert('Could not start session', String(error));
    }
  };

  const endSession = async () => {
    try {
      setSession(await FiniteNative.endSession());
    } catch (error) {
      Alert.alert('Could not end session', String(error));
    }
  };

  return (
    <View style={styles.safeArea}>
      <StatusBar barStyle="light-content" />
      <View style={styles.glow} />
      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled">
        <View style={styles.brandRow}>
          <View style={styles.logo}><Text style={styles.logoText}>F</Text></View>
          <Text style={styles.brand}>FINITE</Text>
        </View>

        <Text style={styles.hero}>
          {session.active ? 'Your session is live.' : 'Scroll with an ending.'}
        </Text>
        <Text style={styles.intro}>
          {session.active
            ? 'Reels and Shorts share one counter. Switch between Instagram and YouTube anytime.'
            : 'Choose your combined Reels and Shorts limit. We’ll remind you when you arrive.'}
        </Text>

        {session.active ? (
          <View style={styles.card}>
            <Text style={styles.eyebrow}>REELS + SHORTS WATCHED</Text>
            <Text style={styles.counter}>{session.count}/{session.limit}</Text>
            <Text style={styles.caption}>{sessionCaption}</Text>
            <Pressable
              accessibilityRole="button"
              onPress={endSession}
              style={({pressed}) => [styles.endButton, pressed && styles.pressed]}>
              <Text style={styles.endButtonText}>End Session</Text>
            </Pressable>
          </View>
        ) : (
          <View style={styles.card}>
            <Text style={styles.eyebrow}>REELS + SHORTS LIMIT</Text>
            <View style={styles.inputRow}>
              <TextInput
                accessibilityLabel="Combined Reels and Shorts limit"
                value={limitText}
                onChangeText={value => {
                  if (/^\d{0,3}$/.test(value)) setLimitText(value);
                }}
                keyboardType="number-pad"
                maxLength={3}
                selectTextOnFocus
                style={styles.input}
                placeholder="5"
                placeholderTextColor="#606268"
              />
              <Text style={styles.inputSuffix}>Videos</Text>
            </View>
            {!validLimit && <Text style={styles.error}>Enter a number from 1 to 100</Text>}
            <Pressable
              accessibilityRole="button"
              disabled={!validLimit}
              onPress={startSession}
              style={({pressed}) => [
                styles.primaryButton,
                !validLimit && styles.disabledButton,
                pressed && styles.pressed,
              ]}>
              <Text style={styles.primaryButtonText}>Start Session</Text>
            </Pressable>
          </View>
        )}

        <View style={styles.permissionCard}>
          <View style={[
            styles.statusDot,
            session.accessibilityEnabled ? styles.readyDot : styles.warningDot,
          ]} />
          <View style={styles.permissionCopy}>
            <Text style={styles.permissionTitle}>
              {session.accessibilityEnabled ? 'Accessibility ready' : 'Setup required'}
            </Text>
            <Text style={styles.permissionSubtitle}>
              {session.accessibilityEnabled
                ? 'On-device Reels and Shorts detection is enabled'
                : 'Allow FINITE to identify Reels and Shorts'}
            </Text>
          </View>
          <Pressable
            accessibilityRole="button"
            onPress={openAccessibilitySettings}
            hitSlop={12}>
            <Text style={styles.settingsLink}>
              {session.accessibilityEnabled ? 'Settings' : 'Enable'}
            </Text>
          </Pressable>
        </View>

        <Text style={styles.statusLabel}>DETECTOR STATUS</Text>
        <Text style={styles.detectorStatus}>{session.detectorStatus}</Text>
        <Text style={styles.privacyCopy}>
          Reels and Shorts counting is best-effort and may miss or repeat a count.
          FINITE processes accessibility metadata on-device and stores no browsing content.
        </Text>
      </ScrollView>
    </View>
  );
}

export default function App(): React.JSX.Element {
  return <FiniteApp />;
}

const styles = StyleSheet.create({
  safeArea: {flex: 1, backgroundColor: '#090A0B'},
  glow: {position: 'absolute', top: -180, right: -180, width: 420, height: 420,
    borderRadius: 210, backgroundColor: '#16200A', opacity: 0.52},
  content: {paddingHorizontal: 22, paddingTop: 52, paddingBottom: 36},
  brandRow: {flexDirection: 'row', alignItems: 'center', marginBottom: 42},
  logo: {width: 38, height: 38, borderRadius: 19, alignItems: 'center',
    justifyContent: 'center', backgroundColor: '#D9FF57'},
  logoText: {color: '#090A0B', fontSize: 24, fontWeight: '900'},
  brand: {marginLeft: 12, color: '#FFFFFF', fontSize: 19, fontWeight: '900', letterSpacing: 3.4},
  hero: {color: '#FFFFFF', fontSize: 34, lineHeight: 39, fontWeight: '700', letterSpacing: -0.8},
  intro: {marginTop: 10, marginBottom: 30, color: '#A5A6A9', fontSize: 16, lineHeight: 23},
  card: {padding: 22, borderRadius: 24, borderWidth: 1, borderColor: '#34363A',
    backgroundColor: '#141517', alignItems: 'center'},
  eyebrow: {alignSelf: 'flex-start', color: '#D9FF57', fontSize: 12,
    fontWeight: '800', letterSpacing: 1.5},
  counter: {marginTop: 12, color: '#FFFFFF', fontSize: 64, fontWeight: '800', letterSpacing: -2},
  caption: {marginTop: 5, color: '#A5A6A9', fontSize: 14},
  inputRow: {width: '100%', height: 66, marginTop: 12, paddingHorizontal: 16,
    flexDirection: 'row', alignItems: 'center', borderRadius: 16, borderWidth: 1,
    borderColor: '#484A50', backgroundColor: '#1B1C1F'},
  input: {flex: 1, color: '#FFFFFF', fontSize: 30, fontWeight: '800'},
  inputSuffix: {color: '#A5A6A9', fontSize: 16},
  error: {alignSelf: 'flex-start', marginTop: 7, color: '#FF8A80'},
  primaryButton: {width: '100%', height: 56, marginTop: 18, borderRadius: 16,
    alignItems: 'center', justifyContent: 'center', backgroundColor: '#D9FF57'},
  primaryButtonText: {color: '#090A0B', fontSize: 16, fontWeight: '800'},
  disabledButton: {backgroundColor: '#34363A'},
  pressed: {opacity: 0.78},
  endButton: {width: '100%', marginTop: 20, paddingVertical: 14, alignItems: 'center'},
  endButtonText: {color: '#FF8A80', fontSize: 15, fontWeight: '800'},
  permissionCard: {marginTop: 18, padding: 18, borderRadius: 20, flexDirection: 'row',
    alignItems: 'center', backgroundColor: '#1B1C1F'},
  statusDot: {width: 10, height: 10, borderRadius: 5, marginRight: 14},
  readyDot: {backgroundColor: '#D9FF57'},
  warningDot: {backgroundColor: '#FF8A80'},
  permissionCopy: {flex: 1},
  permissionTitle: {color: '#FFFFFF', fontSize: 15, fontWeight: '700'},
  permissionSubtitle: {marginTop: 3, color: '#A5A6A9', fontSize: 12},
  settingsLink: {color: '#D9FF57', fontSize: 14, fontWeight: '800'},
  statusLabel: {marginTop: 22, color: '#A5A6A9', fontSize: 11,
    fontWeight: '800', letterSpacing: 1.3},
  detectorStatus: {marginTop: 7, color: '#D0D1D3', fontSize: 14, lineHeight: 20},
  privacyCopy: {marginTop: 14, color: '#77797E', fontSize: 12, lineHeight: 18},
});
