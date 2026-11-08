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

type DayUsage = {
  day: string;
  videos: number;
};

type Props = {
  selectedApps?: string[];
  platformCounts?: Record<string, number | undefined>;
  todayCount?: number;
  dailyLimit?: number;
  weekUsage?: DayUsage[];
  onBack?: () => void;
  onStartScrolling?: () => void;
  onAddReminder?: () => void;
};

const APP_INFO: Record<
  string,
  { name: string; detail: string; icon: string }
> = {
  instagram: { name: "Instagram", detail: "Reels", icon: "◎" },
  youtube: { name: "YouTube", detail: "Shorts", icon: "▷" },
  tiktok: { name: "TikTok", detail: "Short videos", icon: "♪" },
  snapchat: { name: "Snapchat", detail: "Spotlight", icon: "♧" },
};

const DEFAULT_WEEK: DayUsage[] = [
  { day: "Sat", videos: 4 },
  { day: "Sun", videos: 5 },
  { day: "Mon", videos: 3 },
  { day: "Tue", videos: 6 },
  { day: "Wed", videos: 4 },
  { day: "Thu", videos: 4 },
  { day: "Today", videos: 0 },
];

export default function UsageReview({
  selectedApps = ["instagram", "youtube"],
  platformCounts = {},
  todayCount = 0,
  dailyLimit = 7,
  weekUsage = DEFAULT_WEEK,
  onBack,
  onStartScrolling,
  onAddReminder,
}: Props): React.JSX.Element {
  const remaining = Math.max(0, dailyLimit - todayCount);
  const progress = dailyLimit > 0 ? Math.min(todayCount / dailyLimit, 1) : 0;
  const maxWeekUsage = Math.max(1, ...weekUsage.map((item) => item.videos));

  const appsToShow = selectedApps
    .map((id) => ({ id, ...APP_INFO[id] }))
    .filter((app) => app.name);

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
              hitSlop={10}
              style={styles.backButton}
            >
              <Text style={styles.backArrow}>‹</Text>
            </Pressable>

            <View style={styles.brand}>
              <Text style={styles.brandMark}>◷</Text>
              <Text style={styles.brandName}>finite</Text>
            </View>
          </View>

          <View style={styles.titleRow}>
            <Text style={styles.title}>Your usage</Text>
            <Text style={styles.subtitle}>· short videos</Text>
          </View>

          <View style={styles.startCard}>
            <Text style={styles.startHint}>
              Press <Text style={styles.startHintStrong}>Start scrolling</Text>{" "}
              before you begin.
            </Text>

            <Pressable
              accessibilityRole="button"
              onPress={onStartScrolling}
              style={({ pressed }) => [
                styles.startButton,
                pressed && styles.pressed,
              ]}
            >
              <Text style={styles.startButtonText}>Start scrolling</Text>
            </Pressable>
          </View>

          <View style={styles.summaryCard}>
            <View style={styles.summaryTop}>
              <View style={styles.countRow}>
                <Text style={styles.todayCount}>{todayCount}</Text>
                <Text style={styles.countUnit}>videos</Text>
              </View>

              <View style={styles.goalInfo}>
                <Text style={styles.todayLabel}>Today</Text>
                <Text style={styles.goalText}>
                  of your {dailyLimit} video daily goal
                </Text>
              </View>
            </View>

            <View style={styles.progressTrack}>
              <View
                style={[styles.progressFill, { width: `${progress * 100}%` }]}
              />
            </View>

            <Text style={styles.remainingText}>
              {remaining} videos left in today’s goal
            </Text>
          </View>

          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>Today by platform</Text>
            <Text style={styles.sectionNote}>Short videos only</Text>
          </View>

          <View style={styles.platformList}>
            {appsToShow.map((app) => {
              const count = platformCounts[app.id];

              return (
                <View key={app.id} style={styles.platformRow}>
                  <View style={styles.platformName}>
                    <View style={styles.platformIcon}>
                      <Text style={styles.platformIconText}>{app.icon}</Text>
                    </View>
                    <Text style={styles.platformLabel}>
                      {app.name} {app.detail}
                    </Text>
                  </View>

                  <Text style={styles.platformCount}>
                    {count === undefined ? "Not tracked yet" : `${count} videos`}
                  </Text>
                </View>
              );
            })}

            {appsToShow.length === 0 && (
              <Text style={styles.emptyText}>
                Choose the apps you use to see them here.
              </Text>
            )}
          </View>

          <View style={styles.weekHeader}>
            <Text style={styles.sectionTitle}>Past 7 days</Text>
            <Text style={styles.sectionNote}>Videos per day</Text>
          </View>

          <Text style={styles.chartCaption}>{dailyLimit} video goal</Text>

          <View style={styles.chart}>
            {weekUsage.map((item, index) => {
              const height =
                item.videos === 0
                  ? 3
                  : Math.max(8, (item.videos / maxWeekUsage) * 42);

              return (
                <View key={`${item.day}-${index}`} style={styles.chartColumn}>
                  <Text style={styles.barValue}>{item.videos}</Text>
                  <View style={styles.barArea}>
                    <View
                      style={[
                        styles.bar,
                        { height },
                        item.day === "Today" && styles.todayBar,
                      ]}
                    />
                  </View>
                  <Text
                    style={[
                      styles.dayLabel,
                      item.day === "Today" && styles.todayDayLabel,
                    ]}
                  >
                    {item.day}
                  </Text>
                </View>
              );
            })}
          </View>

          <View style={styles.bottom}>
            <Pressable
              accessibilityRole="button"
              onPress={onAddReminder}
              style={({ pressed }) => [
                styles.reminderButton,
                pressed && styles.pressed,
              ]}
            >
              <Text style={styles.reminderButtonText}>Add a reminder ♧</Text>
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
    paddingHorizontal: 15,
    paddingTop: 8,
    paddingBottom: 13,
  },
  topBar: {
    minHeight: 29,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  backButton: {
    width: 25,
    height: 29,
    justifyContent: "center",
  },
  backArrow: {
    color: finiteColors.text,
    fontSize: 24,
    lineHeight: 27,
  },
  brand: {
    flexDirection: "row",
    alignItems: "center",
    gap: 3,
  },
  brandMark: {
    color: finiteColors.text,
    fontSize: 16,
  },
  brandName: {
    color: finiteColors.text,
    fontSize: 13,
    fontWeight: "500",
  },
  titleRow: {
    flexDirection: "row",
    alignItems: "baseline",
    justifyContent: "space-between",
    marginTop: 4,
    marginBottom: 5,
  },
  title: {
    color: finiteColors.text,
    fontSize: 17,
    fontWeight: "600",
  },
  subtitle: {
    color: finiteColors.secondaryText,
    fontSize: 7,
  },
  startCard: {
    backgroundColor: "rgba(255,255,255,0.86)",
    borderRadius: 11,
    padding: 8,
  },
  startHint: {
    color: finiteColors.secondaryText,
    fontSize: 8,
    marginBottom: 5,
  },
  startHintStrong: {
    color: finiteColors.text,
    fontWeight: "600",
  },
  startButton: {
    minHeight: 22,
    borderRadius: 8,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#72A69F",
  },
  startButtonText: {
    color: "#FFFFFF",
    fontSize: 8,
    fontWeight: "500",
  },
  summaryCard: {
    backgroundColor: "rgba(255,255,255,0.84)",
    borderRadius: 11,
    padding: 9,
    marginTop: 8,
  },
  summaryTop: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  countRow: {
    flexDirection: "row",
    alignItems: "baseline",
  },
  todayCount: {
    color: finiteColors.text,
    fontSize: 26,
    lineHeight: 30,
    fontWeight: "500",
    marginRight: 4,
  },
  countUnit: {
    color: finiteColors.text,
    fontSize: 10,
  },
  goalInfo: {
    alignItems: "flex-end",
  },
  todayLabel: {
    color: "#72A69F",
    fontSize: 8,
    fontWeight: "500",
  },
  goalText: {
    color: finiteColors.secondaryText,
    fontSize: 7,
    marginTop: 2,
  },
  progressTrack: {
    height: 4,
    backgroundColor: "#D9E8E6",
    borderRadius: 99,
    overflow: "hidden",
    marginTop: 4,
  },
  progressFill: {
    height: "100%",
    backgroundColor: finiteColors.primary,
    borderRadius: 99,
  },
  remainingText: {
    color: finiteColors.text,
    fontSize: 8,
    fontWeight: "500",
    marginTop: 6,
  },
  sectionHeader: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginTop: 10,
    marginBottom: 4,
  },
  sectionTitle: {
    color: finiteColors.text,
    fontSize: 9,
    fontWeight: "600",
  },
  sectionNote: {
    color: "#84A6A4",
    fontSize: 7,
  },
  platformList: {
    gap: 3,
  },
  platformRow: {
    minHeight: 19,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  platformName: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
  },
  platformIcon: {
    width: 14,
    height: 14,
    borderWidth: 1,
    borderColor: "#87A7A5",
    borderRadius: 4,
    alignItems: "center",
    justifyContent: "center",
  },
  platformIconText: {
    color: finiteColors.primary,
    fontSize: 10,
    lineHeight: 12,
  },
  platformLabel: {
    color: finiteColors.text,
    fontSize: 8,
  },
  platformCount: {
    color: finiteColors.text,
    fontSize: 7,
  },
  emptyText: {
    color: finiteColors.secondaryText,
    fontSize: 8,
    paddingVertical: 5,
  },
  weekHeader: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginTop: 8,
  },
  chartCaption: {
    color: "#84A6A4",
    textAlign: "right",
    fontSize: 7,
    marginTop: 3,
  },
  chart: {
    height: 61,
    flexDirection: "row",
    justifyContent: "space-between",
    paddingHorizontal: 1,
    marginTop: 3,
  },
  chartColumn: {
    flex: 1,
    alignItems: "center",
  },
  barValue: {
    color: finiteColors.text,
    fontSize: 7,
    height: 10,
  },
  barArea: {
    height: 42,
    width: "100%",
    alignItems: "center",
    justifyContent: "flex-end",
  },
  bar: {
    width: 13,
    backgroundColor: "#9CC5BD",
    borderTopLeftRadius: 4,
    borderTopRightRadius: 4,
  },
  todayBar: {
    backgroundColor: "#83ACA7",
  },
  dayLabel: {
    color: finiteColors.secondaryText,
    fontSize: 6,
    marginTop: 2,
  },
  todayDayLabel: {
    color: finiteColors.text,
    fontWeight: "600",
  },
  bottom: {
    marginTop: "auto",
    paddingTop: 13,
  },
  reminderButton: {
    minHeight: 30,
    borderRadius: 9,
    backgroundColor: finiteColors.primary,
    alignItems: "center",
    justifyContent: "center",
  },
  reminderButtonText: {
    color: "#FFFFFF",
    fontSize: 8,
    fontWeight: "500",
  },
  pressed: {
    opacity: 0.78,
  },
});
