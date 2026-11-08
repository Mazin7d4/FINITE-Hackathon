import React, { useMemo, useState } from "react";
import {
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

type ScheduleMode = "date_range" | "repeat_weekly";

export type PersonalGoal = {
  dailyLimit: number;
  scheduleMode: ScheduleMode;
  startsOn: string;
  endsOn: string;
};

type Props = {
  initialLimit?: number;
  scheduleLabel?: string;
  onBack?: () => void;
  onSave?: (goal: PersonalGoal) => void;
};

export default function PersonalizeGoal({
  initialLimit = 7,
  scheduleLabel = "Exam week",
  onBack,
  onSave,
}: Props): React.JSX.Element {
  const [dailyLimit, setDailyLimit] = useState(
    Math.max(1, Math.min(100, Math.round(initialLimit))),
  );
  const [scheduleMode, setScheduleMode] = useState<ScheduleMode>("date_range");
  const [startsOn, setStartsOn] = useState("2 Nov 2026");
  const [endsOn, setEndsOn] = useState("8 Nov 2026");

  const scheduleNote = useMemo(() => {
    if (scheduleMode === "repeat_weekly") {
      return "Applies every week. You can change your goal anytime.";
    }
    return "Applies every day in this range. Ends automatically after 8 Nov.";
  }, [scheduleMode]);

  const adjustLimit = (amount: number) => {
    setDailyLimit((current) => Math.max(1, Math.min(100, current + amount)));
  };

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
        <ScrollView
          contentContainerStyle={styles.page}
          keyboardShouldPersistTaps="handled"
        >
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
            <View style={styles.topBarSpacer} />
          </View>

          <View style={styles.progressRow}>
            <Text style={styles.eyebrow}>02 / PERSONALIZE</Text>
            <View style={styles.progressMarks}>
              <View style={styles.progressMark} />
              <View style={[styles.progressMark, styles.progressActive]} />
              <View style={styles.progressMark} />
            </View>
          </View>

          <Text style={styles.title}>Your pace. Your limit.</Text>
          <Text style={styles.description}>
            Keep space for studying and a few scroll breaks. Choose what feels
            manageable.
          </Text>

          <View style={styles.limitCard}>
            <Text style={styles.limitLabel}>My daily video limit</Text>
            <View style={styles.limitControls}>
              <Pressable
                accessibilityRole="button"
                accessibilityLabel="Decrease daily video limit"
                accessibilityState={{ disabled: dailyLimit <= 1 }}
                disabled={dailyLimit <= 1}
                onPress={() => adjustLimit(-1)}
                style={({ pressed }) => [
                  styles.stepButton,
                  dailyLimit <= 1 && styles.stepButtonDisabled,
                  pressed && styles.pressed,
                ]}
              >
                <Text style={styles.stepButtonText}>−</Text>
              </Pressable>
              <View style={styles.limitValueWrap}>
                <Text style={styles.limitValue}>{dailyLimit}</Text>
                <Text style={styles.limitUnit}>videos per day</Text>
              </View>
              <Pressable
                accessibilityRole="button"
                accessibilityLabel="Increase daily video limit"
                accessibilityState={{ disabled: dailyLimit >= 100 }}
                disabled={dailyLimit >= 100}
                onPress={() => adjustLimit(1)}
                style={({ pressed }) => [
                  styles.stepButton,
                  dailyLimit >= 100 && styles.stepButtonDisabled,
                  pressed && styles.pressed,
                ]}
              >
                <Text style={styles.stepButtonText}>+</Text>
              </Pressable>
            </View>
          </View>

          <View style={styles.questionRow}>
            <Text style={styles.question}>When should this apply?</Text>
            <View style={styles.scheduleBadge}>
              <Text style={styles.scheduleBadgeText}>{scheduleLabel}</Text>
            </View>
          </View>

          <View style={styles.segment} accessibilityRole="tablist">
            <Pressable
              accessibilityRole="tab"
              accessibilityState={{ selected: scheduleMode === "date_range" }}
              onPress={() => setScheduleMode("date_range")}
              style={[
                styles.segmentOption,
                scheduleMode === "date_range" && styles.segmentSelected,
              ]}
            >
              <Text
                style={[
                  styles.segmentText,
                  scheduleMode === "date_range" && styles.segmentTextSelected,
                ]}
              >
                Date range
              </Text>
            </Pressable>
            <Pressable
              accessibilityRole="tab"
              accessibilityState={{ selected: scheduleMode === "repeat_weekly" }}
              onPress={() => setScheduleMode("repeat_weekly")}
              style={[
                styles.segmentOption,
                scheduleMode === "repeat_weekly" && styles.segmentSelected,
              ]}
            >
              <Text
                style={[
                  styles.segmentText,
                  scheduleMode === "repeat_weekly" && styles.segmentTextSelected,
                ]}
              >
                Repeat weekly
              </Text>
            </Pressable>
          </View>

          <View style={styles.dateLabels}>
            <Text style={styles.dateLabel}>Starts on</Text>
            <Text style={styles.dateLabel}>Ends on</Text>
          </View>
          <View style={styles.dateRow}>
            <View style={styles.dateField}>
              <TextInput
                accessibilityLabel="Start date"
                value={startsOn}
                onChangeText={setStartsOn}
                style={styles.dateInput}
                placeholder="2 Nov 2026"
                placeholderTextColor={finiteColors.text}
                returnKeyType="done"
              />
              <Text style={styles.calendarIcon}>▦</Text>
            </View>
            <View style={styles.dateField}>
              <TextInput
                accessibilityLabel="End date"
                value={endsOn}
                onChangeText={setEndsOn}
                style={styles.dateInput}
                placeholder="8 Nov 2026"
                placeholderTextColor={finiteColors.text}
                returnKeyType="done"
              />
              <Text style={styles.calendarIcon}>▦</Text>
            </View>
          </View>
          <Text style={styles.helperText}>{scheduleNote}</Text>

          <View style={styles.bottom}>
            <Pressable
              accessibilityRole="button"
              onPress={() =>
                onSave?.({ dailyLimit, scheduleMode, startsOn, endsOn })
              }
              style={({ pressed }) => [
                styles.saveButton,
                pressed && styles.pressed,
              ]}
            >
              <Text style={styles.saveButtonText}>Save my goal</Text>
              <Text style={styles.saveArrow}>→</Text>
            </Pressable>
            <Text style={styles.footerText}>
              You can adjust this whenever life changes.
            </Text>
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
    paddingTop: 10,
    paddingBottom: 14,
  },
  topBar: {
    minHeight: 34,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  backButton: { width: 30, height: 34, justifyContent: "center" },
  backArrow: { color: finiteColors.text, fontSize: 28, lineHeight: 32 },
  topBarTitle: {
    color: finiteColors.text,
    fontSize: 12,
    fontWeight: "500",
  },
  topBarSpacer: { width: 30 },
  progressRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginTop: 10,
  },
  eyebrow: {
    color: finiteColors.secondaryText,
    fontSize: 9,
    letterSpacing: 0.35,
  },
  progressMarks: { flexDirection: "row", gap: 4 },
  progressMark: {
    width: 14,
    height: 2,
    borderRadius: 2,
    backgroundColor: "rgba(255,255,255,0.7)",
  },
  progressActive: { width: 22, backgroundColor: finiteColors.primary },
  title: {
    color: finiteColors.text,
    fontSize: 22,
    lineHeight: 28,
    fontWeight: "600",
    letterSpacing: -0.35,
    marginTop: 15,
  },
  description: {
    color: finiteColors.secondaryText,
    fontSize: 11,
    lineHeight: 16,
    marginTop: 3,
  },
  limitCard: {
    minHeight: 79,
    backgroundColor: "rgba(255,255,255,0.9)",
    borderRadius: 15,
    paddingHorizontal: 11,
    paddingTop: 8,
    paddingBottom: 6,
    marginTop: 11,
  },
  limitLabel: {
    color: finiteColors.secondaryText,
    textAlign: "center",
    fontSize: 9,
  },
  limitControls: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    flex: 1,
  },
  stepButton: {
    height: 26,
    width: 26,
    borderRadius: 10,
    backgroundColor: "#E8F1FA",
    alignItems: "center",
    justifyContent: "center",
  },
  stepButtonDisabled: { opacity: 0.5 },
  stepButtonText: {
    color: finiteColors.primary,
    fontSize: 17,
    lineHeight: 20,
    fontWeight: "500",
  },
  limitValueWrap: { alignItems: "center", justifyContent: "center" },
  limitValue: {
    color: finiteColors.primary,
    fontSize: 38,
    lineHeight: 41,
    fontWeight: "400",
    marginTop: -5,
  },
  limitUnit: { color: finiteColors.secondaryText, fontSize: 8, marginTop: -1 },
  questionRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginTop: 11,
    marginBottom: 6,
  },
  question: { color: finiteColors.text, fontSize: 10, fontWeight: "600" },
  scheduleBadge: {
    backgroundColor: "rgba(255,255,255,0.75)",
    paddingHorizontal: 7,
    paddingVertical: 3,
    borderRadius: 99,
  },
  scheduleBadgeText: {
    color: finiteColors.primary,
    fontSize: 7,
    fontWeight: "600",
  },
  segment: {
    height: 24,
    flexDirection: "row",
    backgroundColor: "rgba(255,255,255,0.88)",
    borderRadius: 8,
    padding: 2,
  },
  segmentOption: {
    flex: 1,
    borderRadius: 6,
    alignItems: "center",
    justifyContent: "center",
  },
  segmentSelected: { backgroundColor: "#D8E8FF" },
  segmentText: { color: "#7F9BA6", fontSize: 8 },
  segmentTextSelected: { color: finiteColors.text, fontWeight: "500" },
  dateLabels: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginTop: 8,
    marginBottom: 3,
  },
  dateLabel: { width: "48%", color: finiteColors.secondaryText, fontSize: 8 },
  dateRow: { flexDirection: "row", justifyContent: "space-between", gap: 8 },
  dateField: {
    flex: 1,
    height: 25,
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "rgba(255,255,255,0.88)",
    borderRadius: 7,
    paddingHorizontal: 6,
  },
  dateInput: { flex: 1, padding: 0, color: finiteColors.text, fontSize: 8 },
  calendarIcon: { color: finiteColors.primary, fontSize: 11, marginLeft: 3 },
  helperText: {
    color: finiteColors.secondaryText,
    fontSize: 8,
    lineHeight: 12,
    marginTop: 6,
    maxWidth: 270,
  },
  bottom: { marginTop: "auto", paddingTop: 22 },
  saveButton: {
    minHeight: 32,
    backgroundColor: finiteColors.primary,
    borderRadius: 9,
    paddingHorizontal: 10,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  saveButtonText: { color: "#FFFFFF", fontSize: 9, fontWeight: "600" },
  saveArrow: { color: "#FFFFFF", fontSize: 15 },
  footerText: {
    color: "#8DA9A6",
    textAlign: "center",
    fontSize: 8,
    marginTop: 6,
  },
  pressed: { opacity: 0.78 },
});
