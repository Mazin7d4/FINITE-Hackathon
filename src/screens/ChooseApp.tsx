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

const APPS = [
  { id: "instagram", name: "Instagram", detail: "Reels", icon: "◎" },
  { id: "youtube", name: "YouTube", detail: "Shorts", icon: "▷" },
  { id: "tiktok", name: "TikTok", detail: "Short videos", icon: "♪" },
  { id: "snapchat", name: "Snapchat", detail: "Spotlight", icon: "♧" },
];

type Props = {
  initialSelectedApps?: string[];
  onContinue?: (selectedApps: string[]) => void;
};

export default function ChooseAppsScreen({ initialSelectedApps, onContinue }: Props) {
  const [selectedApps, setSelectedApps] = useState<string[]>(
    initialSelectedApps ?? APPS.map((app) => app.id)
  );

  function toggleApp(appId: string) {
    setSelectedApps((current) =>
      current.includes(appId)
        ? current.filter((id) => id !== appId)
        : [...current, appId]
    );
  }

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
          <View style={styles.brand}>
            <Text style={styles.clockMark}>◷</Text>
            <Text style={styles.brandName}>finite</Text>
          </View>

          <View style={styles.heading}>
            <Text style={styles.title}>
              Where do you{"\n"}watch short videos?
            </Text>
            <Text style={styles.description}>
              Let’s start with the apps you use. A little awareness goes a long
              way.
            </Text>
          </View>

          <View style={styles.appList}>
            {APPS.map((app) => {
              const selected = selectedApps.includes(app.id);

              return (
                <Pressable
                  key={app.id}
                  accessibilityRole="checkbox"
                  accessibilityState={{ checked: selected }}
                  accessibilityLabel={`Select ${app.name}`}
                  onPress={() => toggleApp(app.id)}
                  style={({ pressed }) => [
                    styles.appRow,
                    pressed && styles.pressed,
                  ]}
                >
                  <View style={styles.appIcon}>
                    <Text style={styles.appIconText}>{app.icon}</Text>
                  </View>

                  <View style={styles.appCopy}>
                    <Text style={styles.appName}>{app.name}</Text>
                    <Text style={styles.appDetail}>{app.detail}</Text>
                  </View>

                  <View
                    style={[
                      styles.checkbox,
                      selected && styles.checkboxSelected,
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
              onPress={() => onContinue?.(selectedApps)}
              style={({ pressed }) => [
                styles.continueButton,
                pressed && styles.pressed,
              ]}
            >
              <Text style={styles.continueText}>Continue</Text>
              <Text style={styles.arrow}>→</Text>
            </Pressable>

            <Text style={styles.note}>You can change these anytime.</Text>
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
    paddingTop: 18,
    paddingBottom: 16,
  },
  brand: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 30,
  },
  clockMark: {
    color: finiteColors.primary,
    fontSize: 20,
    marginRight: 5,
  },
  brandName: {
    color: finiteColors.text,
    fontSize: 18,
    fontWeight: "600",
  },
  heading: {
    marginBottom: 20,
  },
  title: {
    color: finiteColors.text,
    fontSize: 25,
    lineHeight: 29,
    fontWeight: "600",
  },
  description: {
    color: finiteColors.secondaryText,
    fontSize: 12,
    lineHeight: 17,
    marginTop: 8,
  },
  appList: {
    gap: 9,
  },
  appRow: {
    minHeight: 56,
    backgroundColor: "rgba(255,255,255,0.88)",
    borderRadius: 12,
    paddingHorizontal: 12,
    flexDirection: "row",
    alignItems: "center",
  },
  appIcon: {
    width: 24,
    alignItems: "center",
    marginRight: 10,
  },
  appIconText: {
    color: finiteColors.text,
    fontSize: 19,
    fontWeight: "600",
  },
  appCopy: {
    flex: 1,
  },
  appName: {
    color: finiteColors.text,
    fontSize: 13,
    fontWeight: "500",
  },
  appDetail: {
    color: finiteColors.secondaryText,
    fontSize: 10,
    marginTop: 2,
  },
  checkbox: {
    width: 24,
    height: 24,
    borderWidth: 1.5,
    borderColor: finiteColors.divider,
    borderRadius: 5,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: finiteColors.surface,
  },
  checkboxSelected: {
    backgroundColor: finiteColors.primary,
    borderColor: finiteColors.primary,
  },
  checkmark: {
    color: finiteColors.surface,
    fontSize: 17,
    fontWeight: "700",
    lineHeight: 20,
  },
  bottom: {
    marginTop: "auto",
    paddingTop: 28,
  },
  continueButton: {
    minHeight: 48,
    backgroundColor: finiteColors.primary,
    borderRadius: 12,
    paddingHorizontal: 16,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  continueText: {
    color: finiteColors.surface,
    fontSize: 14,
    fontWeight: "600",
  },
  arrow: {
    color: finiteColors.surface,
    fontSize: 18,
  },
  note: {
    color: finiteColors.secondaryText,
    fontSize: 10,
    textAlign: "center",
    marginTop: 10,
  },
  pressed: {
    opacity: 0.78,
  },
});
