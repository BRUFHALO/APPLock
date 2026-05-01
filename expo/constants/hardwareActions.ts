export type HardwareActionType = 
  | "camera_front"
  | "camera_back"
  | "take_photo"
  | "volume_up"
  | "volume_down"
  | "screen_lock"
  | "flashlight_on"
  | "flashlight_off";

export interface HardwareAction {
  id: HardwareActionType;
  label: string;
  icon: string;
  description: string;
  demoOnly: boolean;
}

export const HARDWARE_ACTIONS: HardwareAction[] = [
  {
    id: "camera_front",
    label: "Cámara Frontal",
    icon: "Camera",
    description: "Abre la cámara frontal para selfies",
    demoOnly: false,
  },
  {
    id: "camera_back",
    label: "Cámara Trasera",
    icon: "Camera",
    description: "Abre la cámara trasera principal",
    demoOnly: false,
  },
  {
    id: "take_photo",
    label: "Tomar Foto",
    icon: "Camera",
    description: "Capturar foto con la cámara actual",
    demoOnly: false,
  },
  {
    id: "volume_up",
    label: "Subir Volumen",
    icon: "Volume2",
    description: "Incrementa el volumen del sistema",
    demoOnly: true,
  },
  {
    id: "volume_down",
    label: "Bajar Volumen",
    icon: "Volume1",
    description: "Reduce el volumen del sistema",
    demoOnly: true,
  },
  {
    id: "screen_lock",
    label: "Bloquear Pantalla",
    icon: "Lock",
    description: "Bloquea la pantalla del dispositivo",
    demoOnly: true,
  },
  {
    id: "flashlight_on",
    label: "Encender Linterna",
    icon: "Flashlight",
    description: "Activa la linterna del dispositivo",
    demoOnly: false,
  },
  {
    id: "flashlight_off",
    label: "Apagar Linterna",
    icon: "FlashlightOff",
    description: "Apaga la linterna del dispositivo",
    demoOnly: false,
  },
];

export const getActionById = (id: HardwareActionType): HardwareAction | undefined => {
  return HARDWARE_ACTIONS.find(action => action.id === id);
};