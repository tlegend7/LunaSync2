# Recursos del diseño

## Inter

Fuente: Google Fonts, `ofl/inter/Inter[opsz,wght].ttf`, descargada de `https://raw.githubusercontent.com/google/fonts/main/ofl/inter/Inter%5Bopsz%2Cwght%5D.ttf`.

Los cuatro archivos `res/font/inter_*.ttf` son instancias estáticas 400/500/600/700, eje óptico 14, generadas mediante `fontTools.varLib.instancer`. Mantienen Inter en dispositivos desde API 24 sin depender de fuentes descargables. Licencia SIL OFL incluida en `assets/licenses/Inter-OFL.txt`.

## Vectores de Ajustes

Exportados de Figma `CwHRs1V8tG4fOCIfE32LlV`, contexto de los frames `5:1011` y `11:1015`, el 11 de noviembre de 2026. Conversión local con `com.android.ide.common.vectordrawable.Svg2Vector` del SDK de Android; no se redibujaron los paths.

| Recurso Android | Archivo exportado | Tamaño original / slot |
|---|---|---|
| `luna_chevron_left.xml` | `72192.svg` | 20 × 20 |
| `luna_chevron_right.xml` | `7941e.svg` | 16 × 16 |
| `luna_circle_user_round.xml` | `4a2fb.svg` | 22 × 22 |
| `luna_heart.xml` | `255ae.svg` | 22 × 22 |
| `luna_moon.xml` | `0c67d.svg` | 22 × 22 |
| `luna_shield_check.xml` | `f00f1.svg` | 22 × 22 |

La exportación es monocroma; el tintado usa los tokens del tema efectivo, como las variables del diseño. Los slots conservan las dimensiones originales. No hay referencias temporales de Figma en el código ejecutable. Atribución Lucide incluida en `assets/licenses/Lucide-LICENSE.txt`.

## Privacidad y Responder

Contextos claros/oscuros: `5:615`, `11:617`, `8:26`, `11:1379`.

| Recurso | Archivo exportado | Tamaño |
|---|---|---|
| `luna_lock_keyhole.xml` | `70abd.svg` | 22 × 22 |
| `luna_message_circle.xml` | `4c584.svg` | 22 × 22 |
| `luna_toggle_light_on.xml` | `9af0c.svg` | 51 × 31 |
| `luna_toggle_light_off.xml` | `6dbb2.svg` | 51 × 31 |
| `luna_toggle_dark_on.xml` | `34d97.svg` | 51 × 31 |
| `luna_toggle_dark_off.xml` | `321bf.svg` | 51 × 31 |

Los toggles conservan su SVG por estado y apariencia, sin tintado, dentro de una superficie táctil de 51 × 48 dp con semántica Switch, nombre, valor y estado habilitado. Se convierten con el mismo conversor oficial de Android.

La respuesta usa el remitente genérico «tu pareja» y fecha real disponible; no inventa una identidad. Para compañía/espacio se presentan las dos opciones del diseño; las demás preguntas mantienen las respuestas válidas existentes. El envío no se cierra hasta confirmar éxito y retiene nota/elección ante error.

## Adaptación de Ajustes a la app

- Áreas seguras y barras del sistema son nativas de Android; no se dibujan la hora ni el indicador iOS del prototipo.
- La fila de cuenta muestra identidad disponible o «Tu cuenta». Solo la demo usa una etiqueta de demostración. No se inventan Ana/Dani para cuentas reales.
- La cuenta es informativa, sin flecha ni botón sin acción: el proyecto no tiene edición de perfil desarrollada.
- Cambiar experiencia solo aparece en demo. Cerrar sesión/salir de demo conserva su operación real y la preferencia de apariencia.
- El contenido crece y se desplaza con texto ampliado; la acción principal permanece fuera del scroll. Volver dispone de área táctil mínima de 48 dp.

## Bienvenida

Contextos `5:32` y `11:31`: `luna_droplets` procede de `9a885.svg` (22 × 22); `luna_welcome_moon_light` de `2ffac.svg` y `luna_welcome_moon_dark` de `9f631.svg` (ambos 40 × 40). Corazón y flecha reutilizan los recursos exactos de 22 y 16 dp. Todos pasan por `Svg2Vector`.

La selección de experiencia tiene estado RadioButton y realce visible; no asigna permisos autenticados. La demostración sigue disponible mediante una acción identificada dentro del contenido desplazable, mientras que el acceso con cuenta conserva la acción fija del diseño. El avatar anónimo LS abre solo apariencia; no ofrece permisos o acciones de una cuenta inexistente. Las iniciales dentro de la app autenticada se derivan de nombre/correo disponibles; AS/DM quedan reservadas a la demo.

## Formularios

Contextos claros/oscuros descargados: Síntomas `5:353`/`11:354`, Menstruación `5:426`/`11:427`, Intimidad `5:478`/`11:479`, Check-in `5:904`/`11:908`, Observación `5:961`/`11:965`. Eliminación de periodo: overlays `19:1009`/`19:1019`.

- `luna_period_droplets`: exportación `8e0ec.svg`, slot 22 × 22; conversión oficial `Svg2Vector`.
- `luna_observation_shield`: exportación `74392.svg`, slot 22 × 22; misma conversión.
- Check-in reutiliza los recursos exactos de mensaje 22, corazón 22 y flecha 16.
- Los toggles de los formularios reutilizan las cuatro exportaciones del sistema, conservando dimensiones y semántica.
- Los pickers de fecha son controles nativos; no usan los iconos dibujados de la barra de estado iOS.
- Se conservan campos reales ausentes en las composiciones reducidas del prototipo mediante secciones adicionales. Ánimo y energía se muestran por separado para no convertir cansancio en tristeza. Los permisos individuales de síntomas y ánimo son independientes.

## Navegación, calendario y estados finales

Se consultaron los 40 contextos (20 estados × claro/oscuro), incluidos vacío `5:1091`/`11:1095`, error `5:1179`/`11:1184`, canje `5:1236`/`11:1241`, invitación `8:73`/`11:1426` y desvinculación `8:121`/`11:1474`.

Los recursos `luna_nav_home`, `luna_nav_calendar`, `luna_nav_heart`, `luna_calendar_chevron_left/right`, `luna_sliders_horizontal`, `luna_record_lock`, `luna_link`, `luna_check`, `luna_hero_heart`, `luna_record_marker`, `luna_selected_record_marker`, `luna_plus`, `luna_wifi_off` y `luna_join_link` proceden de exportaciones SVG del archivo. Conversión: Svg2Vector oficial del SDK, conservando geometría y viewport. `LunaIcons` agrupa las referencias utilizadas; la navegación mantiene slots de 22 dp y controles de mes de 20 dp dentro de botones de 48 dp.

- Los bordes de menstruación estimada son discontinuos; las fechas registradas tienen fondo coral y una descripción accesible distinta. La selección anuncia su estado y cada celda nombra la fecha completa.
- Invitación usa código, versión y caducidad reales. No genera al abrir; verifica otra vez la vigencia al copiar. El estado ya vinculado bloquea copia/regeneración.
- Desvinculación espera confirmación del repositorio, mantiene el diálogo ante fallo y distingue salir del espacio frente a revocar el acceso de la pareja.
- Vacío conserva acceso a síntomas, intimidad, privacidad y mensajes recibidos. Las dos últimas capacidades añadidas al prototipo reducido evitan ocultar funciones existentes por no tener historial menstrual.
- Error conserva todos los campos del formulario. Su motivo real permanece visible junto al reintento; el panel no supone que todo fallo sea de red o que el registro se haya guardado.

Las capturas de `DesignStateCaptureTest` son fixtures visuales deterministas del código de producción, no evidencia de escrituras reales. Las pruebas de repositorios y recorridos conectados aportan esa evidencia por separado.

## Verificación de adaptación con fuente ampliada

Las opciones principales de síntomas conservan sus proporciones normales. Con fuente superior al 115 %, usan ancho intrínseco y pueden pasar a otra fila; esto evita partir palabras al 130 % en 360 dp sin reducir el texto elegido por la persona. El espacio horizontal de chips es de 8 dp y «Otros estados» usa la escala Small del sistema. Las 40 capturas compactas finales prueban esa adaptación. La modificación no elimina ninguna opción ni cambia su valor de dominio.
