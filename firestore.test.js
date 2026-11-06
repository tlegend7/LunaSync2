const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const assert = require("node:assert");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";

const ELLA_UID = "ella_owner_123";
const PARTNER_UID = "partner_novio_456";
const STRANGER_UID = "stranger_789";
const FORMER_PARTNER_UID = "former_partner_999";
const SPACE_ID = "space_luna_abc";
const INVITE_CODE = "LUNA-7X4K";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();

    // Default Seed Data: A paired space where Ella is owner and Partner is partner
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();

      await db.collection("couple_spaces").doc(SPACE_ID).set({
        ownerId: ELLA_UID,
        partnerId: PARTNER_UID,
        status: "PAIRED",
        averageCycleLength: 28,
        averagePeriodLength: 5,
        activeInviteVersion: 1,
        sharingPermissions: {
          shareSymptoms: true,
          shareMood: true,
          shareIntimacy: true,
          sharePartnerNotes: true,
        },
      });

      // Seed physical symptoms (isPrivate = false)
      await db.collection("couple_spaces").doc(SPACE_ID).collection("physical_symptoms").doc("2026-11-05").set({
        spaceId: SPACE_ID,
        date: "2026-11-05",
        flowLevel: "LIGHT",
        painLevel: 2,
        cramps: false,
        headache: false,
        backPain: false,
        bloating: false,
        cervicalMucus: "NONE",
        tenderBreasts: false,
        cravings: false,
        acne: false,
        publicNote: null,
        isPrivate: false,
        loggedBy: ELLA_UID,
      });

      // Seed private physical symptoms (isPrivate = true)
      await db.collection("couple_spaces").doc(SPACE_ID).collection("physical_symptoms").doc("2026-11-04").set({
        spaceId: SPACE_ID,
        date: "2026-11-04",
        flowLevel: "HEAVY",
        painLevel: 4,
        cramps: true,
        headache: false,
        backPain: false,
        bloating: false,
        cervicalMucus: "NONE",
        tenderBreasts: false,
        cravings: false,
        acne: false,
        publicNote: null,
        isPrivate: true,
        loggedBy: ELLA_UID,
      });

      // Seed mood record (isPrivate = false)
      await db.collection("couple_spaces").doc(SPACE_ID).collection("mood_records").doc("2026-11-05").set({
        spaceId: SPACE_ID,
        date: "2026-11-05",
        mood: "CALM",
        energyLevel: 3,
        isPrivate: false,
        loggedBy: ELLA_UID,
      });

      // Seed private note (MUST NEVER BE READ BY PARTNER)
      await db.collection("couple_spaces").doc(SPACE_ID).collection("private_notes").doc("2026-11-05").set({
        spaceId: SPACE_ID,
        date: "2026-11-05",
        privateNote: "Nota secreta de Ella que el novio nunca debe ver",
        loggedBy: ELLA_UID,
      });

      // Seed intimacy log (isPrivate = false)
      await db.collection("couple_spaces").doc(SPACE_ID).collection("intimacy_logs").doc("intimacy_shared").set({
        spaceId: SPACE_ID,
        date: "2026-11-03",
        encounterType: "PROTECTED",
        isPrivate: false,
        loggedBy: ELLA_UID,
      });

      // Seed intimacy log (isPrivate = true)
      await db.collection("couple_spaces").doc(SPACE_ID).collection("intimacy_logs").doc("intimacy_private").set({
        spaceId: SPACE_ID,
        date: "2026-11-02",
        encounterType: "PROTECTED",
        isPrivate: true,
        loggedBy: ELLA_UID,
      });

      // Seed cycle entry
      await db.collection("couple_spaces").doc(SPACE_ID).collection("cycles").doc("cycle_1").set({
        spaceId: SPACE_ID,
        startDate: "2026-10-20",
        periodDuration: 5,
      });
    });
  }
});

// 1. REQUISITO 1: VINCULACIÓN DIRECTA SIN INVITACIÓN
test("1.A Cuenta externa intenta asignarse directamente como partnerId en espacio sin pareja: DENEGADO", async () => {
  // Configurar espacio sin pareja
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("couple_spaces").doc("unpaired_space_1").set({
      ownerId: ELLA_UID,
      partnerId: null,
      status: "UNPAIRED",
      averageCycleLength: 28,
      averagePeriodLength: 5,
      activeInviteVersion: 1,
      sharingPermissions: { shareSymptoms: true, shareMood: true, shareIntimacy: false, sharePartnerNotes: true }
    });
  });

  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  // Intento de escritura directa omitiendo joinWithCode
  await assertFails(
    strangerDb.collection("couple_spaces").doc("unpaired_space_1").update({
      partnerId: STRANGER_UID,
      status: "PAIRED"
    })
  );
});

test("1.B Cuenta externa intenta hacerlo después de una revocación: DENEGADO", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("couple_spaces").doc(SPACE_ID).update({
      partnerId: null,
      status: "UNPAIRED",
      activeInviteVersion: 2
    });
  });

  const formerDb = testEnv.authenticatedContext(FORMER_PARTNER_UID).firestore();
  await assertFails(
    formerDb.collection("couple_spaces").doc(SPACE_ID).update({
      partnerId: FORMER_PARTNER_UID,
      status: "PAIRED"
    })
  );
});

test("1.C Cuenta externa intenta crear o alterar un espacio para atribuir participantes ajenos: DENEGADO", async () => {
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  // Crear atribuyendo a Ella como dueña o fijando pareja arbitraria desde inicio
  await assertFails(
    strangerDb.collection("couple_spaces").doc("fake_space").set({
      ownerId: ELLA_UID,
      partnerId: STRANGER_UID,
      status: "PAIRED",
      averageCycleLength: 28,
      averagePeriodLength: 5,
      sharingPermissions: { shareSymptoms: true, shareMood: true, shareIntimacy: false, sharePartnerNotes: true }
    })
  );
});

test("1.D Canje válido mediante mecanismo autorizado con invitación vigente y atómica: PERMITIDO", async () => {
  const UNPAIRED_SPACE = "unpaired_space_clean";
  const VALID_CODE = "LUNA-9K2M";
  const futureExpiry = new Date(Date.now() + 24 * 3600 * 1000);

  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("couple_spaces").doc(UNPAIRED_SPACE).set({
      ownerId: ELLA_UID,
      partnerId: null,
      status: "UNPAIRED",
      averageCycleLength: 28,
      averagePeriodLength: 5,
      activeInviteVersion: 1,
      sharingPermissions: { shareSymptoms: true, shareMood: true, shareIntimacy: false, sharePartnerNotes: true }
    });
    await db.collection("pairing_invites").doc(VALID_CODE).set({
      spaceId: UNPAIRED_SPACE,
      createdBy: ELLA_UID,
      status: "ACTIVE",
      version: 1,
      expiresAt: futureExpiry,
      createdAt: new Date()
    });
  });

  const partnerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();

  // Canje atómico mediante batch/transaction según reglas
  const batch = partnerDb.batch();
  batch.update(partnerDb.collection("pairing_invites").doc(VALID_CODE), {
    status: "CONSUMED",
    consumedBy: STRANGER_UID
  });
  batch.update(partnerDb.collection("couple_spaces").doc(UNPAIRED_SPACE), {
    partnerId: STRANGER_UID,
    status: "PAIRED",
    claimedInviteCode: VALID_CODE
  });

  await assertSucceeds(batch.commit());
});

test("1.E El canje no modifica ownerId, permisos ni parámetros del ciclo: DENEGADO si los altera", async () => {
  const UNPAIRED_SPACE = "unpaired_space_alter";
  const CODE = "LUNA-ALTER";

  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("couple_spaces").doc(UNPAIRED_SPACE).set({
      ownerId: ELLA_UID,
      partnerId: null,
      status: "UNPAIRED",
      averageCycleLength: 28,
      averagePeriodLength: 5,
      activeInviteVersion: 1,
      sharingPermissions: { shareSymptoms: true, shareMood: true, shareIntimacy: false, sharePartnerNotes: true }
    });
    await db.collection("pairing_invites").doc(CODE).set({
      spaceId: UNPAIRED_SPACE,
      createdBy: ELLA_UID,
      status: "ACTIVE",
      version: 1,
      expiresAt: new Date(Date.now() + 24 * 3600 * 1000),
      createdAt: new Date()
    });
  });

  const partnerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const batch = partnerDb.batch();
  batch.update(partnerDb.collection("pairing_invites").doc(CODE), {
    status: "CONSUMED",
    consumedBy: STRANGER_UID
  });
  batch.update(partnerDb.collection("couple_spaces").doc(UNPAIRED_SPACE), {
    partnerId: STRANGER_UID,
    status: "PAIRED",
    claimedInviteCode: CODE,
    ownerId: STRANGER_UID // intento de secuestro de espacio
  });

  await assertFails(batch.commit());
});

// 2. REQUISITO 2: CANJE LEGÍTIMO Y PRIVACIDAD PRE-CANJE
test("2.A Antes del canje, cuenta externa no puede leer el espacio ni sus datos personales: DENEGADO", async () => {
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  await assertFails(strangerDb.collection("couple_spaces").doc(SPACE_ID).get());
  await assertFails(strangerDb.collection("couple_spaces").doc(SPACE_ID).collection("physical_symptoms").doc("2026-11-05").get());
  await assertFails(strangerDb.collection("couple_spaces").doc(SPACE_ID).collection("private_notes").doc("2026-11-05").get());
});

// 3. REQUISITO 3: ENUMERACIÓN Y CONSUMO DE INVITACIONES
test("3.A Enumeración por cuenta externa (collection get / list): DENEGADA", async () => {
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  await assertFails(strangerDb.collection("pairing_invites").get());
});

test("3.B Consumo aislado de invitación sin vinculación legítima: DENEGADO", async () => {
  const CODE = "LUNA-ISOLATED";
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("pairing_invites").doc(CODE).set({
      spaceId: SPACE_ID,
      createdBy: ELLA_UID,
      status: "ACTIVE",
      expiresAt: new Date(Date.now() + 3600 * 1000),
      createdAt: new Date()
    });
  });

  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  // Intentar marcar CONSUMED sin vincular
  await assertFails(
    strangerDb.collection("pairing_invites").doc(CODE).update({
      status: "CONSUMED"
    })
  );
});

test("3.C Código expirado o sin expiresAt válido es rechazado", async () => {
  const CODE_EXP = "LUNA-EXPD";
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("pairing_invites").doc(CODE_EXP).set({
      spaceId: SPACE_ID,
      createdBy: ELLA_UID,
      status: "ACTIVE",
      expiresAt: new Date(Date.now() - 3600 * 1000), // Expirado en el pasado
      createdAt: new Date()
    });
  });

  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  await assertFails(
    strangerDb.collection("pairing_invites").doc(CODE_EXP).update({
      status: "CONSUMED",
      consumedBy: STRANGER_UID
    })
  );
});

// 4. REQUISITO 7: CHECK-INS Y OBSERVACIONES CON TRANSICIONES ESTRICTAS
test("7.A Pareja intenta crear check-in ya respondido: DENEGADO", async () => {
  const partnerDb = testEnv.authenticatedContext(PARTNER_UID).firestore();
  await assertFails(
    partnerDb.collection("couple_spaces").doc(SPACE_ID).collection("check_ins").doc("hack_checkin_1").set({
      spaceId: SPACE_ID,
      senderUid: PARTNER_UID,
      promptType: "HOW_ARE_YOU_FEELING",
      status: "ANSWERED", // Debe crearse estrictamente PENDING
      responseOption: "CUDDLES_PLEASE"
    })
  );
});

test("7.B Pareja intenta incluir respuesta en check-in pendiente durante creación: DENEGADO", async () => {
  const partnerDb = testEnv.authenticatedContext(PARTNER_UID).firestore();
  await assertFails(
    partnerDb.collection("couple_spaces").doc(SPACE_ID).collection("check_ins").doc("hack_checkin_2").set({
      spaceId: SPACE_ID,
      senderUid: PARTNER_UID,
      promptType: "HOW_ARE_YOU_FEELING",
      status: "PENDING",
      responseOption: "CUDDLES_PLEASE" // Prohibido en creación
    })
  );
});

test("7.C Propietaria responde legítimamente a un check-in pendiente de su pareja: PERMITIDO", async () => {
  const CHECKIN_ID = "legit_checkin_3";
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("couple_spaces").doc(SPACE_ID).collection("check_ins").doc(CHECKIN_ID).set({
      spaceId: SPACE_ID,
      senderUid: PARTNER_UID,
      promptType: "NEED_SPACE_OR_CUDDLES",
      customNote: "Te traje café",
      status: "PENDING",
      responseOption: null,
      responseNote: null,
      answeredAt: null,
      createdAt: new Date()
    });
  });

  const ellaDb = testEnv.authenticatedContext(ELLA_UID).firestore();
  await assertSucceeds(
    ellaDb.collection("couple_spaces").doc(SPACE_ID).collection("check_ins").doc(CHECKIN_ID).update({
      status: "ANSWERED",
      responseOption: "CUDDLES_PLEASE",
      responseNote: "¡Muchas gracias amor!"
    })
  );
});

test("7.D Alterar campos inmutables (senderUid o promptType) en check-in: DENEGADO", async () => {
  const CHECKIN_ID = "immut_checkin_4";
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("couple_spaces").doc(SPACE_ID).collection("check_ins").doc(CHECKIN_ID).set({
      spaceId: SPACE_ID,
      senderUid: PARTNER_UID,
      promptType: "NEED_SPACE_OR_CUDDLES",
      customNote: "Hola",
      status: "PENDING",
      responseOption: null,
      responseNote: null,
      answeredAt: null,
      createdAt: new Date()
    });
  });

  const ellaDb = testEnv.authenticatedContext(ELLA_UID).firestore();
  await assertFails(
    ellaDb.collection("couple_spaces").doc(SPACE_ID).collection("check_ins").doc(CHECKIN_ID).update({
      status: "ANSWERED",
      responseOption: "CUDDLES_PLEASE",
      senderUid: ELLA_UID // Violación de inmutabilidad
    })
  );
});

test("7.E Pareja intenta inyectar privateNote dentro de observation o symptom: DENEGADO", async () => {
  const partnerDb = testEnv.authenticatedContext(PARTNER_UID).firestore();
  await assertFails(
    partnerDb.collection("couple_spaces").doc(SPACE_ID).collection("observations").doc("obs_leak").set({
      spaceId: SPACE_ID,
      date: "2026-11-05",
      perceivedMood: "CALM",
      apparentPainLevel: 1,
      unloggedSpottingReported: false,
      spottingSuggestionStatus: "PENDING",
      careActionTaken: "Te",
      sweetMessage: "Hola",
      loggedBy: PARTNER_UID,
      privateNote: "Nota no permitida en esta subcolección" // Violación de hasOnly / forbidden
    })
  );
});

test("7.F Propietaria actualiza legítimamente spottingSuggestionStatus sin alterar otros campos: PERMITIDO", async () => {
  const OBS_ID = "obs_spotting_legit";
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("couple_spaces").doc(SPACE_ID).collection("observations").doc(OBS_ID).set({
      spaceId: SPACE_ID,
      date: "2026-11-05",
      perceivedMood: "CALM",
      apparentPainLevel: 1,
      unloggedSpottingReported: true,
      spottingSuggestionStatus: "PENDING",
      careActionTaken: "Té",
      sweetMessage: "Te amo",
      loggedBy: PARTNER_UID,
      createdAt: new Date()
    });
  });

  const ellaDb = testEnv.authenticatedContext(ELLA_UID).firestore();
  await assertSucceeds(
    ellaDb.collection("couple_spaces").doc(SPACE_ID).collection("observations").doc(OBS_ID).update({
      spottingSuggestionStatus: "ACCEPTED"
    })
  );
});
