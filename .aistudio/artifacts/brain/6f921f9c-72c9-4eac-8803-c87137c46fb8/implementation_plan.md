# LunaSync: Arquitectura Técnica y Plan Maestro de Producto

LunaSync es una plataforma móvil nativa en Android (Kotlin + Jetpack Compose) diseñada para el seguimiento empático, riguroso y consensuado del ciclo menstrual y hormonal en pareja. Transforma el registro íntimo de la mujer en una experiencia compartida donde su pareja asume un rol activo de soporte mediante recomendaciones contextuales diarias, alertas preventivas, registro de observaciones y un canal bidireccional de check-in emocional sin vulnerar la privacidad personal.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> **Decisiones Fundacionales Confirmadas:**
> - **Backend y Sincronización**: **Firebase Firestore en tiempo real con persistencia offline** y Google Sign-In mediante Android Jetpack `CredentialManager`. Garantiza consistencia inmediata entre ambos dispositivos cuando se generan códigos de emparejamiento, cambios de fase, registros de síntomas o respuestas a check-ins.
> - **Modelo de Privacidad y Visibilidad**: **Transparencia amplia para el perfil de soporte con control granular**. La pareja puede observar fases, días fértiles, síntomas físicos y emocionales compartidos, así como el "Cheat Sheet" diario de cuidados. Los registros íntimos y notas marcadas como *Privadas* (`isPrivate = true`) quedan estrictamente bloqueados y solo visibles en el dispositivo de Ella.
> - **Motor Clínico y Predictivo**: **Algoritmo adaptativo con desviación típica y ventana fértil dinámica**. Utiliza una media móvil ponderada de los últimos ciclos registrados (ajustando la duración promedio del ciclo y la longitud de la fase lútea), proyectando intervalos de confianza estadísticos para las fases Menstrual, Folicular, Ovulatoria y Lútea.
> - **Ajustes Integrados (Revisión Reciente)**:
>   1. **Módulo de Intimidad y Contacto**: Registro de encuentros íntimos (protegidos, no protegidos, tocamientos/sin penetración) con selector de privacidad estricto y toggle maestro de compartición.
>   2. **Observaciones de Pareja Enriquecidas**: Registro por parte del novio de estado de ánimo percibido, nivel aparente de dolor y reporte verbal de manchado no registrado (con banner de sugerencia sutil para Ella).
>   3. **Sistema Bidireccional de Check-in ("¿Cómo te sientes?")**: Pings con preguntas predefinidas enviadas por el novio y respuestas rápidas de un toque por parte de Ella con feedback instantáneo en el dashboard de la pareja.

---

## 1. Overview & Core Concept

- **Propuesta de Valor**: Romper con el paradigma de las apps menstruales unidireccionales o pasivas. LunaSync empodera a Ella con herramientas de registro clínico, de intimidad y control absoluto de privacidad, y ofrece a Él un panel intuitivo con recomendaciones accionables (alimentación, actividad física, masajes, contención emocional y espacio personal) para ser un compañero consciente y empático.
- **Arquetipos de Usuario**:
  - **Ella (Primary / Administrator)**: Administradora del espacio de pareja. Configura parámetros de ciclo, registra síntomas diarios, niveles de energía, flujo, registros de intimidad y notas íntimas. Aprueba observaciones sugeridas por su pareja, responde check-ins con un toque, genera códigos de invitación y audita qué información es compartida.
  - **Él (Support Partner)**: Acompañante proactivo e informado. Ingresa con código de vinculación, consulta el dial circular del ciclo, recibe alertas de transición de fase hormonal, visualiza el "Cheat Sheet" del día, registra observaciones externas (ánimo percibido, dolor aparente, manchados reportados en conversación) y envía check-ins afectivos interactivos.
- **Garantías de Privacidad**: Arquitectura de confianza cero (*zero-trust*). Ningún dato sensible es accesible por terceros y el control de desvinculación es unilateral e instantáneo.

---

## 2. User Experience & Visual Design

### 2.1 Estética Visual: "Ice & Ocean Minimalist iOS-like Suite"
- **Lenguaje Visual**: Tarjetas curvadas con radio amplio (`24.dp` y `28.dp`), micro-sombras difusas, efectos de elevación tonal, bordes sutiles con translucidez simulada (*glassmorphism* mediante pinceles de gradiente semi-transparentes) y tipografía sans-serif limpia con espaciados abiertos.
- **Tokens de Color (Escala Azul)**:
  - `Ice Blue` (`#E3F2FD`): Fondos de contenedores sutiles, chips inactivos y halos de fase.
  - `Soft Cerulean` (`#90CAF9`): Indicador de ventana fértil, bordes y acentos secundarios.
  - `Deep Ocean Blue` (`#1976D2`): Color primario para CTAs, dial activo de fase ovulatoria y toggles.
  - `Slate Navy` (`#0D47A1`): Tipografía principal de títulos, contraste y acento de fase menstrual / lútea.
  - `Neutral Light Background` (`#F8FAFC` / `#FFFFFF`): Fondo general limpio y frío.
  - `Dark Mode Support`: Fondos en `#0A1128` y `#001F54`, superficies elevadas `#13293D`, texto de alto contraste en `#E0E1DD`.
- **Componentes Clave**:
  - **Cycle Dial Ring**: Anillo circular interactivo que mapea los días del ciclo divididos en los cuatro cuadrantes hormonales con gradiente fluido.
  - **Interactive Check-In Banner**: Tarjeta desplegable en el Home de Ella con la pregunta del novio y botones de respuesta rápida ("Todo bien, solo cólicos", "Necesito silencio un rato", "Hablemos", "Mimos por favor").
  - **Spotting Suggestion Toast/Card**: Banner sutil en el dashboard de Ella cuando el novio reporta haber escuchado de un manchado: *"Tu pareja notó que comentaste tener manchado, ¿deseas añadirlo a tu registro?"* con botones [Añadir al registro] y [Descartar].
  - **Intimacy Log Modal**: Selector con chips estilizados para el tipo de intimidad con candado de privacidad prominente.

### 2.2 Flujos de Usuario Principales

```
[ Ella ] -> Onboarding -> Registro Ciclo Base -> Dashboard Ella -> Generar Código ("LUNA-8X4K")
                                                                         │
                                                                   (Vinculación)
                                                                         ▼
[ Él ]   -> Welcome Screen -> "Tengo un código" -> Ingreso Código -> Dashboard Soporte
                                                                         │
                                                                         ├── Enviar Check-In ("¿Cómo te sientes?")
                                                                         │          │
                                                                         │          ▼
                                                         [ Ella ] <──────┴── Notificación / Banner de Respuesta Rápida
                                                            │                       │
                                                            └── Respuesta Rápida ───┘ -> Feedback instantáneo en Dashboard Él
```

---

## 3. Modelo Matemático y Clínico del Ciclo

El motor predictivo (`CycleCalculationEngine`) implementa un modelo estocástico adaptativo basado en el método Ogino-Knaus con corrección por desviación histórica ponderada:

### 3.1 Duración Media y Desviación Típica
Dado un historial de ciclos válidos $\{C_1, C_2, \dots, C_n\}$ (con $21 \le C_i \le 45$ días):
- **Media móvil ponderada ($\bar{C}_w$)**: Los ciclos más recientes reciben mayor peso:
  $$\bar{C}_w = \frac{\sum_{i=1}^{n} w_i \cdot C_i}{\sum_{i=1}^{n} w_i}, \quad \text{donde } w_i = i \text{ (orden cronológico ascendente)}$$
- **Varianza y Desviación Estándar ($\sigma$)**:
  $$\sigma = \sqrt{\frac{1}{n-1} \sum_{i=1}^{n} (C_i - \bar{C}_w)^2}$$

### 3.2 Estimación del Próximo Periodo
- Si el primer día del último ciclo registrado es $T_{last}$:
  $$\hat{T}_{next} = T_{last} + \text{round}(\bar{C}_w) \text{ días}$$
- El intervalo de confianza del 90% para la llegada del periodo se define como:
  $$[\hat{T}_{next} - 1.64\sigma, \; \hat{T}_{next} + 1.64\sigma]$$

### 3.3 Ventana de Fertilidad y Día de Ovulación
La fase lútea clínica normal tiene una duración promedio de 14 días (con variabilidad habitual entre 12 y 16 días):
- **Día de Ovulación Estimado ($D_{ovu}$)**:
  $$D_{ovu} = \hat{T}_{next} - 14 \text{ días}$$
- **Ventana Fértil**: Comprende la supervivencia de los espermatozoides (hasta 5 días antes) y del óvulo (1 a 2 días después):
  $$\text{Inicio Ventana Fértil} = D_{ovu} - 5 \text{ días} - \text{round}(\sigma)$$
  $$\text{Fin Ventana Fértil} = D_{ovu} + 1 \text{ día} + \text{round}(\sigma)$$

### 3.4 Fases Hormonales y Clasificación
1. **Fase Menstrual** (Días 1 a $D_{menst}$, típicamente 1–5): Descamación endometrial, niveles bajos de estrógenos y progesterona.
2. **Fase Folicular** (Días $D_{menst}+1$ hasta $D_{ovu}-1$): Aumento paulatino de FSH y estrógenos.
3. **Fase Ovulatoria** (Ventana de alta fertilidad, días $D_{ovu}-2$ a $D_{ovu}+1$): Pico de LH y estrógeno.
4. **Fase Lútea** (Día $D_{ovu}+2$ hasta el final del ciclo): Dominancia de progesterona seguida de descenso premenstrual.

---

## 4. Arquitectura de Datos y Estrategia de Sincronización

```
┌─────────────────────────────────────────────────────────┐
│                    UI LAYER (Compose)                   │
│  [Ella Dashboard]      [Él Dashboard]   [Pairing View]  │
│  - Cycle Dial Ring     - Care CheatSheet- Code Validator│
│  - Intimacy Logger     - Observation Form               │
│  - Quick Reply Banner  - Check-in Dialog                │
└────────────┬───────────────────┬────────────────┬───────┘
             │                   │                │
┌────────────▼───────────────────▼────────────────▼───────┐
│                 VIEWMODELS & STATEFLOW                  │
│    CycleViewModel   PairingViewModel   PartnerViewModel │
│    IntimacyViewModel                   CheckInViewModel │
└────────────┬───────────────────┬────────────────┬───────┘
             │                   │                │
┌────────────▼───────────────────▼────────────────▼───────┐
│                    REPOSITORY LAYER                     │
│    CycleRepository         PairingRepository            │
│    IntimacyRepository      PartnerSupportRepository     │
└────────────┬────────────────────────────────────┬───────┘
             │                                    │
┌────────────▼────────────────┐  ┌────────────────▼───────┐
│     LOCAL CACHE (Room DB)   │  │  CLOUD (Firebase SDK) │
│ - CycleEntryEntity          │  │ - /couple_spaces/      │
│ - DailySymptomEntity        │  │ - /pairing_invites/    │
│ - IntimacyLogEntity         │  │ - /cycles/             │
│ - PartnerObservationEntity  │  │ - /daily_symptoms/     │
│ - CheckInEntity             │  │ - /intimacy_logs/      │
│                             │  │ - /observations/       │
│                             │  │ - /check_ins/          │
└─────────────────────────────┘  └────────────────────────┘
```

### 4.1 Colecciones en Firestore y Esquema Relacional

#### 1. `couple_spaces`
- `spaceId` (String, PK): ID único del espacio compartido.
- `ownerId` (String): UID de Ella.
- `partnerId` (String?): UID de la pareja (null si está pendiente).
- `status` (String): `"UNPAIRED"` | `"PENDING"` | `"PAIRED"` | `"REVOKED"`.
- `averageCycleLength` (Int): Ej. 28 días.
- `averagePeriodLength` (Int): Ej. 5 días.
- `sharingPermissions` (Map):
  - `shareSymptoms`: Boolean (true por defecto).
  - `shareMood`: Boolean (true por defecto).
  - `shareIntimacy`: Boolean (false por defecto, requiere consentimiento explícito).
  - `sharePartnerNotes`: Boolean (true por defecto).
- `createdAt` / `updatedAt`: Server Timestamps.

#### 2. `pairing_invites`
- `inviteCode` (String, DocumentId): Ej. `"LUNA-8X4K"`.
- `spaceId` (String): Referencia al `couple_spaces`.
- `createdBy` (String): UID de Ella.
- `status` (String): `"ACTIVE"` | `"CONSUMED"` | `"EXPIRED"`.
- `expiresAt` (Timestamp): Validez de 48 horas.
- `createdAt`: Server Timestamp.

#### 3. `cycle_entries` (Subcolección `couple_spaces/{spaceId}/cycles`)
- `cycleId` (String, PK).
- `startDate` (String, ISO `YYYY-MM-DD`): Primer día de sangrado.
- `endDate` (String?): Último día del ciclo previo al siguiente periodo.
- `cycleLength` (Int?): Duración calculada en días.
- `periodDuration` (Int): Días de sangrado registrado.
- `isCalculated` (Boolean): Indica si fue completado o está en curso.

#### 4. `daily_symptoms` (Subcolección `couple_spaces/{spaceId}/symptoms`)
- `entryId` (String, PK): Ej. `"2026-11-05"`.
- `date` (String, ISO `YYYY-MM-DD`).
- `flowLevel` (String): `"NONE"` | `"SPOTTING"` | `"LIGHT"` | `"MEDIUM"` | `"HEAVY"`.
- `painLevel` (Int): Escala 0 a 5.
- `cramps` (Boolean), `headache` (Boolean), `backPain` (Boolean), `bloating` (Boolean).
- `mood` (String): `"HAPPY"` | `"CALM"` | `"IRRITABLE"` | `"ANXIOUS"` | `"SAD"`.
- `energyLevel` (Int): Escala 1 a 5.
- `publicNote` (String?): Comentario visible para ambos.
- `privateNote` (String?): **Solo legible por Ella**; protegido por reglas de seguridad.
- `isPrivate` (Boolean): Flag de privacidad individual.
- `loggedBy` (String): UID del creador.
- `updatedAt`: Server Timestamp.

#### 5. `intimacy_logs` (Subcolección `couple_spaces/{spaceId}/intimacy_logs`)
- `logId` (String, PK): ID único del registro.
- `date` (String, ISO `YYYY-MM-DD`).
- `encounterType` (String):
  - `"PROTECTED"`: Relaciones con protección (preservativo u otro método de barrera).
  - `"UNPROTECTED"`: Relaciones sin protección.
  - `"OUTERCOURSE"`: Tocamientos, estimulación o intimidad sin penetración.
- `notes` (String?): Notas del encuentro.
- `isPrivate` (Boolean): Si es `true`, **solo Ella** puede leer o consultar este documento, incluso si `shareIntimacy` está activo a nivel de espacio.
- `loggedBy` (String): UID de Ella.
- `createdAt`: Server Timestamp.

#### 6. `partner_observations` (Subcolección `couple_spaces/{spaceId}/observations`)
- `observationId` (String, PK).
- `date` (String, ISO `YYYY-MM-DD`).
- `perceivedMood` (String): `"CALM"` | `"TIRED"` | `"SAD"` | `"IRRITABLE"` | `"LOVING"`.
- `apparentPainLevel` (Int): Escala 0 a 5 (nivel de molestia o dolor observado por la pareja).
- `unloggedSpottingReported` (Boolean): Si la pareja escuchó verbalmente de manchado y marca esta alerta para que a Ella le sugiera el registro.
- `spottingSuggestionStatus` (String): `"PENDING"` | `"ACCEPTED"` | `"DISMISSED"`.
- `careActionTaken` (String?): Ej. "Preparé té de manzanilla y bolsa térmica".
- `sweetMessage` (String?): Mensaje afectivo de acompañamiento.
- `loggedBy` (String): UID del novio.
- `createdAt`: Server Timestamp.

#### 7. `check_ins` (Subcolección `couple_spaces/{spaceId}/check_ins`)
- `checkInId` (String, PK).
- `senderUid` (String): UID del novio.
- `promptType` (String):
  - `"HOW_ARE_YOU_FEELING"`: "¿Cómo te sientes en este momento?"
  - `"NEED_SPACE_OR_CUDDLES"`: "¿Necesitas espacio a solas o mimos?"
  - `"ARE_YOU_UPSET_WITH_ME"`: "¿Estás molesta conmigo o es el bajón hormonal?"
  - `"CAN_I_BRING_YOU_SOMETHING"`: "¿Puedo llevarte algo rico o caliente?"
- `customNote` (String?): Mensaje opcional personalizado.
- `status` (String): `"PENDING"` | `"ANSWERED"`.
- `responseOption` (String?):
  - `"ALL_GOOD_JUST_CRAMPS"`: "Todo bien, solo tengo cólicos/cansancio".
  - `"NEED_SILENCE"`: "Necesito un ratito de silencio y descanso a solas".
  - `"LET_US_TALK"`: "Hablemos un ratito, me vendría bien".
  - `"CUDDLES_PLEASE"`: "¡Mimos por favor!".
- `responseNote` (String?): Nota de respuesta opcional.
- `answeredAt` (Timestamp?): Momento de la respuesta.
- `createdAt`: Server Timestamp.

### 4.2 Reglas de Seguridad en Firestore (`firestore.rules`)
- **Aislamiento Multi-Tenant**: Solo miembros de un `couple_spaces` (`request.auth.uid == resource.data.ownerId || request.auth.uid == resource.data.partnerId`) pueden acceder al espacio.
- **Acceso a Intimidad**:
  - Lectura permitida al novio **únicamente** si `resource.data.isPrivate == false` Y `get(/databases/$(database)/documents/couple_spaces/$(spaceId)).data.sharingPermissions.shareIntimacy == true`.
  - Si `isPrivate == true`, la regla rechaza la lectura para cualquier usuario que no sea `ownerId`.
- **Acceso a Check-ins y Observaciones**:
  - El novio puede crear `check_ins` y `partner_observations`.
  - Ella puede actualizar el estado de los `check_ins` (`responseOption`, `answeredAt`, `status`) y `partner_observations` (`spottingSuggestionStatus`).

---

## 5. Mapa de Navegación y Vistas (Jetpack Compose)

```
                       [ AuthGate ]
                            │
               ┌────────────┴────────────┐
               ▼                         ▼
         [ OnboardingElla ]        [ PairingCodeInput (Él) ]
               │                         │
               ▼                         ▼
      [ HomeEllaScaffold ]      [ HomePartnerScaffold ]
       ├── Dial & Day View       ├── Phase Radar & Status
       ├── Quick-Reply Check-In  ├── Daily Care Cheat Sheet
       ├── Spotting Suggestion   ├── Send Check-In Dialog
       ├── Symptom & Intimacy Log├── Register Observation Form
       ├── Calendar View         └── Live Check-In Feedback
       └── Privacy Settings
```

### 5.1 Pantallas Comunes
1. **AuthScreen (`Screen.Auth`)**:
   - Cabecera con isotipo estilizado de luna con halos en azul profundo.
   - Botón interactivo de Google Sign-In con `CredentialManager`.
   - Selector inicial si no tiene espacio: "Configurar mi ciclo (Ella)" o "Tengo un código de pareja (Él)".
2. **PairingGeneratorScreen (`Screen.PairingGenerate`)**:
   - Para Ella: Muestra código `LUNA-XXXX` en tarjeta con gradiente sutil, botón de copiar y contador de expiración.
3. **PairingInputScreen (`Screen.PairingInput`)**:
   - Para Él: Entrada con auto-formateo, validación y feedback animado.

### 5.2 Pantallas del Flujo de Ella
1. **HomeEllaScreen**:
   - **Interactive Cycle Dial**: Componente `Canvas` con degradado `Soft Cerulean` a `Deep Ocean Blue`.
   - **Interactive Check-In Banner**: Aparece cuando hay un check-in pendiente del novio (ej. "¿Cómo te sientes?"). Con un solo toque, Ella selecciona su respuesta rápida ("Todo bien, solo cólicos", "Necesito silencio", etc.).
   - **Spotting Suggestion Card**: Aparece si la pareja reportó un manchado verbal: *"Tu pareja notó que comentaste tener manchado, ¿deseas añadirlo a tu registro?"* con acciones inmediatas [Añadir] y [Descartar].
   - **Floating Quick Log**: Botón para abrir el registro de síntomas o intimidad.
2. **Symptom & Intimacy Log Sheet / Screen**:
   - Tab 1: Síntomas y Malestares (Flujo, Dolor, Ánimo, Energía, Síntomas físicos).
   - Tab 2: **Registro de Intimidad (IntimacyLog)**:
     - Chips para selección de encuentro: "Protegida", "Sin protección", "Sin penetración / Tocamientos".
     - Toggle de privacidad visible y claro: *"Privado: no visible para mi pareja"*.
3. **CycleCalendarScreen**: Cuadrícula mensual con marcadores de fase y eventos de ciclo.
4. **PrivacySettingsScreen**: Toggles granulares (`shareSymptoms`, `shareMood`, `shareIntimacy`) y botón de desvinculación unilateral.

### 5.3 Pantallas del Flujo del Novio (Soporte)
1. **PartnerDashboardScreen**:
   - **Phase Status Banner**: Día del ciclo, fase hormonal y descripción empática.
   - **"Cheat Sheet" de Cuidado Diario**: 3 tarjetas de acción recomendadas según la fase activa.
   - **Bidirectional Check-In Action**:
     - Botón flotante o tarjeta: *"Hacer un Check-in afectivo"*. Abre modal con las 4 preguntas predefinidas + mensaje personalizado.
     - Si ya fue respondido: muestra la tarjeta con la respuesta de Ella en tiempo real (ej. *"Ella respondió: 'Necesito un ratito de silencio y descanso a solas' (hace 10 min)"*).
   - **Register Observation Modal**:
     - Ánimo percibido (Calma, Cansada, Triste, Irritable, Cariñosa).
     - Nivel de dolor aparente (0 a 5).
     - Switch: *"Escuché que mencionó haber manchado"* (activa la sugerencia respetuosa para Ella).
     - Acción de cuidado realizada y mensaje cariñoso.

---

## 6. Matriz de Estados y Lógica de Negocio (ViewModels & StateFlow)

Patrón arquitectónico Unidirectional Data Flow (UDF) con `StateFlow` reactivo:

```
┌────────────────────────────────────────────────────────┐
│                   State Management (UDF)               │
│                                                        │
│   User Intent (Events)                                 │
│        │                                               │
│        ▼                                               │
│   ViewModel (Process Intent -> Update Data Layer)      │
│        │                                               │
│        ▼                                               │
│   Repository (Room Cache & Firestore Snapshots)        │
│        │                                               │
│        ▼                                               │
│   UiState (StateFlow<LunasUiState> -> Compose Recomposes)
└────────────────────────────────────────────────────────┘
```

### 6.1 `CycleViewModel` (Gestión de Ciclo y Síntomas)
- **Inputs/Events**:
  - `OnDateSelected(date: LocalDate)`
  - `OnSaveSymptomEntry(entry: DailySymptom)`
  - `OnAcceptSpottingSuggestion(observationId: String, date: LocalDate)`
  - `OnDismissSpottingSuggestion(observationId: String)`
- **Exposed State**:
  - `cycleState: StateFlow<CycleUiState>` (Loading, Success con fase actual, ventana fértil, entrada del día y sugerencia de manchado activa, Error).

### 6.2 `IntimacyViewModel` (Registro de Intimidad)
- **Inputs/Events**:
  - `OnSaveIntimacyLog(encounterType: EncounterType, notes: String, isPrivate: Boolean)`
  - `OnDeleteIntimacyLog(logId: String)`
- **Exposed State**:
  - `intimacyState: StateFlow<IntimacyUiState>` (Lista de registros filtrados según permisos y privacidad).

### 6.3 `CheckInViewModel` (Check-ins Bidireccionales)
- **Inputs/Events**:
  - `OnSendCheckIn(prompt: CheckInPrompt, customNote: String?)` (Por el novio).
  - `OnRespondCheckIn(checkInId: String, response: QuickResponseOption)` (Por Ella).
- **Exposed State**:
  - `checkInState: StateFlow<CheckInUiState>` (Último check-in activo, estado pendiente o respondido con feedback).

### 6.4 `PartnerSupportViewModel` (Observaciones y Cuidados)
- **Inputs/Events**:
  - `OnLogObservation(perceivedMood: String, painLevel: Int, reportedSpotting: Boolean, careAction: String?)`
- **Exposed State**:
  - `supportState: StateFlow<PartnerSupportUiState>` (Fase del día, recomendaciones del Cheat Sheet, historial de observaciones).

---

## 7. Fases de Ejecución

1. **Módulo 1: Fundación y Configuración de Servicios**
   - Configuración de dependencias en `app/build.gradle.kts` (Firestore, Auth, Credential Manager, Room).
   - Generación de `firebase-blueprint.json` y `firestore.rules` con validación de colecciones para intimidad, observaciones enriquecidas y check-ins bidireccionales.
   - Creación del catálogo de diseño: paleta de azules (`Color.kt`), estilos iOS-like en `Theme.kt`, tipografía y strings en `strings.xml`.

2. **Módulo 2: Motor Clínico y Capa de Datos**
   - Implementación de `CycleCalculationEngine` con pruebas unitarias de cálculo adaptativo y ventanas fértiles.
   - Entidades locales de Room y DAOs para almacenamiento offline (`LunaDatabase`).
   - Repositorios con snapshot listeners de Firestore en tiempo real para ciclos, síntomas, intimidad, observaciones y check-ins.

3. **Módulo 3: Flujo de Autenticación y Emparejamiento**
   - Google Sign-In con `CredentialManager`.
   - Generador y validador atómico de códigos `LUNA-XXXX` en `PairingRepository`.
   - Resolución de perfiles (Ella vs Novio) y guardias de navegación.

4. **Módulo 4: UI de Ella (Ciclo, Intimidad y Respuesta a Check-Ins)**
   - Componente `CycleDialComponent` con animaciones de arco.
   - Banner interactivo de respuesta rápida a check-ins del novio.
   - Tarjeta de sugerencia sutil para manchado reportado verbalmente.
   - Sheet de registro de síntomas y módulo de intimidad con selector de privacidad estricto.

5. **Módulo 5: UI del Novio (Dashboard de Soporte, Observaciones y Check-Ins)**
   - Vista de fase y radar de estado del ciclo.
   - Feed dinámico de "Cheat Sheet" con consejos empáticos según la fase activa.
   - Modal interactivo para enviar check-in ("¿Cómo te sientes?") y visualizador de respuesta en tiempo real.
   - Formulario enriquecido de observaciones (ánimo percibido, dolor aparente y aviso de manchado).

6. **Módulo 6: Verificación, Pruebas y Compilación**
   - Pruebas de reglas de Firestore y unitarias de algoritmos clínicos.
   - Verificación de compilación nativa (`compile_applet`) asegurando cero errores y diseño fluido.
