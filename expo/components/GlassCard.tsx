import React from "react";
import { View, StyleSheet, ViewStyle } from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Colors } from "@/constants/colors";

interface GlassCardProps {
  children: React.ReactNode;
  style?: ViewStyle;
  intensity?: "light" | "medium" | "strong";
}

export function GlassCard({ children, style, intensity = "medium" }: GlassCardProps) {
  const getBackgroundColor = () => {
    switch (intensity) {
      case "light":
        return Colors.glass.white;
      case "strong":
        return Colors.glass.whiteStrong;
      default:
        return Colors.glass.white;
    }
  };

  return (
    <View style={[styles.container, style]}>
      <LinearGradient
        colors={[getBackgroundColor(), getBackgroundColor()]}
        style={styles.gradient}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 1 }}
      >
        <View style={styles.content}>
          {children}
        </View>
      </LinearGradient>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    borderRadius: 20,
    overflow: "hidden",
    borderWidth: 1,
    borderColor: Colors.glass.border,
  },
  gradient: {
    flex: 1,
  },
  content: {
    padding: 20,
  },
});