import React from "react";
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
import type { PersonalGoal } from "./PersonalizeGoal";

type Props = {
  goal?: PersonalGoal;
  scheduleLabel?: string;
  onEdit?: () => void;
  onDone?: () => void;
  onBack?: () => void;
};

const DEFAULT_GOAL: PersonalGoal = {
  dailyLimit: 7,
  scheduleMode: "date_range",
  startsOn: "2 Nov 2026",
  endsOn: "8 Nov 2026",
};

export default function GoalSaved({
  goal = DEFAULT_GOAL,
  scheduleLabel = "Exam week",
  onEdit,
  onDone,
  onBack,
}: Props): React.JSX.Element {
  const schedule = "Every day · Monday to Sunday";

  const endNote =
    goal.scheduleMode === "repeat_weekly"
      ? "Your goal repeats every week. You can adjust it anytime."
      : `This goal ends automatically after ${goal.endsOn}.`;

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
            <Text style={styles.eyebrow}>03 / SAVED</Text>

            <View style={styles.progressMarks}>
              <View style={[styles.progressMark, styles.progressComplete]} />
              <View style={[styles.progressMark, styles.progressComplete]} />
              <View style={[styles.progressMark, styles.progressComplete]} />
            </View>
          </View>

          <View style={styles.savedLabel}>
            <View style={styles.checkCircle}>
              <Text style={styles.checkmark}>✓</Text>
            </View>

            <Text style={styles.savedLabelText}>Goal saved</Text>
          </View>

          <Text style={styles.title}>
            {"A little more space\n"}for exam week.
          </Text>

          <Text style={styles.description}>
            Your personal goal is ready. Study time and downtime can both have
            a place.
          </Text>

          <View style={styles.goalCard}>
            <Text style={styles.cardHeading}>{scheduleLabel}</Text>

            <View style={styles.countRow}>
              <Text style={styles.count}>{goal.dailyLimit}</Text>
              <Text style={styles.countUnit}>videos per day</Text>
            </View>

            <View style={styles.divider} />

            <View style={styles.detailRow}>
              <Text style={styles.detailIcon}>▦</Text>

              <View style={styles.detailTextWrap}>
                <Text style={styles.detailLabel}>Exam period</Text>
                <Text style={styles.detailValue}>
                  {goal.startsOn} – {goal.endsOn}
                </Text>
              </View>
            </View>

            <View style={[styles.detailRow, styles.repeatRow]}>
              <Text style={styles.detailIcon}>⇄</Text>

              <View style={styles.detailTextWrap}>
                <Text style={styles.detailLabel}>Applies on</Text>
                <Text style={styles.detailValue}>{schedule}</Text>
              </View>
            </View>

            <Text style={styles.endNote}>{endNote}</Text>
          </View>

          <Pressable
            accessibilityRole="button"
            onPress={onEdit}
            style={({ pressed }) => [
              styles.editButton,
              pressed && styles.pressed,
            ]}
          >
            <Text style={styles.editButtonText}>Edit goal or dates</Text>
            <Text style={styles.editIcon}>✎</Text>
          </Pressable>

          <View style={styles.bottom}>
            <Pressable
              accessibilityRole="button"
              onPress={onDone}
              style={({ pressed }) => [
                styles.doneButton,
                pressed && styles.pressed,
              ]}
            >
              <Text style={styles.doneText}>Done</Text>
              <Text style={styles.doneCheck}>✓</Text>
            </Pressable>
          </View>
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
  backButton: {
    width: 30,
    height: 34,
    justifyContent: "center",
  },
  backArrow: {
    color: finiteColors.text,
    fontSize: 28,
    lineHeight: 32,
  },
  topBarTitle: {
    color: finiteColors.text,
    fontSize: 12,
    fontWeight: "500",
    marginLeft: "auto",
  },
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
  progressMarks: {
    flexDirection: "row",
    gap: 4,
  },
  progressMark: {
    width: 14,
    height: 2,
    borderRadius: 2,
    backgroundColor: "rgba(255,255,255,0.7)",
  },
  progressComplete: {
    width: 14,
    backgroundColor: finiteColors.primary,
  },
  savedLabel: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 16,
    gap: 6,
  },
  checkCircle: {
    height: 19,
    width: 19,
    borderRadius: 10,
    backgroundColor: "rgba(255,255,255,0.7)",
    alignItems: "center",
    justifyContent: "center",
  },
  checkmark: {
    color: finiteColors.primary,
    fontSize: 12,
    fontWeight: "700",
  },
  savedLabelText: {
    color: finiteColors.text,
    fontSize: 9,
    fontWeight: "500",
  },
  title: {
    color: finiteColors.text,
    fontSize: 22,
    lineHeight: 25,
    fontWeight: "600",
    letterSpacing: -0.35,
    marginTop: 8,
  },
  description: {
    color: finiteColors.secondaryText,
    fontSize: 10,
    lineHeight: 15,
    marginTop: 7,
  },
  goalCard: {
    backgroundColor: "rgba(255,255,255,0.84)",
    borderRadius: 15,
    paddingHorizontal: 13,
    paddingTop: 12,
    paddingBottom: 11,
    marginTop: 15,
  },
  cardHeading: {
    color: finiteColors.text,
    fontSize: 9,
    fontWeight: "600",
  },
  countRow: {
    flexDirection: "row",
    alignItems: "baseline",
    marginTop: 1,
  },
  count: {
    color: finiteColors.text,
    fontSize: 40,
    lineHeight: 45,
    fontWeight: "500",
    marginRight: 10,
  },
  countUnit: {
    color: finiteColors.secondaryText,
    fontSize: 10,
  },
  divider: {
    height: 1,
    backgroundColor: finiteColors.divider,
    marginBottom: 8,
  },
  detailRow: {
    flexDirection: "row",
    alignItems: "center",
  },
  repeatRow: {
    marginTop: 6,
  },
  detailIcon: {
    color: finiteColors.primary,
    fontSize: 13,
    width: 20,
  },
  detailTextWrap: {
    flex: 1,
  },
  detailLabel: {
    color: finiteColors.secondaryText,
    fontSize: 8,
  },
  detailValue: {
    color: finiteColors.text,
    fontSize: 9,
    fontWeight: "500",
    marginTop: 1,
  },
  endNote: {
    color: finiteColors.secondaryText,
    fontSize: 8,
    lineHeight: 11,
    marginTop: 7,
  },
  editButton: {
    minHeight: 34,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    backgroundColor: "rgba(255,255,255,0.82)",
    borderRadius: 9,
    paddingHorizontal: 11,
    marginTop: 15,
  },
  editButtonText: {
    color: finiteColors.text,
    fontSize: 9,
    fontWeight: "500",
  },
  editIcon: {
    color: finiteColors.text,
    fontSize: 14,
  },
  bottom: {
    marginTop: "auto",
    paddingTop: 20,
  },
  doneButton: {
    minHeight: 34,
    backgroundColor: finiteColors.primary,
    borderRadius: 9,
    alignItems: "center",
    justifyContent: "center",
    flexDirection: "row",
  },
  doneText: {
    color: "#FFFFFF",
    fontSize: 10,
    fontWeight: "600",
  },
  doneCheck: {
    color: "#FFFFFF",
    fontSize: 13,
    marginLeft: 8,
  },
  pressed: {
    opacity: 0.78,
  },
});
