import { StyleSheet } from "react-native";

export const finiteColors = {
  backgroundTop: "#B0D6DD",
  backgroundMiddle: "#BADBCC",
  backgroundBottom: "#C3DDC5",
  primary: "#5B7A8C",
  text: "#162B39",
  secondaryText: "#607684",
  surface: "#FFFFFF",
  divider: "#DCE8E5",
  progressTrack: "#EAF0EE",
} as const;

export const finiteGradientColors = [
  finiteColors.backgroundTop,
  finiteColors.backgroundMiddle,
  finiteColors.backgroundBottom,
] as const;

export const styles = StyleSheet.create({
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
    color: finiteColors.text,
    fontSize: 20,
    fontWeight: "600",
  },
  logoMark: {
    color: finiteColors.primary,
  },
  eyebrow: {
    color: finiteColors.secondaryText,
    fontSize: 12,
    fontWeight: "600",
    letterSpacing: 1,
    marginBottom: 9,
  },
  title: {
    color: finiteColors.text,
    fontSize: 30,
    fontWeight: "600",
    letterSpacing: -0.5,
  },
  intro: {
    color: finiteColors.secondaryText,
    fontSize: 15,
    lineHeight: 22,
    marginTop: 10,
    marginBottom: 25,
  },
  usageCard: {
    backgroundColor: finiteColors.surface,
    borderRadius: 18,
    padding: 22,
    shadowColor: "#22414B",
    shadowOpacity: 0.08,
    shadowRadius: 14,
    shadowOffset: { width: 0, height: 6 },
    elevation: 3,
  },
  cardLabel: {
    color: finiteColors.secondaryText,
    fontSize: 14,
    marginBottom: 12,
  },
  countRow: {
    flexDirection: "row",
    alignItems: "baseline",
    marginBottom: 17,
  },
  count: {
    color: finiteColors.text,
    fontSize: 42,
    fontWeight: "700",
    marginRight: 8,
  },
  countTotal: {
    color: finiteColors.secondaryText,
    fontSize: 16,
  },
  progressTrack: {
    height: 9,
    backgroundColor: finiteColors.progressTrack,
    borderRadius: 999,
    overflow: "hidden",
  },
  progressFill: {
    height: "100%",
    backgroundColor: finiteColors.primary,
    borderRadius: 999,
  },
  cardFooter: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginTop: 12,
  },
  footerText: {
    color: finiteColors.secondaryText,
    fontSize: 13,
  },
  actions: {
    marginTop: 24,
  },
  primaryButton: {
    minHeight: 50,
    backgroundColor: finiteColors.primary,
    borderRadius: 13,
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 12,
  },
  primaryButtonText: {
    color: finiteColors.surface,
    fontSize: 15,
    fontWeight: "600",
  },
  secondaryButton: {
    minHeight: 50,
    backgroundColor: finiteColors.surface,
    borderRadius: 13,
    alignItems: "center",
    justifyContent: "center",
  },
  secondaryButtonText: {
    color: finiteColors.text,
    fontSize: 15,
    fontWeight: "600",
  },
  buttonPressed: {
    opacity: 0.8,
  },
  note: {
    color: finiteColors.secondaryText,
    fontSize: 13,
    lineHeight: 19,
    textAlign: "center",
    marginTop: 20,
    paddingHorizontal: 10,
  },
});