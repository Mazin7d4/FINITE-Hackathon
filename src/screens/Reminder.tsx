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

type RepeatOption = "Never" | "Every day" | "Every week";
type DateMode = "today" | "choose_date";

export type ReminderSettings = {
  id?: string;
  videoCount: number;
  dateMode: DateMode;
  date: string;
  repeat: RepeatOption;
  timestamp: number;
};

export type ReminderRecord = ReminderSettings & { id: string };

type Props = {
  initialVideoCount?: number;
  onBack?: () => void;
  onClose?: () => void;
  onSave?: (settings: ReminderSettings) => void;
  onDelete?: (id: string) => void;
  savedReminders?: ReminderRecord[];
  editingReminder?: ReminderRecord;
  onEdit?: (id: string) => void;
  onAddNew?: () => void;
};

const formatReminderDate = (value: Date) => value.toLocaleString([], {
  weekday: "short",
  month: "short",
  day: "numeric",
  hour: "numeric",
  minute: "2-digit",
});

export default function AddReminder({
  initialVideoCount = 4,
  onBack,
  onClose,
  onSave,
  onDelete,
  savedReminders = [],
  editingReminder,
  onEdit,
  onAddNew,
}: Props): React.JSX.Element {
  const [videoCount, setVideoCount] = useState(
    Math.max(1, Math.min(100, initialVideoCount)),
  );
  const [dateMode, setDateMode] = useState<DateMode>(editingReminder?.dateMode ?? "choose_date");
  const [date, setDate] = useState(formatReminderDate(new Date(editingReminder?.timestamp ?? Date.now() + 60 * 60 * 1000)));
  const [repeat, setRepeat] = useState<RepeatOption>(editingReminder?.repeat ?? "Never");
  const [reminderDate, setReminderDate] = useState(() => new Date(editingReminder?.timestamp ?? Date.now() + 60 * 60 * 1000));

  React.useEffect(() => {
    setVideoCount(Math.max(1, Math.min(100, editingReminder?.videoCount ?? initialVideoCount)));
    setDateMode(editingReminder?.dateMode ?? "choose_date");
    setDate(formatReminderDate(new Date(editingReminder?.timestamp ?? Date.now() + 60 * 60 * 1000)));
    setRepeat(editingReminder?.repeat ?? "Never");
    setReminderDate(new Date(editingReminder?.timestamp ?? Date.now() + 60 * 60 * 1000));
  }, [editingReminder, initialVideoCount]);

  const setToday = () => {
    const next = new Date();
    next.setHours(reminderDate.getHours(), reminderDate.getMinutes(), 0, 0);
    if (next.getTime() <= Date.now()) next.setTime(Date.now() + 2 * 60 * 1000);
    setDateMode("today");
    setReminderDate(next);
    setDate(formatReminderDate(next));
  };

  const changeDate = async () => {
    try {
      const pickedTimestamp = await FiniteNative.pickReminderDateTime(reminderDate.getTime());
      if (pickedTimestamp == null) return;
      const picked = new Date(pickedTimestamp);
      setReminderDate(picked);
      setDateMode(picked.toDateString() === new Date().toDateString() ? "today" : "choose_date");
      setDate(formatReminderDate(picked));
    } catch {
      Alert.alert("Unable to choose a date", "Please try again while FINITE is open.");
    }
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

          <Text style={styles.title}>{editingReminder ? "Edit reminder" : "Add reminder"}</Text>
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

          {savedReminders.length > 0 && <View style={styles.savedCard}>
            <Text style={styles.savedHeading}>Saved reminders</Text>
            {savedReminders.map(item => <View key={item.id} style={styles.savedRow}>
              <Pressable accessibilityRole="button" onPress={() => onEdit?.(item.id)} style={styles.savedCopy}>
                <Text style={styles.savedTitle}>{item.videoCount} videos · {new Date(item.timestamp).toLocaleString([], {weekday: "short", hour: "numeric", minute: "2-digit"})}</Text>
                <Text style={styles.savedDetail}>{item.repeat === "Never" ? "One time" : item.repeat}</Text>
              </Pressable>
              <Pressable accessibilityRole="button" accessibilityLabel={`Delete ${item.videoCount} video reminder`} onPress={() => onDelete?.(item.id)} style={styles.savedDelete}>
                <Text style={styles.savedDeleteText}>Delete</Text>
              </Pressable>
            </View>)}
          </View>}

          <Text style={styles.question}>When would you like a reminder?</Text>

          <View style={styles.dateChoices}>
            <Pressable
              accessibilityRole="button"
              accessibilityState={{ selected: dateMode === "today" }}
              onPress={() => {
                setToday();
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
              onPress={() => {
                if (reminderDate.getTime() <= Date.now()) {
                  Alert.alert("Choose a future time", "This reminder time has already passed. Pick a later date or time.");
                  return;
                }
                onSave?.({ id: editingReminder?.id, videoCount, dateMode, date, repeat, timestamp: reminderDate.getTime() });
              }}
              style={({ pressed }) => [
                styles.saveButton,
                pressed && styles.pressed,
              ]}
            >
              <Text style={styles.bell}>♧</Text>
              <Text style={styles.saveText}>Save reminder</Text>
            </Pressable>

            <Text style={styles.footerText}>
              We’ll remind you at the scheduled date and time.
            </Text>
            {editingReminder && onAddNew && <Pressable accessibilityRole="button" onPress={onAddNew} style={styles.addNewButton}>
              <Text style={styles.addNewText}>Add new reminder</Text>
            </Pressable>}
            {editingReminder && onDelete && <Pressable accessibilityRole="button" onPress={() => onDelete(editingReminder.id)} style={styles.deleteButton}>
              <Text style={styles.deleteText}>Delete this reminder</Text>
            </Pressable>}
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
  savedCard: { backgroundColor: "rgba(255,255,255,0.46)", borderRadius: 16, padding: 12, marginBottom: 15 },
  savedHeading: { color: finiteColors.text, fontSize: 12, fontWeight: "800", letterSpacing: 0.6, marginBottom: 5 },
  savedRow: { flexDirection: "row", alignItems: "center", borderTopWidth: 1, borderTopColor: "rgba(35,39,43,0.10)", paddingVertical: 8 },
  savedCopy: { flex: 1 },
  savedTitle: { color: finiteColors.text, fontSize: 13, fontWeight: "700" },
  savedDetail: { color: finiteColors.secondaryText, fontSize: 11, marginTop: 2 },
  savedDelete: { paddingHorizontal: 5, paddingVertical: 6 },
  savedDeleteText: { color: "#B54B43", fontSize: 11, fontWeight: "700" },
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
  deleteButton: { alignItems: "center", paddingTop: 14 },
  deleteText: { color: finiteColors.secondaryText, fontSize: 12, textDecorationLine: "underline" },
  addNewButton: { alignItems: "center", paddingTop: 12 },
  addNewText: { color: finiteColors.primary, fontSize: 11, fontWeight: "700" },
});
