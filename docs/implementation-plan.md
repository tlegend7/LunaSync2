# Completar LunaSync: diseño, funciones y verificación

Base: `81d94df`. Rama de trabajo: `codex/complete-ui-and-flows`.
Alcance autorizado: corregir los 14 hallazgos de auditoría y completar las 20 pantallas/estados del diseño en claro y oscuro. Conservar repositorios, datos, validaciones y seguridad. Las funciones existentes siguen siendo reales. Las funciones nuevas sin backend deben declararse no disponibles o demo, sin éxitos ficticios ni escrituras reales desde mocks.

## A. Permisos, operaciones y base de pruebas
- [x] A1. Suite Android compila: aserciones, Flow.first y recorridos/IDs.
- [x] A2. Rol autenticado derivado del espacio; cambio de experiencia solo en demo.
- [x] A3. Privacidad: edición solo de dueña; espera confirmación, retiene borrador/error y bloquea duplicados.
- [x] A4. Intimidad nueva privada por defecto; compartir exige una decisión explícita.
- [x] A5. Desvinculación: operación autorizada, confirmación asíncrona, error/reintento; no ofrecer acciones reales falsas a pareja.
- [x] A6. Arranque/build de demo reproducible sin configuración secreta; login real requiere configuración válida del entorno.
- [x] A7. Pruebas focalizadas de autorización, sesión, fallos y reintento.

## B. Completar conexiones y recuperar información
- [x] B1. Respuesta a check-in accesible, pregunta/enums correctos, nota opcional y confirmación real.
- [x] B2. Hoy anclado al reloj; selección histórica navega al calendario y conserva fecha explícita de formularios.
- [x] B3. Calendario con flujo, ánimo autorizado, intimidad y detalle confirmado/estimado; sin datos no equivale a no compartido.
- [x] B4. Pareja consulta ánimo e intimidad autorizados; no infiere estados o intensidad.
- [x] B5. Motor sigue siendo fuente de verdad: mostrar respaldo, incertidumbre y retraso sin inventar certeza fisiológica.
- [x] B6. Invitaciones con vencimiento real, regeneración y estados de carga/error/reintento.
- [x] B7. Vacío y errores con acciones útiles y conservación de borradores.

## C. Completar implementación visual de Figma
- [x] C1. Contexto Figma y activos exactos descargados de los 20 estados y sus dos modos.
- [x] C2. Tokens semánticos claros/oscuros, Inter, vectores Lucide, espaciado y componentes reutilizables.
- [x] C3. Formularios nuevos de síntomas, periodo, intimidad, check-in y observación conservando campos reales y errores.
- [x] C4. Integración final de welcome, ambos inicios/calendarios/espacios, permisos, ajustes, respuesta, invitación, confirmaciones, vacío/error.
- [x] C5. Tema persistido desde la raíz; barras del sistema y áreas seguras coherentes.
- [x] C6. Accesibilidad: nombres/estados de toggles, áreas táctiles, contraste, texto ampliado, teclado y pantalla compacta.

## D. Cierre con evidencia del estado final
- [x] D1. Compilación/APK y suite Android completa, sin excluir pruebas para presentar verde.
- [x] D2. Reglas Firestore y pruebas reales de repositorios contra emuladores Auth/Firestore.
- [x] D3. Recorridos UI de ambos roles, ambas apariencias, fecha explícita, errores/reintento, privacidad y vínculo.
- [x] D4. Capturas Android comparadas con cada pantalla/estado Figma; matriz de las 20 filas con evidencia y diferencias justificadas.
- [x] D5. Revisar diff y fuentes conectadas; registrar comandos/resultados, pendientes externos y configuración del entorno.

La meta solo se cierra con evidencia por requisito. Un build o una captura aislados no cierran toda la aplicación. No se despliega ni escribe en Firebase de producción durante la verificación.

## Registro
- Checkpoint de implementación: 112/112 pruebas Android aprobadas, cero omitidas, APK compilada, Auth/Firestore emulados; `android-navigation-recheck.log`. Reglas: 38/38. No cierra C4/C6/D: sigue pendiente la comparación de las 20 pantallas en ambos modos y los recorridos nativos finales. Resumen de evidencia en `verification-progress.md`.
- Plan creado antes de modificar código. GitHub actualizado y limpio en 81d94df; no hay AGENTS.md adicionales en el alcance inspeccionado.
- Verificación de partida: APK de demo con configuración ficticia compila; suite Android falla al compilar; reglas 31/31; captura de ambos roles y temas disponible en la auditoría.
- 11 noviembre: A/B tienen correcciones locales de rol, privacidad asíncrona, intimidad privada por defecto, salida propia de pareja, respuesta accesible, fecha de Hoy, consultas compartidas y vigencia de invitaciones. Reglas actualizadas: 36/36 aprobadas; los archivos ejecutados coinciden por SHA-256 con las fuentes locales. La ejecución Android real de todos los repositorios sigue pendiente.
- C en curso: Inter 400/500/600/700 integrada con licencia; preferencia Sistema/Claro/Oscuro persistida y compartida por raíz/ajustes; barras nativas según tema; tokens oscuros corregidos; SVG exactos convertidos con herramienta Android. Ajustes, privacidad y respuesta trasladados a pantalla completa, con acciones fuera del scroll, errores/borradores y semántica de selección. Bienvenida adaptada con áreas seguras, vectores, elección de rol y acceso funcional a apariencia antes de iniciar sesión. No cerrar C: faltan los cinco formularios principales y resto de pantallas.
- Primer checkpoint de fundamentos: 11/11 tests aprobados (`foundation-tests.log`). El checkpoint posterior compiló APK, pero detectó dos pruebas de UI que aún asumían botones dentro del scroll/opciones siempre visibles en ventana compacta. Se actualizaron sus recorridos conservando las aserciones de selección, bloqueo, error, nota y éxito; la nueva ejecución debe verificarse antes de declarar cierre.
- Reanudación 04:00, 11 noviembre: se inspeccionó el plan y se confirmó terminal la ejecución anterior. Se inició una única nueva ejecución focalizada + APK del árbol actual; log `work/implementation-verification/flows-ui-tests-build.log`, sesión de ejecución `23126`. No iniciar otra mientras esta siga activa. Próximo paso: inspeccionar resultado, instalar APK actual en `emulator-5554` ya iniciado, verificar ajustes/privacidad/respuesta/bienvenida en ambos modos y persistencia tras reinicio. Luego continuar formularios y los cierres D completos.

- Continuación: GitHub vuelto a consultar, main permanece en 81d94df. Invitación/desvinculación migradas al contenedor de pantalla completa; 9/9 pruebas focalizadas aprobadas. Suite ampliada detectó y permitió corregir error fuera de vista, acceso de intimidad en vacío y un selector que seguía tratando la acción fija como contenido desplazable. La prueba Android real de salida de pareja pasó y comprobó pérdida de acceso al servidor y conservación del historial de la dueña. Reejecución completa y capturas finales pendientes tras estas correcciones.

- Cierre funcional comprobado del árbol actual: `responsive-final-verification.log`, 116/116 Android, cero omitidas, APK SHA-256 `99ED18CA1760BE6F0AC434D6B23B8DCDF4A1D3AA1A19677D970CAC52A7F3FBFE`. Reglas 38/38 mediante npm ci + npm run test:rules. La nueva prueba real de intimidad comprueba consentimiento individual/global, lectura privada de dueña, lectura filtrada de pareja y denegación tras revocar. El recorrido nativo completo pasó antes del último ajuste de distribución adaptable de chips; se está repitiendo junto con las 40 capturas del árbol actual. C6/D3/D4/D5 permanecen abiertos hasta inspeccionar esos resultados y el teclado.

- Verificación final de la misma APK `99ED18CA`: 20 estados × dos apariencias capturados y examinados a 393 dp/fuente normal; otros 40 a 360 dp/fuente 130 %. `responsive-normal-and-flow.log`: OK (2 tests), incluye recorrido Activity/ViewModel de ambos roles. `responsive-compact.log`: OK (1 test), 40 estados. Teclado verificado manualmente con UIAutomator y captura: Guardar sobre IME, nota visible y borrador retenido al cerrar teclado. Preferencia DARK y LIGHT comprobada desde UI, force-stop y nueva bienvenida; SharedPreferences confirma ambos valores. Fuente restaurada a 1.0. Matriz en `design-state-matrix.md`; cierre de los 14 hallazgos en `delivery-verification.md`. Configuración Google real y despliegue de reglas quedan como pasos externos explícitos, sin simulación ni cambios en producción.
