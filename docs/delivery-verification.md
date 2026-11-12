# Entrega local y cierre de correcciones

Rama `codex/complete-ui-and-flows`, base GitHub `81d94df`. Cambios locales sin publicar. La entrega abarca los 14 hallazgos originales y las 20 pantallas/estados en claro y oscuro, conservando el backend existente. Los detalles añadidos de formularios preservan campos que no aparecen en las composiciones reducidas de Figma.

## Cierre de los 14 hallazgos

| Hallazgo | Corrección y evidencia |
|---|---|
| 1. Rol real alterable | Cambio de experiencia exclusivo de demo y guardia en ViewModel; rol real derivado del espacio. `SessionIsolationAndAuthorizationTest`, `PrivacyAndSettingsComposeTest`. |
| 2. Privacidad/desvinculación cierran antes de confirmar | Callbacks asíncronos, bloqueo durante operación, borrador/error y reintento. Pruebas Compose y sesiones pendientes. |
| 3. Intimidad compartible por defecto | Nuevo registro privado; consentimiento individual y global necesarios. Pruebas Compose, SDK real con pareja vinculada y denegación del servidor tras revocación. |
| 4. Respuesta inaccesible | Mensaje abre respuesta, pregunta y opciones correctas, nota y confirmación. Recorrido nativo envía, responde y recibe nota en vista de pareja. |
| 5. Controles de pareja rechazados | Permisos de solo consulta; salida propia mediante transacción y regla limitada. Prueba Android real confirma pérdida de acceso y conservación de historial de dueña. |
| 6. Datos consultables perdidos | Calendario y vista de pareja presentan físico, ánimo e intimidad autorizados; detalle por fecha y nota privada solo de dueña. Pruebas de presentación, SDK real y recorridos conectados. |
| 7. Fecha de Hoy incorrecta | Hoy usa reloj; navegación histórica no cambia estado de hoy; formularios capturan fecha explícita. Seis recorridos conectados y pruebas de sesión/calendario. |
| 8. No compartido confundido con ausencia | Indicadores de existencia física/ánimo separados y textos de permiso distintos. Nunca se atribuyen calma ni ausencia de molestias a un documento ausente. |
| 9. Advertencias del motor ocultas | Origen de estimación y días desde fecha estimada visibles; proyecciones del motor con variabilidad observada y mensaje de insuficiencia cuando corresponde. Sin fechas personales inventadas en vacío. |
| 10. Texto marino ilegible en oscuro | Tokens del tema en controles, héroes y componentes adicionales; ambas apariencias examinadas. Contraste de texto secundario: 4,67:1 claro y 9,27:1 oscuro; acciones coral oscuro: 7,05:1. |
| 11. Diseño parcial/apariencia efímera | Inter con licencia, SVG exportados, formularios amplios, 20 estados × ambos modos y apariencia persistida desde raíz. Reinicio nativo comprobado con DARK y LIGHT. |
| 12. Invitación vencida sin recuperación | Metadatos de versión/caducidad, generación explícita, regeneración, copia revalidada al tocar y estado ya vinculado. Pruebas de invitación, canje real y reglas. |
| 13. Accesibilidad/áreas seguras | Switch/RadioButton/Tab con estados, áreas táctiles principales, fechas completas, bordes discontinuos, ventanas con IME. 40 capturas normales y 40 compactas al 130 %, más revisión manual de teclado. |
| 14. Suite no compila/recorridos antiguos | Suite completa 116/116 sin exclusiones, reglas 38/38, APK e instrumentación compiladas. Fixtures conectados esperan observadores reales en vez de inyectar un estado para forzar verde. |

Además se corrigió el mapeo Firestore de `isPrivate`, descubierto al leer escrituras reales del SDK; la privacidad independiente de ánimo no expone los registros antiguos. La demo puede editar/borrar ciclos y canjear una invitación simulada con validación, exclusivamente en memoria.

## Evidencia final

- `work/implementation-verification/responsive-final-verification.log`: wrapper, APK, instrumentación y suite Android completa; 116 pruebas, cero fallos/errores/omisiones. Los XML de resultados están en `app/build/test-results/testDebugUnitTest`.
- `reproducible-rules.log`: 38 pruebas de reglas con las dependencias del lockfile; cero fallos u omisiones.
- `responsive-normal-and-flow.log`: 20 estados en dos temas y recorrido nativo de Activity/ViewModel, OK (2 tests).
- `responsive-compact.log`: los mismos 40 estados a 360 dp, fuente 130 %, OK (1 test). La distribución de síntomas se ajusta sin partir palabras ni reducir la fuente.
- Capturas definitivas en `work/implementation-verification/final-design-captures` y `final-compact-captures`; índice de estados, fuentes Figma y adaptaciones en [design-state-matrix.md](design-state-matrix.md).
- `final-symptom-keyboard.png` y XML: nota editable y acción por encima del IME; el borrador sigue tras cerrarlo. Capturas `final-persisted-dark-welcome.png`/`final-persisted-light-welcome.png`: preferencias conservadas al terminar y relanzar el proceso.

APK entregable: `outputs/LunaSync-debug-99ED18CA.apk`. SHA-256 `99ED18CA1760BE6F0AC434D6B23B8DCDF4A1D3AA1A19677D970CAC52A7F3FBFE`.

No se suman ejecuciones repetidas como pruebas distintas. Las capturas con fixtures prueban composición; las de demo prueban el flujo local; las pruebas SDK con emuladores prueban escrituras/lecturas/autorización. No se atribuye a una categoría el alcance de otra.

## Límites externos

Google real requiere la configuración auténtica Firebase/OAuth y huella de firma. Sin ella, demo funciona y el acceso muestra una explicación, sin simular éxito. Tampoco se desplegaron reglas o datos en Firebase de producción. Para usar en producción las nuevas operaciones autorizadas, las reglas revisadas deben desplegarse mediante el proceso del proyecto.

La compilación mantiene avisos de APIs obsoletas y un mensaje no fatal del procesador KSP en JDK 23; las tareas y pruebas terminan correctamente. La validación visual se realizó en emulador Android API 34, no en todos los modelos físicos. Preparación reproducible y comandos en [local-development.md](local-development.md).
