export type HardwareActionType = 
  | "camera_front"
  | "camera_back"
  | "volume_up"
  | "volume_down"
  | "screen_lock"
  | "flashlight_on"
  | "flashlight_off"
  | "take_photo";

export interface VoiceCommand {
  id: string;
  phrase: string;
  action: HardwareActionType;
  createdAt: number;
  usageCount: number;
}

export interface AppSettings {
  fuzzyThreshold: number;
  enableHaptics: boolean;
  enableSounds: boolean;
  demoMode: boolean;
}

export type PermissionStatus = "granted" | "denied" | "undetermined";

export interface PermissionState {
  microphone: PermissionStatus;
  camera: PermissionStatus;
}

export type RecordingStatus = "idle" | "recording" | "processing" | "completed" | "error";
