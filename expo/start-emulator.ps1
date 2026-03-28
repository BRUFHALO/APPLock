# Script para iniciar el emulador Android y compilar la app con módulo nativo
# Autor: Auto-generado para Voice Control App
# Uso: .\start-emulator.ps1

Write-Host "[INICIO] Iniciando emulador Android y compilando app nativa..." -ForegroundColor Cyan

# Detectar la ruta del Android SDK
$androidSdkPath = $env:ANDROID_HOME
if (-not $androidSdkPath) {
    $androidSdkPath = "$env:LOCALAPPDATA\Android\Sdk"
}

$emulatorPath = "$androidSdkPath\emulator\emulator.exe"
$adbPath = "$androidSdkPath\platform-tools\adb.exe"

# Verificar que el emulador existe
if (-not (Test-Path $emulatorPath)) {
    Write-Host "[ERROR] No se encontró el emulador de Android" -ForegroundColor Red
    Write-Host "Ruta buscada: $emulatorPath" -ForegroundColor Yellow
    Write-Host "Por favor, instala Android Studio y configura el Android SDK" -ForegroundColor Yellow
    Write-Host "Configura la variable de entorno ANDROID_HOME apuntando a tu SDK" -ForegroundColor Yellow
    exit 1
}

# Listar emuladores disponibles
Write-Host "`n[INFO] Buscando emuladores disponibles..." -ForegroundColor Yellow
$avds = & $emulatorPath -list-avds

if ($avds.Count -eq 0) {
    Write-Host "[ERROR] No se encontraron emuladores configurados" -ForegroundColor Red
    Write-Host "Crea un emulador en Android Studio (Tools > Device Manager)" -ForegroundColor Yellow
    Write-Host "Recomendación: Crea un dispositivo con API 30+ (Android 11+)" -ForegroundColor Yellow
    exit 1
}

# Mostrar emuladores disponibles
Write-Host "`n[INFO] Emuladores disponibles:" -ForegroundColor Cyan
for ($i = 0; $i -lt $avds.Count; $i++) {
    Write-Host "  [$i] $($avds[$i])" -ForegroundColor White
}

# Usar el primer emulador disponible (o Medium_Phone si existe)
$selectedAvd = $avds[0]
if ($avds -contains "Medium_Phone") {
    $selectedAvd = "Medium_Phone"
} elseif ($avds -contains "Pixel_5_API_30") {
    $selectedAvd = "Pixel_5_API_30"
}

Write-Host "`n[OK] Emulador seleccionado: $selectedAvd" -ForegroundColor Green

# Verificar si ya hay un emulador corriendo
Write-Host "`n[CHECK] Verificando si hay un emulador en ejecución..." -ForegroundColor Yellow
$runningDevices = & $adbPath devices | Select-String "emulator"

if ($runningDevices) {
    Write-Host "[OK] Ya hay un emulador en ejecución" -ForegroundColor Green
} else {
    # Iniciar el emulador en segundo plano
    Write-Host "`n[INICIO] Iniciando emulador $selectedAvd..." -ForegroundColor Cyan
    Start-Process $emulatorPath -ArgumentList "-avd", $selectedAvd -WindowStyle Normal

    # Esperar a que el emulador esté listo
    Write-Host "[ESPERA] Esperando a que el emulador arranque..." -ForegroundColor Yellow
    Write-Host "Esto puede tomar 1-2 minutos dependiendo de tu PC" -ForegroundColor Yellow
    
    $maxWaitTime = 120 # 2 minutos
    $waitedTime = 0
    $bootComplete = $false

    while ($waitedTime -lt $maxWaitTime -and -not $bootComplete) {
        Start-Sleep -Seconds 5
        $waitedTime += 5
        
        # Verificar si el emulador está listo
        $bootStatus = & $adbPath shell getprop sys.boot_completed 2>$null
        if ($bootStatus -eq "1") {
            $bootComplete = $true
            Write-Host "[OK] Emulador iniciado correctamente!" -ForegroundColor Green
        } else {
            Write-Host "  Esperando... ($waitedTime segundos)" -ForegroundColor Gray
        }
    }

    if (-not $bootComplete) {
        Write-Host "[ADVERTENCIA] El emulador está tardando más de lo esperado" -ForegroundColor Yellow
        Write-Host "Continuando de todas formas..." -ForegroundColor Yellow
    }

    # Esperar 5 segundos adicionales para asegurar estabilidad
    Start-Sleep -Seconds 5
}

# Compilar e instalar la app en el emulador
Write-Host "`n[BUILD] Compilando e instalando la app en el emulador..." -ForegroundColor Cyan
Write-Host "Esto incluye el módulo nativo de bloqueo de pantalla" -ForegroundColor Yellow
Write-Host "La primera compilación puede tardar varios minutos..." -ForegroundColor Yellow

npx expo run:android

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n[EXITO] App compilada e instalada exitosamente!" -ForegroundColor Green
    Write-Host "`n[INFO] Instrucciones para usar el bloqueo de pantalla:" -ForegroundColor Cyan
    Write-Host "  1. Crea un comando de voz: 'bloquear' -> Acción: 'Bloquear Pantalla'" -ForegroundColor White
    Write-Host "  2. La primera vez, la app pedirá permisos de Device Admin" -ForegroundColor White
    Write-Host "  3. Activa los permisos en la configuración del emulador" -ForegroundColor White
    Write-Host "  4. Usa el comando de voz 'bloquear' para bloquear la pantalla" -ForegroundColor White
} else {
    Write-Host "`n[ERROR] Hubo un problema al compilar la app" -ForegroundColor Red
    Write-Host "Revisa los errores arriba para más detalles" -ForegroundColor Yellow
}

Write-Host "`nPresiona cualquier tecla para salir..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
