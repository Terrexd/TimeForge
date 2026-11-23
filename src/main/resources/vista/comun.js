"use strict";
const $ = (s, root = document) => root.querySelector(s);
const esc = (v) =>
  String(v ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
const paths = {
  trash: "M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7m4-7v7",
  home: "M3 11 12 3l9 8M5 10v11h5v-7h4v7h5V10",
  tasks: "M9 4h11v17H4V4h2M8 2h8v4H8zM7 11l2 2 3-4M14 11h3M7 17h10",
  timer: "M9 2h6M12 8v5l3 2M17 5l2-2M21 13a9 9 0 1 1-18 0 9 9 0 0 1 18 0",
  book: "M3 4h7l2 2 2-2h7v16h-7l-2 2-2-2H3zM12 6v16",
  oracle: "m12 2 3 7 7 3-7 3-3 7-3-7-7-3 7-3zM20 2v4M18 4h4",
  medal:
    "m7 2 5 7 5-7M5 2h14M18 15a6 6 0 1 1-12 0 6 6 0 0 1 12 0m-6-3 1 2 2 1-2 1-1 2-1-2-2-1 2-1z",
  user: "M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0M4 21v-3a8 8 0 0 1 16 0v3",
  settings:
    "m12 2 2 3 4-1 1 4 3 2-2 3 1 4-4 1-2 3-3-2-4 1-1-4-3-2 2-3-1-4 4-1zM15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0",
  play: "m8 4 12 8-12 8z",
  pause: "M8 4v16M16 4v16",
  plus: "M12 4v16M4 12h16",
  arrow: "M4 12h16m-6-6 6 6-6 6",
  flame:
    "M12 2c2 6 6 5 7 10 2 6-2 10-7 10S3 18 5 12c1-3 3-4 3-7 1 3 2 4 4 5 1-3 1-5 0-8z",
  coin: "M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0M13 5l-5 8h4l-1 6 5-9h-4z",
  check: "m5 12 4 4L20 5",
  game: "M7 7h10c4 0 6 12 3 13-2 1-4-4-5-4H9c-1 0-3 5-5 4-3-1-1-13 3-13zM6 11h4M8 9v4M16 11h.1M18 13h.1",
  exit: "M9 3H3v18h6M8 12h14m-5-5 5 5-5 5",
  shield: "m12 2 9 4v7c0 5-9 9-9 9S3 18 3 13V6z",
  search: "M17 10a7 7 0 1 1-14 0 7 7 0 0 1 14 0m-2 5 7 7",
};
const icon = (n) =>
  `<svg class="icon" aria-hidden="true" viewBox="0 0 24 24"><path d="${paths[n] || paths.oracle}"/></svg>`;
const types = {
  GAME: "Videojuego",
  MANGA: "Manga",
  COMIC: "Cómic",
  SERIES: "Serie",
  ANIME: "Anime",
  BOOK: "Libro",
  MOVIE: "Película",
};
const states = {
  PENDING: "Pendiente",
  IN_PROGRESS: "En curso",
  RUNNING: "En curso",
  PAUSED: "En pausa",
  COMPLETED: "Completada",
};
const units = (t) =>
  t === "GAME"
    ? "horas"
    : ["MANGA", "COMIC", "BOOK"].includes(t)
      ? "capítulos"
      : t === "MOVIE"
        ? "películas"
        : "episodios";
const date = (v) =>
  new Date(v).toLocaleString("es-ES", {
    timeZone: "Europe/Madrid",
    dateStyle: "short",
    timeStyle: "short",
  });
const fmt = (s) => {
  s = Math.max(0, Math.floor(s));
  return `${Math.floor(s / 60)
    .toString()
    .padStart(2, "0")}:${(s % 60).toString().padStart(2, "0")}`;
};
const S = {
  me: null,
  tasks: [],
  catalog: [],
  rests: [],
  missions: [],
  friends: [],
  page: "home",
  filter: "ALL",
  catalogType: "ALL",
  query: "",
  offset: 0,
  register: false,
};
let csrf,
  toastTimer,
  busy = false,
  lastRestAlert = null;
const now = () => Date.now() + S.offset;
const elapsed = (t) =>
  t.elapsedSeconds +
  (t.startedAt ? Math.max(0, (now() - Date.parse(t.startedAt)) / 1000) : 0);
const activeTask = () => S.tasks.find((t) => t.state === "RUNNING");
const activeRest = () =>
  S.rests.find(
    (r) => !r.cancelled && !r.completed && (r.paused || Date.parse(r.endsAt) > now()),
  );
function toast(message, error = false) {
  const el = $("#toast");
  el.textContent = message;
  el.className = error ? "error" : "";
  el.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (el.hidden = true), 6500);
}
async function token() {
  csrf = await (await fetch("/api/auth/csrf")).json();
}
async function api(path, method = "GET", body) {
  if (!csrf) await token();
  const headers = {};
  if (method !== "GET") headers[csrf.headerName] = csrf.token;
  if (body !== undefined) headers["Content-Type"] = "application/json";
  let response;
  try {
    response = await fetch("/api" + path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch (e) {
    throw Object.assign(
      new Error("Conexión perdida. Revisa la conexión y vuelve a intentarlo"),
      { status: 0 },
    );
  }
  const text = await response.text();
  let result;
  try {
    result = text ? JSON.parse(text) : null;
  } catch {
    result = null;
  }
  if (!response.ok) {
    const error = new Error(
      result?.message || "No se pudo completar la operación",
    );
    error.status = response.status;
    error.fields = result?.fields;
    if (response.status === 401 && S.me) {
      S.me = null;
      mostrarAcceso();
    }
    throw error;
  }
  return result;
}
