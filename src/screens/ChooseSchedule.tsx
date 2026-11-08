import React, { useState } from "react";
import {
  Pressable,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { SafeAreaView } from "react-native-safe-area-context";
import { finiteColors, finiteGradientColors } from "./theme";

const SCHEDULES = [
  { id: "exam_week", title: "Exam Week — Mon–Sun", icon: "▤" },
  { id: "weekdays", title: "Weekdays — Mon–Fri", icon: "▦" },
  { id: "busy_week", title: "Busy week — Customize days", icon: "▱" },
] as const;

type ScheduleId = (typeof SCHEDULES)[number]["id"];

type Props = {
  onBack?: () => void;
  onContinue?: (schedule: ScheduleId) => void;
};

export default function ChooseSchedule({
  onBack,
  onContinue,
}: Props) {
  const [selectedSchedule, setSelectedSchedule] =
    useState<ScheduleId>("exam_week");

  const selectedTitle =
    SCHEDULES.find((schedule) => schedule.id === selectedSchedule)?.title ??
    "Exam Week — Mon–Sun";

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
        <ScrollView contentContainerStyle={styles.page}>
          <View style={styles.topBar}>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Go back"
              onPress={onBack}
              hitSlop={12}
              style={styles.backButton}
            >
              <Text style={styles.backArrow}>‹</Text>
            </Pressable>

            <Text style={styles.topBarTitle}>Your video goal</Text>
          </View>

          <View style={styles.progressRow}>
            <Text style={styles.eyebrow}>01 / CONTEXT</Text>
            <View style={styles.progressMarks}>
              <View style={[styles.progressMark, styles.progressActive]} />
              <View style={styles.progressMark} />
              <View style={styles.progressMark} />
            </View>
          </View>

          <Text style={styles.title}>Make room for{"\n"}what matters.</Text>
          <Text style={styles.description}>
            Choose the part of student life you’re planning around. Your goal
            can flex with you.
          </Text>

          <Text style={styles.question}>What does your schedule look like?</Text>

          <View style={styles.options}>
            {SCHEDULES.map((schedule) => {
              const selected = selectedSchedule === schedule.id;

              return (
                <Pressable
                  key={schedule.id}
                  accessibilityRole="radio"
                  accessibilityState={{ selected }}
                  onPress={() => setSelectedSchedule(schedule.id)}
                  style={({ pressed }) => [
                    styles.option,
                    selected && styles.optionSelected,
                    pressed && styles.pressed,
                  ]}
                >
                  <Text style={styles.optionIcon}>{schedule.icon}</Text>

                  <Text style={styles.optionTitle}>{schedule.title}</Text>

                  <View
                    style={[
                      styles.selectButton,
                      selected && styles.selectButtonSelected,
                    ]}
                  >
                    {selected && <Text style={styles.checkmark}>✓</Text>}
                  </View>
                </Pressable>
              );
            })}
          </View>

          <View style={styles.bottom}>
            <Pressable
              accessibilityRole="button"
              onPress={() => onContinue?.(selectedSchedule)}
              style={({ pressed }) => [
                styles.continueButton,
                pressed && styles.pressed,
              ]}
            >
              <Text style={styles.continueText}>
                Continue with {selectedTitle.toLowerCase()}
              </Text>
              <Text style={styles.continueArrow}>→</Text>
            </Pressable>
          </View>
        </ScrollView>
      </SafeAreaView>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  background: { flex: 1 },
  safeArea: { flex: 1 },
  page: {
    flexGrow: 1,
    width: "100%",
    maxWidth: 430,
    alignSelf: "center",
    paddingHorizontal: 22,
    paddingTop: 14,
    paddingBottom: 20,
  },
  topBar: {
    minHeight: 38,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  backButton: {
    width: 30,
    height: 36,
    justifyContent: "center",
  },
  backArrow: {
    color: finiteColors.text,
    fontSize: 30,
    lineHeight: 32,
  },
  topBarTitle: {
    color: finiteColors.text,
    fontSize: 13,
    fontWeight: "500",
  },
  progressRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginTop: 14,
  },
  eyebrow: {
    color: finiteColors.secondaryText,
    fontSize: 10,
    letterSpacing: 0.4,
  },
  progressMarks: {
    flexDirection: "row",
    gap: 4,
  },
  progressMark: {
    width: 14,
    height: 3,
    borderRadius: 2,
    backgroundColor: "rgba(255,255,255,0.65)",
  },
  progressActive: {
    width: 38,
    backgroundColor: finiteColors.primary,
  },
  title: {
    color: finiteColors.text,
    fontSize: 27,
    lineHeight: 31,
    fontWeight: "600",
    marginTop: 20,
  },
  description: {
    color: finiteColors.secondaryText,
    fontSize: 13,
    lineHeight: 19,
    marginTop: 8,
  },
  question: {
    color: finiteColors.text,
    fontSize: 12,
    fontWeight: "500",
    marginTop: 22,
    marginBottom: 10,
  },
  options: {
    gap: 10,
  },
  option: {
    minHeight: 72,
    backgroundColor: finiteColors.surface,
    borderRadius: 16,
    borderWidth: 1.5,
    borderColor: "transparent",
    paddingHorizontal: 15,
    flexDirection: "row",
    alignItems: "center",
  },
  optionSelected: {
    borderColor: finiteColors.primary,
  },
  optionIcon: {
    color: finiteColors.text,
    width: 27,
    fontSize: 20,
    marginRight: 9,
  },
  optionTitle: {
    color: finiteColors.text,
    flex: 1,
    fontSize: 12,
    lineHeight: 17,
    fontWeight: "600",
    paddingRight: 8,
  },
  selectButton: {
    width: 36,
    height: 36,
    borderRadius: 5,
    backgroundColor: finiteColors.progressTrack,
    borderWidth: 1,
    borderColor: finiteColors.divider,
    alignItems: "center",
    justifyContent: "center",
  },
  selectButtonSelected: {
    backgroundColor: finiteColors.primary,
    borderColor: finiteColors.primary,
  },
  checkmark: {
    color: finiteColors.surface,
    fontSize: 22,
    fontWeight: "700",
  },
  bottom: {
    marginTop: "auto",
    paddingTop: 24,
  },
  continueButton: {
    minHeight: 48,
    backgroundColor: finiteColors.primary,
    borderRadius: 12,
    paddingHorizontal: 15,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  continueText: {
    color: finiteColors.surface,
    fontSize: 12,
    fontWeight: "600",
    flex: 1,
    paddingRight: 8,
  },
  continueArrow: {
    color: finiteColors.surface,
    fontSize: 19,
  },
  pressed: {
    opacity: 0.8,
  },
});