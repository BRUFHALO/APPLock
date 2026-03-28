import { useState, useEffect, useCallback } from "react";
import { Camera } from "expo-camera";
import { Audio } from "expo-av";
import type { PermissionState, PermissionStatus } from "@/types";

export function usePermissions() {
  const [permissions, setPermissions] = useState<PermissionState>({
    microphone: "undetermined",
    camera: "undetermined",
  });

  useEffect(() => {
    checkPermissions();
  }, []);

  const checkPermissions = async () => {
    const [cameraStatus, audioStatus] = await Promise.all([
      Camera.getCameraPermissionsAsync(),
      Audio.getPermissionsAsync(),
    ]);

    setPermissions({
      camera: cameraStatus.status as PermissionStatus,
      microphone: audioStatus.status as PermissionStatus,
    });
  };

  const requestCameraPermission = useCallback(async (): Promise<boolean> => {
    const { status } = await Camera.requestCameraPermissionsAsync();
    setPermissions(prev => ({ ...prev, camera: status as PermissionStatus }));
    return status === "granted";
  }, []);

  const requestMicrophonePermission = useCallback(async (): Promise<boolean> => {
    const { status } = await Audio.requestPermissionsAsync();
    setPermissions(prev => ({ ...prev, microphone: status as PermissionStatus }));
    return status === "granted";
  }, []);

  const requestAllPermissions = useCallback(async (): Promise<boolean> => {
    const [cameraGranted, micGranted] = await Promise.all([
      requestCameraPermission(),
      requestMicrophonePermission(),
    ]);
    return cameraGranted && micGranted;
  }, [requestCameraPermission, requestMicrophonePermission]);

  const hasAllPermissions = permissions.camera === "granted" && permissions.microphone === "granted";

  return {
    permissions,
    hasAllPermissions,
    requestCameraPermission,
    requestMicrophonePermission,
    requestAllPermissions,
    refreshPermissions: checkPermissions,
  };
}