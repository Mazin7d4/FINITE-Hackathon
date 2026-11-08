import React, { useState } from "react";
import {
  Alert,
  Pressable,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { SafeAreaView } from "react-native-safe-area-context";
import { FiniteNative } from "../native/finite";
import { finiteColors, finiteGradientColors } from "./theme";

const SCHEDULES = [
  { id: "exam_week", title: "Exam Week — Mon–Sun", icon: "▤" },
  { id: "weekdays", title: "Weekdays — Mon–Fri", icon: "▦" },
  { id: "busy_week", title: "Busy week — Customize days", icon: "▱" },
] as const;
export type ScheduleId = (typeof SCHEDULES)[number]["id"];
const DAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const DEFAULT_DAYS: Record<ScheduleId, number[]> = {
  exam_week: [0, 1, 2, 3, 4, 5, 6],
  weekdays: [1, 2, 3, 4, 5],
  busy_week: [1, 2, 3, 4, 5],
};

export type ScheduleSettings = {
  id: ScheduleId;
  days: number[];
  startMinute: number;
  endMinute: number;
};

type Props = {
  initialSchedule?: ScheduleId;
  initialSettings?: ScheduleSettings;
  onBack?: () => void;
  onContinue?: (schedule: ScheduleSettings) => void;
};

export default function ChooseSchedule({
  initialSchedule = "exam_week",
  initialSettings,
  onBack,
  onContinue,
}: Props) {
  const [selectedSchedule, setSelectedSchedule] =
    useState<ScheduleId>(initialSchedule);
  const [startMinute, setStartMinute] = useState(initialSettings?.startMinute ?? 8 * 60);
  const [endMinute, setEndMinute] = useState(initialSettings?.endMinute ?? 22 * 60);
  const initialDays = initialSettings?.days?.filter((day) => day >= 0 && day <= 6) ?? [];
  const [selectedDays, setSelectedDays] = useState<number[]>(
    initialSettings?.days ? [...new Set(initialDays)] : DEFAULT_DAYS[initialSchedule],
  );
  const daysFor = (id: ScheduleId) => id === "busy_week" ? selectedDays : DEFAULT_DAYS[id];
  const formatTime = (minute: number) => `${String(Math.floor(minute / 60)).padStart(2, "0")}:${String(minute % 60).padStart(2, "0")}`;
  const chooseTime = async (kind: "start" | "end") => {
    try {
      const pickedMinute = await FiniteNative.pickTime(kind === "start" ? startMinute : endMinute);
      if (pickedMinute == null) return;
      if (kind === "start") setStartMinute(pickedMinute);
      else setEndMinute(pickedMinute);
    } catch {
      Alert.alert("Unable to choose a time", "Please try again while FINITE is open.");
    }
  };
  const toggleDay = (day: number) => {
    if (selectedDays.includes(day)) {
      if (selectedDays.length === 1) {
        Alert.alert("Choose at least one day", "Busy week needs at least one selected day.");
        return;
      }
      setSelectedDays(selectedDays.filter((selectedDay) => selectedDay !== day));
      return;
    }
    setSelectedDays([...selectedDays, day].sort((a, b) => a - b));
  };
  const timeExplanation = startMinute === endMinute
    ? "All day — FINITE counts throughout the selected days."
    : startMinute > endMinute
      ? "Overnight — this window continues past midnight into the next day."
      : undefined;

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
                  onPress={() => {
                    setSelectedSchedule(schedule.id);
                    if (schedule.id === "exam_week") { setStartMinute(8 * 60); setEndMinute(22 * 60); }
                    if (schedule.id === "weekdays") { setStartMinute(7 * 60); setEndMinute(22 * 60); }
                  }}
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

          {selectedSchedule === "busy_week" && (
            <View style={styles.dayPicker}>
              <Text style={styles.dayPickerLabel}>Active days</Text>
              <View style={styles.dayRow}>
                {DAYS.map((day, index) => {
                  const selected = selectedDays.includes(index);
                  return (
                    <Pressable
                      key={day}
                      accessibilityRole="checkbox"
                      accessibilityLabel={day}
                      accessibilityState={{ checked: selected }}
                      onPress={() => toggleDay(index)}
                      style={({ pressed }) => [
                        styles.dayChoice,
                        selected && styles.dayChoiceSelected,
                        pressed && styles.pressed,
                      ]}
                    >
                      <Text style={[styles.dayText, selected && styles.dayTextSelected]}>{day}</Text>
                    </Pressable>
                  );
                })}
              </View>
            </View>
          )}

          <Text style={styles.question}>When should FINITE count?</Text>
          <View style={styles.timeRow}>
            <Pressable style={styles.timeChoice} onPress={() => chooseTime("start")} accessibilityRole="button">
              <Text style={styles.timeLabel}>Start</Text><Text style={styles.timeValue}>{formatTime(startMinute)}</Text>
            </Pressable>
            <Text style={styles.timeDash}>→</Text>
            <Pressable style={styles.timeChoice} onPress={() => chooseTime("end")} accessibilityRole="button">
              <Text style={styles.timeLabel}>End</Text><Text style={styles.timeValue}>{formatTime(endMinute)}</Text>
            </Pressable>
          </View>
          {timeExplanation && <Text style={styles.timeExplanation}>{timeExplanation}</Text>}

          <View style={styles.bottom}>
            <Pressable
              accessibilityRole="button"
              onPress={() => {
                const days = daysFor(selectedSchedule);
                if (days.length === 0) {
                  Alert.alert("Choose at least one day", "Select at least one day before continuing.");
                  return;
                }
                onContinue?.({id: selectedSchedule, days, startMinute, endMinute});
              }}
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
  timeRow: { flexDirection: "row", alignItems: "center", gap: 8, marginTop: 10 },
  timeChoice: { flex: 1, minHeight: 48, backgroundColor: finiteColors.surface, borderRadius: 12, paddingHorizontal: 13, justifyContent: "center" },
  timeLabel: { color: finiteColors.secondaryText, fontSize: 10 },
  timeValue: { color: finiteColors.text, fontSize: 14, fontWeight: "600", marginTop: 2 },
  timeDash: { color: finiteColors.secondaryText, fontSize: 16 },
  dayPicker: { marginTop: 14, backgroundColor: finiteColors.surface, borderRadius: 14, padding: 12 },
  dayPickerLabel: { color: finiteColors.secondaryText, fontSize: 10, marginBottom: 8 },
  dayRow: { flexDirection: "row", justifyContent: "space-between", gap: 5 },
  dayChoice: { flex: 1, minHeight: 34, borderRadius: 9, borderWidth: 1, borderColor: finiteColors.divider, alignItems: "center", justifyContent: "center" },
  dayChoiceSelected: { backgroundColor: finiteColors.primary, borderColor: finiteColors.primary },
  dayText: { color: finiteColors.secondaryText, fontSize: 10, fontWeight: "600" },
  dayTextSelected: { color: finiteColors.surface },
  timeExplanation: { color: finiteColors.secondaryText, fontSize: 11, lineHeight: 16, marginTop: 8 },
});
