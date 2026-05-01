import React from "react";
import { View, Text, StyleSheet, TouchableOpacity } from "react-native";
import { Mic, Camera, Check, AlertCircle } from "lucide-react-native";
import { GlassCard } from "./GlassCard";
import { Colors } from "@/constants/colors";
import type { PermissionStatus } from "@/types";

interface PermissionCardProps {
  microphone: PermissionStatus;
  camera: PermissionStatus;
  onRequestPermissions: () => void;
}

export function PermissionCard({ microphone, camera, onRequestPermissions }: PermissionCardProps) {
  const allGranted = microphone === "granted" && camera === "granted";

  if (allGranted) return null;

  return (
    <GlassCard style={styles.container} intensity="strong">
      <View style={styles.header}>
        <AlertCircle size={24} color={Colors.status.warning} />
        <Text style={styles.title}>Permisos Requeridos</Text>
      </View>
      
      <Text style={styles.description}>
        La app necesita acceso al micrófono y cámara para funcionar correctamente.
      </Text>

      <View style={styles.permissionList}>
        <View style={styles.permissionItem}>
          <View style={styles.permissionIcon}>
            <Mic size={20} color={Colors.text.primary} />
          </View>
          <Text style={styles.permissionText}>Micrófono</Text>
          {microphone === "granted" ? (
            <Check size={20} color={Colors.status.success} />
          ) : (
            <View style={styles.badgePending} />
          )}
        </View>

        <View style={styles.permissionItem}>
          <View style={styles.permissionIcon}>
            <Camera size={20} color={Colors.text.primary} />
          </View>
          <Text style={styles.permissionText}>Cámara</Text>
          {camera === "granted" ? (
            <Check size={20} color={Colors.status.success} />
          ) : (
            <View style={styles.badgePending} />
          )}
        </View>
      </View>

      <TouchableOpacity style={styles.button} onPress={onRequestPermissions}>
        <Text style={styles.buttonText}>Conceder Permisos</Text>
      </TouchableOpacity>
    </GlassCard>
  );
}

const styles = StyleSheet.create({
  container: {
    marginHorizontal: 20,
    marginBottom: 20,
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
    marginBottom: 12,
  },
  title: {
    fontSize: 18,
    fontWeight: "700",
    color: Colors.text.primary,
  },
  description: {
    fontSize: 14,
    color: Colors.text.secondary,
    marginBottom: 16,
    lineHeight: 20,
  },
  permissionList: {
    gap: 12,
    marginBottom: 16,
  },
  permissionItem: {
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
  },
  permissionIcon: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: Colors.glass.white,
    justifyContent: "center",
    alignItems: "center",
  },
  permissionText: {
    flex: 1,
    fontSize: 16,
    color: Colors.text.primary,
  },
  badgePending: {
    width: 12,
    height: 12,
    borderRadius: 6,
    backgroundColor: Colors.status.warning,
  },
  button: {
    backgroundColor: Colors.primary.emerald,
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: "center",
  },
  buttonText: {
    fontSize: 16,
    fontWeight: "600",
    color: Colors.primary.darkBlue,
  },
});