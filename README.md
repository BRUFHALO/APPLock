# No Veas Mi Beta 🔒

App nativa Android para bloqueo de pantalla instantáneo.

## Características

- ✅ Bloqueo de pantalla con un toque
- ✅ Interfaz simple y moderna
- ✅ 100% nativa (Java)
- ✅ APK pequeño (~2-3 MB)
- ✅ Sin dependencias externas

## Cómo compilar

### Opción 1: Android Studio (Recomendado)
1. Abre Android Studio
2. File → Open → Selecciona la carpeta `NoVeasMiBeta`
3. Espera a que Gradle sincronice
4. Build → Build Bundle(s) / APK(s) → Build APK(s)
5. El APK estará en: `app/build/outputs/apk/debug/app-debug.apk`

### Opción 2: Línea de comandos
```bash
cd NoVeasMiBeta
gradlew assembleDebug
```

El APK estará en: `app/build/outputs/apk/debug/app-debug.apk`

## Cómo usar

1. Instala el APK en tu dispositivo Android
2. Abre la app "No Veas Mi Beta"
3. Toca "ACTIVAR PERMISOS"
4. Activa la app como Administrador del Dispositivo
5. Toca "BLOQUEAR PANTALLA" para bloquear instantáneamente

## Requisitos

- Android 7.0 (API 24) o superior
- Permisos de Device Admin (se solicitan en la app)

## Desinstalar

Para desinstalar la app:
1. Ve a Configuración → Seguridad → Administradores del dispositivo
2. Desactiva "No Veas Mi Beta"
3. Ahora puedes desinstalar normalmente

## Estructura del proyecto

```
NoVeasMiBeta/
├── app/
│   ├── src/main/
│   │   ├── java/com/noveasmibeta/
│   │   │   ├── MainActivity.java      # Actividad principal
│   │   │   └── AdminReceiver.java     # Receptor de Device Admin
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml  # UI principal
│   │   │   ├── drawable/              # Botones y fondos
│   │   │   ├── values/
│   │   │   │   ├── strings.xml
│   │   │   │   └── themes.xml
│   │   │   └── xml/
│   │   │       └── device_admin.xml   # Config de Device Admin
│   │   └── AndroidManifest.xml        # Manifest con permisos
│   └── build.gradle                   # Config de la app
├── build.gradle                       # Config del proyecto
├── settings.gradle
└── README.md
```

## Licencia

Uso personal únicamente.
