# Compilación y comprobaciones locales

Entorno comprobado: Windows, PowerShell, JDK 23, Android SDK 36.1, Build Tools 36.0.0 y emulador API 34. El wrapper fija Gradle 9.3.1 y verifica el SHA-256 de la distribución publicado por Gradle.

## Preparar la demo

1. Configurar `JAVA_HOME` y el SDK mediante `ANDROID_HOME` o un `local.properties` local con `sdk.dir`.
2. Crear la clave de desarrollo local si falta. El proyecto conserva la ruta de firma requerida por AI Studio; la clave no se versiona:

```powershell
keytool -genkeypair -keystore debug.keystore -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
.\gradlew.bat :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

La demo permite explorar ambos roles sin `google-services.json`. Sus modificaciones viven en memoria; se pierden al salir o reiniciar. Ninguna operación demo escribe en Firebase. Un resultado demo se identifica como tal; no vincula cuentas reales.

## Acceso con cuentas reales

Requiere la configuración auténtica de Firebase/Google para el paquete `com.aistudio.applet.kdholq`: proyecto, cliente web OAuth y huella de la clave de firma autorizada. El archivo Android de Google Services corresponde a ese entorno y va en `app/google-services.json`. Revisar también el ID de base de datos configurado en recursos. Sin cliente configurado, el acceso muestra un aviso; no se simula autenticación satisfactoria.

No se verificó el acceso Google real en esta entrega porque esa configuración no estaba disponible. No se desplegaron reglas ni datos en producción.

## Pruebas sin producción

Instalar las versiones fijadas de Node/Firebase con `npm ci`. Se requiere una versión de Node admitida por las dependencias y Java disponible para el emulador Firestore.

```powershell
npm run test:rules
npx firebase emulators:exec --project demo-no-project --only firestore,auth --config firebase.emulators.json '.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug'
.\gradlew.bat :app:connectedDebugAndroidTest
```

Ejecutar reglas y pruebas Android de repositorio en secuencia: la suite de reglas vacía los datos del emulador y no debe compartir una ejecución simultánea con fixtures Android. La configuración `firebase.emulators.json` apunta a la base local predeterminada, separada del archivo de configuración del entorno real.

`DesignStateCaptureTest` genera 40 capturas nativas con fixtures deterministas del código de producción en `Android/data/com.aistudio.applet.kdholq/files/design-captures`. Son evidencia visual, no prueba de una escritura real. `NativeDemoFlowTest` recorre la Activity y el ViewModel reales con datos de demo. Las pruebas unitarias de repositorios ejecutan escrituras, lecturas y permisos contra Auth/Firestore emulados.
