import React, { useState } from "react";
import {
  Alert,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { SafeAreaView } from "react-native-safe-area-context";
import { finiteColors, finiteGradientColors } from "./theme";

type Props = {
  onLogin?: (email: string, password: string) => void;
  onForgotPassword?: () => void;
  onAppleSignIn?: () => void;
  onGoogleSignIn?: () => void;
  onCreateAccount?: () => void;
};

export default function LoginScreen({
  onLogin,
  onForgotPassword,
  onAppleSignIn,
  onGoogleSignIn,
  onCreateAccount,
}: Props) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const authUnavailable = () => Alert.alert("FINITE", "Authentication is not connected yet. You can continue setting up your goal.");

  return (
    <LinearGradient
      colors={finiteGradientColors}
      locations={[0, 0.55, 1]}
      start={{ x: 0, y: 0 }}
      end={{ x: 0, y: 1 }}
      style={styles.background}
    >
      <StatusBar barStyle="dark-content" />

      <SafeAreaView style={styles.safeArea} edges={["top", "bottom"]}>
        <KeyboardAvoidingView
          style={styles.keyboardView}
          behavior={Platform.OS === "ios" ? "padding" : "height"}
        >
          <ScrollView
            contentContainerStyle={styles.page}
            keyboardShouldPersistTaps="handled"
          >
            <View style={styles.topSection}>
              <View style={styles.header}>
                <View style={styles.brand}>
                  <Text style={styles.clockMark}>◷</Text>
                  <Text style={styles.brandName}>finite</Text>
                </View>
                <View style={styles.headerCircle} />
              </View>

              <Text style={styles.title}>Welcome back.</Text>
            </View>

            <View style={styles.form}>
              <Text style={styles.label}>Email address</Text>
              <TextInput
                value={email}
                onChangeText={setEmail}
                placeholder="you@example.com"
                placeholderTextColor={finiteColors.secondaryText}
                keyboardType="email-address"
                autoCapitalize="none"
                autoCorrect={false}
                style={styles.input}
              />

              <Text style={styles.passwordLabel}>Password</Text>
              <View style={styles.passwordBox}>
                <TextInput
                  value={password}
                  onChangeText={setPassword}
                  placeholder="Enter your password"
                  placeholderTextColor={finiteColors.secondaryText}
                  secureTextEntry={!showPassword}
                  autoCapitalize="none"
                  style={[styles.input, styles.passwordInput]}
                />

                <Pressable
                  onPress={() => setShowPassword((value) => !value)}
                  accessibilityRole="button"
                  accessibilityLabel={
                    showPassword ? "Hide password" : "Show password"
                  }
                  style={styles.eyeButton}
                >
                  <View style={styles.eyeOutline}>
                    <View style={styles.eyePupil} />
                  </View>
                  {!showPassword && <View style={styles.eyeSlash} />}
                </Pressable>
              </View>

              <Pressable
                onPress={onForgotPassword ?? authUnavailable}
                style={styles.forgotButton}
              >
                <Text style={styles.forgotText}>Forgot password?</Text>
              </Pressable>

              <Pressable
                onPress={() => onLogin?.(email, password)}
                style={({ pressed }) => [
                  styles.loginButton,
                  pressed && styles.pressed,
                ]}
              >
                <Text style={styles.loginText}>Log in</Text>
                <Text style={styles.arrow}>→</Text>
              </Pressable>

              <View style={styles.dividerRow}>
                <View style={styles.dividerLine} />
                <Text style={styles.dividerText}>or continue with</Text>
                <View style={styles.dividerLine} />
              </View>

              <View style={styles.socialRow}>
                <Pressable
                  onPress={onAppleSignIn ?? authUnavailable}
                  style={({ pressed }) => [
                    styles.socialButton,
                    pressed && styles.pressed,
                  ]}
                >
                  <Text style={styles.appleSymbol}>●</Text>
                  <Text style={styles.socialText}>Apple</Text>
                </Pressable>

                <Pressable
                  onPress={onGoogleSignIn ?? authUnavailable}
                  style={({ pressed }) => [
                    styles.socialButton,
                    pressed && styles.pressed,
                  ]}
                >
                  <Text style={styles.googleMark}>G</Text>
                  <Text style={styles.socialText}>Google</Text>
                </Pressable>
              </View>

              <Text style={styles.timeNote}>
                Your time is yours. Let’s keep it that way.
              </Text>
            </View>

            <View style={styles.accountRow}>
              <Text style={styles.accountPrompt}>New to Finite? </Text>
              <Pressable accessibilityRole="button" onPress={onCreateAccount ?? authUnavailable}>
                <Text style={styles.createAccount}>Create an account</Text>
              </Pressable>
            </View>
          </ScrollView>
        </KeyboardAvoidingView>
      </SafeAreaView>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  background: {
    flex: 1,
  },
  safeArea: {
    flex: 1,
  },
  keyboardView: {
    flex: 1,
  },
  page: {
    flexGrow: 1,
    width: "100%",
    maxWidth: 430,
    alignSelf: "center",
    justifyContent: "space-between",
    paddingHorizontal: 20,
    paddingTop: 12,
    paddingBottom: 24,
  },
  topSection: {},
  header: {
    height: 48,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  brand: {
    flexDirection: "row",
    alignItems: "center",
  },
  clockMark: {
    color: finiteColors.text,
    fontSize: 25,
    marginRight: 5,
  },
  brandName: {
    color: finiteColors.text,
    fontSize: 20,
  },
  headerCircle: {
    width: 48,
    height: 48,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: "rgba(255,255,255,0.12)",
  },
  title: {
    color: finiteColors.text,
    fontSize: 25,
    fontWeight: "600",
    marginTop: 38,
  },
  form: {
    marginTop: 38,
  },
  label: {
    color: "#344957",
    fontSize: 11,
    marginBottom: 6,
  },
  passwordLabel: {
    color: "#344957",
    fontSize: 11,
    marginTop: 13,
    marginBottom: 6,
  },
  input: {
    height: 42,
    backgroundColor: finiteColors.surface,
    borderRadius: 10,
    paddingHorizontal: 12,
    color: finiteColors.text,
    fontSize: 13,
  },
  passwordBox: {
    position: "relative",
  },
  passwordInput: {
    paddingRight: 48,
  },
  eyeButton: {
    position: "absolute",
    right: 12,
    top: 0,
    width: 25,
    height: 42,
    alignItems: "center",
    justifyContent: "center",
  },
  eyeOutline: {
    width: 16,
    height: 10,
    borderWidth: 1.3,
    borderColor: finiteColors.text,
    borderRadius: 10,
    alignItems: "center",
    justifyContent: "center",
  },
  eyePupil: {
    width: 4,
    height: 4,
    borderRadius: 2,
    backgroundColor: finiteColors.text,
  },
  eyeSlash: {
    position: "absolute",
    width: 19,
    height: 1.5,
    backgroundColor: finiteColors.text,
    transform: [{ rotate: "45deg" }],
  },
  forgotButton: {
    alignSelf: "flex-end",
    marginTop: 10,
  },
  forgotText: {
    color: finiteColors.secondaryText,
    fontSize: 11,
  },
  loginButton: {
    height: 44,
    backgroundColor: finiteColors.primary,
    borderRadius: 11,
    paddingHorizontal: 14,
    marginTop: 13,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  loginText: {
    color: finiteColors.surface,
    fontSize: 13,
    fontWeight: "600",
  },
  arrow: {
    color: finiteColors.surface,
    fontSize: 20,
  },
  dividerRow: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 19,
  },
  dividerLine: {
    flex: 1,
    height: 1,
    backgroundColor: "rgba(255,255,255,0.28)",
  },
  dividerText: {
    color: finiteColors.secondaryText,
    fontSize: 10,
    marginHorizontal: 10,
  },
  socialRow: {
    flexDirection: "row",
    marginTop: 14,
  },
  socialButton: {
    height: 42,
    flex: 1,
    backgroundColor: finiteColors.surface,
    borderRadius: 10,
    alignItems: "center",
    justifyContent: "center",
    flexDirection: "row",
    marginHorizontal: 5,
  },
  appleSymbol: {
    color: finiteColors.text,
    fontSize: 14,
    marginRight: 7,
  },
  googleMark: {
    color: finiteColors.text,
    fontSize: 14,
    fontWeight: "700",
    marginRight: 7,
  },
  socialText: {
    color: finiteColors.text,
    fontSize: 12,
    fontWeight: "500",
  },
  timeNote: {
    color: finiteColors.secondaryText,
    fontSize: 10,
    textAlign: "center",
    marginTop: 20,
  },
  accountRow: {
    flexDirection: "row",
    justifyContent: "center",
    alignItems: "center",
    marginTop: 38,
  },
  accountPrompt: {
    color: finiteColors.secondaryText,
    fontSize: 11,
  },
  createAccount: {
    color: finiteColors.text,
    fontSize: 11,
    fontWeight: "600",
  },
  pressed: {
    opacity: 0.8,
  },
});/*import React, { useState } from "react";
import {
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StatusBar,
  Text,
  TextInput,
  View,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { SafeAreaView } from "react-native-safe-area-context";
import {
  finiteColors,
  finiteGradientColors,
  loginStyles as styles,
} from "./theme";

type Props = {
  onLogin?: (email: string, password: string) => void;
  onForgotPassword?: () => void;
  onAppleSignIn?: () => void;
  onGoogleSignIn?: () => void;
  onCreateAccount?: () => void;
};

export default function LoginScreen({
  onLogin,
  onForgotPassword,
  onAppleSignIn,
  onGoogleSignIn,
  onCreateAccount,
}: Props) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  return (
    <LinearGradient
      colors={finiteGradientColors}
      locations={[0, 0.55, 1]}
      start={{ x: 0, y: 0 }}
      end={{ x: 0, y: 1 }}
      style={styles.background}
    >
      <StatusBar barStyle="dark-content" />

      <SafeAreaView style={styles.safeArea} edges={["top", "bottom"]}>
        <KeyboardAvoidingView
          style={styles.keyboardView}
          behavior={Platform.OS === "ios" ? "padding" : undefined}
        >
          <ScrollView
            contentContainerStyle={styles.page}
            keyboardShouldPersistTaps="handled"
          >
            <View style={styles.topSection}>
              <View style={styles.header}>
                <View style={styles.brand}>
                  <Text style={styles.clockMark}>◷</Text>
                  <Text style={styles.brandName}>finite</Text>
                </View>
                <View style={styles.headerCircle} />
              </View>

              <Text style={styles.title}>Welcome back.</Text>
            </View>

            <View style={styles.form}>
              <Text style={styles.label}>Email address</Text>
              <TextInput
                value={email}
                onChangeText={setEmail}
                placeholder="you@example.com"
                placeholderTextColor={finiteColors.secondaryText}
                keyboardType="email-address"
                autoCapitalize="none"
                autoCorrect={false}
                style={styles.input}
              />

              <Text style={styles.passwordLabel}>Password</Text>
              <View style={styles.passwordBox}>
                <TextInput
                  value={password}
                  onChangeText={setPassword}
                  placeholder="Enter your password"
                  placeholderTextColor={finiteColors.secondaryText}
                  secureTextEntry={!showPassword}
                  autoCapitalize="none"
                  style={[styles.input, styles.passwordInput]}
                />
                <Pressable
                  onPress={() => setShowPassword(!showPassword)}
                  accessibilityRole="button"
                  accessibilityLabel={
                    showPassword ? "Hide password" : "Show password"
                  }
                  style={styles.eyeButton}
                >
                  <View style={styles.eyeOutline}>
                    <View style={styles.eyePupil} />
                  </View>
                  {!showPassword && <View style={styles.eyeSlash} />}
                </Pressable>
              </View>

              <Pressable
                onPress={onForgotPassword}
                style={styles.forgotButton}
              >
                <Text style={styles.forgotText}>Forgot password?</Text>
              </Pressable>

              <Pressable
                onPress={() => onLogin?.(email, password)}
                style={({ pressed }) => [
                  styles.loginButton,
                  pressed && styles.pressed,
                ]}
              >
                <Text style={styles.loginText}>Log in</Text>
                <Text style={styles.arrow}>→</Text>
              </Pressable>

              <View style={styles.dividerRow}>
                <View style={styles.dividerLine} />
                <Text style={styles.dividerText}>or continue with</Text>
                <View style={styles.dividerLine} />
              </View>

              <View style={styles.socialRow}>
                <Pressable
                  onPress={onAppleSignIn}
                  style={({ pressed }) => [
                    styles.socialButton,
                    pressed && styles.pressed,
                  ]}
                >
                  <Text style={styles.appleSymbol}>●</Text>
                  <Text style={styles.socialText}>Apple</Text>
                </Pressable>

                <Pressable
                  onPress={onGoogleSignIn}
                  style={({ pressed }) => [
                    styles.socialButton,
                    pressed && styles.pressed,
                  ]}
                >
                  <Text style={styles.googleMark}>G</Text>
                  <Text style={styles.socialText}>Google</Text>
                </Pressable>
              </View>

              <Text style={styles.timeNote}>
                Your time is yours. Let’s keep it that way.
              </Text>
            </View>

            <View style={styles.accountRow}>
              <Text style={styles.accountPrompt}>New to Finite? </Text>
              <Pressable onPress={onCreateAccount}>
                <Text style={styles.createAccount}>Create an account</Text>
              </Pressable>
            </View>
          </ScrollView>
        </KeyboardAvoidingView>
      </SafeAreaView>
    </LinearGradient>
  );
}*/
