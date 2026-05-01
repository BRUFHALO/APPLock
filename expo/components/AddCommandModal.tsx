import React, { useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  TextInput,
  Modal,
  ScrollView,
  Alert,
} from "react-native";
import { X, Mic, ChevronDown, Check } from "lucide-react-native";
import { LinearGradient } from "expo-linear-gradient";
import { GlassCard } from "./GlassCard";
import { MicButton } from "./MicButton";
import { Colors } from "@/constants/colors";
import { HARDWARE_ACTIONS } from "@/constants/hardwareActions";
import type { HardwareActionType } from "@/types";

interface AddCommandModalProps {
  visible: boolean;
  onClose: () => void;
  onSave: (phrase: string, action: HardwareActionType) => void;
  isRecording: boolean;
  onStartRecording: () => void;
  onStopRecording: () => void;
  recordedPhrase: string;
}

export function AddCommandModal({
  visible,
  onClose,
  onSave,
  isRecording,
  onStartRecording,
  onStopRecording,
  recordedPhrase,
}: AddCommandModalProps) {
  const [phrase, setPhrase] = useState("");
  const [selectedAction, setSelectedAction] = useState<HardwareActionType | null>(null);
  const [showActionSelector, setShowActionSelector] = useState(false);

  const handleSave = () => {
    const finalPhrase = recordedPhrase || phrase;
    if (!finalPhrase.trim()) {
      Alert.alert("Error", "Por favor graba o escribe una frase de comando");
      return;
    }
    if (!selectedAction) {
      Alert.alert("Error", "Por favor selecciona una acción");
      return;
    }

    onSave(finalPhrase, selectedAction);
    setPhrase("");
    setSelectedAction(null);
  };

  const handleClose = () => {
    setPhrase("");
    setSelectedAction(null);
    onClose();
  };

  const selectedActionData = HARDWARE_ACTIONS.find(a => a.id === selectedAction);

  return (
    <Modal
      visible={visible}
      transparent
      animationType="slide"
      onRequestClose={handleClose}
    >
      <View style={styles.overlay}>
        <LinearGradient
          colors={[Colors.primary.darkBlue, "#1a2d4a"]}
          style={styles.gradient}
        >
          <View style={styles.header}>
            <Text style={styles.title}>Nuevo Comando</Text>
            <TouchableOpacity onPress={handleClose} style={styles.closeButton}>
              <X size={24} color={Colors.text.primary} />
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.content} showsVerticalScrollIndicator={false}>
            <Text style={styles.sectionTitle}>1. Graba tu frase</Text>
            
            <View style={styles.micContainer}>
              <MicButton
                isRecording={isRecording}
                mode="hold"
                onPressIn={onStartRecording}
                onPressOut={onStopRecording}
              />
              <Text style={styles.micHint}>
                {isRecording ? "Grabando... Suelta para detener" : "Mantén presionado para grabar"}
              </Text>
            </View>

            {(recordedPhrase || phrase) && (
              <GlassCard style={styles.phraseCard}>
                <Text style={styles.phraseLabel}>Frase reconocida:</Text>
                <Text style={styles.phraseText}>
                  "{recordedPhrase || phrase}"
                </Text>
              </GlassCard>
            )}

            <Text style={styles.orText}>o escribe manualmente:</Text>
            
            <TextInput
              style={styles.input}
              placeholder="Ej: ¡Acción!"
              placeholderTextColor={Colors.text.muted}
              value={phrase}
              onChangeText={setPhrase}
            />

            <Text style={styles.sectionTitle}>2. Selecciona la acción</Text>

            <TouchableOpacity
              style={styles.actionSelector}
              onPress={() => setShowActionSelector(!showActionSelector)}
            >
              <Text style={selectedActionData ? styles.actionSelected : styles.actionPlaceholder}>
                {selectedActionData ? selectedActionData.label : "Seleccionar acción..."}
              </Text>
              <ChevronDown size={20} color={Colors.text.secondary} />
            </TouchableOpacity>

            {showActionSelector && (
              <View style={styles.actionList}>
                {HARDWARE_ACTIONS.map((action) => (
                  <TouchableOpacity
                    key={action.id}
                    style={[
                      styles.actionItem,
                      selectedAction === action.id && styles.actionItemSelected,
                    ]}
                    onPress={() => {
                      setSelectedAction(action.id);
                      setShowActionSelector(false);
                    }}
                  >
                    <View>
                      <Text style={styles.actionItemLabel}>{action.label}</Text>
                      <Text style={styles.actionItemDesc}>{action.description}</Text>
                      {action.demoOnly && (
                        <Text style={styles.demoLabel}>Demo</Text>
                      )}
                    </View>
                    {selectedAction === action.id && (
                      <Check size={20} color={Colors.primary.emerald} />
                    )}
                  </TouchableOpacity>
                ))}
              </View>
            )}

            <View style={styles.spacer} />
          </ScrollView>

          <View style={styles.footer}>
            <TouchableOpacity style={styles.cancelButton} onPress={handleClose}>
              <Text style={styles.cancelText}>Cancelar</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.saveButton} onPress={handleSave}>
              <LinearGradient
                colors={[Colors.primary.emerald, "#7ab332"]}
                style={styles.saveGradient}
              >
                <Text style={styles.saveText}>Guardar Comando</Text>
              </LinearGradient>
            </TouchableOpacity>
          </View>
        </LinearGradient>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: "rgba(0, 0, 0, 0.5)",
    justifyContent: "flex-end",
  },
  gradient: {
    flex: 1,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    marginTop: 60,
  },
  header: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: Colors.glass.border,
  },
  title: {
    fontSize: 20,
    fontWeight: "700",
    color: Colors.text.primary,
  },
  closeButton: {
    padding: 4,
  },
  content: {
    flex: 1,
    padding: 20,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: "600",
    color: Colors.primary.emerald,
    marginBottom: 16,
  },
  micContainer: {
    alignItems: "center",
    marginBottom: 20,
  },
  micHint: {
    marginTop: 12,
    fontSize: 14,
    color: Colors.text.secondary,
  },
  phraseCard: {
    marginBottom: 20,
  },
  phraseLabel: {
    fontSize: 12,
    color: Colors.text.muted,
    marginBottom: 4,
  },
  phraseText: {
    fontSize: 18,
    fontWeight: "600",
    color: Colors.text.primary,
  },
  orText: {
    fontSize: 14,
    color: Colors.text.secondary,
    textAlign: "center",
    marginBottom: 12,
  },
  input: {
    backgroundColor: Colors.glass.white,
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 14,
    fontSize: 16,
    color: Colors.text.primary,
    marginBottom: 24,
    borderWidth: 1,
    borderColor: Colors.glass.border,
  },
  actionSelector: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    backgroundColor: Colors.glass.white,
    borderRadius: 12,
    paddingHorizontal: 16,
    paddingVertical: 14,
    borderWidth: 1,
    borderColor: Colors.glass.border,
  },
  actionSelected: {
    fontSize: 16,
    color: Colors.text.primary,
  },
  actionPlaceholder: {
    fontSize: 16,
    color: Colors.text.muted,
  },
  actionList: {
    backgroundColor: Colors.glass.white,
    borderRadius: 12,
    marginTop: 8,
    overflow: "hidden",
  },
  actionItem: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: Colors.glass.border,
  },
  actionItemSelected: {
    backgroundColor: "rgba(152, 202, 63, 0.1)",
  },
  actionItemLabel: {
    fontSize: 16,
    fontWeight: "600",
    color: Colors.text.primary,
    marginBottom: 2,
  },
  actionItemDesc: {
    fontSize: 12,
    color: Colors.text.secondary,
  },
  demoLabel: {
    fontSize: 10,
    fontWeight: "700",
    color: Colors.status.warning,
    marginTop: 4,
  },
  spacer: {
    height: 40,
  },
  footer: {
    flexDirection: "row",
    padding: 20,
    gap: 12,
    borderTopWidth: 1,
    borderTopColor: Colors.glass.border,
  },
  cancelButton: {
    flex: 1,
    paddingVertical: 14,
    borderRadius: 12,
    backgroundColor: Colors.glass.white,
    alignItems: "center",
  },
  cancelText: {
    fontSize: 16,
    fontWeight: "600",
    color: Colors.text.primary,
  },
  saveButton: {
    flex: 2,
    borderRadius: 12,
    overflow: "hidden",
  },
  saveGradient: {
    paddingVertical: 14,
    alignItems: "center",
  },
  saveText: {
    fontSize: 16,
    fontWeight: "700",
    color: Colors.primary.darkBlue,
  },
});