function botonNavegacion(page, label, symbol) {
  return `<button data-page="${page}" class="${S.page === page ? "active" : ""}" ${S.page === page ? 'aria-current="page"' : ""}>${icon(symbol)}<span>${label}</span>${page === "tasks" ? `<span class="count">${S.tasks.filter((t) => t.state !== "COMPLETED").length}</span>` : ""}</button>`;
}
const pages = {
  home: [
    "Panel de aventura",
    "¡Hola, " + "",
    "Hoy es un buen día para avanzar.",
  ],
  tasks: [
    "Centro de productividad",
    "Mis tareas",
    "Organiza tu jornada y acumula monedas.",
  ],
  timer: [
    "Sesión de enfoque",
    "Tu tiempo, tu progreso",
    "Una tarea a la vez. Cada minuto cuenta.",
  ],
  catalog: [
    "Tu universo pendiente",
    "Biblioteca de ocio",
    "Continúa las historias que te esperan.",
  ],
  oracle: [
    "Gacha y recomendación inteligente",
    "El Oráculo de Descanso",
    "Transforma tu esfuerzo en un descanso con propósito.",
  ],
  missions: [
    "Cada paso suma",
    "Misiones y recompensas",
    "Pequeños objetivos para seguir avanzando.",
  ],
  friends: [
    "Forja en compañía",
    "Perfil y amigos",
    "Comparte tus avances y descubre historias en común.",
  ],
  wallet: [
    "Tu esfuerzo tiene valor",
    "Billetera",
    "Todos tus movimientos, sin perder la cuenta.",
  ],
  settings: [
    "Tu espacio",
    "Configuración",
    "Haz que TimeForge se sienta como tú.",
  ],
  admin: [
    "Administración",
    "Control del sistema",
    "Gestiona cuentas y supervisa el servicio de IA.",
  ],
};
function shell(content) {
  const p = pages[S.page],
    a = S.me;
  $("#app").innerHTML =
    `<aside class="sidebar"><div class="brand"><img src="/favicon.svg" alt=""><span class="brand-label">Time<em>Forge</em></span></div><nav class="nav" aria-label="Navegación principal">${botonNavegacion("home", "Inicio", "home")}${botonNavegacion("tasks", "Mis tareas", "tasks")}${botonNavegacion("timer", "Temporizador", "timer")}${botonNavegacion("catalog", "Biblioteca de ocio", "book")}${botonNavegacion("oracle", "El Oráculo", "oracle")}${botonNavegacion("missions", "Misiones y recompensas", "medal")}${botonNavegacion("friends", "Perfil y amigos", "user")}${a.role === "ADMIN" ? botonNavegacion("admin", "Administración", "shield") : ""}</nav><div class="sidebar-bottom"><div class="streak">${icon("flame")}<div><strong>Racha de ${a.streak} días</strong><small>¡Sigue construyendo tu hábito!</small></div></div><button class="profile-mini" data-page="friends"><div class="avatar">${esc(a.avatar)}</div><div><strong>${esc(a.displayName)}</strong><small>Explorador nivel ${a.level}</small></div></button><nav class="nav">${botonNavegacion("settings", "Configuración", "settings")}</nav></div></aside><main class="main"><header class="topbar"><div><div class="eyebrow">${p[0]}</div><h1>${S.page === "home" ? "¡Hola, " + esc(a.displayName) + "!" : p[1]}</h1><p>${p[2]}</p></div><div class="top-actions"><button class="balance" data-page="wallet" aria-label="Consultar billetera"><span class="coin">${icon("coin")}</span><span><small>Tu saldo</small><strong>${a.balance.toLocaleString("es-ES")} monedas</strong></span></button>${S.page === "tasks" ? `<button class="lime" data-action="new-task">＋ Nueva tarea</button>` : ""}</div></header>${banner()}<div id="page-content">${content}</div><div class="footer-note">TimeForge · Tu esfuerzo construye tu tiempo libre.</div></main>`;
  tick();
}
function banner() {
  const t = activeTask(),
    r = activeRest();
  if (t && S.page !== "timer")
    return `<div class="timer-banner">${icon("timer")}<span>En progreso: <b>${esc(t.title)}</b></span><strong data-clock-task="${t.id}">${fmt(elapsed(t))}</strong><button class="small" data-task="${t.id}" data-action="pause">Pausar</button><button class="small ghost" data-page="timer">Ver sesión →</button></div>`;
  if (r && S.page !== "oracle")
    return `<div class="timer-banner">${icon("oracle")}<span>Descanso: <b>${esc(r.work.title)}</b></span><strong data-rest-clock="${r.id}"></strong><button class="small" data-page="oracle">Ver descanso →</button></div>`;
  return "";
}
function empty(title, description, button = "") {
  return `<div class="empty">${icon("oracle")}<h3>${title}</h3><p>${description}</p>${button}</div>`;
}
function filaTarea(t) {
  return `<div class="task-row"><button class="small ${t.state === "RUNNING" ? "primary" : ""}" data-action="${t.state === "RUNNING" ? "pause" : "start"}" data-task="${t.id}" aria-label="${t.state === "RUNNING" ? "Pausar" : "Iniciar"} ${esc(t.title)}">${icon(t.state === "RUNNING" ? "pause" : "play")}</button><div class="task-copy"><h3>${esc(t.title)}</h3><small>${esc(t.category)} · ${states[t.state]}</small></div><span class="tag ${t.priority}">${t.priority}</span><small>${t.estimatedMinutes} min</small></div>`;
}
function level() {
  return `<section class="card level-card"><div class="level-emblem"><strong>${S.me.level}</strong><small>NIVEL</small></div><div class="level-copy"><small>Explorador del tiempo</small><h2>Nivel ${S.me.level}</h2><div class="progress-label"><span>Tu próximo nivel</span><strong>${S.me.xp % 200} / 200 XP</strong></div><progress value="${S.me.xp % 200}" max="200"></progress><small>Te faltan ${200 - (S.me.xp % 200)} XP para seguir creciendo</small></div></section>`;
}
function tarjetaCronometro(large = false) {
  const t =
    activeTask() ||
    S.tasks.find((t) => t.state === "PAUSED") ||
    S.tasks.find((t) => t.state === "PENDING");
  return `<section class="card focus-card ${large ? "focus-large" : ""}"><div class="card-head"><div><span class="eyebrow">Sesión de enfoque</span><h2>${large && t ? esc(t.title) : "Concentración"}</h2></div><span class="tag cyan">${t ? states[t.state] : "Lista"}</span></div><div class="timer-ring"><svg viewBox="0 0 180 180" aria-hidden="true"><circle cx="90" cy="90" r="83"/><circle cx="90" cy="90" r="83"/></svg><strong ${t ? `data-clock-task="${t.id}"` : ""}>${t ? fmt(elapsed(t)) : "00:00"}</strong><small>TIEMPO INVERTIDO</small></div>${t ? `<div class="selected-task"><small>Tarea seleccionada · estimación ${t.estimatedMinutes} minutos</small><strong>${esc(t.title)}</strong></div><div class="task-actions"><button class="primary" data-action="${t.state === "RUNNING" ? "pause" : "start"}" data-task="${t.id}">${t.state === "RUNNING" ? "Ⅱ Pausar" : "▷ Iniciar concentración"}</button>${large ? `<button class="lime" data-action="complete" data-task="${t.id}">✓ Completar tarea</button><button class="ghost" data-action="cancel" data-task="${t.id}">Cancelar sesión</button>` : ""}</div>${large ? `<p>Recompensa base: <b class="gold">${t.estimatedMinutes * 2} monedas</b><br><small>Mínimo 5 minutos cronometrados para obtener monedas. La demora reduce la recompensa.</small></p>` : ""}` : empty("Tu próximo paso empieza aquí", "Crea una tarea para comenzar tu sesión.", `<button class="primary" data-action="new-task">Crear tarea</button>`)}</section>`;
}
function resumenMision(m) {
  return `<article class="card mission-mini">${icon("medal")}<span class="tag">+${m.reward} monedas</span><h3>${esc(m.work.title)}</h3><p>Avanza ${m.target} ${units(m.work.type)}</p><progress value="${Math.min(m.progress, m.target)}" max="${m.target}"></progress><small>${Math.min(m.progress, m.target)} / ${m.target}${m.claimed ? " · Reclamada" : ""}</small></article>`;
}
function home() {
  const pending = S.tasks.filter((t) => t.state !== "COMPLETED");
  return `<div class="dashboard"><div>${level()}<div class="section-title"><h2>Tus tareas pendientes <span class="count">${pending.length}</span></h2><button class="small ghost" data-page="tasks">Ver todas ›</button></div><section class="card list">${pending.length ? pending.slice(0, 4).map(filaTarea).join("") : empty("Un nuevo comienzo", "Añade tu primera tarea y dale espacio a lo que importa.", `<button class="primary" data-action="new-task">＋ Crear tarea</button>`)}</section><div class="section-title"><h2>Misiones semanales</h2><button class="small ghost" data-page="missions">Todas las misiones ›</button></div><div class="grid3">${S.missions.length ? S.missions.slice(0, 3).map(resumenMision).join("") : `<section class="card full">${empty("Tu siguiente aventura", "Añade obras a tu biblioteca para recibir retos personalizados.", `<button data-page="catalog">Ir a la biblioteca</button>`)}</section>`}</div></div><aside class="stack">${tarjetaCronometro()}<section class="card oracle-preview"><span class="eyebrow">✧ Descanso recomendado</span><h2>Recarga tu energía</h2><p>Una pausa con intención. El Oráculo encontrará una obra que encaje con tu tiempo.</p><div class="media-types"><div>${icon("game")}Una partida</div><div>${icon("play")}Un episodio</div><div>${icon("book")}Un capítulo</div></div><button class="wide" data-page="oracle">✧ Consultar al Oráculo　›</button></section></aside></div>`;
}
function tarjetaTarea(t) {
  const done = t.state === "COMPLETED";
  return `<article class="card task-card"><div class="card-head"><span class="tag">${esc(t.category)}</span>${!t.timed && !done ? `<button class="small ghost" data-action="edit-task" data-id="${t.id}" aria-label="Editar ${esc(t.title)}">Editar</button>` : ""}</div><h3>${esc(t.title)}</h3><p>${esc(t.description || "Un paso más hacia tu objetivo.")}</p><div class="task-meta"><span class="tag ${t.priority}">${t.priority}</span><small>◷ ${t.estimatedMinutes} min</small></div><div class="task-reward"><span class="coin">${icon("coin")}</span><div><small>${done ? "RECOMPENSA COBRADA" : "RECOMPENSA BASE"}</small><strong>+${done ? t.coins : t.estimatedMinutes * 2} monedas</strong></div></div>${done ? `<p class="help">Tiempo real: ${fmt(t.elapsedSeconds)} · Desviación: ${(t.elapsedSeconds / 60 - t.estimatedMinutes).toFixed(1)} min</p><span class="tag">✓ Completada</span>` : `<div class="task-actions"><button class="primary small" data-task="${t.id}" data-action="${t.state === "RUNNING" ? "pause" : "start"}">${t.state === "RUNNING" ? "Ⅱ Pausar" : "▷ Comenzar"}</button><button class="small" data-task="${t.id}" data-action="complete">Completar</button>${!t.timed ? `<button class="small ghost danger" data-id="${t.id}" data-action="delete-task">Borrar</button>` : ""}</div>`}</article>`;
}
function pantallaTareas() {
  const groups = [
      ["PENDING", "Pendientes"],
      ["ACTIVE", "En curso"],
      ["COMPLETED", "Completadas"],
    ],
    filter = S.filter;
  return `<div class="card toolbar">${[["ALL", "Todas"], ...groups].map(([v, l]) => `<button class="small ${filter === v ? "selected" : ""}" data-filter="${v}">${l}</button>`).join("")}<button class="small ghost" data-action="history">Historial y métricas ↗</button></div><div class="kanban">${groups
    .filter(([v]) => filter === "ALL" || filter === v)
    .map(([v, l]) => {
      const list = S.tasks.filter((t) =>
        v === "ACTIVE"
          ? ["RUNNING", "PAUSED"].includes(t.state)
          : t.state === v,
      );
      return `<section class="column"><h2>● ${l} <span class="count">${list.length}</span></h2>${list.length ? list.map(tarjetaTarea).join("") : empty("Sin tareas", "Aquí aparecerán tus tareas " + l.toLowerCase() + ".")}${v === "PENDING" ? '<button class="wide ghost" data-action="new-task">＋ Añadir tarea</button>' : ""}</section>`;
    })
    .join("")}</div>`;
}
function pantallaCronometro() {
  return `<div class="focus-layout">${tarjetaCronometro(true)}<aside class="stack"><section class="card"><div class="card-head"><div><span class="eyebrow">Cola de trabajo</span><h2>Próximas tareas</h2></div></div>${
    S.tasks
      .filter((t) => t.state === "PENDING" || t.state === "PAUSED")
      .slice(0, 5)
      .map(filaTarea)
      .join("") || empty("Todo despejado", "Añade una tarea para empezar.")
  }<button class="wide ghost" data-page="tasks">Gestionar tareas →</button></section><section class="card"><span class="eyebrow">En equilibrio</span><h2>Concéntrate sin perder tu progreso</h2><p class="help">Puedes pausar y reanudar. Al cancelar, se descarta el tiempo acumulado de esta tarea. Cerrar la página no detiene el cronómetro: pausa antes de salir.</p></section></aside></div>`;
}
function tarjetaObra(p) {
  const w = p.work;
  const juego = w.type === "GAME";
  const objetivo = juego ? w.horasHistoriaPrincipal : w.totalUnits;
  const horas = objetivo?.toLocaleString("es-ES");
  const etiqueta = juego ? `${p.currentUnits} h jugadas` : `${p.currentUnits} / ${w.totalUnits} ${units(w.type)}`;
  let porcentaje = 0;
  if (objetivo) porcentaje = p.state === "COMPLETED" ? 100 : Math.min(100, Math.round(100 * p.currentUnits / objetivo));
  const barra = objetivo ? `<progress value="${porcentaje}" max="100"></progress>` : "";
  const duracion = juego ? (objetivo ? `Historia principal · ≈ ${horas} h` : "Historia principal · Sin estimación")
    : `${w.minutesPerUnit} min por unidad`;
  return `<article class="card work-card"><div class="poster ${w.type}"><img src="/api/covers/${w.id}" alt="" loading="lazy" data-cover><span class="tag">${types[w.type]}</span><strong class="initials">${esc(
    w.title
      .split(" ")
      .map((s) => s[0])
      .slice(0, 3)
      .join(""),
  )}</strong><small>BIBLIOTECA TIMEFORGE</small></div><div class="work-body"><small>${esc(w.genres || types[w.type])}</small><h3>${esc(w.title)}</h3><span class="tag ${p.state === "IN_PROGRESS" ? "cyan" : ""}">${states[p.state]}</span><div class="progress-label"><span>${etiqueta}</span>${objetivo ? `<strong>${porcentaje}%</strong>` : ""}</div>${barra}<p>${duracion}${w.quickSession ? " · Partida rápida" : ""}</p><button data-action="edit-progress" data-id="${p.id}">Actualizar progreso →</button></div></article>`;
}
function pantallaCatalogo() {
  let list = S.catalog.filter(
    (p) =>
      (S.catalogType === "ALL" || p.work.type === S.catalogType) &&
      p.work.title.toLowerCase().includes(S.query.toLowerCase()),
  );
  return `<div class="card toolbar"><label class="sr-only" for="catalog-query">Buscar en tu biblioteca</label><input id="catalog-query" type="search" placeholder="Buscar en tu biblioteca…" value="${esc(S.query)}"><label class="sr-only" for="catalog-type">Tipo de obra</label><select id="catalog-type"><option value="ALL">Todos los formatos</option>${Object.entries(
    types,
  )
    .map(
      ([v, l]) =>
        `<option value="${v}" ${v === S.catalogType ? "selected" : ""}>${l}</option>`,
    )
    .join(
      "",
    )}</select><button data-action="search-external">⌕ Buscar online</button><button class="lime" data-action="new-work">＋ Añadir obra</button></div><div class="section-title"><div><span class="eyebrow">Mi colección</span><h2>Continúa donde lo dejaste</h2></div><small>${list.length} obras</small></div><div class="catalog-grid" id="catalog-grid">${list.map(tarjetaObra).join("")}</div>${!list.length ? `<div class="card">${empty("Tu biblioteca está por escribir", "Añade videojuegos, lecturas o series. Puedes registrarlos manualmente o buscarlos online.", `<button class="primary" data-action="new-work">Añadir mi primera obra</button>`)}</div>` : ""}<p class="footer-note">Catálogos externos: <a href="https://www.igdb.com" target="_blank" rel="noreferrer">IGDB</a>, <a href="https://store.steampowered.com" target="_blank" rel="noreferrer">Steam</a> y <a href="https://jikan.moe" target="_blank" rel="noreferrer">Jikan / MyAnimeList</a>. Las fichas importadas y sus portadas se guardan localmente.</p>`;
}
function pantallaOraculo() {
  const r = activeRest(),
    last = r || S.rests[0];
  return `<div class="oracle-layout"><section class="card wheel-card"><div class="card-head"><div><span class="eyebrow">✧ Ruleta inteligente</span><h2>Deja tu descanso en manos del Oráculo</h2></div></div><div class="wheel" id="wheel"><span class="wheel-label one">VIDEOJUEGO</span><span class="wheel-label two">MANGA</span><span class="wheel-label three">SERIE / ANIME</span><span class="wheel-label four">LECTURA</span><div class="wheel-core">${icon("oracle")}</div></div><span class="eyebrow">Un descanso que encaja contigo</span><p>◷ Tu tiempo disponible　·　✧ Tu progreso　·　▥ Tu catálogo</p></section><aside class="stack"><section class="card"><span class="eyebrow">Configura tu tirada</span><h2>Elige tu pausa</h2><p class="help">Las modalidades y sus costes son fijos.</p><form id="spin-form">${[
    [15, 80, "Pausa corta"],
    [30, 150, "Pausa media"],
    [60, 280, "Pausa extendida"],
  ]
    .map(
      ([m, c, l]) =>
        `<label class="pause-option"><input type="radio" name="minutes" value="${m}" ${m === 15 ? "checked" : ""}><div><strong>${l}</strong><small>${m} minutos</small></div><b class="gold">◉ ${c}</b></label>`,
    )
    .join(
      "",
    )}<button class="primary wide" ${r || activeTask() ? "disabled" : ""}>✧ Girar la ruleta · <span id="spin-cost">80</span> monedas</button><p class="help" id="spin-help">${activeTask() ? "Pausa la tarea activa antes de descansar." : r ? "Ya estás disfrutando de un descanso." : "Se cobra únicamente si hay una obra compatible."}</p></form></section><section class="card rest-result"><span class="eyebrow">${r ? "Tu descanso está en marcha" : "Última recomendación"}</span><h2>${last ? esc(last.work.title) : "Un descanso bien merecido"}</h2>${last ? `<span class="tag">${types[last.work.type]} · ${last.minutes} min${last.partnerId ? " · Cooperativo" : ""}</span>${r ? `<div class="countdown" data-rest-clock="${r.id}"></div>` : ""}<p class="oracle-text" id="oracle-text">¡Gran trabajo! Disfruta de este tiempo para recargar energía.</p><small id="oracle-source"></small>${r ? `<button class="wide" data-action="cancel-rest" data-id="${r.id}">Terminar descanso ahora</button><p class="help">Terminar antes no devuelve las monedas.</p>` : `<p class="help">${last.cancelled ? "Finalizado antes de tiempo" : "Descanso completado"}</p>`}` : "<p>Completa tareas, gana monedas y descubre qué historia te espera. No se seleccionan obras completadas o en pausa.</p>"}</section></aside></div><section class="card section-title"><div><h3>¿Un descanso en compañía?</h3><p class="help">Invita a un amigo para elegir una obra que tengáis en común.</p></div><button data-page="friends">Ver amigos →</button></section>`;
}
function badges() {
  return S.me.badges.length
    ? `<div class="badges">${S.me.badges.map((b) => `<div class="card badge">${icon("medal")}<div><h3>${esc(b.title)}</h3><small>${date(b.createdAt)}</small></div></div>`).join("")}</div>`
    : `<div class="card">${empty("Tu historia comienza ahora", "Las insignias se desbloquean al completar tareas, mantener rachas y terminar obras.")}</div>`;
}
function pantallaMisiones() {
  return `${level()}<div class="section-title"><div><span class="eyebrow">Objetivos de la semana</span><h2>Recupera tus historias pendientes</h2></div><small>Reinicio: lunes 00:00 · Madrid</small></div><div class="grid3">${S.missions.map((m) => `<section class="card mission-card"><div class="card-head">${icon("medal")}<span class="tag gold">+${m.reward} monedas</span></div><h3>${esc(m.description)}</h3><p class="help">Caduca: ${date(m.expiresAt)}</p><div class="progress-label"><span>Progreso</span><b>${Math.min(m.progress, m.target)} / ${m.target}</b></div><progress value="${Math.min(m.progress, m.target)}" max="${m.target}"></progress><button class="${!m.claimed && m.progress >= m.target ? "lime" : ""}" data-action="claim" data-id="${m.id}" ${m.claimed || m.progress < m.target || Date.parse(m.expiresAt) <= now() ? "disabled" : ""}>${m.claimed ? "✓ Recompensa reclamada" : "Reclamar recompensa"}</button></section>`).join("") || `<section class="card">${empty("Sin misiones todavía", "Añade obras pendientes a la biblioteca para generar tus retos.", `<button data-page="catalog">Ir a biblioteca</button>`)}</section>`}</div><div class="section-title"><h2>Tus logros desbloqueados</h2></div>${badges()}`;
}
function pantallaAmigos() {
  return `<section class="card profile-banner"><div class="avatar">${esc(S.me.avatar)}</div><div><span class="eyebrow">Explorador del tiempo · Nivel ${S.me.level}</span><h2>@${esc(S.me.username)}</h2><p>${esc(S.me.bio || "Cada día es una nueva oportunidad para avanzar.")}</p></div><button data-page="settings">Editar perfil</button></section><div class="stats"><div class="card stat"><small>Experiencia</small><strong>${S.me.xp} XP</strong></div><div class="card stat"><small>Racha</small><strong>${S.me.streak} días</strong></div><div class="card stat"><small>Logros</small><strong>${S.me.badges.length}</strong></div><div class="card stat"><small>Amigos</small><strong>${S.friends.filter((f) => f.state === "ACCEPTED").length}</strong></div></div><div class="focus-layout"><section class="card"><div class="card-head"><h2>Tu círculo</h2><button class="small" data-action="find-friend">＋ Añadir amigo</button></div>${S.friends.map((f) => `<div class="friend-row"><div class="avatar">${esc(f.profile.avatar)}</div><div><h3>${esc(f.profile.displayName)} <small>@${esc(f.profile.username)}</small></h3><p>Nivel ${f.profile.level} · Racha ${f.profile.streak} días · ${f.profile.badges.length} logros</p><small>${f.state === "ACCEPTED" ? "Amistad aceptada" : f.incoming ? "Te ha enviado una solicitud" : "Solicitud enviada"}${!f.available ? " · Cuenta no disponible" : ""}</small></div>${f.state === "PENDING" && f.incoming ? `<button class="small lime" data-action="accept-friend" data-id="${f.id}">Aceptar</button><button class="small" data-action="reject-friend" data-id="${f.id}">Rechazar</button>` : f.state === "ACCEPTED" && f.available ? `<button class="small" data-action="coop" data-id="${f.profile.id}">Tirada conjunta</button>` : ""}<button class="small ghost" data-action="remove-friend" data-id="${f.id}">Quitar</button></div>`).join("") || empty("Mejor en compañía", "Busca una cuenta por su nombre para enviarle una solicitud.")}</section><section class="card"><h2>Cómo funciona la tirada conjunta</h2><p class="help">Necesitáis una amistad aceptada, saldo suficiente y al menos una obra pendiente o en curso en común. La selección debe encajar en el descanso de ambos.</p><div id="coop-invitations"></div></section></div><div class="section-title"><h2>Tu colección de logros</h2></div>${badges()}`;
}
function pantallaAjustes() {
  return `<section class="card"><div class="card-head"><h2>Perfil público</h2><span class="tag">@${esc(S.me.username)}</span></div><form id="profile-form"><div class="form-grid"><label class="field">Nombre para mostrar<input name="displayName" value="${esc(S.me.displayName)}" maxlength="80" required></label><label class="field">Avatar (emoji o iniciales)<input name="avatar" value="${esc(S.me.avatar)}" maxlength="8" required></label><label class="field full">Sobre ti<textarea name="bio" maxlength="250">${esc(S.me.bio)}</textarea></label></div><div class="form-actions"><button class="primary">Guardar cambios</button></div></form></section><section class="card section-title"><div><h2>Tu sesión</h2><p class="help">${esc(S.me.email)} · Pausa tu cronómetro antes de salir.</p></div><button class="danger" data-action="logout">Cerrar sesión</button></section><div class="notice">Las pausas son de 15, 30 y 60 minutos y cuestan 80, 150 y 280 monedas. Estos valores no son configurables.</div>`;
}
async function render() {
  if (!S.me) return mostrarAcceso();
  const views = {
    home,
    tasks: pantallaTareas,
    timer: pantallaCronometro,
    catalog: pantallaCatalogo,
    oracle: pantallaOraculo,
    missions: pantallaMisiones,
    friends: pantallaAmigos,
    settings: pantallaAjustes,
  };
  shell(
    views[S.page] ? views[S.page]() : '<div class="card empty">Cargando…</div>',
  );
  if (S.page === "oracle") {
    updateSpin();
    cargarOraculo();
  }
  if (S.page === "wallet") await pantallaBilletera();
  if (S.page === "admin") await pantallaAdministracion();
  if (S.page === "friends") await cargarInvitaciones();
}
async function refresh() {
  const [me, tasks, catalog, rests, missions, friends] = await Promise.all(
    ["/me", "/tasks", "/catalog", "/rests", "/missions", "/friends"].map((p) =>
      api(p),
    ),
  );
  Object.assign(S, { me, tasks, catalog, rests, missions, friends });
  S.offset = Date.parse(me.serverNow) - Date.now();
  await render();
}
async function navigate(page) {
  S.page = pages[page] ? page : "home";
  history.replaceState(null, "", "#" + S.page);
  await refresh();
}
async function pantallaBilletera() {
  const w = await api("/wallet");
  if (S.page !== "wallet") return;
  $("#page-content").innerHTML =
    `<div class="card"><div class="card-head"><h2>Historial de movimientos</h2><span class="gold">${w.balance} monedas</span></div><div class="table-wrap"><table><thead><tr><th>Fecha</th><th>Concepto</th><th>Movimiento</th></tr></thead><tbody>${w.movements.map((m) => `<tr><td>${date(m.createdAt)}</td><td>${m.concept}</td><td class="${m.amount >= 0 ? "lime-text" : "gold"}">${m.amount >= 0 ? "+" : ""}${m.amount}</td></tr>`).join("")}</tbody></table></div>${!w.movements.length ? empty("Todavía sin movimientos", "Las recompensas y los descansos aparecerán aquí.") : ""}</div>`;
}
async function pantallaAdministracion() {
  const [users, ai] = await Promise.all([
    api("/admin/users"),
    api("/admin/ai"),
  ]);
  if (S.page !== "admin") return;
  $("#page-content").innerHTML = `<div class="stats">${[
    ["Peticiones IA", ai.metrics.requests],
    ["Errores", ai.metrics.errors],
    ["Latencia media", Math.round(ai.metrics.averageMs) + " ms"],
    [
      "Tokens entrada / salida",
      ai.metrics.inputTokens + " / " + ai.metrics.outputTokens,
    ],
  ]
    .map(
      ([l, v]) =>
        `<div class="card stat"><small>${l}</small><strong>${v}</strong></div>`,
    )
    .join(
      "",
    )}</div><section class="card"><h2>Cuentas</h2><div class="table-wrap"><table><thead><tr><th>Usuario</th><th>Correo</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>${users.map((u) => `<tr><td>${esc(u.username)} · ${u.role}</td><td>${esc(u.email)}</td><td>${u.active ? "Activa" : "Suspendida"}</td><td>${u.role === "ADMIN" ? "Administrador" : `<button class="small" data-action="admin-user" data-id="${u.id}" data-operation="${u.active ? "SUSPEND" : "REACTIVATE"}">${u.active ? "Suspender" : "Reactivar"}</button><button class="small danger" data-action="admin-user" data-id="${u.id}" data-operation="DELETE">Dar de baja</button>`}</td></tr>`).join("")}</tbody></table></div></section><section class="card section-title"><div class="table-wrap"><h2>Últimas llamadas de IA</h2><table><thead><tr><th>Fecha</th><th>Operación</th><th>Estado</th><th>Duración</th><th>Tokens</th></tr></thead><tbody>${ai.calls.map((c) => `<tr><td>${date(c.createdAt)}</td><td>${c.operation}</td><td>${c.outcome}</td><td>${c.durationMs} ms</td><td>${c.inputTokens} / ${c.outputTokens}</td></tr>`).join("")}</tbody></table></div></section>`;
}
async function cargarOraculo() {
  const r = activeRest() || S.rests[0];
  if (!r) return;
  try {
    const rec = await api("/rests/" + r.id + "/oracle");
    if ($("#oracle-text")) {
      $("#oracle-text").textContent = rec.text;
      $("#oracle-source").textContent =
        rec.source === "GEMINI"
          ? "Justificación generada por IA"
          : "Mensaje de respaldo · la IA puede no estar configurada";
    }
  } catch (e) {
    toast(e.message, true);
  }
}
function updateSpin() {
  const form = $("#spin-form");
  if (!form) return;
  const minutes = Number(new FormData(form).get("minutes")),
    cost = { 15: 80, 30: 150, 60: 280 }[minutes];
  $("#spin-cost").textContent = cost;
  form.querySelector("button").disabled =
    !!activeTask() || !!activeRest() || S.me.balance < cost;
  if (S.me.balance < cost)
    $("#spin-help").textContent =
      `Necesitas ${cost - S.me.balance} monedas más. Completa tareas para ganarlas.`;
  else if (!activeTask() && !activeRest())
    $("#spin-help").textContent =
      "Se cobra únicamente si hay una obra compatible.";
}
