import AsyncStorage from "@react-native-async-storage/async-storage";
import { useEffect, useState, useCallback } from "react";
import type { VoiceCommand, AppSettings } from "@/types";

const COMMANDS_KEY = "@voice_commands";
const SETTINGS_KEY = "@app_settings";

const DEFAULT_SETTINGS: AppSettings = {
  fuzzyThreshold: 0.6,
  enableHaptics: true,
  enableSounds: true,
  demoMode: true,
};

export function useCommandStorage() {
  const [commands, setCommands] = useState<VoiceCommand[]>([]);
  const [settings, setSettings] = useState<AppSettings>(DEFAULT_SETTINGS);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [commandsJson, settingsJson] = await Promise.all([
        AsyncStorage.getItem(COMMANDS_KEY),
        AsyncStorage.getItem(SETTINGS_KEY),
      ]);

      let loadedCommands: VoiceCommand[] = [];
      
      if (commandsJson) {
        loadedCommands = JSON.parse(commandsJson);
      }

      // Agregar comando predeterminado "bloquear" si no existe
      if (!loadedCommands.some(cmd => cmd.phrase.toLowerCase() === "bloquear")) {
        const defaultCommand: VoiceCommand = {
          id: "default-lock",
          phrase: "bloquear",
          action: "screen_lock",
          createdAt: Date.now(),
          usageCount: 0
        };
        
        loadedCommands.unshift(defaultCommand);
        await AsyncStorage.setItem(COMMANDS_KEY, JSON.stringify(loadedCommands));
      }

      setCommands(loadedCommands);

      if (settingsJson) {
        setSettings({ ...DEFAULT_SETTINGS, ...JSON.parse(settingsJson) });
      }
    } catch (error) {
      console.error("Error loading data:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const saveCommand = useCallback(async (command: Omit<VoiceCommand, "id" | "createdAt" | "usageCount">) => {
    const newCommand: VoiceCommand = {
      ...command,
      id: Date.now().toString(),
      createdAt: Date.now(),
      usageCount: 0,
    };

    const updatedCommands = [...commands, newCommand];
    await AsyncStorage.setItem(COMMANDS_KEY, JSON.stringify(updatedCommands));
    setCommands(updatedCommands);
    return newCommand;
  }, [commands]);

  const deleteCommand = useCallback(async (id: string) => {
    const updatedCommands = commands.filter(cmd => cmd.id !== id);
    await AsyncStorage.setItem(COMMANDS_KEY, JSON.stringify(updatedCommands));
    setCommands(updatedCommands);
  }, [commands]);

  const incrementUsage = useCallback(async (id: string) => {
    const updatedCommands = commands.map(cmd =>
      cmd.id === id ? { ...cmd, usageCount: cmd.usageCount + 1 } : cmd
    );
    await AsyncStorage.setItem(COMMANDS_KEY, JSON.stringify(updatedCommands));
    setCommands(updatedCommands);
  }, [commands]);

  const updateSettings = useCallback(async (newSettings: Partial<AppSettings>) => {
    const updatedSettings = { ...settings, ...newSettings };
    await AsyncStorage.setItem(SETTINGS_KEY, JSON.stringify(updatedSettings));
    setSettings(updatedSettings);
  }, [settings]);

  return {
    commands,
    settings,
    isLoading,
    saveCommand,
    deleteCommand,
    incrementUsage,
    updateSettings,
  };
}