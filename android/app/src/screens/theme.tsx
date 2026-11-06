import React from "react";
import {
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { SafeAreaView } from "react-native-safe-area-context";

type Props = {
  watched?: number;
  dailyLimit?: number;
  onStartScrolling?: () => void;
  onChangeLimit?: () => void;
};

export default function FiniteUsageScreen({
  watched = 4,
  dailyLimit = 70,
  onStartScrolling,
  onChangeLimit,
}: Props) {
  const progress =
    dailyLimit > 0 ? Math.min((watched / dailyLimit) * 100, 100) : 0;
  const remaining = Math.max(dailyLimit - watched, 0);

  return (
    <LinearGradient
      colors={["#B0D6DD", "#BADBCC", "#C3DDC5"]}
      locations={[0, 0.55, 1]}
      start={{ x: 0, y: 0 }}
      end={{ x: 0, y: 1 }}
      style={styles.background}
    >
      <SafeAreaView style={styles.safeArea} edges={["top", "bottom"]}>
        <ScrollView contentContainerStyle={styles.screen}>
          <View style={styles.header}>
            <Text style={styles.logo}>
              <Text style={styles.logoMark}>◷ </Text>
              finite
            </Text>
          </View>

          <Text style={styles.eyebrow}>YOUR DAILY CHECK-IN</Text>
          <Text style={styles.title}>Today’s usage</Text>
          <Text style={styles.intro}>
            Here’s how many short videos you’ve watched today.
          </Text>

          <View style={styles.usageCard}>
            <Text style={styles.cardLabel}>Short videos today</Text>

            <View style={styles.countRow}>
              <Text style={styles.count}>{watched}</Text>
              <Text style={styles.countTotal}>of {dailyLimit} videos</Text>
            </View>

            <View
              style={styles.progressTrack}
              accessibilityLabel={`${watched} of ${dailyLimit} videos used`}
            >
              <View style={[styles.progressFill, { width: `${progress}%` }]} />
            </View>

            <View style={styles.cardFooter}>
              <Text style={styles.footerText}>{remaining} left today</Text>
              <Text style={styles.footerText}>Daily limit: {dailyLimit}</Text>
            </View>
          </View>

          <View style={styles.actions}>
            <Pressable
              onPress={onStartScrolling}
              style={({ pressed }) => [
                styles.primaryButton,
                pressed && styles.buttonPressed,
              ]}
            >
              <Text style={styles.primaryButtonText}>Start scrolling</Text>
            </Pressable>

            <Pressable
              onPress={onChangeLimit}
              style={({ pressed }) => [
                styles.secondaryButton,
                pressed && styles.buttonPressed,
              ]}
            >
              <Text style={styles.secondaryButtonText}>Change my limit</Text>
            </Pressable>
          </View>

          <Text style={styles.note}>
            Your limit is yours to change anytime.
          </Text>
        </ScrollView>
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
  screen: {
    flexGrow: 1,
    justifyContent: "center",
    width: "100%",
    maxWidth: 430,
    alignSelf: "center",
    paddingHorizontal: 22,
    paddingTop: 24,
    paddingBottom: 28,
  },
  header: {
    marginBottom: 42,
  },
  logo: {
    color: "#162B39",
    fontSize: 20,
    fontWeight: "600",
  },
  logoMark: {
    color: "#5B7A8C",
  },
  eyebrow: {
    color: "#607684",
    fontSize: 12,
    fontWeight: "600",
    letterSpacing: 1,
    marginBottom: 9,
  },
  title: {
    color: "#162B39",
    fontSize: 30,
    fontWeight: "600",
    letterSpacing: -0.5,
  },
  intro: {
    color: "#607684",
    fontSize: 15,
    lineHeight: 22,
    marginTop: 10,
    marginBottom: 25,
  },
  usageCard: {
    backgroundColor: "#FFFFFF",
    borderRadius: 18,
    padding: 22,
    shadowColor: "#22414B",
    shadowOpacity: 0.08,
    shadowRadius: 14,
    shadowOffset: { width: 0, height: 6 },
    elevation: 3,
  },
  cardLabel: {
    color: "#607684",
    fontSize: 14,
    marginBottom: 12,
  },
  countRow: {
    flexDirection: "row",
    alignItems: "baseline",
    marginBottom: 17,
  },
  count: {
    color: "#162B39",
    fontSize: 42,
    fontWeight: "700",
    marginRight: 8,
  },
  countTotal: {
    color: "#607684",
    fontSize: 16,
  },
  progressTrack: {
    height: 9,
    backgroundColor: "#EAF0EE",
    borderRadius: 999,
    overflow: "hidden",
  },
  progressFill: {
    height: "100%",
    backgroundColor: "#5B7A8C",
    borderRadius: 999,
  },
  cardFooter: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginTop: 12,
  },
  footerText: {
    color: "#607684",
    fontSize: 13,
  },
  actions: {
    gap: 12,
    marginTop: 24,
  },
  primaryButton: {
    minHeight: 50,
    backgroundColor: "#5B7A8C",
    borderRadius: 13,
    alignItems: "center",
    justifyContent: "center",
  },
  primaryButtonText: {
    color: "#FFFFFF",
    fontSize: 15,
    fontWeight: "600",
  },
  secondaryButton: {
    minHeight: 50,
    backgroundColor: "#FFFFFF",
    borderRadius: 13,
    alignItems: "center",
    justifyContent: "center",
  },
  secondaryButtonText: {
    color: "#162B39",
    fontSize: 15,
    fontWeight: "600",
  },
  buttonPressed: {
    opacity: 0.8,
  },
  note: {
    color: "#607684",
    fontSize: 13,
    lineHeight: 19,
    textAlign: "center",
    marginTop: 20,
    paddingHorizontal: 10,
  },
});