async function accionTarea(id, action) {
  if (
    action === "complete" &&
    !S.tasks.find((t) => t.id === Number(id))?.timed
  ) {
    confirmarAccion(
      "Completar sin cronómetro",
      "La tarea se guardará en el historial con 0 monedas. ¿Quieres continuar?",
      "manual-complete",
      id,
    );
    return;
  }
  try {
    const t = await api(`/tasks/${id}/${action}`, "POST");
    toast(
      action === "complete"
        ? `Tarea completada. Has ganado ${t.coins} monedas.`
        : "Sesión actualizada",
    );
    await refresh();
  } catch (e) {
    if (action === "complete" && (e.status === 0 || e.status === 503)) {
      const key = "timeforge.pending." + S.me.id;
      const pending = JSON.parse(localStorage.getItem(key) || "[]");
      if (!pending.includes(Number(id))) pending.push(Number(id));
      localStorage.setItem(key, JSON.stringify(pending));
      toast(
        "Conexión perdida. La finalización queda pendiente y se reintentará al reconectar.",
        true,
      );
    } else throw e;
  }
}
async function reintentarPendientes() {
  if (!S.me || busy) return;
  const key = "timeforge.pending." + S.me.id;
  let pending = JSON.parse(localStorage.getItem(key) || "[]");
  for (const id of [...pending]) {
    try {
      await api(`/tasks/${id}/complete`, "POST");
      pending = pending.filter((v) => v !== id);
      localStorage.setItem(key, JSON.stringify(pending));
      toast("La tarea pendiente se ha sincronizado.");
      await refresh();
    } catch (e) {
      if ([400, 404].includes(e.status)) {
        pending = pending.filter((v) => v !== id);
        localStorage.setItem(key, JSON.stringify(pending));
        toast(e.message, true);
      }
      break;
    }
  }
}
async function cargarInvitaciones() {
  if (!$("#coop-invitations")) return;
  try {
    const invitations = await api("/coop");
    if (!$("#coop-invitations")) return;
    $("#coop-invitations").innerHTML = invitations
      .map(
        (i) =>
          `<div class="friend-row"><div><h3>${esc(i.senderName)} te invita</h3><p>${i.minutes} minutos · ${i.cost} monedas por persona</p></div><button class="small lime" data-action="accept-coop" data-id="${i.id}">Aceptar y canjear</button><button class="small" data-action="reject-coop" data-id="${i.id}">Rechazar</button></div>`,
      )
      .join("");
  } catch (e) {
    toast(e.message, true);
  }
}
document.addEventListener("click", async (event) => {
  const b = event.target.closest("button");
  if (!b || b.disabled || (b.type === "submit" && b.closest("form"))) return;
  if (b.dataset.page) {
    try {
      await navigate(b.dataset.page);
    } catch (e) {
      toast(e.message, true);
    }
    return;
  }
  if (b.dataset.filter) {
    S.filter = b.dataset.filter;
    render();
    return;
  }
  const action = b.dataset.action,
    id = b.dataset.id;
  if (!action) return;
  if (action === "close-modal") {
    $("#modal").close();
    return;
  }
  if (action === "new-task") return formularioTarea();
  if (action === "edit-task") return formularioTarea(id);
  if (action === "new-work") return formularioObra();
  if (action === "edit-progress") return formularioProgreso(id);
  if (action === "search-external") return buscarObrasExternas();
  if (action === "find-friend") return buscarAmigo();
  if (b.dataset.task) {
    if (busy) return;
    busy = true;
    try {
      await accionTarea(b.dataset.task, action);
    } catch (e) {
      toast(e.message, true);
    } finally {
      busy = false;
    }
    return;
  }
  if (action === "delete-task")
    return confirmarAccion(
      "Borrar tarea",
      "Se eliminará esta tarea pendiente.",
      "confirm-delete-task",
      id,
    );
  if (action === "delete-work")
    return confirmarAccion(
      "Retirar obra",
      "Se eliminará de tu biblioteca. El historial de descansos y misiones se conserva.",
      "confirm-delete-work",
      id,
    );
  if (action === "cancel-rest")
    return confirmarAccion(
      "Terminar descanso",
      "Volverás a tus tareas. Las monedas gastadas no se devuelven.",
      "confirm-cancel-rest",
      id,
    );
  if (action === "admin-user")
    return confirmarAccion(
      "Confirmar cambio de cuenta",
      "Acción: " +
        b.dataset.operation +
        ". Se aplicará a la cuenta seleccionada.",
      "confirm-admin",
      id,
      `data-operation="${b.dataset.operation}"`,
    );
  if (action === "coop") {
    modal(
      "Invitar a una tirada conjunta",
      `<form id="coop-form" data-id="${id}"><p>Tu amigo deberá aceptar. Ambos pagaréis el coste de la pausa y recibiréis la misma obra.</p><label class="field">Duración<select name="minutes"><option value="15">15 min · 80 monedas por persona</option><option value="30">30 min · 150 monedas por persona</option><option value="60">60 min · 280 monedas por persona</option></select></label>${actions("Enviar invitación")}</form>`,
    );
    return;
  }
  if (busy) return;
  busy = true;
  b.disabled = true;
  try {
    let close = true;
    switch (action) {
      case "confirm-delete-task":
        await api("/tasks/" + id, "DELETE");
        break;
      case "manual-complete": {
        const t = await api("/tasks/" + id + "/complete", "POST");
        toast(`Tarea completada: ${t.coins} monedas.`);
        break;
      }
      case "confirm-delete-work":
        await api("/catalog/" + id, "DELETE");
        break;
      case "confirm-cancel-rest":
        await api("/rests/" + id + "/cancel", "POST");
        break;
      case "claim":
        await api("/missions/" + id + "/claim", "POST");
        toast("¡Recompensa reclamada!");
        break;
      case "classify": {
        const a = await api("/catalog/" + id + "/classify", "POST");
        $("#classification").hidden = false;
        $("#classification").textContent =
          a.text + (a.source === "FALLBACK" ? " (Modo de respaldo)" : "");
        close = false;
        break;
      }
      case "import-work":
        await api("/catalog/import", "POST", {
          source: b.dataset.source,
          externalId: Number(id),
        });
        toast("Obra importada y guardada.");
        break;
      case "add-friend":
        await api("/friends", "POST", { username: b.dataset.username });
        toast("Solicitud enviada.");
        break;
      case "accept-friend":
      case "reject-friend":
        await api("/friends/" + id, "POST", {
          accept: action === "accept-friend",
        });
        break;
      case "remove-friend":
        await api("/friends/" + id, "DELETE");
        break;
      case "accept-coop":
      case "reject-coop":
        await api("/coop/" + id, "POST", { accept: action === "accept-coop" });
        if (action === "accept-coop") S.page = "oracle";
        break;
      case "confirm-admin":
        await api("/admin/users/" + id, "POST", {
          action: b.dataset.operation,
        });
        break;
      case "history": {
        const m = await api("/metrics");
        modal(
          "Tu historial de concentración",
          `<div class="stats"><div class="stat"><small>Tareas completadas</small><strong>${m.completed}</strong></div><div class="stat"><small>Tiempo real</small><strong>${Math.round(m.focusSeconds / 60)} min</strong></div><div class="stat"><small>A tiempo</small><strong>${m.onTime}</strong></div><div class="stat"><small>Monedas ganadas</small><strong>${m.coins}</strong></div></div><p class="help">Tiempo estimado total: ${m.estimatedMinutes} minutos. Las tareas completadas sin cronómetro permanecen en el historial, sin recompensa.</p><div class="form-actions"><button data-action="close-modal">Cerrar</button></div>`,
        );
        close = false;
        break;
      }
      case "logout":
        await api("/auth/logout", "POST");
        S.me = null;
        csrf = null;
        await token();
        mostrarAcceso();
        return;
      case "toggle-auth":
        S.register = !S.register;
        mostrarAcceso();
        return;
    }
    if (close) {
      $("#modal").close();
      await refresh();
    }
  } catch (e) {
    if ($("#modal").open && $("#modal-error"))
      $("#modal-error").textContent = e.message;
    else toast(e.message, true);
  } finally {
    busy = false;
    b.disabled = false;
  }
});
document.addEventListener("submit", async (event) => {
  const form = event.target;
  if (!(form instanceof HTMLFormElement)) return;
  event.preventDefault();
  if (busy) return;
  busy = true;
  const button = form.querySelector('button[type="submit"],button:not([type])');
  if (button) button.disabled = true;
  const data = Object.fromEntries(new FormData(form));
  try {
    let keep = false;
    switch (form.id) {
      case "auth-form": {
        if (S.register) await api("/auth/register", "POST", data);
        const body = new URLSearchParams({
          username: data.username.toLowerCase(),
          password: data.password,
        });
        const response = await fetch("/api/auth/login", {
          method: "POST",
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
            [csrf.headerName]: csrf.token,
          },
          body,
        });
        if (!response.ok)
          throw new Error("Credenciales incorrectas o cuenta suspendida");
        await token();
        S.page = pages[location.hash.slice(1)]
          ? location.hash.slice(1)
          : "home";
        await refresh();
        return;
      }
      case "task-form":
        data.estimatedMinutes = Number(data.estimatedMinutes);
        await api(
          "/tasks" + (form.dataset.id ? "/" + form.dataset.id : ""),
          form.dataset.id ? "PUT" : "POST",
          data,
        );
        break;
      case "work-form":
        data.totalUnits = Number(data.totalUnits);
        data.minutesPerUnit = Number(data.minutesPerUnit);
        data.quickSession = data.quickSession === "on";
        await api("/catalog", "POST", data);
        break;
      case "progress-form":
        for (const k of ["currentUnits", "volume", "season"])
          data[k] = Number(data[k]);
        await api("/catalog/" + form.dataset.id, "PUT", data);
        break;
      case "profile-form":
        await api("/me", "PUT", data);
        toast("Perfil actualizado.");
        break;
      case "spin-form":
        $("#wheel")?.classList.add("spinning");
        await api("/rests", "POST", { minutes: Number(data.minutes) });
        toast("¡Tu descanso ya está en marcha!");
        break;
      case "external-search": {
        $("#modal-error").textContent = "";
        $("#external-results").replaceChildren();
        const list = await api(
          "/catalog/search?source=" +
            encodeURIComponent(data.source) +
            "&q=" +
            encodeURIComponent(data.q.trim()),
        );
        $("#external-results").innerHTML =
          list
            .map(
              (w) =>
                `<div class="search-result">${w.imagen ? `<img src="${esc(w.imagen)}" alt="" loading="lazy" referrerpolicy="no-referrer">` : ""}<span>${esc(w.title)}${w.anio ? ` (${esc(w.anio)})` : ""}${["STEAM", "IGDB"].includes(w.source) ? `<small>Historia principal · ${w.horasHistoriaPrincipal ? `≈ ${w.horasHistoriaPrincipal.toLocaleString("es-ES")} h` : "Sin estimación"}</small>` : ""}</span><button type="button" class="small lime" data-action="import-work" data-id="${w.id}" data-source="${w.source}">Añadir</button></div>`,
            )
            .join("") ||
          "<p>No se encontraron obras. Prueba con otro título.</p>";
        keep = true;
        break;
      }
      case "friend-search": {
        const list = await api("/users?q=" + encodeURIComponent(data.q));
        $("#friend-results").innerHTML =
          list
            .filter((u) => u.id !== S.me.id)
            .map(
              (u) =>
                `<div class="search-result"><span>${esc(u.displayName)} · @${esc(u.username)} · Nivel ${u.level}</span><button type="button" class="small" data-action="add-friend" data-username="${esc(u.username)}">Añadir</button></div>`,
            )
            .join("") || "<p>No se encontraron usuarios.</p>";
        keep = true;
        break;
      }
      case "coop-form":
        await api("/coop", "POST", {
          friendId: Number(form.dataset.id),
          minutes: Number(data.minutes),
        });
        toast("Invitación enviada. Tu amigo debe aceptarla.");
        break;
    }
    if (!keep) {
      $("#modal").close();
      await refresh();
    }
  } catch (e) {
    const error = $("#modal").open ? $("#modal-error") : $("#auth-error");
    if (error)
      error.textContent =
        e.message + (e.fields ? " · " + Object.keys(e.fields).join(", ") : "");
    else toast(e.message, true);
  } finally {
    busy = false;
    if (button) button.disabled = false;
    $("#wheel")?.classList.remove("spinning");
    if (form.id === "spin-form") updateSpin();
  }
});
document.addEventListener("change", (e) => {
  if (e.target.matches("#work-form select[name=type]")) mostrarUnidadesObra();
  if (e.target.name === "minutes") updateSpin();
  if (e.target.id === "catalog-type") {
    S.catalogType = e.target.value;
    render();
  }
});
document.addEventListener("input", (e) => {
  if (e.target.id === "catalog-query") {
    S.query = e.target.value;
    const list = S.catalog.filter(
      (p) =>
        (S.catalogType === "ALL" || p.work.type === S.catalogType) &&
        p.work.title.toLowerCase().includes(S.query.toLowerCase()),
    );
    $("#catalog-grid").innerHTML =
      list.map(tarjetaObra).join("") ||
      empty("Sin coincidencias", "Prueba con otro título.");
  }
});
document.addEventListener(
  "error",
  (e) => {
    if (e.target.matches?.("[data-cover]")) e.target.remove();
  },
  true,
);
document.addEventListener(
  "load",
  (e) => {
    if (e.target.matches?.("[data-cover]")) {
      const initials = e.target.parentNode.querySelector(".initials");
      if (initials) initials.hidden = true;
    }
  },
  true,
);
function tick() {
  if (!S.me) return;
  document.querySelectorAll("[data-clock-task]").forEach((el) => {
    const t = S.tasks.find((t) => t.id === Number(el.dataset.clockTask));
    if (t) el.textContent = fmt(elapsed(t));
  });
  document.querySelectorAll("[data-rest-clock]").forEach((el) => {
    const r = S.rests.find((r) => r.id === Number(el.dataset.restClock));
    if (r) el.textContent = fmt((Date.parse(r.endsAt) - now()) / 1000);
  });
  const expired = S.rests.find(
    (r) => !r.cancelled && !r.completed && Date.parse(r.endsAt) <= now(),
  );
  if (expired && lastRestAlert !== expired.id) {
    lastRestAlert = expired.id;
    toast("Tu descanso ha terminado. ¡Es hora de volver a tus objetivos!");
    api("/rests/" + expired.id + "/finish", "POST")
      .then(() => refresh())
      .catch((e) => toast(e.message, true));
  }
}
function mostrarAcceso() {
  $("#app").innerHTML =
    `<main class="auth-page"><section class="auth-intro"><div class="brand"><img src="/favicon.svg" alt="">Time<em>Forge</em></div><h1>Forja tu tiempo.<br><span>Disfruta tu recompensa.</span></h1><p>Convierte tus tareas en progreso y tu progreso en tiempo para las historias que te apasionan.</p><div class="auth-steps"><div><strong>01</strong><small>Encuentra tu foco</small></div><div><strong>02</strong><small>Gana monedas</small></div><div><strong>03</strong><small>Disfruta tu pausa</small></div></div></section><section class="card auth-card"><span class="eyebrow">Tu aventura comienza aquí</span><h2>${S.register ? "Crea tu cuenta" : "Bienvenido de nuevo"}</h2><p>${S.register ? "Un espacio para tus metas y tus historias." : "Tu próximo gran paso te está esperando."}</p><form id="auth-form">${field("Nombre de usuario", "username", "", 'required minlength="3" maxlength="40" pattern="[a-zA-Z0-9_]{3,40}" autocomplete="username"')}${S.register ? field("Correo electrónico", "email", "", 'type="email" required maxlength="160" autocomplete="email"') : ""}${field("Contraseña", "password", "", `type="password" required ${S.register ? 'minlength="10" maxlength="72" autocomplete="new-password"' : 'autocomplete="current-password"'}`)}<p class="error" id="auth-error" role="alert"></p><button class="primary wide" type="submit">${S.register ? "Crear cuenta" : "Entrar en TimeForge"} →</button></form><div class="form-actions"><button type="button" class="ghost wide" data-action="toggle-auth">${S.register ? "Ya tengo cuenta · Iniciar sesión" : "¿Tu primera vez? Crea una cuenta"}</button></div><footer>Tus datos se guardan en tu cuenta. Tu biblioteca, tareas y monedas estarán aquí cuando vuelvas.</footer></section></main>`;
}
window.addEventListener("online", reintentarPendientes);
setInterval(tick, 1000);
setInterval(() => {
  if (S.me && !busy && !$("#modal").open) {
    reintentarPendientes();
    if (S.page === "oracle") cargarOraculo();
    if (S.page === "friends") cargarInvitaciones();
  }
}, 10000);
setInterval(async () => {
  if (
    S.me &&
    !busy &&
    !$("#modal").open &&
    !document.querySelector("input:focus,textarea:focus,select:focus")
  )
    try {
      await refresh();
    } catch {
      /* Se mantienen los datos visibles mientras no haya conexión. */
    }
}, 30000);
(async () => {
  try {
    await token();
    S.me = await api("/me");
    S.page = pages[location.hash.slice(1)] ? location.hash.slice(1) : "home";
    await refresh();
    await reintentarPendientes();
  } catch (e) {
    mostrarAcceso();
    if (e.status !== 401) toast(e.message, true);
  }
})();
