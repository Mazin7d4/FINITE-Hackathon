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
import { finiteColors, finiteGradientColors } from "./theme";

type RepeatOption = "Never" | "Every day" | "Every week";
type DateMode = "today" | "choose_date";

export type ReminderSettings = {
  videoCount: number;
  dateMode: DateMode;
  date: string;
  repeat: RepeatOption;
};

type Props = {
  initialVideoCount?: number;
  onBack?: () => void;
  onClose?: () => void;
  onSave?: (settings: ReminderSettings) => void;
};

export default function AddReminder({
  initialVideoCount = 4,
  onBack,
  onClose,
  onSave,
}: Props): React.JSX.Element {
  const [videoCount, setVideoCount] = useState(
    Math.max(1, Math.min(100, initialVideoCount)),
  );
  const [dateMode, setDateMode] = useState<DateMode>("choose_date");
  const [date, setDate] = useState("Wed, Nov 4");
  const [repeat, setRepeat] = useState<RepeatOption>("Never");

  const changeDate = () => {
    Alert.alert("Choose a date", "Select a reminder date.", [
      { text: "Today", onPress: () => setDate("Today") },
      { text: "Wed, Nov 4", onPress: () => setDate("Wed, Nov 4") },
      { text: "Thu, Nov 5", onPress: () => setDate("Thu, Nov 5") },
      { text: "Cancel", style: "cancel" },
    ]);
  };

  const changeRepeat = () => {
    Alert.alert("Repeat reminder", "How often should this reminder repeat?", [
      { text: "Never", onPress: () => setRepeat("Never") },
      { text: "Every day", onPress: () => setRepeat("Every day") },
      { text: "Every week", onPress: () => setRepeat("Every week") },
      { text: "Cancel", style: "cancel" },
    ]);
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
        <ScrollView contentContainerStyle={styles.page}>
          <View style={styles.topBar}>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Go back"
              onPress={onBack}
              style={styles.roundButton}
            >
              <Text style={styles.backArrow}>‹</Text>
            </Pressable>

            <Text style={styles.topLabel}>WATCH LATER</Text>

            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Close"
              onPress={onClose}
              style={styles.closeButton}
            >
              <Text style={styles.closeText}>×</Text>
            </Pressable>
          </View>

          <Text style={styles.title}>Add reminder</Text>
          <Text style={styles.description}>
            A little nudge to make time for the videos you’ve saved.
          </Text>

          <View style={styles.countCard}>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Remove one video"
              disabled={videoCount <= 1}
              onPress={() => setVideoCount((count) => Math.max(1, count - 1))}
              style={({ pressed }) => [
                styles.stepButton,
                videoCount <= 1 && styles.disabledButton,
                pressed && styles.pressed,
              ]}
            >
              <Text style={styles.stepText}>−</Text>
            </Pressable>

            <View style={styles.countWrap}>
              <Text style={styles.count}>{videoCount}</Text>
              <Text style={styles.countCaption}>videos selected</Text>
            </View>

            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Add one video"
              disabled={videoCount >= 100}
              onPress={() => setVideoCount((count) => Math.min(100, count + 1))}
              style={({ pressed }) => [
                styles.stepButton,
                videoCount >= 100 && styles.disabledButton,
                pressed && styles.pressed,
              ]}
            >
              <Text style={styles.stepText}>+</Text>
            </Pressable>
          </View>

          <Text style={styles.question}>When would you like a reminder?</Text>

          <View style={styles.dateChoices}>
            <Pressable
              accessibilityRole="button"
              accessibilityState={{ selected: dateMode === "today" }}
              onPress={() => {
                setDateMode("today");
                setDate("Today");
              }}
              style={[
                styles.dateChoice,
                dateMode === "today" && styles.dateChoiceSelected,
              ]}
            >
              <Text style={styles.dateChoiceText}>Today</Text>
            </Pressable>

            <Pressable
              accessibilityRole="button"
              accessibilityState={{ selected: dateMode === "choose_date" }}
              onPress={() => setDateMode("choose_date")}
              style={[
                styles.dateChoice,
                dateMode === "choose_date" && styles.dateChoiceSelected,
              ]}
            >
              <Text style={styles.dateChoiceText}>Choose date</Text>
            </Pressable>
          </View>

          <View style={styles.optionsCard}>
            <Pressable
              accessibilityRole="button"
              onPress={changeDate}
              style={styles.optionRow}
            >
              <View style={styles.optionLeft}>
                <Text style={styles.optionIcon}>▦</Text>
                <Text style={styles.optionLabel}>Date</Text>
              </View>
              <View style={styles.optionRight}>
                <Text style={styles.optionValue}>{date}</Text>
                <Text style={styles.chevron}>›</Text>
              </View>
            </Pressable>

            <View style={styles.divider} />

            <Pressable
              accessibilityRole="button"
              onPress={changeRepeat}
              style={styles.optionRow}
            >
              <View style={styles.optionLeft}>
                <Text style={styles.optionIcon}>⇄</Text>
                <Text style={styles.optionLabel}>Repeat</Text>
              </View>
              <View style={styles.optionRight}>
                <Text style={styles.optionValue}>{repeat}</Text>
                <Text style={styles.chevron}>›</Text>
              </View>
            </Pressable>
          </View>

          <View style={styles.bottom}>
            <Pressable
              accessibilityRole="button"
              onPress={() =>
                onSave?.({ videoCount, dateMode, date, repeat })
              }
              style={({ pressed }) => [
                styles.saveButton,
                pressed && styles.pressed,
              ]}
            >
              <Text style={styles.bell}>♧</Text>
              <Text style={styles.saveText}>Save reminder</Text>
            </Pressable>

            <Text style={styles.footerText}>
              We’ll remind you when you reach your selected video count.
            </Text>
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
    paddingHorizontal: 15,
    paddingTop: 10,
    paddingBottom: 10,
  },
  topBar: {
    minHeight: 32,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  roundButton: {
    width: 22,
    height: 22,
    borderRadius: 11,
    backgroundColor: "rgba(255,255,255,0.55)",
    alignItems: "center",
    justifyContent: "center",
  },
  backArrow: {
    color: finiteColors.text,
    fontSize: 20,
    lineHeight: 22,
  },
  topLabel: {
    color: finiteColors.secondaryText,
    fontSize: 7,
    letterSpacing: 0.2,
  },
  closeButton: {
    width: 22,
    height: 22,
    alignItems: "center",
    justifyContent: "center",
  },
  closeText: {
    color: finiteColors.text,
    fontSize: 17,
  },
  title: {
    color: finiteColors.text,
    fontSize: 17,
    fontWeight: "600",
    marginTop: 10,
  },
  description: {
    color: finiteColors.secondaryText,
    fontSize: 9,
    lineHeight: 13,
    marginTop: 5,
    maxWidth: 260,
  },
  countCard: {
    minHeight: 68,
    backgroundColor: "rgba(255,255,255,0.84)",
    borderRadius: 14,
    paddingHorizontal: 11,
    marginTop: 12,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  stepButton: {
    width: 24,
    height: 24,
    borderRadius: 9,
    backgroundColor: "#DDEFEA",
    alignItems: "center",
    justifyContent: "center",
  },
  stepText: {
    color: "#73A49C",
    fontSize: 16,
    lineHeight: 19,
  },
  disabledButton: {
    opacity: 0.45,
  },
  countWrap: {
    alignItems: "center",
  },
  count: {
    color: finiteColors.text,
    fontSize: 30,
    lineHeight: 32,
    fontWeight: "500",
  },
  countCaption: {
    color: finiteColors.secondaryText,
    fontSize: 8,
    marginTop: 1,
  },
  question: {
    color: finiteColors.text,
    fontSize: 8,
    fontWeight: "600",
    marginTop: 13,
    marginBottom: 6,
  },
  dateChoices: {
    flexDirection: "row",
    gap: 5,
    marginBottom: 7,
  },
  dateChoice: {
    flex: 1,
    minHeight: 23,
    borderRadius: 99,
    backgroundColor: "rgba(255,255,255,0.55)",
    alignItems: "center",
    justifyContent: "center",
  },
  dateChoiceSelected: {
    backgroundColor: "rgba(255,255,255,0.82)",
  },
  dateChoiceText: {
    color: finiteColors.secondaryText,
    fontSize: 7,
  },
  optionsCard: {
    backgroundColor: "rgba(255,255,255,0.82)",
    borderRadius: 13,
    paddingHorizontal: 10,
  },
  optionRow: {
    minHeight: 29,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  optionLeft: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
  },
  optionRight: {
    flexDirection: "row",
    alignItems: "center",
    gap: 7,
  },
  optionIcon: {
    color: finiteColors.primary,
    fontSize: 11,
    width: 13,
  },
  optionLabel: {
    color: finiteColors.text,
    fontSize: 8,
  },
  optionValue: {
    color: finiteColors.text,
    fontSize: 8,
  },
  chevron: {
    color: finiteColors.secondaryText,
    fontSize: 14,
  },
  divider: {
    height: 1,
    backgroundColor: finiteColors.divider,
  },
  bottom: {
    marginTop: "auto",
    paddingTop: 20,
  },
  saveButton: {
    minHeight: 30,
    borderRadius: 9,
    backgroundColor: finiteColors.primary,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 6,
  },
  bell: {
    color: "#FFFFFF",
    fontSize: 10,
  },
  saveText: {
    color: "#FFFFFF",
    fontSize: 8,
    fontWeight: "500",
  },
  footerText: {
    color: finiteColors.secondaryText,
    fontSize: 6,
    textAlign: "center",
    marginTop: 7,
  },
  pressed: {
    opacity: 0.78,
  },
});