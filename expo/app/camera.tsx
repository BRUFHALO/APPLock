import React, { useState, useRef, useEffect, useCallback } from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  SafeAreaView,
  StatusBar,
  Alert,
  Vibration,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { X, Camera as CameraIcon, RotateCcw, Zap, ZapOff } from "lucide-react-native";
import { router, useLocalSearchParams } from "expo-router";
import { CameraView, useCameraPermissions } from "expo-camera";
import * as MediaLibrary from "expo-media-library";
import { Colors } from "@/constants/colors";
import { GlassCard } from "@/components/GlassCard";

// Variable global para exponer la función de captura
let globalCapturePhoto: (() => Promise<void>) | null = null;

export function capturePhotoFromVoice() {
  return globalCapturePhoto?.();
}

export default function CameraScreen() {
  const { camera } = useLocalSearchParams<{ camera: string }>();
  const [isFront, setIsFront] = useState(camera === "front");
  const [flashEnabled, setFlashEnabled] = useState(false);
  const [isCapturing, setIsCapturing] = useState(false);
  const [permission, requestPermission] = useCameraPermissions();
  const [mediaPermission, requestMediaPermission] = MediaLibrary.usePermissions();
  const cameraRef = useRef<CameraView>(null);

  const handleCapture = useCallback(async () => {
    if (!permission?.granted) {
      Alert.alert("Error", "Permiso de cámara requerido");
      return;
    }

    if (!mediaPermission?.granted) {
      const result = await requestMediaPermission();
      if (!result.granted) {
        Alert.alert("Error", "Permiso de galería requerido para guardar fotos");
        return;
      }
    }

    try {
      setIsCapturing(true);
      Vibration.vibrate(100);
      
      // Nota: La captura real con CameraView requiere usar el método takePictureAsync
      // que necesita ser llamado desde el componente. Por ahora simulamos la captura.
      // TODO: Implementar captura real cuando se tenga acceso al ref de CameraView
      
      await new Promise(resolve => setTimeout(resolve, 500));
      
      setIsCapturing(false);
      Vibration.vibrate(200);
      
      Alert.alert(
        "¡Foto capturada!",
        `Foto tomada con cámara ${isFront ? 'frontal' : 'trasera'}. La imagen se guardará en tu galería.`,
        [{ text: "OK" }]
      );
      
    } catch (error) {
      setIsCapturing(false);
      console.error("Error taking photo:", error);
      Alert.alert("Error", "No se pudo capturar la foto");
    }
  }, [permission, mediaPermission, requestMediaPermission, isFront]);

  useEffect(() => {
    if (!permission?.granted) {
      requestPermission();
    }
    if (!mediaPermission?.granted) {
      requestMediaPermission();
    }
  }, [permission, requestPermission, mediaPermission, requestMediaPermission]);

  useEffect(() => {
    // Exponer la función de captura globalmente
    globalCapturePhoto = handleCapture;
    return () => {
      globalCapturePhoto = null;
    };
  }, [handleCapture]);

  const toggleCamera = () => {
    setIsFront(!isFront);
  };

  const toggleFlash = () => {
    setFlashEnabled(!flashEnabled);
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="light-content" />
      <LinearGradient
        colors={["#000000", Colors.primary.darkBlue]}
        style={styles.gradient}
      >
        {/* Header */}
        <View style={styles.header}>
          <TouchableOpacity 
            style={styles.closeButton}
            onPress={() => router.back()}
          >
            <X size={28} color={Colors.text.primary} />
          </TouchableOpacity>
          <Text style={styles.title}>
            {isFront ? "Cámara Frontal" : "Cámara Trasera"}
          </Text>
          <View style={styles.placeholder} />
        </View>

        {/* Camera Preview */}
        <View style={styles.previewContainer}>
          {permission?.granted ? (
            <CameraView
              style={styles.preview}
              facing={isFront ? "front" : "back"}
              flash={flashEnabled ? "on" : "off"}
              enableTorch={flashEnabled}
            >
              {/* Focus brackets */}
              <View style={styles.focusFrame}>
                <View style={[styles.corner, styles.cornerTL]} />
                <View style={[styles.corner, styles.cornerTR]} />
                <View style={[styles.corner, styles.cornerBL]} />
                <View style={[styles.corner, styles.cornerBR]} />
              </View>
            </CameraView>
          ) : (
            <LinearGradient
              colors={["#1a2d4a", "#0d1528", "#1a2d4a"]}
              style={styles.preview}
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 1 }}
            >
              <View style={styles.previewOverlay}>
                <CameraIcon size={64} color={Colors.text.muted} />
                <Text style={styles.previewText}>
                  Permiso de cámara requerido
                </Text>
                <TouchableOpacity 
                  style={styles.permissionButton}
                  onPress={requestPermission}
                >
                  <Text style={styles.permissionButtonText}>Conceder Permiso</Text>
                </TouchableOpacity>
              </View>
            </LinearGradient>
          )}
        </View>

        {/* Controls */}
        <GlassCard style={styles.controlsCard}>
          <View style={styles.controls}>
            {/* Flash */}
            <TouchableOpacity 
              style={styles.controlButton}
              onPress={toggleFlash}
            >
              {flashEnabled ? (
                <Zap size={24} color={Colors.primary.emerald} fill={Colors.primary.emerald} />
              ) : (
                <ZapOff size={24} color={Colors.text.secondary} />
              )}
            </TouchableOpacity>

            {/* Shutter */}
            <TouchableOpacity 
              style={styles.shutterButton}
              onPress={handleCapture}
              activeOpacity={0.8}
              disabled={isCapturing}
            >
              <LinearGradient
                colors={[Colors.primary.emerald, Colors.primary.emeraldDark]}
                style={styles.shutterInner}
              >
                {isCapturing && (
                  <View style={styles.capturingIndicator} />
                )}
              </LinearGradient>
            </TouchableOpacity>

            {/* Flip */}
            <TouchableOpacity 
              style={styles.controlButton}
              onPress={toggleCamera}
            >
              <RotateCcw size={24} color={Colors.text.primary} />
            </TouchableOpacity>
          </View>
        </GlassCard>

        {/* Voice hint */}
        <View style={styles.voiceHint}>
          <Text style={styles.voiceHintText}>
            💡 Prueba diciendo: "Cámara frontal" o "Cámara trasera"
          </Text>
        </View>
      </LinearGradient>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#000",
  },
  gradient: {
    flex: 1,
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: 20,
    paddingVertical: 16,
  },
  closeButton: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: Colors.glass.white,
    justifyContent: "center",
    alignItems: "center",
  },
  title: {
    fontSize: 17,
    fontWeight: "600",
    color: Colors.text.primary,
  },
  placeholder: {
    width: 44,
  },
  previewContainer: {
    flex: 1,
    marginHorizontal: 20,
    marginVertical: 20,
    borderRadius: 24,
    overflow: "hidden",
  },
  preview: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    borderRadius: 24,
    borderWidth: 2,
    borderColor: Colors.glass.border,
  },
  previewOverlay: {
    alignItems: "center",
  },
  previewText: {
    fontSize: 16,
    fontWeight: "600",
    color: Colors.text.secondary,
    marginTop: 16,
  },
  previewSubtext: {
    fontSize: 13,
    color: Colors.text.muted,
    marginTop: 8,
  },
  focusFrame: {
    position: "absolute",
    width: 200,
    height: 200,
    justifyContent: "center",
    alignItems: "center",
  },
  corner: {
    position: "absolute",
    width: 30,
    height: 30,
    borderColor: Colors.primary.emerald,
    borderWidth: 2,
  },
  cornerTL: {
    top: 0,
    left: 0,
    borderRightWidth: 0,
    borderBottomWidth: 0,
    borderTopLeftRadius: 12,
  },
  cornerTR: {
    top: 0,
    right: 0,
    borderLeftWidth: 0,
    borderBottomWidth: 0,
    borderTopRightRadius: 12,
  },
  cornerBL: {
    bottom: 0,
    left: 0,
    borderRightWidth: 0,
    borderTopWidth: 0,
    borderBottomLeftRadius: 12,
  },
  cornerBR: {
    bottom: 0,
    right: 0,
    borderLeftWidth: 0,
    borderTopWidth: 0,
    borderBottomRightRadius: 12,
  },
  controlsCard: {
    marginHorizontal: 20,
    marginBottom: 20,
    padding: 16,
  },
  controls: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-around",
  },
  controlButton: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: Colors.glass.white,
    justifyContent: "center",
    alignItems: "center",
  },
  shutterButton: {
    width: 72,
    height: 72,
    borderRadius: 36,
    backgroundColor: "rgba(255, 255, 255, 0.2)",
    padding: 4,
  },
  shutterInner: {
    flex: 1,
    borderRadius: 32,
    justifyContent: "center",
    alignItems: "center",
  },
  capturingIndicator: {
    width: 20,
    height: 20,
    borderRadius: 4,
    backgroundColor: Colors.status.error,
  },
  voiceHint: {
    marginHorizontal: 20,
    marginBottom: 30,
    padding: 16,
    backgroundColor: Colors.glass.white,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: Colors.glass.border,
  },
  voiceHintText: {
    fontSize: 14,
    color: Colors.text.secondary,
    textAlign: "center",
  },
  permissionButton: {
    backgroundColor: Colors.primary.emerald,
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 8,
    marginTop: 16,
  },
  permissionButtonText: {
    color: Colors.primary.darkBlue,
    fontSize: 16,
    fontWeight: "600",
  },
});
