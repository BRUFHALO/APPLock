export const Colors = {
  primary: {
    emerald: "#98CA3F",
    emeraldDark: "#7ab332",
    darkBlue: "#121F3D",
    darkBlueLight: "#1a2d4a",
    darkBlueLighter: "#243b6a",
  },
  glass: {
    white: "rgba(255, 255, 255, 0.08)",
    whiteMedium: "rgba(255, 255, 255, 0.12)",
    whiteStrong: "rgba(255, 255, 255, 0.2)",
    border: "rgba(255, 255, 255, 0.1)",
  },
  text: {
    primary: "#FFFFFF",
    secondary: "rgba(255, 255, 255, 0.7)",
    muted: "rgba(255, 255, 255, 0.5)",
    dark: "#121F3D",
  },
  status: {
    success: "#98CA3F",
    error: "#FF6B6B",
    warning: "#FFB800",
    info: "#4A90D9",
  },
  gradient: {
    primary: ["#121F3D", "#1a2d4a"],
    emerald: ["#98CA3F", "#7ab332"],
    card: ["rgba(152, 202, 63, 0.1)", "rgba(18, 31, 61, 0.4)"],
  },
} as const;

export default Colors;