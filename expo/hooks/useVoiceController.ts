import { useState, useCallback, useRef } from "react";
import { Audio } from "expo-av";
import * as Haptics from "expo-haptics";
import { Platform, Alert } from "react-native";
import type { RecordingStatus } from "@/types";

interface UseVoiceControllerProps {
  onRecordingComplete?: (transcript: string) => void;
  enableHaptics?: boolean;
}

export function useVoiceController({ 
  onRecordingComplete, 
  enableHaptics = true 
}: UseVoiceControllerProps = {}) {
  const [status, setStatus] = useState<RecordingStatus>("idle");
  const [transcript, setTranscript] = useState("");
  const [permissionDenied, setPermissionDenied] = useState(false);
  const recordingRef = useRef<Audio.Recording | null>(null);
  const isStartingRef = useRef(false);

  const checkAndRequestPermission = useCallback(async (): Promise<boolean> => {
    try {
      const { status: currentStatus } = await Audio.getPermissionsAsync();
      
      if (currentStatus === "granted") {
        return true;
      }

      const { status } = await Audio.requestPermissionsAsync();
      
      if (status === "granted") {
        setPermissionDenied(false);
        return true;
      } else {
        setPermissionDenied(true);
        Alert.alert(
          "Permiso de Micrófono Requerido",
          "Para usar comandos de voz, necesitamos acceso al micrófono. Por favor, habilita el permiso en la configuración de tu dispositivo.",
          [
            { text: "Cancelar", style: "cancel" },
            { text: "OK", style: "default" }
          ]
        );
        return false;
      }
    } catch (error) {
      console.error("Error checking microphone permission:", error);
      return false;
    }
  }, []);

  const startRecording = useCallback(async () => {
    // Prevenir llamadas concurrentes o si ya está grabando
    if (isStartingRef.current || recordingRef.current || status === "recording") {
      console.log("Recording already in progress, ignoring call");
      return;
    }

    try {
      isStartingRef.current = true;

      const hasPermission = await checkAndRequestPermission();
      if (!hasPermission) {
        setStatus("error");
        isStartingRef.current = false;
        return;
      }

      setStatus("recording");
      setTranscript("");

      if (enableHaptics && Platform.OS !== "web") {
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
      }

      await Audio.setAudioModeAsync({
        allowsRecordingIOS: true,
        playsInSilentModeIOS: true,
      });

      const { recording } = await Audio.Recording.createAsync(
        Audio.RecordingOptionsPresets.HIGH_QUALITY
      );
      
      recordingRef.current = recording;
      isStartingRef.current = false;
    } catch (error) {
      console.error("Error starting recording:", error);
      setStatus("error");
      recordingRef.current = null;
      isStartingRef.current = false;
      
      if (error instanceof Error && error.message.includes("Permission")) {
        setPermissionDenied(true);
        Alert.alert(
          "Permiso Denegado",
          "No se pudo acceder al micrófono. Por favor, verifica los permisos en la configuración de tu dispositivo."
        );
      } else if (error instanceof Error && error.message.includes("Recording")) {
        Alert.alert(
          "Error de Grabación",
          "Ya hay una grabación en proceso. Espera un momento e intenta nuevamente."
        );
      }
    }
  }, [enableHaptics, checkAndRequestPermission, status]);

  const stopRecording = useCallback(async () => {
    try {
      if (!recordingRef.current) {
        isStartingRef.current = false;
        return;
      }

      setStatus("processing");

      if (enableHaptics) {
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
      }

      const currentRecording = recordingRef.current;
      recordingRef.current = null;
      isStartingRef.current = false;

      await currentRecording.stopAndUnloadAsync();
      
      // Resetear el modo de audio
      await Audio.setAudioModeAsync({
        allowsRecordingIOS: false,
        playsInSilentModeIOS: true,
      });
      
      // Simular procesamiento de voz a texto
      // En una implementación real, aquí iría el procesamiento con un modelo local
      await new Promise(resolve => setTimeout(resolve, 800));
      
      // Por ahora, simulamos un resultado basado en el audio
      const uri = currentRecording.getURI();
      console.log("Recording URI:", uri);
      
      // Simulamos un transcript (en producción usaríamos un modelo de STT local)
      // Por ahora, dejamos vacío para que el usuario pueda escribir manualmente
      const simulatedTranscript = "";
      
      setTranscript(simulatedTranscript);
      setStatus("completed");
      
      onRecordingComplete?.(simulatedTranscript);
    } catch (error) {
      console.error("Error stopping recording:", error);
      setStatus("error");
      recordingRef.current = null;
      isStartingRef.current = false;
    }
  }, [enableHaptics, onRecordingComplete]);

  const reset = useCallback(() => {
    setStatus("idle");
    setTranscript("");
    recordingRef.current = null;
    isStartingRef.current = false;
  }, []);

  return {
    status,
    transcript,
    isRecording: status === "recording",
    isProcessing: status === "processing",
    permissionDenied,
    startRecording,
    stopRecording,
    reset,
  };
}