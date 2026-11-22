const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

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
  }
});

test("Unauthenticated user cannot read or write profiles, memories, or messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("memories").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("messages").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_1").set({
      userId: ALICE_UID,
      category: "FACT",
      title: "Unauthorized",
      content: "Should fail",
      source: "MANUAL",
      pinned: false,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated owner can create, read, update, and delete memories with Kotlin-aligned payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_1").set({
      userId: ALICE_UID,
      category: "PREFERENCE",
      title: "Morning Tea",
      content: "Prefers cardamom chai without sugar.",
      source: "AUTO",
      pinned: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_1").update({
      pinned: false,
      content: "Prefers cardamom chai with honey.",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field `id` or immutable `createdAt` tampering
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_2").set({
      id: "mem_2",
      userId: ALICE_UID,
      category: "FACT",
      title: "Ghost ID",
      content: "Must fail",
      source: "MANUAL",
      pinned: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_3").set({
      userId: ALICE_UID,
      category: "FACT",
      title: "Future",
      content: "Tampered timestamp",
      source: "MANUAL",
      pinned: false,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_1").delete()
  );
});

test("Cross-user access to memories and messages is strictly denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    const now = new Date(Date.now() - 5000);
    await db.collection("users").doc(BOB_UID).collection("memories").doc("bob_mem").set({
      userId: BOB_UID,
      category: "FACT",
      title: "Bob Secret",
      content: "Private note",
      source: "MANUAL",
      pinned: false,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(BOB_UID).collection("messages").doc("bob_msg").set({
      userId: BOB_UID,
      role: "user",
      text: "Hello Jani",
      language: "EN",
      createdAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("memories").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("memories").doc("bob_mem").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("messages").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("memories").doc("bob_mem").update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Chat messages and user profile adhere to schema and immutability rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 3000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      preferredLanguage: "UR",
      autoSpeak: true,
      speechRate: 1.0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "HI",
      speechRate: 1.25,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("msg_1").set({
      userId: ALICE_UID,
      role: "user",
      text: "Salam Jani, aaj ka plan kya hai?",
      language: "UR",
      createdAt: now,
    })
  );

  // Optional commandAction omitted when null, allowed when string
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("msg_2").set({
      userId: ALICE_UID,
      role: "assistant",
      text: "Walaikum Assalam! Main aapki madad ke liye tayyar hoon.",
      language: "UR",
      commandAction: "WEB_SEARCH: today weather",
      createdAt: now,
    })
  );

  // Updates to messages are forbidden (append-only)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("msg_1").update({
      text: "Edited",
    })
  );
});
