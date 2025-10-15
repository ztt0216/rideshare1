// Frontend API helpers aligned with Part 3 servlet backend (with optional mock fallback)

export const USE_MOCK = String(import.meta.env.VITE_USE_MOCK || "0") === "1";
export const BASE = (import.meta.env.VITE_API_BASE || "/api").replace(/\/$/, "");

const STORAGE_KEY = "session_user";
const DAY_TO_INDEX = {
  MON: 1,
  TUE: 2,
  WED: 3,
  THU: 4,
  FRI: 5,
  SAT: 6,
  SUN: 7,
};
const INDEX_TO_DAY = Object.fromEntries(
  Object.entries(DAY_TO_INDEX).map(([k, v]) => [v, k])
);

let TOKEN = sessionStorage.getItem("auth_token") || "";

const MOCK_DELAY = 180;
const MOCK_STATE = USE_MOCK ? createMockState() : null;

function createMockState() {
  const now = Date.now();
  return {
    nextUserId: 3,
    users: [
      {
        id: 1,
        name: "Riley Rider",
        email: "rider@example.com",
        password: "password",
        role: "RIDER",
      },
      {
        id: 2,
        name: "Derek Driver",
        email: "driver@example.com",
        password: "password",
        role: "DRIVER",
      },
    ],
    wallets: new Map([
      [1, 220],
      [2, 140],
    ]),
    availability: new Map([
      [2, [
        { id: 1, day: "MON", start: "09:00", end: "17:00" },
        { id: 2, day: "TUE", start: "10:00", end: "16:00" },
      ]],
    ]),
    nextAvailabilityId: 3,
    rides: [
      {
        id: 1,
        riderId: 1,
        driverId: 2,
        status: "COMPLETED",
        pickup: "3000",
        destination: "3056",
        fare: 40,
        requestedAt: now - 1000 * 60 * 60 * 24 * 2,
        acceptedAt: now - 1000 * 60 * 60 * 24 * 2 + 1000 * 60 * 15,
        startedAt: now - 1000 * 60 * 60 * 24 * 2 + 1000 * 60 * 30,
        completedAt: now - 1000 * 60 * 60 * 24 * 2 + 1000 * 60 * 50,
        paymentTimestamp: now - 1000 * 60 * 60 * 24 * 2 + 1000 * 60 * 50,
      },
      {
        id: 2,
        riderId: 1,
        driverId: null,
        status: "REQUESTED",
        pickup: "3056",
        destination: "3045",
        fare: 60,
        requestedAt: now - 1000 * 60 * 45,
      },
    ],
    nextRideId: 3,
  };
}

function deepCopy(value) {
  if (value === undefined) return undefined;
  return JSON.parse(JSON.stringify(value));
}

function mockResolve(value) {
  return new Promise((resolve) => {
    setTimeout(() => resolve(deepCopy(value)), MOCK_DELAY);
  });
}

function mockReject(message, status) {
  return new Promise((_, reject) => {
    setTimeout(() => {
      const error = new Error(message);
      if (status) error.status = status;
      reject(error);
    }, MOCK_DELAY);
  });
}

function mockFindUserByEmail(email) {
  const lower = String(email || "").toLowerCase();
  return MOCK_STATE.users.find((u) => u.email.toLowerCase() === lower) || null;
}

function mockEnsureWallet(userId) {
  if (!MOCK_STATE.wallets.has(userId)) {
    MOCK_STATE.wallets.set(userId, 0);
  }
  return MOCK_STATE.wallets.get(userId);
}

function mockCalcFare(pickup, destination) {
  const p = Number.parseInt(pickup, 10);
  const d = Number.parseInt(destination, 10);
  if (Number.isNaN(p) || Number.isNaN(d)) return 40;
  if (p === 3045 || d === 3045) return 60;
  const isVic = (x) => x >= 3000 && x <= 3999;
  const isMetro = (x) => x >= 3000 && x <= 3299;
  const isRegional = (x) => isVic(x) && !isMetro(x);
  if (!isVic(p) || !isVic(d)) return 500;
  if (isRegional(p) || isRegional(d)) return 220;
  return 40;
}

function mockAvailabilityFor(driverId) {
  if (!MOCK_STATE.availability.has(driverId)) {
    MOCK_STATE.availability.set(driverId, []);
  }
  return MOCK_STATE.availability.get(driverId);
}

function ensureTrailingSlash(value) {
  return value.endsWith("/") ? value : `${value}/`;
}

function baseForUrl() {
  if (BASE.startsWith("http://") || BASE.startsWith("https://")) {
    return ensureTrailingSlash(BASE);
  }

  if (typeof window === "undefined") {
    return ensureTrailingSlash(`http://localhost:8080${BASE.startsWith("/") ? BASE : `/${BASE}`}`);
  }

  if (window.location.protocol === "file:") {
    return ensureTrailingSlash(`http://localhost:8080${BASE.startsWith("/") ? BASE : `/${BASE}`}`);
  }

  const origin = window.location.origin || "";
  const prefix = BASE.startsWith("/") ? BASE : `/${BASE}`;
  return ensureTrailingSlash(`${origin}${prefix}`);
}

function buildUrl(path, query, base = baseForUrl()) {
  const normalizedPath = path.replace(/^\//, "");
  const url = new URL(normalizedPath, base);
  if (query && typeof query === "object") {
    Object.entries(query)
      .filter(([, value]) => value !== undefined && value !== null)
      .forEach(([key, value]) => url.searchParams.append(key, value));
  }
  return url.toString();
}

function resolveBaseCandidates() {
  const candidates = [];
  const seen = new Set();
  const add = (candidate) => {
    if (!candidate) return;
    const normalized = ensureTrailingSlash(candidate);
    if (!seen.has(normalized)) {
      seen.add(normalized);
      candidates.push(normalized);
    }
  };

  add(baseForUrl());

  if (typeof window !== "undefined") {
    const { protocol, hostname, port } = window.location;
    if (
      protocol && protocol.startsWith("http") &&
      hostname &&
      !BASE.startsWith("http") &&
      BASE === "/api"
    ) {
      if (port && port !== "" && port !== "80" && port !== "443" && port !== "8080") {
        add(`${protocol}//${hostname}:8080/api/`);
      }
    }

    if (window.location.protocol === "file:" && !BASE.startsWith("http")) {
      add(`http://localhost:8080${BASE.startsWith("/") ? BASE : `/${BASE}`}`);
    }
  }

  return candidates;
}

async function http(path, options = {}) {
  const {
    method = "GET",
    headers = {},
    body,
    query,
    raw = false,
  } = options;

  const candidates = resolveBaseCandidates();
  let lastError;

  const preparedBody =
    body === undefined ||
    body === null ||
    body instanceof FormData ||
    body instanceof Blob ||
    typeof body === "string"
      ? body
      : JSON.stringify(body);
  const isJsonBody =
    body !== undefined &&
    body !== null &&
    !(body instanceof FormData) &&
    !(body instanceof Blob) &&
    typeof body !== "string";

  for (const base of candidates) {
    try {
      const initHeaders = new Headers(headers);
      const token = TOKEN || sessionStorage.getItem("auth_token") || "";
      if (token) initHeaders.set("Authorization", `Bearer ${token}`);
      const init = {
        method,
        headers: initHeaders,
        credentials: "include",
      };

      if (preparedBody !== undefined) {
        init.body = preparedBody;
        if (isJsonBody && !initHeaders.has("Content-Type")) {
          initHeaders.set("Content-Type", "application/json");
        }
      }

      const url = buildUrl(path, query, base);
      const response = await fetch(url, init);

      if (!response.ok) {
        const text = await response.text().catch(() => "");
        const error = new Error(text || `HTTP ${response.status}`);
        error.status = response.status;
        error.body = text;
        error.url = url;

        if (response.status === 404 && base !== candidates[candidates.length - 1]) {
          lastError = error;
          continue;
        }

        throw error;
      }

      if (raw) return response;

      const contentType = response.headers.get("content-type") || "";
      if (contentType.includes("application/json")) {
        return response.json();
      }
      if (response.status === 204) {
        return null;
      }
      return response.text();
    } catch (error) {
      lastError = error;
    }
  }

  throw lastError ?? new Error("Request failed");
}

async function requestWithPaths(paths, options) {
  let lastError;
  for (const path of paths) {
    try {
      return await http(path, options);
    } catch (error) {
      lastError = error;
    }
  }
  throw lastError ?? new Error("Request failed");
}

function saveSessionUser(user) {
  if (!user) return;
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(user));
}

function clearSessionUser() {
  sessionStorage.removeItem(STORAGE_KEY);
}

export function getSessionUser() {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch (error) {
    console.warn("Failed to parse session user", error);
    return null;
  }
}

export function me() {
  return getSessionUser() || {};
}

function requireSession(role) {
  const user = getSessionUser();
  if (!user?.id) {
    throw new Error("Please sign in first.");
  }
  if (role && String(user.role).toUpperCase() !== String(role).toUpperCase()) {
    throw new Error(`This action requires role ${role}.`);
  }
  return user;
}

async function mockAuthLogin(email, password) {
  const user = mockFindUserByEmail(email);
  if (!user || user.password !== password) {
    return mockReject("Invalid email or password", 401);
  }
  return mockResolve({
    userId: user.id,
    name: user.name,
    email: user.email,
    role: user.role,
  });
}

async function mockAuthRegister({ name, email, password, role }) {
  if (!email || !password || !name || !role) {
    return mockReject("Missing fields", 400);
  }
  if (mockFindUserByEmail(email)) {
    return mockReject("Email already registered", 409);
  }
  const user = {
    id: MOCK_STATE.nextUserId++,
    name,
    email,
    password,
    role: role.toUpperCase(),
  };
  MOCK_STATE.users.push(user);
  MOCK_STATE.wallets.set(user.id, user.role === "DRIVER" ? 0 : 150);
  return mockResolve({
    userId: user.id,
    name: user.name,
    email: user.email,
    role: user.role,
  });
}

async function mockFetchWallet(userId) {
  const balance = mockEnsureWallet(userId);
  return mockResolve({ ok: true, userId, balance });
}

async function mockTopUpWallet(userId, amount) {
  if (amount <= 0) {
    return mockReject("Amount must be positive", 400);
  }
  const current = mockEnsureWallet(userId);
  MOCK_STATE.wallets.set(userId, current + amount);
  return mockResolve({ ok: true, userId, balance: current + amount });
}

async function mockListAvailability(driverId) {
  const slots = mockAvailabilityFor(driverId).map((slot) => ({ ...slot }));
  return mockResolve({ driverId, items: slots });
}

async function mockAddAvailability(driverId, day, start, end) {
  const slots = mockAvailabilityFor(driverId);
  const slot = {
    id: MOCK_STATE.nextAvailabilityId++,
    day,
    start,
    end,
  };
  slots.push(slot);
  return mockResolve({ ok: true, slot });
}

async function mockDeleteAvailability(driverId, slot) {
  const slots = mockAvailabilityFor(driverId);
  const id = typeof slot === "number" ? slot : slot?.id;
  if (!id) {
    return mockReject("slot id required", 400);
  }
  const index = slots.findIndex((s) => s.id === id);
  if (index >= 0) {
    slots.splice(index, 1);
  }
  return mockResolve({ ok: true });
}

function mockIsDriverAvailableNow(driverId) {
  const slots = mockAvailabilityFor(driverId);
  if (slots.length === 0) return true;
  const now = new Date();
  const today = INDEX_TO_DAY[now.getDay()] || "SUN";
  const minutes = now.getHours() * 60 + now.getMinutes();
  return slots.some((slot) => {
    if ((slot.day || "").toUpperCase() !== today) return false;
    const start = parseHm(slot.start);
    const end = parseHm(slot.end);
    return start <= minutes && minutes < end;
  });
}

function parseHm(value) {
  const [h, m] = String(value || "00:00").split(":").map((n) => parseInt(n, 10) || 0);
  return h * 60 + m;
}

async function mockListDriverAvailableRides(driverId) {
  const insideWindow = mockIsDriverAvailableNow(driverId);
  const rides = MOCK_STATE.rides
    .filter((ride) => ride.status === "REQUESTED")
    .map((ride) => ({
      id: ride.id,
      pickup: ride.pickup,
      destination: ride.destination,
      fare: ride.fare,
      requestedAt: ride.requestedAt,
    }));
  return mockResolve(insideWindow ? rides : []);
}

async function mockCreateRide({ riderId, pickup, destination }) {
  const fare = mockCalcFare(pickup, destination);
  const balance = mockEnsureWallet(riderId);
  if (balance < fare) {
    return mockReject("Insufficient funds", 400);
  }
  MOCK_STATE.wallets.set(riderId, balance - fare);
  const ride = {
    id: MOCK_STATE.nextRideId++,
    riderId,
    driverId: null,
    status: "REQUESTED",
    pickup,
    destination,
    fare,
    requestedAt: Date.now(),
  };
  MOCK_STATE.rides.push(ride);
  return mockResolve({
    ok: true,
    rideId: ride.id,
    riderId,
    pickup_postcode: pickup,
    destination_postcode: destination,
    fare,
  });
}

async function mockCancelRide(rideId, riderId) {
  const ride = MOCK_STATE.rides.find((r) => r.id === rideId && r.riderId === riderId);
  if (!ride) {
    return mockReject("Ride not found", 404);
  }
  if (ride.status === "COMPLETED") {
    return mockReject("Ride already completed", 400);
  }
  if (ride.status !== "REQUESTED") {
    return mockReject("Ride cannot be cancelled now", 409);
  }
  ride.status = "CANCELLED";
  const balance = mockEnsureWallet(riderId);
  MOCK_STATE.wallets.set(riderId, balance + ride.fare);
  return mockResolve({ ok: true, rideId });
}

async function mockAcceptRide(rideId, driverId) {
  const ride = MOCK_STATE.rides.find((r) => r.id === rideId);
  if (!ride) {
    return mockReject("Ride not found", 404);
  }
  if (ride.status !== "REQUESTED") {
    return mockReject("Ride already taken", 409);
  }
  ride.status = "ACCEPTED";
  ride.driverId = driverId;
  ride.acceptedAt = Date.now();
  return mockResolve({ ok: true, rideId, status: "ACCEPTED" });
}

async function mockBeginRide(rideId, driverId) {
  const ride = MOCK_STATE.rides.find((r) => r.id === rideId && r.driverId === driverId);
  if (!ride) {
    return mockReject("Ride not found", 404);
  }
  if (ride.status !== "ACCEPTED") {
    return mockReject("Ride not in ACCEPTED state", 409);
  }
  ride.status = "ENROUTE";
  ride.startedAt = Date.now();
  return mockResolve({ ok: true, rideId, status: "ENROUTE" });
}

async function mockCompleteRide(rideId, driverId) {
  const ride = MOCK_STATE.rides.find((r) => r.id === rideId && r.driverId === driverId);
  if (!ride) {
    return mockReject("Ride not found", 404);
  }
  if (ride.status !== "ENROUTE") {
    return mockReject("Ride not in ENROUTE state", 409);
  }
  ride.status = "COMPLETED";
  ride.completedAt = Date.now();
  ride.paymentTimestamp = ride.completedAt;
  const driverBalance = mockEnsureWallet(driverId);
  MOCK_STATE.wallets.set(driverId, driverBalance + ride.fare);
  return mockResolve({ ok: true, rideId, status: "COMPLETED" });
}
async function mockFetchRiderHistoryApi(riderId) {
  return mockResolve(
    MOCK_STATE.rides
      .filter((ride) => ride.riderId === riderId)
      .map((ride) => ({
        rideId: ride.id,
        pickup: ride.pickup,
        destination: ride.destination,
        status: ride.status,
        fare: ride.fare,
        paymentTimestamp: ride.paymentTimestamp || null,
        counterparty: ride.driverId
          ? MOCK_STATE.users.find((u) => u.id === ride.driverId)?.name || null
          : null,
      }))
      .sort((a, b) => (b.rideId ?? 0) - (a.rideId ?? 0))
  );
}

async function mockFetchDriverHistoryApi(driverId) {
  return mockResolve(
    MOCK_STATE.rides
      .filter((ride) => ride.driverId === driverId)
      .map((ride) => ({
        rideId: ride.id,
        pickup: ride.pickup,
        destination: ride.destination,
        status: ride.status,
        fare: ride.fare,
        paymentTimestamp: ride.paymentTimestamp || null,
        counterparty: MOCK_STATE.users.find((u) => u.id === ride.riderId)?.name || null,
      }))
      .sort((a, b) => (b.rideId ?? 0) - (a.rideId ?? 0))
  );
}

export async function authLogin({ email, password }) {
  if (USE_MOCK) {
    const payload = await mockAuthLogin(email, password);
    const user = {
      id: payload.userId,
      name: payload.name,
      email: payload.email,
      role: payload.role,
    };
    const token = `mock-${payload.userId}`;
    TOKEN = token;
    sessionStorage.setItem("auth_token", token);
    saveSessionUser(user);
    return user;
  }

  const payload = await requestWithPaths(
    ["/auth/login", "/login"],
    {
      method: "POST",
      body: { email, password },
    }
  );
  const token = payload?.token || "";
  if (token) {
    TOKEN = token;
    sessionStorage.setItem("auth_token", token);
  } else {
    TOKEN = "";
    sessionStorage.removeItem("auth_token");
  }
  const rawUser = payload?.user || payload;
  const user = {
    id: rawUser?.userId ?? rawUser?.id,
    name: rawUser?.name,
    email: rawUser?.email ?? email,
    role: rawUser?.role,
  };
  saveSessionUser(user);
  return user;
}

export async function authRegister({ name, email, password, role }) {
  if (USE_MOCK) {
    await mockAuthRegister({ name, email, password, role });
    return authLogin({ email, password });
  }

  await requestWithPaths(
    ["/auth/register", "/register"],
    {
      method: "POST",
      body: { name, email, password, role },
    }
  );
  return authLogin({ email, password });
}

export async function authLogout() {
  clearSessionUser();
  TOKEN = "";
  sessionStorage.removeItem("auth_token");
  if (USE_MOCK) {
    return mockResolve(true);
  }
  try {
    await http("/logout", { method: "POST" });
  } catch {
    // ignore
  }
  return true;
}

export function formatAud(value, { minimumFractionDigits = 2 } = {}) {
  const number =
    typeof value === "string" && Number.isNaN(Number(value))
      ? Number(String(value).replace(/[^\d.-]/g, ""))
      : Number(value);
  if (Number.isNaN(number)) return String(value ?? "");
  return new Intl.NumberFormat("en-AU", {
    style: "currency",
    currency: "AUD",
    minimumFractionDigits,
    maximumFractionDigits: minimumFractionDigits,
  }).format(number);
}

export async function fetchWallet(userId) {
  const user = userId ? { id: userId } : requireSession();
  if (USE_MOCK) {
    return mockFetchWallet(user.id);
  }
  const response = await http(`/wallet/${user.id}`, { raw: true });
  const text = await response.text();
  try {
    return JSON.parse(text);
  } catch (error) {
    return { ok: false, balance: 0, raw: text };
  }
}

export async function topUpWallet(amount, userId) {
  const user = userId ? { id: userId } : requireSession();
  const numericAmount = Number(amount);
  if (!Number.isFinite(numericAmount) || numericAmount <= 0) {
    throw new Error("Amount must be a positive number.");
  }
  if (USE_MOCK) {
    return mockTopUpWallet(user.id, numericAmount);
  }
  return http("/wallet/topup", {
    method: "POST",
    body: { userId: user.id, amount: numericAmount },
  });
}

function normalizeSlot(raw, fallbackDriverId) {
  if (!raw || typeof raw !== "object") return null;
  const day =
    raw.day?.toUpperCase?.() ??
    raw.availableDay?.toUpperCase?.() ??
    INDEX_TO_DAY[raw.dayOfWeek] ??
    "";
  const start =
    raw.start ??
    raw.startTime ??
    raw.start_time ??
    raw.begin ??
    raw.from ??
    "";
  const end =
    raw.end ??
    raw.endTime ??
    raw.end_time ??
    raw.finish ??
    raw.to ??
    "";
  return {
    id: raw.id ?? raw.availabilityId ?? null,
    driverId: raw.driverId ?? fallbackDriverId ?? null,
    day,
    start: String(start).slice(0, 5),
    end: String(end).slice(0, 5),
    dayOfWeek:
      raw.dayOfWeek ??
      raw.day_of_week ??
      raw.dayIndex ??
      DAY_TO_INDEX[day] ??
      null,
  };
}

export async function listAvailability(driverId) {
  const user = driverId ? { id: driverId } : requireSession("DRIVER");
  if (USE_MOCK) {
    return mockListAvailability(user.id).then((result) => result.items);
  }
  const response = await http(`/availability/${user.id}`, { raw: true });
  const text = await response.text();
  const parsed = text ? JSON.parse(text) : [];
  const items = Array.isArray(parsed)
    ? parsed
    : Array.isArray(parsed.items)
    ? parsed.items
    : Array.isArray(parsed.value)
    ? parsed.value
    : [];
  return items
    .map((item) => normalizeSlot(item, user.id))
    .filter(Boolean);
}

function normalizeDay(day) {
  return String(day || "")
    .trim()
    .slice(0, 3)
    .toUpperCase();
}

export async function addAvailability({ availableDay, startTime, endTime }) {
  const user = requireSession("DRIVER");
  const upperDay = normalizeDay(availableDay);
  const dayOfWeek = DAY_TO_INDEX[upperDay];
  if (!dayOfWeek) {
    throw new Error("Day must be one of MON..SUN");
  }
  if (!startTime || !endTime || endTime <= startTime) {
    throw new Error("Start must be before end");
  }
  if (USE_MOCK) {
    return mockAddAvailability(user.id, upperDay, startTime.slice(0, 5), endTime.slice(0, 5));
  }
  return http("/availability", {
    method: "POST",
    body: {
      driver_id: user.id,
      day_of_week: dayOfWeek,
      start_time: startTime.slice(0, 5),
      end_time: endTime.slice(0, 5),
    },
  });
}

export async function deleteAvailability(slot) {
  const user = requireSession("DRIVER");
  if (USE_MOCK) {
    return mockDeleteAvailability(user.id, slot);
  }
  throw new Error("Removing availability slots is not supported yet.");
}

export const deleteAvailabilitySmart = deleteAvailability;

const DAYS = [
  "SUNDAY",
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
];

export function isInsideAvailabilityNow(slots, now = new Date()) {
  if (!Array.isArray(slots) || slots.length === 0) return true;
  const today = DAYS[now.getDay()];
  const toMinutes = (hhmm) => {
    const [h, m] = String(hhmm ?? "0:0")
      .slice(0, 5)
      .split(":")
      .map((n) => parseInt(n, 10) || 0);
    return h * 60 + m;
  };
  const current = now.getHours() * 60 + now.getMinutes();
  return slots
    .filter(
      (slot) =>
        slot &&
        (slot.day === today ||
          slot.availableDay === today ||
          INDEX_TO_DAY[slot.dayOfWeek] === today)
    )
    .some((slot) => {
      const start = toMinutes(slot.start ?? slot.startTime);
      const end = toMinutes(slot.end ?? slot.endTime);
      return start <= current && current < end;
    });
}

function normalizeFareInput(value) {
  return String(value ?? "")
    .replace(/\D/g, "")
    .padStart(4, "0")
    .slice(0, 4);
}

export async function createRide({ origin, destination }) {
  const rider = requireSession("RIDER");
  const pickup = normalizeFareInput(origin);
  const dropoff = normalizeFareInput(destination);
  if (!pickup || !dropoff) {
    throw new Error("Please enter two valid 4-digit postcodes.");
  }
  if (USE_MOCK) {
    return mockCreateRide({ riderId: rider.id, pickup, destination: dropoff });
  }
  return requestWithPaths(
    ["/rider/ride/request", "/rides/request"],
    {
      method: "POST",
      body: {
        riderId: rider.id,
        pickup_postcode: pickup,
        destination_postcode: dropoff,
      },
    }
  );
}

export async function cancelRide(rideId) {
  const rider = requireSession("RIDER");
  if (USE_MOCK) {
    return mockCancelRide(Number(rideId), rider.id);
  }
  return requestWithPaths(
    ["/rider/ride/cancel", "/rides/cancel"],
    {
      method: "POST",
      body: { rideId },
    }
  );
}

export async function acceptRide(rideId) {
  const driver = requireSession("DRIVER");
  if (USE_MOCK) {
    return mockAcceptRide(Number(rideId), driver.id);
  }
  return requestWithPaths(
    ["/driver/ride/accept", "/rides/accept"],
    {
      method: "POST",
      body: { rideId, driverId: driver.id },
    }
  );
}

export async function beginRide(rideId) {
  const driver = requireSession("DRIVER");
  if (USE_MOCK) {
    return mockBeginRide(Number(rideId), driver.id);
  }
  return http("/driver/ride/begin", {
    method: "POST",
    body: { rideId },
  });
}

export async function completeRide(rideId) {
  const driver = requireSession("DRIVER");
  if (USE_MOCK) {
    return mockCompleteRide(Number(rideId), driver.id);
  }
  return http("/driver/ride/complete", {
    method: "POST",
    body: { rideId },
  });
}

export async function listDriverAvailableRides() {
  const driver = requireSession("DRIVER");
  if (USE_MOCK) {
    return mockListDriverAvailableRides(driver.id);
  }
  const data = await requestWithPaths(
    ["/driver/ride/requestList", "/rides/visible"],
    {
      method: "POST",
      body: { driverId: driver.id },
    }
  );

  if (Array.isArray(data)) {
    return data.map((item, index) => ({
      id: item.id ?? item.rideId ?? item.ride_id ?? index,
      pickup: item.pickup ?? item.pickup_postcode ?? item.origin ?? "",
      destination: item.destination ?? item.destination_postcode ?? item.dest ?? "",
      fare: Number(item.fare ?? item.fare_estimate ?? 0),
      requestedAt: item.requestedAt ?? item.requested_time ?? item.created_at ?? null,
      raw: item,
    }));
  }

  if (data && typeof data === "object") {
    const items = Array.isArray(data.items)
      ? data.items
      : Array.isArray(data.rides)
      ? data.rides
      : [];
    return items.map((item, index) => ({
      id: item.id ?? item.rideId ?? item.ride_id ?? index,
      pickup: item.pickup ?? item.pickup_postcode ?? item.origin ?? "",
      destination: item.destination ?? item.destination_postcode ?? item.dest ?? "",
      fare: Number(item.fare ?? item.fare_estimate ?? 0),
      requestedAt: item.requestedAt ?? item.requested_time ?? item.created_at ?? null,
      raw: item,
    }));
  }

  return [];
}

function normalizeHistoryItem(item, role) {
  if (!item) return null;
  const lower = Object.fromEntries(
    Object.entries(item).map(([key, value]) => [key.toLowerCase(), value])
  );
  return {
    rideId: lower.rideid ?? lower.id ?? item.rideId ?? item.id,
    pickup: lower.pickup,
    destination: lower.destination,
    status: (lower.status ?? "").toUpperCase(),
    fare: Number(lower.fare ?? 0),
    paymentTimestamp: lower.paymenttimestamp ?? lower.timestamp ?? null,
    counterparty:
      role === "RIDER" ? lower.drivername ?? lower.driver : lower.ridername,
  };
}

export async function fetchRiderHistory() {
  const rider = requireSession("RIDER");
  if (USE_MOCK) {
    return mockFetchRiderHistoryApi(rider.id);
  }
  const data = await http("/history/rider", {
    method: "GET",
    query: { userId: rider.id },
  });
  const list = Array.isArray(data) ? data : JSON.parse(data ?? "[]");
  return list
    .map((item) => normalizeHistoryItem(item, "RIDER"))
    .filter(Boolean);
}

export async function fetchDriverHistory() {
  const driver = requireSession("DRIVER");
  if (USE_MOCK) {
    return mockFetchDriverHistoryApi(driver.id);
  }
  const data = await http("/history/driver", {
    method: "GET",
    query: { userId: driver.id },
  });
  const list = Array.isArray(data) ? data : JSON.parse(data ?? "[]");
  return list
    .map((item) => normalizeHistoryItem(item, "DRIVER"))
    .filter(Boolean);
}

export function groupRidesByStatus(rides = []) {
  return rides.reduce((acc, ride) => {
    const status = ride?.status ?? "UNKNOWN";
    if (!acc[status]) acc[status] = [];
    acc[status].push(ride);
    return acc;
  }, {});
}
