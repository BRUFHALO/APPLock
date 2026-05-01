import React from "react";
import { TouchableOpacity, StyleSheet, Animated, Easing } from "react-native";
import { Mic } from "lucide-react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Colors } from "@/constants/colors";
import { WaveAnimation } from "./WaveAnimation";

interface MicButtonProps {
  isRecording: boolean;
  mode?: 'hold' | 'toggle';
  onPressIn?: () => void;
  onPressOut?: () => void;
  onPress?: () => void;
}

export function MicButton({ 
  isRecording, 
  mode = 'hold',
  onPressIn, 
  onPressOut,
  onPress 
}: MicButtonProps) {
  const scaleAnim = React.useRef(new Animated.Value(1)).current;

  React.useEffect(() => {
    Animated.timing(scaleAnim, {
      toValue: isRecording ? 0.95 : 1,
      duration: 150,
      easing: Easing.inOut(Easing.ease),
      useNativeDriver: true,
    }).start();
  }, [isRecording, scaleAnim]);

  return (
    <TouchableOpacity
      onPressIn={mode === 'hold' ? onPressIn : undefined}
      onPressOut={mode === 'hold' ? onPressOut : undefined}
      onPress={mode === 'toggle' ? onPress : undefined}
      activeOpacity={0.8}
      style={styles.container}
      testID="mic-button"
    >
      <WaveAnimation isActive={isRecording} />
      <Animated.View style={{ transform: [{ scale: scaleAnim }] }}>
        <LinearGradient
          colors={[Colors.primary.emerald, "#7ab332"]}
          style={styles.button}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 1 }}
        >
          <Mic size={32} color={Colors.text.primary} strokeWidth={2.5} />
        </LinearGradient>
      </Animated.View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: {
    width: 100,
    height: 100,
    justifyContent: "center",
    alignItems: "center",
  },
  button: {
    width: 80,
    height: 80,
    borderRadius: 40,
    justifyContent: "center",
    alignItems: "center",
    shadowColor: Colors.primary.emerald,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
  },
});
