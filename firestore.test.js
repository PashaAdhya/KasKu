const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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
  }
});

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_1")
      .set({
        userId: ALICE_UID,
        title: "Kopi",
        amount: 25000,
        type: "EXPENSE",
        category: "FOOD",
        paymentMethod: "QRIS",
        dateEpochDay: 20776,
        monthKey: "2026-11",
        createdAt: now,
        updatedAt: now,
      })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("budgets").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("recurring").get()
  );
});

test("Authenticated owner: valid transactions create, query, partial update, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const txCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("transactions");

  // Empty subcollection list query succeeds
  await assertSucceeds(txCol.get());

  // Create without optional note
  await assertSucceeds(
    txCol.doc("tx_1").set({
      userId: ALICE_UID,
      title: "Makan Siang",
      amount: 35000,
      type: "EXPENSE",
      category: "FOOD",
      paymentMethod: "QRIS",
      dateEpochDay: 20776,
      monthKey: "2026-11",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Create with optional note
  await assertSucceeds(
    txCol.doc("tx_2").set({
      userId: ALICE_UID,
      title: "Gaji Bulanan",
      amount: 8500000,
      type: "INCOME",
      category: "SALARY",
      paymentMethod: "TRANSFER",
      note: "Gaji November",
      dateEpochDay: 20770,
      monthKey: "2026-11",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Filtered query by monthKey
  await assertSucceeds(txCol.where("monthKey", "==", "2026-11").get());

  // Partial update
  await assertSucceeds(
    txCol.doc("tx_1").update({
      title: "Makan Siang Nasi Padang",
      amount: 42000,
      note: "Tambah es teh",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Delete
  await assertSucceeds(txCol.doc("tx_2").delete());
});

test("Transactions security bounds: rejects shadow fields, @DocumentId id, future timestamps, invalid amount/type, and immutable field tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const txCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("transactions");

  await assertSucceeds(
    txCol.doc("tx_base").set({
      userId: ALICE_UID,
      title: "Kopi",
      amount: 20000,
      type: "EXPENSE",
      category: "FOOD",
      paymentMethod: "CASH",
      dateEpochDay: 20776,
      monthKey: "2026-11",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Rejects 'id' field (DocumentId collision guard)
  await assertFails(
    txCol.doc("tx_bad_id").set({
      id: "tx_bad_id",
      userId: ALICE_UID,
      title: "Bad",
      amount: 10000,
      type: "EXPENSE",
      category: "FOOD",
      paymentMethod: "CASH",
      dateEpochDay: 20776,
      monthKey: "2026-11",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Rejects ghost field
  await assertFails(
    txCol.doc("tx_ghost").set({
      userId: ALICE_UID,
      title: "Ghost",
      amount: 10000,
      type: "EXPENSE",
      category: "FOOD",
      paymentMethod: "CASH",
      dateEpochDay: 20776,
      monthKey: "2026-11",
      isVerified: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Rejects future timestamp
  await assertFails(
    txCol.doc("tx_future").set({
      userId: ALICE_UID,
      title: "Future",
      amount: 10000,
      type: "EXPENSE",
      category: "FOOD",
      paymentMethod: "CASH",
      dateEpochDay: 20776,
      monthKey: "2026-11",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Rejects amount <= 0 and invalid type
  await assertFails(
    txCol.doc("tx_zero").set({
      userId: ALICE_UID,
      title: "Zero",
      amount: 0,
      type: "EXPENSE",
      category: "FOOD",
      paymentMethod: "CASH",
      dateEpochDay: 20776,
      monthKey: "2026-11",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    txCol.doc("tx_bad_type").set({
      userId: ALICE_UID,
      title: "Invalid",
      amount: 1000,
      type: "TRANSFER_OUT",
      category: "FOOD",
      paymentMethod: "CASH",
      dateEpochDay: 20776,
      monthKey: "2026-11",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Rejects immutable createdAt or userId tamper on update
  await assertFails(
    txCol.doc("tx_base").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    txCol.doc("tx_base").update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation: Alice cannot read or write Bob's transactions, budgets, or recurring items", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const bobRoot = context.firestore().collection("users").doc(BOB_UID);
    await bobRoot.collection("transactions").doc("bob_tx").set({
      userId: BOB_UID,
      title: "Bob Expense",
      amount: 50000,
      type: "EXPENSE",
      category: "SHOPPING",
      paymentMethod: "CARD",
      dateEpochDay: 20776,
      monthKey: "2026-11",
      createdAt: created,
      updatedAt: created,
    });
    await bobRoot.collection("budgets").doc("2026-11_OVERALL").set({
      userId: BOB_UID,
      monthKey: "2026-11",
      category: "OVERALL",
      limitAmount: 3000000,
      createdAt: created,
      updatedAt: created,
    });
    await bobRoot.collection("recurring").doc("rec_1").set({
      userId: BOB_UID,
      title: "Internet",
      amount: 350000,
      type: "EXPENSE",
      category: "BILLS",
      paymentMethod: "TRANSFER",
      dueDay: 15,
      createdAt: created,
      updatedAt: created,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobRootFromAlice = aliceDb.collection("users").doc(BOB_UID);

  await assertFails(bobRootFromAlice.collection("transactions").get());
  await assertFails(
    bobRootFromAlice.collection("transactions").doc("bob_tx").get()
  );
  await assertFails(
    bobRootFromAlice
      .collection("transactions")
      .doc("bob_tx")
      .update({ title: "Hijack", updatedAt: new Date(Date.now() - 1000) })
  );
  await assertFails(
    bobRootFromAlice.collection("transactions").doc("bob_tx").delete()
  );
  await assertFails(bobRootFromAlice.collection("budgets").get());
  await assertFails(bobRootFromAlice.collection("recurring").get());
});

test("Budgets and Recurring templates: owner create/update/delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const budgetsCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("budgets");
  const recCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("recurring");

  await assertSucceeds(
    budgetsCol.doc("2026-11_FOOD").set({
      userId: ALICE_UID,
      monthKey: "2026-11",
      category: "FOOD",
      limitAmount: 1500000,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    budgetsCol.doc("2026-11_FOOD").update({
      limitAmount: 1800000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    recCol.doc("rec_kos").set({
      userId: ALICE_UID,
      title: "Sewa Kos",
      amount: 1200000,
      type: "EXPENSE",
      category: "BILLS",
      paymentMethod: "TRANSFER",
      dueDay: 1,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    recCol.doc("rec_kos").update({
      lastLoggedMonth: "2026-11",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(budgetsCol.doc("2026-11_FOOD").delete());
  await assertSucceeds(recCol.doc("rec_kos").delete());
});
