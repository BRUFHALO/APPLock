import React, { useState, useCallback } from "react";
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  SafeAreaView,
  StatusBar,
  Alert,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Mic, Plus, Settings, Camera, Volume2, Lock, Flashlight } from "lucide-react-native";
import { router } from "expo-router";
import { Colors } from "@/constants/colors";
import { HARDWARE_ACTIONS } from "@/constants/hardwareActions";
import { GlassCard } from "@/components/GlassCard";
import { CommandCard } from "@/components/CommandCard";
import { MicButton } from "@/components/MicButton";
import { WaveAnimation } from "@/components/WaveAnimation";
import { AddCommandModal } from "@/components/AddCommandModal";
import { useCommandStorage } from "@/hooks/useCommandStorage";
import { useVoiceController } from "@/hooks/useVoiceController";
import { useFuzzyMatcher } from "@/hooks/useFuzzyMatcher";
import { useHardwareControl } from "@/hooks/useHardwareControl";
import { capturePhotoFromVoice } from "./camera";
import type { HardwareActionType } from "@/types";

const getActionIcon = (action: HardwareActionType) => {
  switch (action) {
    case "camera_front":
    case "camera_back":
    case "take_photo":
      return Camera;
    case "volume_up":
    case "volume_down":
      return Volume2;
    case "screen_lock":
      return Lock;
    case "flashlight_on":
    case "flashlight_off":
      return Flashlight;
    default:
      return Mic;
  }
};

export default function Dashboard() {
  const [modalVisible, setModalVisible] = useState(false);
  const { commands, settings, saveCommand, deleteCommand, incrementUsage, isLoading } = useCommandStorage();
  const { findMatch } = useFuzzyMatcher(commands, settings.fuzzyThreshold);
  const { executeAction: executeHardwareAction } = useHardwareControl({
    enableHaptics: settings.enableHaptics,
    demoMode: settings.demoMode,
  });

  const handleRecordingComplete = useCallback((transcript: string) => {
    // Si el transcript está vacío (modo simulado), pedir al usuario que escriba el comando
    if (!transcript || transcript.trim() === "") {
      Alert.prompt(
        "Escribe tu comando",
        "El reconocimiento de voz está en modo simulado. Escribe el comando que quieres ejecutar:",
        [
          { text: "Cancelar", style: "cancel" },
          { 
            text: "Ejecutar", 
            onPress: (text?: string) => {
              if (text) {
                const match = findMatch(text);
                if (match) {
                  executeAction(match.command.action, match.command.id);
                } else {
                  Alert.alert(
                    "Comando no reconocido",
                    `"${text}" no coincide con ningún comando guardado.`,
                    [{ text: "OK" }]
                  );
                }
              }
            }
          }
        ],
        "plain-text",
        "bloquear"
      );
      return;
    }
    
    const match = findMatch(transcript);
    
    if (match) {
      Alert.alert(
        "¡Comando Reconocido!",
        `"${transcript}" coincide con "${match.command.phrase}"\n\nEjecutando: ${HARDWARE_ACTIONS.find(a => a.id === match.command.action)?.label}`,
        [{ text: "OK", onPress: () => executeAction(match.command.action, match.command.id) }]
      );
    } else {
      Alert.alert(
        "Comando no reconocido",
        `"${transcript}" no coincide con ningún comando guardado.`,
        [{ text: "OK" }]
      );
    }
  }, [findMatch, commands]);

  const { 
    isRecording, 
    startRecording, 
    stopRecording, 
    transcript: recordedPhrase,
    reset 
  } = useVoiceController({
    onRecordingComplete: handleRecordingComplete,
    enableHaptics: settings.enableHaptics,
  });

  const toggleRecording = useCallback(() => {
    if (isRecording) {
      stopRecording();
    } else {
      startRecording();
    }
  }, [isRecording, startRecording, stopRecording]);

  const executeAction = async (action: HardwareActionType, commandId?: string) => {
    if (commandId) {
      incrementUsage(commandId);
    }

    switch (action) {
      case "camera_front":
      case "camera_back":
        router.push({
          pathname: "/camera",
          params: { camera: action === "camera_front" ? "front" : "back" }
        });
        break;
      case "take_photo":
        // Intentar capturar foto si la pantalla de cámara está abierta
        const photoTaken = await capturePhotoFromVoice();
        if (!photoTaken) {
          Alert.alert(
            "Cámara no disponible",
            "Abre la pantalla de cámara primero para tomar fotos con comandos de voz.",
            [{ text: "OK" }]
          );
        }
        break;
      case "volume_up":
      case "volume_down":
      case "screen_lock":
      case "flashlight_on":
      case "flashlight_off":
        await executeHardwareAction(action);
        break;
    }
  };

  const handleSaveCommand = async (phrase: string, action: HardwareActionType) => {
    await saveCommand({ phrase: phrase.trim(), action });
    setModalVisible(false);
    reset();
    Alert.alert("¡Comando Guardado!", `"${phrase}" ha sido vinculado a la acción seleccionada.`);
  };

  const quickActions = HARDWARE_ACTIONS.slice(0, 4);

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="light-content" />
      <LinearGradient
        colors={[Colors.primary.darkBlue, "#0d1528", Colors.primary.darkBlue]}
        style={styles.gradient}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 1 }}
      >
        <ScrollView 
          style={styles.scrollView}
          contentContainerStyle={styles.content}
          showsVerticalScrollIndicator={false}
        >
          {/* Header */}
          <View style={styles.header}>
            <View>
              <Text style={styles.greeting}>Voice Control</Text>
              <Text style={styles.subtitle}>Control por voz 100% offline</Text>
            </View>
            <TouchableOpacity 
              style={styles.settingsButton}
              onPress={() => router.push("/settings")}
            >
              <Settings size={24} color={Colors.text.primary} />
            </TouchableOpacity>
          </View>

          {/* Voice Activation Section */}
          <GlassCard style={styles.voiceCard}>
            <View style={styles.voiceContainer}>
              <Text style={styles.voiceTitle}>Toca para hablar</Text>
              <Text style={styles.voiceSubtitle}>
                {isRecording ? "Escuchando tu comando..." : "Toca una vez para iniciar"}
              </Text>
              
              <View style={styles.micWrapper}>
                <WaveAnimation isActive={isRecording} />
                <MicButton
                  isRecording={isRecording}
                  mode="toggle"
                  onPress={toggleRecording}
                />
              </View>

              {isRecording && (
                <Text style={styles.listeningText}>
                  {recordedPhrase || "Escuchando..."}
                </Text>
              )}
            </View>
          </GlassCard>

          {/* Quick Actions */}
          <Text style={styles.sectionTitle}>Acciones Rápidas</Text>
          <View style={styles.quickActionsGrid}>
            {quickActions.map((action) => {
              const Icon = getActionIcon(action.id);
              return (
                <TouchableOpacity
                  key={action.id}
                  style={styles.quickActionButton}
                  onPress={() => executeAction(action.id)}
                >
                  <LinearGradient
                    colors={["rgba(152, 202, 63, 0.2)", "rgba(18, 31, 61, 0.3)"]}
                    style={styles.quickActionGradient}
                    start={{ x: 0, y: 0 }}
                    end={{ x: 1, y: 1 }}
                  >
                    <Icon size={24} color={Colors.primary.emerald} />
                    <Text style={styles.quickActionText}>{action.label}</Text>
                  </LinearGradient>
                </TouchableOpacity>
              );
            })}
          </View>

          {/* Saved Commands */}
          <View style={styles.commandsHeader}>
            <Text style={styles.sectionTitle}>Comandos Guardados</Text>
            <Text style={styles.commandsCount}>{commands.length}</Text>
          </View>

          {isLoading ? (
            <Text style={styles.loadingText}>Cargando...</Text>
          ) : commands.length === 0 ? (
            <GlassCard style={styles.emptyCard}>
              <Mic size={40} color={Colors.text.muted} />
              <Text style={styles.emptyTitle}>Sin comandos</Text>
              <Text style={styles.emptyText}>
                Crea tu primer comando de voz presionando el botón + abajo
              </Text>
            </GlassCard>
          ) : (
            <View style={styles.commandsList}>
              {commands.map((command) => (
                <CommandCard
                  key={command.id}
                  command={command}
                  onDelete={deleteCommand}
                  isDemo={HARDWARE_ACTIONS.find(a => a.id === command.action)?.demoOnly}
                />
              ))}
            </View>
          )}

          <View style={styles.bottomSpacer} />
        </ScrollView>

        {/* Floating Add Button */}
        <TouchableOpacity
          style={styles.fab}
          onPress={() => setModalVisible(true)}
          activeOpacity={0.9}
        >
          <LinearGradient
            colors={[Colors.primary.emerald, Colors.primary.emeraldDark]}
            style={styles.fabGradient}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
          >
            <Plus size={28} color={Colors.primary.darkBlue} strokeWidth={3} />
          </LinearGradient>
        </TouchableOpacity>

        {/* Add Command Modal */}
        <AddCommandModal
          visible={modalVisible}
          onClose={() => {
            setModalVisible(false);
            reset();
          }}
          onSave={handleSaveCommand}
          isRecording={isRecording}
          onStartRecording={startRecording}
          onStopRecording={stopRecording}
          recordedPhrase={recordedPhrase}
        />
      </LinearGradient>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.primary.darkBlue,
  },
  gradient: {
    flex: 1,
  },
  scrollView: {
    flex: 1,
  },
  content: {
    paddingHorizontal: 20,
    paddingTop: 20,
  },
  header: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "flex-start",
    marginBottom: 24,
  },
  greeting: {
    fontSize: 32,
    fontWeight: "800",
    color: Colors.text.primary,
    letterSpacing: -0.5,
  },
  subtitle: {
    fontSize: 14,
    color: Colors.text.secondary,
    marginTop: 4,
  },
  settingsButton: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: Colors.glass.white,
    justifyContent: "center",
    alignItems: "center",
    borderWidth: 1,
    borderColor: Colors.glass.border,
  },
  voiceCard: {
    marginBottom: 24,
  },
  voiceContainer: {
    alignItems: "center",
    paddingVertical: 24,
  },
  voiceTitle: {
    fontSize: 18,
    fontWeight: "600",
    color: Colors.text.primary,
    marginBottom: 8,
  },
  voiceSubtitle: {
    fontSize: 14,
    color: Colors.text.secondary,
    marginBottom: 24,
  },
  micWrapper: {
    width: 100,
    height: 100,
    justifyContent: "center",
    alignItems: "center",
    marginBottom: 16,
  },
  listeningText: {
    fontSize: 16,
    color: Colors.primary.emerald,
    fontWeight: "500",
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: "700",
    color: Colors.text.primary,
    marginBottom: 16,
  },
  quickActionsGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 12,
    marginBottom: 24,
  },
  quickActionButton: {
    width: "48%",
    aspectRatio: 1.5,
    borderRadius: 16,
    overflow: "hidden",
  },
  quickActionGradient: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    gap: 8,
    borderWidth: 1,
    borderColor: Colors.glass.border,
    borderRadius: 16,
  },
  quickActionText: {
    fontSize: 13,
    fontWeight: "600",
    color: Colors.text.primary,
  },
  commandsHeader: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    marginBottom: 16,
  },
  commandsCount: {
    fontSize: 14,
    fontWeight: "600",
    color: Colors.primary.emerald,
    backgroundColor: "rgba(152, 202, 63, 0.15)",
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
  },
  loadingText: {
    color: Colors.text.secondary,
    textAlign: "center",
    paddingVertical: 20,
  },
  emptyCard: {
    alignItems: "center",
    paddingVertical: 40,
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: "600",
    color: Colors.text.primary,
    marginTop: 16,
    marginBottom: 8,
  },
  emptyText: {
    fontSize: 14,
    color: Colors.text.secondary,
    textAlign: "center",
    paddingHorizontal: 20,
  },
  commandsList: {
    gap: 12,
  },
  bottomSpacer: {
    height: 100,
  },
  fab: {
    position: "absolute",
    right: 20,
    bottom: 30,
    width: 60,
    height: 60,
    borderRadius: 30,
    overflow: "hidden",
    shadowColor: Colors.primary.emerald,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
  },
  fabGradient: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
  },
});