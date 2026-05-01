import React, { useEffect, useRef } from "react";
import { View, StyleSheet, Animated, Easing } from "react-native";
import { Colors } from "@/constants/colors";

interface WaveAnimationProps {
  isActive: boolean;
  color?: string;
}

export function WaveAnimation({ isActive, color = Colors.primary.emerald }: WaveAnimationProps) {
  const wave1 = useRef(new Animated.Value(0)).current;
  const wave2 = useRef(new Animated.Value(0)).current;
  const wave3 = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (!isActive) {
      wave1.setValue(0);
      wave2.setValue(0);
      wave3.setValue(0);
      return;
    }

    const createWaveAnimation = (animatedValue: Animated.Value, delay: number) => {
      return Animated.loop(
        Animated.sequence([
          Animated.delay(delay),
          Animated.timing(animatedValue, {
            toValue: 1,
            duration: 1500,
            easing: Easing.inOut(Easing.ease),
            useNativeDriver: true,
          }),
          Animated.timing(animatedValue, {
            toValue: 0,
            duration: 1500,
            easing: Easing.inOut(Easing.ease),
            useNativeDriver: true,
          }),
        ])
      );
    };

    const anim1 = createWaveAnimation(wave1, 0);
    const anim2 = createWaveAnimation(wave2, 200);
    const anim3 = createWaveAnimation(wave3, 400);

    anim1.start();
    anim2.start();
    anim3.start();

    return () => {
      anim1.stop();
      anim2.stop();
      anim3.stop();
    };
  }, [isActive, wave1, wave2, wave3]);

  const getAnimatedStyle = (animatedValue: Animated.Value) => ({
    transform: [
      {
        scale: animatedValue.interpolate({
          inputRange: [0, 1],
          outputRange: [1, 1.5],
        }),
      },
    ],
    opacity: animatedValue.interpolate({
      inputRange: [0, 1],
      outputRange: [0.8, 0],
    }),
  });

  if (!isActive) return null;

  return (
    <View style={styles.container}>
      <Animated.View style={[styles.wave, { backgroundColor: color }, getAnimatedStyle(wave1)]} />
      <Animated.View style={[styles.wave, { backgroundColor: color }, getAnimatedStyle(wave2)]} />
      <Animated.View style={[styles.wave, { backgroundColor: color }, getAnimatedStyle(wave3)]} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    position: "absolute",
    width: 100,
    height: 100,
    justifyContent: "center",
    alignItems: "center",
  },
  wave: {
    position: "absolute",
    width: 60,
    height: 60,
    borderRadius: 30,
  },
});