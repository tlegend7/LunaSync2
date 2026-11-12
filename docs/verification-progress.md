# Verificación final — LunaSync

Estado final local en `codex/complete-ui-and-flows`, base `81d94df`. Los registros cronológicos posteriores documentan checkpoints anteriores; el siguiente resultado es el que corresponde al árbol final.

- **116/116** pruebas Android, cero fallos, errores u omisiones: `responsive-final-verification.log`. Incluye SDK real contra Auth/Firestore emulados, migraciones, seguridad, sesiones, ciclos y formularios conectados.
- **38/38** reglas, cero omitidas: `reproducible-rules.log`, ejecutado con npm ci y dependencias fijadas en el repositorio.
- Compilación de APK e instrumentación con wrapper completo: correcta. SHA-256 APK: `99ED18CA1760BE6F0AC434D6B23B8DCDF4A1D3AA1A19677D970CAC52A7F3FBFE`.
- Android API 34, 20 estados × ambos temas y recorrido real de demo: **OK (2 tests)** en `responsive-normal-and-flow.log`.
- Android API 34, 360 dp de ancho, texto 130 %, mismos 40 estados: **OK (1 test)** en `responsive-compact.log`. Imágenes examinadas después de corregir las palabras partidas de los chips.
- Teclado comprobado mediante UIAutomator + captura `final-symptom-keyboard.png`: nota visible, Guardar sobre teclado y texto conservado al cerrarlo.
- Persistencia nativa: DARK y LIGHT seleccionados desde UI, force-stop y relanzamiento; capturas `final-persisted-dark-welcome.png` y `final-persisted-light-welcome.png`, valores de SharedPreferences comprobados. Fuente restaurada a 1.0.
- Sin push ni despliegue. Google real no probado porque falta configuración auténtica; no se usa un éxito ficticio. Instrucciones en `local-development.md`.

Matriz y diferencias justificadas: `design-state-matrix.md`. Cierre por hallazgo: `delivery-verification.md`.

---

## Registro de checkpoints anteriores

# Verificación en curso — LunaSync

Base de GitHub: `81d94df`. Correcciones locales en `codex/complete-ui-and-flows`, sin publicar. Este registro no equivale al cierre de los 20 estados del producto.

## Evidencia completada

- Preferencia Sistema/Claro/Oscuro: 2 pruebas de persistencia y resolución del modo, incluidas en el checkpoint focalizado de 15/15 (`flows-ui-tests-build.log`).
- Reglas Firestore: **38/38 aprobadas**, sin pruebas omitidas, en `work/implementation-verification/android-and-rules-current.log`. Incluye permisos, notas privadas, invitaciones, autodesvinculación y percepción opcional nula/inválida. El estado inicial de observación sigue siendo PENDING; no se cambió quién puede aceptar o descartar manchado.
- Suite Android ampliada: **112/112 aprobadas**, cero fallos, errores o pruebas omitidas, con Auth y Firestore emulados y APK compilada (`android-navigation-recheck.log`). Los seis recorridos de formularios conectados usan ahora los observadores reales de sesión/datos. Al cambiar de pestaña se descarta la notificación de la operación anterior para que no tape la siguiente acción. La lectura de `isPrivate` se mapea explícitamente en los modelos, comprobada con escrituras/lecturas y una pareja vinculada real del emulador.
- APK instalada en API 34. La prueba de cambiar a oscuro desde Bienvenida, forzar cierre y volver a abrir mantuvo oscuro. Captura `welcome-dark-restarted.png`.
- Ajustes y Privacidad examinados en el emulador en claro y oscuro. Las barras grises del diálogo se corrigieron mediante flags de ventana y control de iconos; la captura estable corregida es `settings-light-window-fixed.png`. Sus controles se leen sobre el fondo claro sin la antigua franja gris.
- Intimidad examinada en claro; fecha real, privacidad activada por defecto y controles de la composición nueva presentes. Con teclado visible la acción Guardar permanece encima del teclado. Se añadió además ajuste de ventana para evitar el desplazamiento nativo que recortaba la cabecera; falta comprobar esa última versión.

Capturas, XML de accesibilidad y logs están fuera del repositorio, en `work/implementation-verification`. No atribuir capturas de una APK anterior a una versión posterior: repetir las capturas al cerrar el árbol final.

## Implementación añadida que se está verificando

Los cinco formularios usan ya la pantalla completa compartida: síntomas, menstruación, intimidad, envío de check-in y observación. Privacidad, respuesta, ajustes y apariencia previa al acceso también usan esa estructura.

- Menstruación conserva inicio/fin explícitos, duración inclusiva, selección de fechas y eliminación confirmada. La confirmación de eliminación ahora permanece abierta durante la operación y ante error; hay pruebas de un día exacto y de reintento. No se propone una duración ficticia de cinco días al confirmar el fin.
- Intimidad conserva sus tres tipos, cinco efectos, lubricante, orgasmo, notas y decisión de privacidad. Opciones adicionales bajo «Más detalles del encuentro»; privado por defecto.
- Observación permite un gesto de cuidado sin atribuir ánimo ni dolor: ambos son opcionales, no valores predeterminados ocultos. Se conservan como detalles de percepción, separados de los síntomas confirmados por la dueña.
- Check-in conserva las cuatro preguntas reales. Tres se presentan como opciones principales y la restante mediante «Más preguntas». Pregunta enviada y recibida comparten el mismo mapeo de textos.
- Síntomas conserva los cinco niveles de sangrado, dolor 0–5, ocho estados de ánimo, energía 1–5, moco cervical, siete molestias, notas pública/privada y privacidad independiente para datos físicos y ánimo. Los campos adicionales son desplegables. «Cansada» no se convierte en «Triste»: energía y ánimo siguen siendo dimensiones distintas.
- La privacidad separada usa el `isPrivate` de cada documento protegido existente. El nuevo dato agregado de ánimo es transitorio; su valor nulo hereda el indicador antiguo para mantener privados los registros legados. Se añadió una prueba Android con vínculo real y consultas al servidor para comprobar ambos sentidos y la protección de notas privadas.

## Pendiente antes de cerrar

1. Conservar el checkpoint de 112/112 y volver a verificar los cambios posteriores según su alcance.
2. Capturar y recorrer las cinco versiones nuevas en ambos modos, incluida selección adicional, teclado, texto ampliado y pantalla compacta.
3. Completar fidelidad e iconografía de ambos inicios/calendarios/espacios, invitación, canje, confirmación, vacío y error.
4. Presentar proyecciones e incertidumbre sustentadas por el motor y completar su distinción visual.
5. Ejecutar recorridos conectados de invitación/salida, respuesta y revocación de permisos durante la consulta.
6. Preparar evidencia final de las 20 filas, compilación reproducible y documentación de configuración externa. Login Google real sigue sin validar por falta de configuración auténtica del entorno; no se sustituyó por un éxito ficticio.

## Referencia de adaptación de ventanas

Se conserva la dependencia Compose existente. El ajuste nativo sigue las [propiedades de Dialog](https://developer.android.com/reference/kotlin/androidx/compose/ui/window/DialogProperties) y los flags/insets de la [implementación oficial de AndroidX](https://android.googlesource.com/platform/frameworks/support/+/d463c995fe1c3128619cc6e8039fddef46ad890a/compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/window/AndroidDialog.android.kt), verificado contra la ventana real del emulador. No se dibujan barras iOS ni se ocultan las barras nativas.

## Checkpoint actual del árbol de implementación

- Suite ampliada final de componentes y repositorios: **116/116**, cero fallos, errores u omisiones; `integration-and-native-components.log`. Incluye el SDK real de intimidad con consentimiento individual/global, lectura privada correcta para la dueña y rechazo del servidor después de revocar el permiso global.
- Instalación reproducible de herramientas desde `package-lock.json` y `npm ci`: correcta. `npm run test:rules`: **38/38**, cero omitidas (`reproducible-rules.log`). Wrapper completo 9.3.1 generado con SHA-256 de distribución verificado.
- Recorrido de la Activity/ViewModel reales en Android API 34: aprobado (`native-current-components.log`). Registra síntomas, mantiene nota privada fuera de la vista de pareja, guarda intimidad privada, envía/contesta check-in con nota y registra observación.
- La prueba de captura PixelCopy aprobó su ejecución pero sus imágenes de diálogos no son válidas: capturó la ventana subyacente. No se utilizan como cierre visual. Se corrigió la captura para obtener el compositor completo, validar la cabecera esperada y esperar el fotograma presentado. La verificación visual definitiva y la variante compacta con fuente 130 % siguen pendientes de revisar.
- La demo de invitaciones valida el código y la vigencia, cambia el vínculo en memoria y no anuncia una conexión real. Un ciclo en curso de demo conserva duración de ciclo nula hasta tener un inicio siguiente.
- Se corrigieron además los colores de los componentes adicionales de demo, cuidados y sugerencia de manchado; sus acciones conservan los callbacks existentes.
