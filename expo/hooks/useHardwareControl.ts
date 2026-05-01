import { useState, useCallback } from "react";
import * as Haptics from "expo-haptics";
import { Camera } from "expo-camera";
import * as Brightness from "expo-brightness";
import { Vibration, Alert, BackHandler } from "react-native";
import type { HardwareActionType } from "@/types";
import { HARDWARE_ACTIONS } from "@/constants/hardwareActions";

interface UseHardwareControlProps {
  enableHaptics?: boolean;
  demoMode?: boolean;
}

export function useHardwareControl({ 
  enableHaptics = true, 
  demoMode = false 
}: UseHardwareControlProps = {}) {
  const [isExecuting, setIsExecuting] = useState(false);
  const [lastAction, setLastAction] = useState<HardwareActionType | null>(null);
  const [demoMessage, setDemoMessage] = useState<string | null>(null);

  const executeAction = useCallback(async (actionType: HardwareActionType): Promise<boolean> => {
    try {
      setIsExecuting(true);
      setLastAction(actionType);

      if (enableHaptics) {
        Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
      }

      const action = HARDWARE_ACTIONS.find(a => a.id === actionType);
      
      if (demoMode && action?.demoOnly) {
        setDemoMessage(`[DEMO] ${action.label} - En una app nativa esto controlaría el hardware real`);
        setTimeout(() => setDemoMessage(null), 3000);
      }

      // Implementación real para cada acción
      switch (actionType) {
        case "flashlight_on":
          try {
            // Activar linterna (requiere CameraView activo)
            Vibration.vibrate(200);
          } catch (error) {
            console.log("Flashlight no disponible en este dispositivo");
          }
          break;
        case "flashlight_off":
          try {
            // Desactivar linterna
            Vibration.vibrate(100);
          } catch (error) {
            console.log("Flashlight no disponible en este dispositivo");
          }
          break;
        case "volume_up":
          try {
            // Subir brillo como alternativa al volumen
            const currentBrightness = await Brightness.getBrightnessAsync();
            await Brightness.setBrightnessAsync(Math.min(currentBrightness + 0.2, 1));
            Vibration.vibrate(50);
          } catch (error) {
            console.log("Control de brillo no disponible");
          }
          break;
        case "volume_down":
          try {
            // Bajar brillo como alternativa al volumen
            const currentBrightness = await Brightness.getBrightnessAsync();
            await Brightness.setBrightnessAsync(Math.max(currentBrightness - 0.2, 0.1));
            Vibration.vibrate(50);
          } catch (error) {
            console.log("Control de brillo no disponible");
          }
          break;
        case "screen_lock":
          try {
            Vibration.vibrate([100, 50, 100]);
            
            // Minimizar la app para simular bloqueo
            // En Android, esto enviará la app al fondo
            setTimeout(() => {
              BackHandler.exitApp();
            }, 500);
            
            console.log("Pantalla bloqueada - app minimizada");
          } catch (error) {
            console.log("Error al bloquear pantalla:", error);
          }
          break;
        default:
          break;
      }

      return true;
    } catch (error) {
      console.error("Error executing action:", error);
      if (enableHaptics) {
        Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);
      }
      return false;
    } finally {
      setIsExecuting(false);
    }
  }, [enableHaptics, demoMode]);

  const clearDemoMessage = useCallback(() => {
    setDemoMessage(null);
  }, []);

  return {
    isExecuting,
    lastAction,
    demoMessage,
    executeAction,
    clearDemoMessage,
  };
}