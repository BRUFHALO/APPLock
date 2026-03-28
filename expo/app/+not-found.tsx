import { Link, Stack } from "expo-router";
import { View, Text, StyleSheet, TouchableOpacity } from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { Home, AlertCircle } from "lucide-react-native";
import { Colors } from "@/constants/colors";
import { GlassCard } from "@/components/GlassCard";

export default function NotFoundScreen() {
  return (
    <LinearGradient
      colors={[Colors.primary.darkBlue, "#0d1528"]}
      style={styles.container}
    >
      <Stack.Screen options={{ title: "Página no encontrada", headerShown: false }} />
      
      <View style={styles.content}>
        <GlassCard style={styles.card}>
          <View style={styles.iconContainer}>
            <AlertCircle size={48} color={Colors.status.warning} />
          </View>
          
          <Text style={styles.title}>Página no encontrada</Text>
          <Text style={styles.description}>
            Lo sentimos, la página que buscas no existe o ha sido movida.
          </Text>

          <Link href="/" asChild>
            <TouchableOpacity style={styles.button}>
              <LinearGradient
                colors={[Colors.primary.emerald, Colors.primary.emeraldDark]}
                style={styles.buttonGradient}
              >
                <Home size={20} color={Colors.primary.darkBlue} />
                <Text style={styles.buttonText}>Volver al inicio</Text>
              </LinearGradient>
            </TouchableOpacity>
          </Link>
        </GlassCard>
      </View>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    paddingHorizontal: 20,
  },
  card: {
    width: "100%",
    maxWidth: 360,
    alignItems: "center",
    padding: 32,
  },
  iconContainer: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: "rgba(255, 184, 0, 0.15)",
    justifyContent: "center",
    alignItems: "center",
    marginBottom: 24,
  },
  title: {
    fontSize: 24,
    fontWeight: "700",
    color: Colors.text.primary,
    marginBottom: 12,
  },
  description: {
    fontSize: 15,
    color: Colors.text.secondary,
    textAlign: "center",
    marginBottom: 24,
    lineHeight: 22,
  },
  button: {
    width: "100%",
    borderRadius: 12,
    overflow: "hidden",
  },
  buttonGradient: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    paddingVertical: 14,
  },
  buttonText: {
    fontSize: 16,
    fontWeight: "700",
    color: Colors.primary.darkBlue,
  },
});
