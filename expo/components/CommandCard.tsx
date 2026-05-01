import React from "react";
import { View, Text, StyleSheet, TouchableOpacity } from "react-native";
import { Mic, Trash2, Camera, Volume2, Volume1, Lock, Flashlight, FlashlightOff } from "lucide-react-native";
import { GlassCard } from "./GlassCard";
import { Colors } from "@/constants/colors";
import type { VoiceCommand, HardwareActionType } from "@/types";

interface CommandCardProps {
  command: VoiceCommand;
  onDelete?: (id: string) => void;
  isDemo?: boolean;
}

const getActionIcon = (action: HardwareActionType) => {
  switch (action) {
    case "camera_front":
    case "camera_back":
      return Camera;
    case "volume_up":
      return Volume2;
    case "volume_down":
      return Volume1;
    case "screen_lock":
      return Lock;
    case "flashlight_on":
      return Flashlight;
    case "flashlight_off":
      return FlashlightOff;
    default:
      return Camera;
  }
};

const getActionLabel = (action: HardwareActionType) => {
  switch (action) {
    case "camera_front":
      return "Cámara Frontal";
    case "camera_back":
      return "Cámara Trasera";
    case "volume_up":
      return "Subir Volumen";
    case "volume_down":
      return "Bajar Volumen";
    case "screen_lock":
      return "Bloquear";
    case "flashlight_on":
      return "Linterna ON";
    case "flashlight_off":
      return "Linterna OFF";
    default:
      return action;
  }
};

export function CommandCard({ command, onDelete, isDemo }: CommandCardProps) {
  const Icon = getActionIcon(command.action);

  return (
    <GlassCard style={styles.container}>
      <View style={styles.content}>
        <View style={styles.iconContainer}>
          <Mic size={20} color={Colors.primary.emerald} />
        </View>
        
        <View style={styles.textContainer}>
          <Text style={styles.phrase}>"{command.phrase}"</Text>
          <View style={styles.actionRow}>
            <Icon size={14} color={Colors.text.secondary} />
            <Text style={styles.action}>{getActionLabel(command.action)}</Text>
            {isDemo && (
              <View style={styles.demoBadge}>
                <Text style={styles.demoText}>DEMO</Text>
              </View>
            )}
          </View>
        </View>

        {onDelete && (
          <TouchableOpacity
            style={styles.deleteButton}
            onPress={() => onDelete(command.id)}
            testID={`delete-command-${command.id}`}
          >
            <Trash2 size={18} color={Colors.status.error} />
          </TouchableOpacity>
        )}
      </View>
      
      {command.usageCount > 0 && (
        <Text style={styles.usageText}>
          Usado {command.usageCount} {command.usageCount === 1 ? "vez" : "veces"}
        </Text>
      )}
    </GlassCard>
  );
}

const styles = StyleSheet.create({
  container: {
    marginBottom: 12,
  },
  content: {
    flexDirection: "row",
    alignItems: "center",
  },
  iconContainer: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: "rgba(152, 202, 63, 0.15)",
    justifyContent: "center",
    alignItems: "center",
    marginRight: 12,
  },
  textContainer: {
    flex: 1,
  },
  phrase: {
    fontSize: 16,
    fontWeight: "600",
    color: Colors.text.primary,
    marginBottom: 4,
  },
  actionRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
  },
  action: {
    fontSize: 13,
    color: Colors.text.secondary,
  },
  demoBadge: {
    backgroundColor: "rgba(255, 184, 0, 0.2)",
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  demoText: {
    fontSize: 10,
    fontWeight: "700",
    color: Colors.status.warning,
  },
  deleteButton: {
    padding: 8,
    marginLeft: 8,
  },
  usageText: {
    fontSize: 11,
    color: Colors.text.muted,
    marginTop: 8,
    marginLeft: 56,
  },
});