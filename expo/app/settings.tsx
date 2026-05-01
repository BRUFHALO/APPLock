import React from "react";
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Switch,
  SafeAreaView,
  Alert,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Trash2, Info, ChevronRight } from "lucide-react-native";
import { Colors } from "@/constants/colors";
import { GlassCard } from "@/components/GlassCard";
import { useCommandStorage } from "@/hooks/useCommandStorage";

export default function SettingsScreen() {
  const { commands, settings, updateSettings, deleteCommand } = useCommandStorage();

  const handleDeleteAll = () => {
    Alert.alert(
      "Eliminar todos los comandos",
      "¿Estás seguro de que quieres eliminar todos los comandos guardados? Esta acción no se puede deshacer.",
      [
        { text: "Cancelar", style: "cancel" },
        { 
          text: "Eliminar", 
          style: "destructive",
          onPress: async () => {
            for (const cmd of commands) {
              await deleteCommand(cmd.id);
            }
            Alert.alert("Comandos eliminados", "Todos los comandos han sido eliminados.");
          }
        },
      ]
    );
  };

  const settingItems = [
    {
      id: "enableHaptics",
      title: "Vibración",
      description: "Vibrar al grabar y ejecutar comandos",
      value: settings.enableHaptics,
    },
    {
      id: "demoMode",
      title: "Modo Demo",
      description: "Mostrar funciones de hardware en modo simulación",
      value: settings.demoMode,
    },
  ];

  return (
    <SafeAreaView style={styles.container}>
      <LinearGradient
        colors={[Colors.primary.darkBlue, "#0d1528", Colors.primary.darkBlue]}
        style={styles.gradient}
      >
        <ScrollView style={styles.scrollView} contentContainerStyle={styles.content}>
          <Text style={styles.sectionTitle}>Preferencias</Text>

          <GlassCard style={styles.settingsCard}>
            {settingItems.map((item, index) => (
              <View 
                key={item.id}
                style={[
                  styles.settingItem,
                  index !== settingItems.length - 1 && styles.settingItemBorder
                ]}
              >
                <View style={styles.settingInfo}>
                  <Text style={styles.settingTitle}>{item.title}</Text>
                  <Text style={styles.settingDescription}>{item.description}</Text>
                </View>
                <Switch
                  value={item.value}
                  onValueChange={(value) => 
                    updateSettings({ [item.id]: value })
                  }
                  trackColor={{ false: Colors.glass.white, true: Colors.primary.emerald }}
                  thumbColor={item.value ? Colors.text.primary : "#f4f3f4"}
                />
              </View>
            ))}
          </GlassCard>

          <Text style={styles.sectionTitle}>Información</Text>

          <GlassCard style={styles.infoCard}>
            <View style={styles.infoItem}>
              <Info size={20} color={Colors.text.secondary} />
              <View style={styles.infoTextContainer}>
                <Text style={styles.infoTitle}>Funcionamiento Offline</Text>
                <Text style={styles.infoDescription}>
                  Esta app funciona completamente sin conexión a internet. Los comandos de voz 
                  se procesan localmente en tu dispositivo.
                </Text>
              </View>
            </View>
          </GlassCard>

          <Text style={styles.sectionTitle}>Comandos</Text>

          <TouchableOpacity style={styles.deleteButton} onPress={handleDeleteAll}>
            <LinearGradient
              colors={["rgba(255, 107, 107, 0.2)", "rgba(255, 107, 107, 0.1)"]}
              style={styles.deleteGradient}
            >
              <Trash2 size={20} color={Colors.status.error} />
              <Text style={styles.deleteText}>Eliminar todos los comandos</Text>
            </LinearGradient>
          </TouchableOpacity>

          <Text style={styles.statsText}>
            Total de comandos guardados: {commands.length}
          </Text>

          <View style={styles.bottomSpacer} />
        </ScrollView>
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
    padding: 20,
  },
  sectionTitle: {
    fontSize: 14,
    fontWeight: "600",
    color: Colors.text.secondary,
    textTransform: "uppercase",
    letterSpacing: 1,
    marginTop: 24,
    marginBottom: 12,
  },
  settingsCard: {
    overflow: "hidden",
  },
  settingItem: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingVertical: 16,
  },
  settingItemBorder: {
    borderBottomWidth: 1,
    borderBottomColor: Colors.glass.border,
  },
  settingInfo: {
    flex: 1,
    marginRight: 16,
  },
  settingTitle: {
    fontSize: 16,
    fontWeight: "600",
    color: Colors.text.primary,
    marginBottom: 4,
  },
  settingDescription: {
    fontSize: 13,
    color: Colors.text.secondary,
  },
  infoCard: {
    padding: 16,
  },
  infoItem: {
    flexDirection: "row",
    gap: 12,
  },
  infoTextContainer: {
    flex: 1,
  },
  infoTitle: {
    fontSize: 15,
    fontWeight: "600",
    color: Colors.text.primary,
    marginBottom: 6,
  },
  infoDescription: {
    fontSize: 13,
    color: Colors.text.secondary,
    lineHeight: 20,
  },
  deleteButton: {
    borderRadius: 12,
    overflow: "hidden",
    marginTop: 8,
  },
  deleteGradient: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 10,
    paddingVertical: 16,
    borderWidth: 1,
    borderColor: "rgba(255, 107, 107, 0.3)",
    borderRadius: 12,
  },
  deleteText: {
    fontSize: 16,
    fontWeight: "600",
    color: Colors.status.error,
  },
  statsText: {
    fontSize: 13,
    color: Colors.text.muted,
    textAlign: "center",
    marginTop: 20,
  },
  bottomSpacer: {
    height: 40,
  },
});