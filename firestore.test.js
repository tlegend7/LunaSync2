const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const assert = require("node:assert");
const fs = require("node:fs");
const { serverTimestamp } = require("firebase/firestore");

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

test("10.1 Pareja finaliza su vínculo, pierde acceso e invalida invitaciones pendientes", async () => {
  const partnerDb = testEnv.authenticatedContext(PARTNER_UID).firestore();
  await assertSucceeds(partnerDb.collection("couple_spaces").doc(SPACE_ID).update({
    partnerId: null, status: "UNPAIRED", activeInviteVersion: 2, updatedAt: serverTimestamp(),
  }));
  await assertFails(partnerDb.collection("couple_spaces").doc(SPACE_ID).get());
  await assertFails(partnerDb.collection("couple_spaces").doc(SPACE_ID).collection("physical_symptoms").doc("2026-11-05").get());
  const ownerDb = testEnv.authenticatedContext(ELLA_UID).firestore();
  const space = await assertSucceeds(ownerDb.collection("couple_spaces").doc(SPACE_ID).get());
  assert.strictEqual(space.data().activeInviteVersion, 2);
  assert.strictEqual(space.data().ownerId, ELLA_UID);
  await assertSucceeds(ownerDb.collection("couple_spaces").doc(SPACE_ID).collection("private_notes").doc("2026-11-05").get());
});

for (const [name, changes] of [
  ["permisos de la dueña", {sharingPermissions: {shareSymptoms: false, shareMood: false, shareIntimacy: false, sharePartnerNotes: false}}],
  ["parámetros del ciclo", {averageCycleLength: 20}],
  ["dueña", {ownerId: PARTNER_UID}],
  ["asignar otra pareja", {partnerId: STRANGER_UID, status: "PAIRED"}],
  ["conservar versión de invitaciones", {activeInviteVersion: 1}],
  ["alterar estado", {status: "REVOKED"}],
]) {
  test(`10.2 Autodesvinculación no permite modificar ${name}`, async () => {
    const db = testEnv.authenticatedContext(PARTNER_UID).firestore();
    await assertFails(db.collection("couple_spaces").doc(SPACE_ID).update({
      partnerId: null, status: "UNPAIRED", activeInviteVersion: 2, updatedAt: serverTimestamp(), ...changes,
    }));
  });
}

test("10.3 Tercero o pareja anterior no puede desvincular la pareja actual", async () => {
  for (const uid of [STRANGER_UID, FORMER_PARTNER_UID]) {
    const db = testEnv.authenticatedContext(uid).firestore();
    await assertFails(db.collection("couple_spaces").doc(SPACE_ID).update({
      partnerId: null, status: "UNPAIRED", activeInviteVersion: 2, updatedAt: serverTimestamp(),
    }));
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

test("1.F Auto-vinculación: dueña intenta canjear su propio código como pareja: DENEGADO", async () => {
  const UNPAIRED_SPACE = "unpaired_space_self";
  const CODE = "LUNA-SELF";

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

  const ellaDb = testEnv.authenticatedContext(ELLA_UID).firestore();
  const batch = ellaDb.batch();
  batch.update(ellaDb.collection("pairing_invites").doc(CODE), {
    status: "CONSUMED",
    consumedBy: ELLA_UID
  });
  batch.update(ellaDb.collection("couple_spaces").doc(UNPAIRED_SPACE), {
    partnerId: ELLA_UID,
    status: "PAIRED",
    claimedInviteCode: CODE
  });

  await assertFails(batch.commit());
});

test("1.G Sustitución de pareja: canjear sobre espacio que ya tiene partnerId: DENEGADO", async () => {
  const CODE = "LUNA-REPAIR";
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    // SPACE_ID ya tiene partnerId = PARTNER_UID y status = PAIRED
    await db.collection("pairing_invites").doc(CODE).set({
      spaceId: SPACE_ID,
      createdBy: ELLA_UID,
      status: "ACTIVE",
      version: 1,
      expiresAt: new Date(Date.now() + 24 * 3600 * 1000),
      createdAt: new Date()
    });
  });

  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const batch = strangerDb.batch();
  batch.update(strangerDb.collection("pairing_invites").doc(CODE), {
    status: "CONSUMED",
    consumedBy: STRANGER_UID
  });
  batch.update(strangerDb.collection("couple_spaces").doc(SPACE_ID), {
    partnerId: STRANGER_UID,
    status: "PAIRED",
    claimedInviteCode: CODE
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

test("2.B Cuenta externa autenticada puede leer el documento de la invitación por código: PERMITIDO", async () => {
  const CODE = "LUNA-GET-OK";
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("pairing_invites").doc(CODE).set({
      spaceId: SPACE_ID,
      createdBy: ELLA_UID,
      status: "ACTIVE",
      version: 1,
      expiresAt: new Date(Date.now() + 24 * 3600 * 1000),
      createdAt: new Date()
    });
  });

  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const docSnap = await assertSucceeds(strangerDb.collection("pairing_invites").doc(CODE).get());
  assert.strictEqual(docSnap.data().spaceId, SPACE_ID);
});

// 3. REQUISITO 3: ATOMICIDAD BIDIRECCIONAL Y RESISTENCIA A CANJE AISLADO
test("3.A Enumeración por cuenta externa (collection get / list): DENEGADA", async () => {
  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  await assertFails(strangerDb.collection("pairing_invites").get());
});

test("3.B.1 Consumo aislado de invitación CON consumedBy correcto pero sin vincular el espacio: DENEGADO", async () => {
  const UNPAIRED_SPACE = "unpaired_space_iso_inv";
  const CODE = "LUNA-ISO-INV";

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
      expiresAt: new Date(Date.now() + 3600 * 1000),
      createdAt: new Date()
    });
  });

  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  // Intentar actualizar SOLAMENTE la invitación a CONSUMED con consumedBy válido
  await assertFails(
    strangerDb.collection("pairing_invites").doc(CODE).update({
      status: "CONSUMED",
      consumedBy: STRANGER_UID
    })
  );
});

test("3.B.2 Vinculación aislada del espacio CON claimedInviteCode válido pero sin consumir invitación: DENEGADA", async () => {
  const UNPAIRED_SPACE = "unpaired_space_iso_spc";
  const CODE = "LUNA-ISO-SPC";

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
      expiresAt: new Date(Date.now() + 3600 * 1000),
      createdAt: new Date()
    });
  });

  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  // Intentar actualizar SOLAMENTE el espacio a PAIRED
  await assertFails(
    strangerDb.collection("couple_spaces").doc(UNPAIRED_SPACE).update({
      partnerId: STRANGER_UID,
      status: "PAIRED",
      claimedInviteCode: CODE
    })
  );
});

test("3.C Código expirado es rechazado: DENEGADO", async () => {
  const UNPAIRED_SPACE = "unpaired_space_exp";
  const CODE_EXP = "LUNA-EXPD";

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
    await db.collection("pairing_invites").doc(CODE_EXP).set({
      spaceId: UNPAIRED_SPACE,
      createdBy: ELLA_UID,
      status: "ACTIVE",
      version: 1,
      expiresAt: new Date(Date.now() - 3600 * 1000), // Expirado en el pasado
      createdAt: new Date()
    });
  });

  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const batch = strangerDb.batch();
  batch.update(strangerDb.collection("pairing_invites").doc(CODE_EXP), {
    status: "CONSUMED",
    consumedBy: STRANGER_UID
  });
  batch.update(strangerDb.collection("couple_spaces").doc(UNPAIRED_SPACE), {
    partnerId: STRANGER_UID,
    status: "PAIRED",
    claimedInviteCode: CODE_EXP
  });

  await assertFails(batch.commit());
});

test("3.D Código de versión revocada (espacio renovado a v2, código en v1): DENEGADO", async () => {
  const UNPAIRED_SPACE = "unpaired_space_rev";
  const CODE_REV = "LUNA-REVOKED";

  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("couple_spaces").doc(UNPAIRED_SPACE).set({
      ownerId: ELLA_UID,
      partnerId: null,
      status: "UNPAIRED",
      averageCycleLength: 28,
      averagePeriodLength: 5,
      activeInviteVersion: 2, // Espacio ahora está en versión 2
      sharingPermissions: { shareSymptoms: true, shareMood: true, shareIntimacy: false, sharePartnerNotes: true }
    });
    await db.collection("pairing_invites").doc(CODE_REV).set({
      spaceId: UNPAIRED_SPACE,
      createdBy: ELLA_UID,
      status: "ACTIVE",
      version: 1, // Invitación vieja en versión 1
      expiresAt: new Date(Date.now() + 3600 * 1000),
      createdAt: new Date()
    });
  });

  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const batch = strangerDb.batch();
  batch.update(strangerDb.collection("pairing_invites").doc(CODE_REV), {
    status: "CONSUMED",
    consumedBy: STRANGER_UID
  });
  batch.update(strangerDb.collection("couple_spaces").doc(UNPAIRED_SPACE), {
    partnerId: STRANGER_UID,
    status: "PAIRED",
    claimedInviteCode: CODE_REV
  });

  await assertFails(batch.commit());
});

test("3.E Código ya CONSUMED no puede canjearse nuevamente: DENEGADO", async () => {
  const UNPAIRED_SPACE = "unpaired_space_cons";
  const CODE_CONS = "LUNA-ALREADY-USED";

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
    await db.collection("pairing_invites").doc(CODE_CONS).set({
      spaceId: UNPAIRED_SPACE,
      createdBy: ELLA_UID,
      status: "CONSUMED", // Ya consumido
      version: 1,
      expiresAt: new Date(Date.now() + 3600 * 1000),
      consumedBy: "previous_user",
      createdAt: new Date()
    });
  });

  const strangerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const batch = strangerDb.batch();
  batch.update(strangerDb.collection("pairing_invites").doc(CODE_CONS), {
    status: "CONSUMED",
    consumedBy: STRANGER_UID
  });
  batch.update(strangerDb.collection("couple_spaces").doc(UNPAIRED_SPACE), {
    partnerId: STRANGER_UID,
    status: "PAIRED",
    claimedInviteCode: CODE_CONS
  });

  await assertFails(batch.commit());
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

test("7.E2 Gesto de cuidado sin ánimo ni dolor inferidos: PERMITIDO solo para pareja", async () => {
  const observation = {
    spaceId: SPACE_ID, date: "2026-11-05", perceivedMood: null, apparentPainLevel: null,
    unloggedSpottingReported: false, spottingSuggestionStatus: "PENDING",
    careActionTaken: "Preparé la cena", sweetMessage: null, loggedBy: PARTNER_UID, createdAt: serverTimestamp(),
  };
  const partnerDb = testEnv.authenticatedContext(PARTNER_UID).firestore();
  await assertSucceeds(partnerDb.collection("couple_spaces").doc(SPACE_ID).collection("observations").doc("care_only").set(observation));
  const ellaDb = testEnv.authenticatedContext(ELLA_UID).firestore();
  await assertSucceeds(ellaDb.collection("couple_spaces").doc(SPACE_ID).collection("observations").doc("care_only").get());
  for (const uid of [ELLA_UID, STRANGER_UID, FORMER_PARTNER_UID]) {
    const db = testEnv.authenticatedContext(uid).firestore();
    await assertFails(db.collection("couple_spaces").doc(SPACE_ID).collection("observations").doc(`forbidden_${uid}`).set({ ...observation, loggedBy: uid }));
  }
});

test("7.E3 Percepción opcional conserva validación de enums y nivel: DENEGADO si son inválidos", async () => {
  const db = testEnv.authenticatedContext(PARTNER_UID).firestore();
  const observation = {
    spaceId: SPACE_ID, date: "2026-11-05", perceivedMood: null, apparentPainLevel: null,
    unloggedSpottingReported: false, spottingSuggestionStatus: "PENDING",
    careActionTaken: "Preparé la cena", sweetMessage: null, loggedBy: PARTNER_UID, createdAt: serverTimestamp(),
  };
  for (const [index, invalid] of [{ perceivedMood: "UNKNOWN" }, { perceivedMood: 1 },
    { apparentPainLevel: -1 }, { apparentPainLevel: 6 }, { apparentPainLevel: "3" }].entries()) {
    await assertFails(db.collection("couple_spaces").doc(SPACE_ID).collection("observations").doc(`invalid_optional_${index}`).set({ ...observation, ...invalid }));
  }
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

// 8. PRUEBAS OBLIGATORIAS: INMUTABILIDAD DE VERSIÓN, ATAQUE DE REVOCACIÓN Y MIGRACIÓN CON MIGRATED_AT

test("8.1 Canje que modifica activeInviteVersion a 0: DENEGADO", async () => {
  const UNPAIRED_SPACE = "unpaired_v_test_0";
  const CODE = "LUNA-VTEST0";

  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("couple_spaces").doc(UNPAIRED_SPACE).set({
      ownerId: ELLA_UID,
      partnerId: null,
      status: "UNPAIRED",
      averageCycleLength: 28,
      averagePeriodLength: 5,
      activeInviteVersion: 2,
      sharingPermissions: { shareSymptoms: true, shareMood: true, shareIntimacy: false, sharePartnerNotes: true }
    });
    await db.collection("pairing_invites").doc(CODE).set({
      spaceId: UNPAIRED_SPACE,
      createdBy: ELLA_UID,
      status: "ACTIVE",
      version: 2,
      expiresAt: new Date(Date.now() + 3600 * 1000),
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
    activeInviteVersion: 0 // Intento malicioso de forzar la versión a 0
  });

  await assertFails(batch.commit());
});

test("8.2 Canje que reduce, aumenta, elimina o cambia de tipo activeInviteVersion: DENEGADO", async () => {
  const UNPAIRED_SPACE = "unpaired_v_types";
  const CODE = "LUNA-VTYPES";

  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("couple_spaces").doc(UNPAIRED_SPACE).set({
      ownerId: ELLA_UID,
      partnerId: null,
      status: "UNPAIRED",
      averageCycleLength: 28,
      averagePeriodLength: 5,
      activeInviteVersion: 3,
      sharingPermissions: { shareSymptoms: true, shareMood: true, shareIntimacy: false, sharePartnerNotes: true }
    });
    await db.collection("pairing_invites").doc(CODE).set({
      spaceId: UNPAIRED_SPACE,
      createdBy: ELLA_UID,
      status: "ACTIVE",
      version: 3,
      expiresAt: new Date(Date.now() + 3600 * 1000),
      createdAt: new Date()
    });
  });

  const partnerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();

  // Intento A: aumentar versión a 4
  const batchA = partnerDb.batch();
  batchA.update(partnerDb.collection("pairing_invites").doc(CODE), {
    status: "CONSUMED",
    consumedBy: STRANGER_UID
  });
  batchA.update(partnerDb.collection("couple_spaces").doc(UNPAIRED_SPACE), {
    partnerId: STRANGER_UID,
    status: "PAIRED",
    claimedInviteCode: CODE,
    activeInviteVersion: 4
  });
  await assertFails(batchA.commit());

  // Intento B: cambiar de tipo a string "3"
  const batchB = partnerDb.batch();
  batchB.update(partnerDb.collection("pairing_invites").doc(CODE), {
    status: "CONSUMED",
    consumedBy: STRANGER_UID
  });
  batchB.update(partnerDb.collection("couple_spaces").doc(UNPAIRED_SPACE), {
    partnerId: STRANGER_UID,
    status: "PAIRED",
    claimedInviteCode: CODE,
    activeInviteVersion: "3"
  });
  await assertFails(batchB.commit());
});

test("8.3 Consumo de invitación que altera version o createdAt en pairing_invites: DENEGADO", async () => {
  const UNPAIRED_SPACE = "unpaired_inv_immut";
  const CODE = "LUNA-INVIMM";

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
      expiresAt: new Date(Date.now() + 3600 * 1000),
      createdAt: new Date()
    });
  });

  const partnerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();
  const batch = partnerDb.batch();
  batch.update(partnerDb.collection("pairing_invites").doc(CODE), {
    status: "CONSUMED",
    consumedBy: STRANGER_UID,
    version: 99 // Intento de alterar la versión de la invitación
  });
  batch.update(partnerDb.collection("couple_spaces").doc(UNPAIRED_SPACE), {
    partnerId: STRANGER_UID,
    status: "PAIRED",
    claimedInviteCode: CODE
  });

  await assertFails(batch.commit());
});

test("8.4 Intento de reactivar una invitación ya consumida o revocada: DENEGADO", async () => {
  const CODE_REVOKED = "LUNA-WAS-REVOKED";
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("pairing_invites").doc(CODE_REVOKED).set({
      spaceId: SPACE_ID,
      createdBy: ELLA_UID,
      status: "REVOKED",
      version: 1,
      expiresAt: new Date(Date.now() + 3600 * 1000),
      createdAt: new Date()
    });
  });

  const ellaDb = testEnv.authenticatedContext(ELLA_UID).firestore();
  // La dueña no puede reactivar a ACTIVE una invitación que ya fue revocada
  await assertFails(
    ellaDb.collection("pairing_invites").doc(CODE_REVOKED).update({
      status: "ACTIVE"
    })
  );
});

test("8.5 Secuencia completa del fallo reportado: rechazada desde el intento de modificar activeInviteVersion", async () => {
  const SPACE_ATTACK = "space_attack_scenario";
  const CODE_V1 = "LUNA-ATTACK-V1";
  const CODE_V2 = "LUNA-ATTACK-V2";
  const future = new Date(Date.now() + 24 * 3600 * 1000);

  // A. Espacio sin pareja y tiene activeInviteVersion = 2
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("couple_spaces").doc(SPACE_ATTACK).set({
      ownerId: ELLA_UID,
      partnerId: null,
      status: "UNPAIRED",
      averageCycleLength: 28,
      averagePeriodLength: 5,
      activeInviteVersion: 2,
      sharingPermissions: { shareSymptoms: true, shareMood: true, shareIntimacy: false, sharePartnerNotes: true }
    });
    // B. Invitación antigua v1
    await db.collection("pairing_invites").doc(CODE_V1).set({
      spaceId: SPACE_ATTACK,
      createdBy: ELLA_UID,
      status: "ACTIVE",
      version: 1,
      expiresAt: future,
      createdAt: new Date()
    });
    // C. Invitación vigente v2
    await db.collection("pairing_invites").doc(CODE_V2).set({
      spaceId: SPACE_ATTACK,
      createdBy: ELLA_UID,
      status: "ACTIVE",
      version: 2,
      expiresAt: future,
      createdAt: new Date()
    });
  });

  const attackerDb = testEnv.authenticatedContext(STRANGER_UID).firestore();

  // D. La cuenta externa intenta canjear v2 y forzar activeInviteVersion a 0
  const attackBatch = attackerDb.batch();
  attackBatch.update(attackerDb.collection("pairing_invites").doc(CODE_V2), {
    status: "CONSUMED",
    consumedBy: STRANGER_UID
  });
  attackBatch.update(attackerDb.collection("couple_spaces").doc(SPACE_ATTACK), {
    partnerId: STRANGER_UID,
    status: "PAIRED",
    claimedInviteCode: CODE_V2,
    activeInviteVersion: 0
  });

  // DEBE RECHAZARSE EL ATAQUE AQUÍ MISMO
  await assertFails(attackBatch.commit());

  // Canje legítimo de v2 sin alterar la versión: PERMITIDO
  const legitBatch = attackerDb.batch();
  legitBatch.update(attackerDb.collection("pairing_invites").doc(CODE_V2), {
    status: "CONSUMED",
    consumedBy: STRANGER_UID
  });
  legitBatch.update(attackerDb.collection("couple_spaces").doc(SPACE_ATTACK), {
    partnerId: STRANGER_UID,
    status: "PAIRED",
    claimedInviteCode: CODE_V2
  });
  await assertSucceeds(legitBatch.commit());

  // E. La propietaria revoca el espacio e incrementa versión a 3
  const ellaDb = testEnv.authenticatedContext(ELLA_UID).firestore();
  await assertSucceeds(
    ellaDb.collection("couple_spaces").doc(SPACE_ATTACK).update({
      partnerId: null,
      status: "UNPAIRED",
      activeInviteVersion: 3
    })
  );

  // F. La expareja intenta canjear la invitación antigua v1: DENEGADO
  const retryV1Batch = attackerDb.batch();
  retryV1Batch.update(attackerDb.collection("pairing_invites").doc(CODE_V1), {
    status: "CONSUMED",
    consumedBy: STRANGER_UID
  });
  retryV1Batch.update(attackerDb.collection("couple_spaces").doc(SPACE_ATTACK), {
    partnerId: STRANGER_UID,
    status: "PAIRED",
    claimedInviteCode: CODE_V1
  });
  await assertFails(retryV1Batch.commit());

  // G. Propietaria genera nueva invitación v3
  const CODE_V3 = "LUNA-ATTACK-V3";
  await assertSucceeds(
    ellaDb.collection("pairing_invites").doc(CODE_V3).set({
      spaceId: SPACE_ATTACK,
      createdBy: ELLA_UID,
      status: "ACTIVE",
      version: 3,
      expiresAt: future,
      createdAt: new Date()
    })
  );

  // H. Nuevo canje legítimo de v3: PERMITIDO
  const NEW_PARTNER_UID = "new_partner_real";
  const newPartnerDb = testEnv.authenticatedContext(NEW_PARTNER_UID).firestore();
  const legitV3Batch = newPartnerDb.batch();
  legitV3Batch.update(newPartnerDb.collection("pairing_invites").doc(CODE_V3), {
    status: "CONSUMED",
    consumedBy: NEW_PARTNER_UID
  });
  legitV3Batch.update(newPartnerDb.collection("couple_spaces").doc(SPACE_ATTACK), {
    partnerId: NEW_PARTNER_UID,
    status: "PAIRED",
    claimedInviteCode: CODE_V3
  });
  await assertSucceeds(legitV3Batch.commit());
});

// 9. PRUEBAS DE MIGRACIÓN: RECONOCIMIENTO DE MIGRATED_AT EN REGLAS

test("9.1 Propietaria escribe documentos de migración con migratedAt: PERMITIDO", async () => {
  const ellaDb = testEnv.authenticatedContext(ELLA_UID).firestore();
  const DATE = "2026-09-15";

  // A. physical_symptoms con migratedAt
  await assertSucceeds(
    ellaDb.collection("couple_spaces").doc(SPACE_ID).collection("physical_symptoms").doc(DATE).set({
      spaceId: SPACE_ID,
      date: DATE,
      flowLevel: "MEDIUM",
      painLevel: 3,
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
      migratedAt: new Date(),
      updatedAt: new Date()
    })
  );

  // B. mood_records con migratedAt
  await assertSucceeds(
    ellaDb.collection("couple_spaces").doc(SPACE_ID).collection("mood_records").doc(DATE).set({
      spaceId: SPACE_ID,
      date: DATE,
      mood: "HAPPY",
      energyLevel: 4,
      isPrivate: true,
      loggedBy: ELLA_UID,
      migratedAt: new Date(),
      updatedAt: new Date()
    })
  );

  // C. private_notes con migratedAt
  await assertSucceeds(
    ellaDb.collection("couple_spaces").doc(SPACE_ID).collection("private_notes").doc(DATE).set({
      spaceId: SPACE_ID,
      date: DATE,
      privateNote: "Nota histórica migrada con éxito",
      loggedBy: ELLA_UID,
      migratedAt: new Date(),
      updatedAt: new Date()
    })
  );
});

test("9.2 Pareja o tercero intenta escribir registros de migración: DENEGADO", async () => {
  const partnerDb = testEnv.authenticatedContext(PARTNER_UID).firestore();
  const DATE = "2026-09-16";

  await assertFails(
    partnerDb.collection("couple_spaces").doc(SPACE_ID).collection("physical_symptoms").doc(DATE).set({
      spaceId: SPACE_ID,
      date: DATE,
      flowLevel: "LIGHT",
      painLevel: 1,
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
      loggedBy: PARTNER_UID,
      migratedAt: new Date(),
      updatedAt: new Date()
    })
  );
});

