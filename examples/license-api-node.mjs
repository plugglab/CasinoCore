import { createHash, createSign, randomUUID } from "node:crypto";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { createServer } from "node:http";

const PORT = process.env.PORT ? Number(process.env.PORT) : 8080;
const PRIVATE_KEY_PEM = process.env.CASINOCORE_PRIVATE_KEY_PEM || "";
const STORAGE_FILE = process.env.CASINOCORE_LICENSE_DB || "./examples/license-storage.json";

if (!PRIVATE_KEY_PEM) {
  throw new Error("Set CASINOCORE_PRIVATE_KEY_PEM in the environment.");
}

const database = loadDatabase(STORAGE_FILE);

createServer(async (req, res) => {
  if (req.method === "POST" && req.url === "/api/casinocore/validate") {
    try {
      const body = await readJsonBody(req);
      return writeJson(res, 200, validateLicense(body));
    } catch (error) {
      return writeJson(res, 500, { ok: false, error: error instanceof Error ? error.message : "internal error" });
    }
  }

  if (req.method === "POST" && req.url === "/api/casinocore/licenses") {
    try {
      const body = await readJsonBody(req);
      const created = createLicense(body);
      return writeJson(res, 201, { ok: true, license: created });
    } catch (error) {
      return writeJson(res, 400, { ok: false, error: error instanceof Error ? error.message : "invalid request" });
    }
  }

  if (req.method === "POST" && req.url === "/api/casinocore/licenses/block") {
    try {
      const body = await readJsonBody(req);
      blockLicense(body.licenseKey, body.reason || "blocked manually");
      return writeJson(res, 200, { ok: true });
    } catch (error) {
      return writeJson(res, 400, { ok: false, error: error instanceof Error ? error.message : "invalid request" });
    }
  }

  writeJson(res, 404, { ok: false, error: "not found" });
}).listen(PORT, () => {
  console.log(`CasinoCore license API listening on http://127.0.0.1:${PORT}`);
  console.log(`Storage file: ${STORAGE_FILE}`);
});

function validateLicense(body) {
  assert(body.product === "CasinoCore", "invalid product");
  assert(body.variant === "protected", "invalid variant");
  assert(typeof body.licenseKey === "string" && body.licenseKey.trim() !== "", "license key missing");
  assert(typeof body.serverId === "string" && body.serverId.trim() !== "", "server id missing");
  assert(typeof body.instanceId === "string" && body.instanceId.trim() !== "", "instance id missing");
  assert(typeof body.machineFingerprint === "string" && body.machineFingerprint.trim() !== "", "machine fingerprint missing");

  const key = body.licenseKey.trim();
  const record = database.licenses[key];
  assert(record, "license not found");
  assert(record.status === "active", record.blockReason || "license inactive");

  const now = Math.floor(Date.now() / 1000);
  assert(!record.expiresAt || now <= record.expiresAt, "license expired");

  if (!record.firstActivationAt) {
    record.firstActivationAt = now;
  }

  bindOnFirstUse(record, body);
  enforceBinding(record, body);

  record.lastSeenAt = now;
  record.lastSeenIp = body.serverIp || "";
  record.lastSeenVersion = body.pluginVersion || "";
  record.validationCount = Number(record.validationCount || 0) + 1;

  saveDatabase(STORAGE_FILE, database);

  const tokenLifetimeSeconds = Number(record.tokenLifetimeSeconds || 6 * 60 * 60);
  const payloadText = [
    "product=CasinoCore",
    `owner=${record.owner || ""}`,
    `serverId=${record.boundServerId || body.serverId}`,
    `variant=${record.variant || "protected"}`,
    `jarHash=${body.jarHash || ""}`,
    `nonce=${body.nonce || ""}`,
    `licenseKeyHash=${sha256(key)}`,
    `issuedAt=${now}`,
    `refreshAfter=${now + 15 * 60}`,
    `expiresAt=${now + tokenLifetimeSeconds}`
  ].join("\n");

  const payload = Buffer.from(payloadText, "utf8").toString("base64");
  const signer = createSign("RSA-SHA256");
  signer.update(payload, "utf8");
  signer.end();
  const signature = signer.sign(PRIVATE_KEY_PEM).toString("base64");

  return {
    ok: true,
    payload,
    signature,
    binding: {
      serverId: record.boundServerId,
      instanceId: record.boundInstanceId,
      machineFingerprint: record.boundMachineFingerprint
    }
  };
}

function bindOnFirstUse(record, body) {
  if (!record.boundServerId) {
    record.boundServerId = body.serverId;
  }
  if (!record.boundInstanceId) {
    record.boundInstanceId = body.instanceId;
  }
  if (!record.boundMachineFingerprint) {
    record.boundMachineFingerprint = body.machineFingerprint;
  }
}

function enforceBinding(record, body) {
  if (record.boundServerId !== body.serverId) {
    return autoBlock(record, "license used with a different serverId");
  }
  if (record.boundInstanceId !== body.instanceId) {
    return autoBlock(record, "license used from a different plugin instance");
  }
  if (record.boundMachineFingerprint !== body.machineFingerprint) {
    return autoBlock(record, "license used from a different machine fingerprint");
  }
}

function autoBlock(record, reason) {
  record.status = "blocked";
  record.blockReason = reason;
  record.blockedAt = Math.floor(Date.now() / 1000);
  saveDatabase(STORAGE_FILE, database);
  throw new Error(reason);
}

function createLicense(input) {
  const licenseKey = input.licenseKey || `cc_${randomUUID().replace(/-/g, "")}`;
  if (database.licenses[licenseKey]) {
    throw new Error("license key already exists");
  }

  const now = Math.floor(Date.now() / 1000);
  const expiresAt = Number(input.expiresAt || now + 30 * 24 * 60 * 60);
  const record = {
    owner: input.owner || "",
    status: "active",
    variant: "protected",
    createdAt: now,
    expiresAt,
    tokenLifetimeSeconds: Number(input.tokenLifetimeSeconds || 6 * 60 * 60),
    validationCount: 0,
    blockReason: ""
  };

  database.licenses[licenseKey] = record;
  saveDatabase(STORAGE_FILE, database);
  return { licenseKey, ...record };
}

function blockLicense(licenseKey, reason) {
  const record = database.licenses[licenseKey];
  assert(record, "license not found");
  record.status = "blocked";
  record.blockReason = reason;
  record.blockedAt = Math.floor(Date.now() / 1000);
  saveDatabase(STORAGE_FILE, database);
}

function loadDatabase(file) {
  if (!existsSync(file)) {
    const empty = { licenses: {} };
    saveDatabase(file, empty);
    return empty;
  }
  return JSON.parse(readFileSync(file, "utf8"));
}

function saveDatabase(file, value) {
  writeFileSync(file, JSON.stringify(value, null, 2), "utf8");
}

function writeJson(res, statusCode, payload) {
  res.writeHead(statusCode, { "content-type": "application/json; charset=utf-8" });
  res.end(JSON.stringify(payload));
}

function sha256(value) {
  return createHash("sha256").update(value, "utf8").digest("hex");
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function readJsonBody(req) {
  return new Promise((resolve, reject) => {
    let raw = "";
    req.setEncoding("utf8");
    req.on("data", chunk => {
      raw += chunk;
      if (raw.length > 1024 * 1024) {
        reject(new Error("request body too large"));
      }
    });
    req.on("end", () => resolve(JSON.parse(raw || "{}")));
    req.on("error", reject);
  });
}
