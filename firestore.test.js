const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, ts = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    displayName: "Giulia",
    ageCohort: "25-34",
    termsAccepted: true,
    calibrationCompleted: false,
    selectedTheme: "ZEN_PEBBLE",
    selectedMascot: "OWL",
    soundEnabled: true,
    zenFlowMode: false,
    reminderEnabled: false,
    reminderHour: 9,
    reminderMinute: 0,
    currentStreak: 1,
    bestStreak: 1,
    streakFreezesAvailable: 1,
    lastFreezeRefillMonth: "2026-11",
    logicScore: 50,
    memoryScore: 50,
    reflexScore: 50,
    empathyScore: 50,
    speedIndex: 520,
    unlockedLevels: {
      "raven-matrix": 1,
      "neural-circuit": 1,
      "perspective-compass": 1,
    },
    createdAt: ts,
    updatedAt: ts,
  };
}

function validSessionPayload(uid, ts = new Date(Date.now() - 1000)) {
  return {
    userId: uid,
    gameId: "neural-circuit",
    pillar: "LOGIC",
    level: 3,
    score: 840,
    accuracyPercent: 92,
    avgReactionMs: 680,
    durationSec: 45,
    zenModeUsed: false,
    dateKey: "2026-11-26",
    createdAt: ts,
  };
}

function validDailyStatPayload(uid, dateKey = "2026-11-26", ts = new Date(Date.now() - 1000)) {
  return {
    userId: uid,
    dateKey,
    sessionsCount: 2,
    totalScore: 1520,
    logicCompleted: true,
    memoryCompleted: true,
    reflexCompleted: false,
    empathyCompleted: false,
    avgAccuracy: 88,
    createdAt: ts,
    updatedAt: ts,
  };
}

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
  }
});

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("sessions").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("dailyStats").get()
  );
});

test("UserProfile: owner can create, read, partially update settings & progress, and delete; ghost fields & immutable tampering rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Listing /users is forbidden
  await assertFails(aliceDb.collection("users").get());

  // Valid settings update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      selectedTheme: "OBSIDIAN_DARK",
      zenFlowMode: true,
      selectedMascot: "FOX",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Valid progression update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      currentStreak: 2,
      bestStreak: 2,
      logicScore: 68,
      speedIndex: 615,
      lastWorkoutDate: "2026-11-26",
      unlockedLevels: { "raven-matrix": 2, "neural-circuit": 2 },
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field ("isAdmin")
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 300),
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Reject future timestamp tampering
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      logicScore: 70,
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Reject invalid theme value
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      selectedTheme: "INVALID_THEME",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Owner GDPR delete succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).delete());
});

test("Cross-user isolation: Bob cannot read, write, or delete Alice's profile, sessions, or dailyStats", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID));
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_1")
      .set(validSessionPayload(ALICE_UID));
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("dailyStats")
      .doc("2026-11-26")
      .set(validDailyStatPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked", updatedAt: new Date(Date.now() - 1000) })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).delete());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("sessions").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("sessions").doc("sess_2").set(validSessionPayload(BOB_UID))
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("dailyStats").get());
});

test("TrainingSession: owner can create, list (including empty), and delete; updates & extra id field denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Empty subcollection list succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sessions").orderBy("createdAt", "desc").limit(50).get()
  );

  // Valid create succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sessions").doc("sess_1").set(validSessionPayload(ALICE_UID))
  );

  // Extra @DocumentId 'id' field rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_bad")
      .set({ ...validSessionPayload(ALICE_UID), id: "sess_bad" })
  );

  // Append-only: update denied
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("sessions").doc("sess_1").update({ score: 9999 })
  );

  // Owner delete succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sessions").doc("sess_1").delete()
  );
});

test("DailyStat: owner can create, list, update rings/totals, and delete; date mismatch rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Empty list succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("dailyStats").orderBy("dateKey", "desc").limit(30).get()
  );

  // Valid create
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("dailyStats")
      .doc("2026-11-26")
      .set(validDailyStatPayload(ALICE_UID, "2026-11-26", created))
  );

  // Valid partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("dailyStats").doc("2026-11-26").update({
      sessionsCount: 3,
      totalScore: 2200,
      reflexCompleted: true,
      avgAccuracy: 90,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Date key mismatch with doc ID rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("dailyStats")
      .doc("2026-11-25")
      .set(validDailyStatPayload(ALICE_UID, "2026-11-26", created))
  );

  // Delete succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("dailyStats").doc("2026-11-26").delete()
  );
});
