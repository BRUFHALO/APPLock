# Voice Control App - Control de Dispositivo por Voz Offline

## Descripción

App móvil para controlar funciones del dispositivo mediante comandos de voz personalizables, funcionando 100% offline con diseño moderno estilo Platzi.

## Características Principales

### Funcionalidad

- **Reconocimiento de voz local** usando expo-av + procesamiento JavaScript
- **Diccionario de comandos personalizable** - graba frases y vínculas a acciones
- **Fuzzy matching** - reconoce comandos aunque no sean exactos
- **Control de hardware (modo demo)** - volumen, bloqueo de pantalla, cámara
- **Cámara rápida** - capturas con comandos de voz especificando frontal/trasera
- **Persistencia local** - AsyncStorage para guardar comandos y configuración

### Diseño (Estética Platzi)

- **Paleta de colores**: Gradiente verde esmeralda (#98CA3F) a azul oscuro profundo (#121F3D)
- **Glassmorphism**: Tarjetas semi-transparentes con blur
- **Animación de ondas**: Efecto visual cuando la app está escuchando
- **Gradientes modernos**: Fondos y botones con react-native-linear-gradient
- **Interfaz móvil nativa**: Tipografía clara, espaciado optimizado para touch

## Pantallas / Estructura

### 1. Dashboard Principal

- Header con logo y estado de escucha
- Grid de tarjetas glassmorphism mostrando:
  - Estado de permisos (micrófono, cámara)
  - Lista de comandos guardados
  - Botón flotante para grabar nuevo comando
- Animación de ondas de sonido cuando escucha

### 2. Modal "Grabar Nuevo Comando"

- Input para nombre del comando
- Botón de grabar frase de voz
- Selector de acción a vincular (cámara frontal, cámara trasera, volumen up, volumen down, bloquear pantalla)
- Preview del comando guardado

### 3. Pantalla de Cámara

- Vista previa en tiempo real
- Indicador de comando de voz activo
- Controles manuales como fallback
- Flash y cambio de cámara

### 4. Configuración

- Lista de comandos guardados (editable/eliminable)
- Ajuste de sensibilidad del fuzzy matching
- Toggle para vibración y sonidos de feedback

## Navegación

- Stack navigator principal
- Tabs ocultos, navegación por cards del dashboard
- Modales para flujos secundarios

## Hooks Personalizados

- `useVoiceController` - manejo de grabación y procesamiento de voz
- `useCommandStorage` - persistencia de comandos en AsyncStorage
- `useFuzzyMatcher` - algoritmo de coincidencia difusa
- `useHardwareControl` - control de funciones del dispositivo (modo demo)
- `usePermissions` - gestión de permisos de micrófono y cámara

## Icono de App

- Icono con gradiente verde esmeralda a azul oscuro
- Símbolo de ondas de sonido o micrófono estilizado
- Diseño moderno y minimalista

## Flujo de Usuario

1. Usuario abre app → ve dashboard con tarjetas glassmorphism
2. Presiona botón de micrófono → animación de ondas comienza
3. Dice comando grabado previamente → app ejecuta acción
4. Para crear comando: abre modal, graba frase, selecciona acción
5. La app guarda en AsyncStorage y está lista para usar offline