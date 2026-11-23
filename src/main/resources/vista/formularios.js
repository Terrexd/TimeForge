function modal(title, html, cabecera = "") {
  $("#modal-content").innerHTML =
    `${cabecera || `<h2 id="titulo-modal">${title}</h2>`}${html}<p class="error" id="modal-error" role="alert"></p>`;
  if (!$("#modal").open) $("#modal").showModal();
}
function actions(label = "Guardar") {
  return `<div class="form-actions"><button type="button" data-action="close-modal">Cancelar</button><button class="primary" type="submit">${label}</button></div>`;
}
function field(label, name, value = "", attrs = "") {
  return `<label class="field">${label}<input name="${name}" value="${esc(value)}" ${attrs}></label>`;
}
function formularioTarea(id) {
  const t = S.tasks.find((t) => t.id === Number(id)) || {
    title: "",
    description: "",
    category: "ESTUDIO",
    priority: "MEDIA",
    estimatedMinutes: 25,
  };
  modal(
    id ? "Editar tarea" : "Una nueva misión personal",
    `<form id="task-form" data-id="${id || ""}"><div class="form-grid"><label class="field full">¿Qué quieres conseguir?<input name="title" value="${esc(t.title)}" required maxlength="150" placeholder="Ej. Preparar el proyecto de Java"></label><label class="field full">Descripción<textarea name="description" maxlength="2000">${esc(t.description)}</textarea></label><label class="field">Categoría<select name="category">${["ESTUDIO", "TRABAJO", "PERSONAL"].map((v) => `<option ${v === t.category ? "selected" : ""}>${v}</option>`).join("")}</select></label><label class="field">Prioridad<select name="priority">${["ALTA", "MEDIA", "BAJA"].map((v) => `<option ${v === t.priority ? "selected" : ""}>${v}</option>`).join("")}</select></label>${field("Tiempo estimado (minutos)", "estimatedMinutes", t.estimatedMinutes, 'type="number" min="1" max="720" required')}<p class="help">Gana 2 monedas por minuto estimado si terminas a tiempo. Debes cronometrar al menos 5 minutos.</p></div>${actions(id ? "Guardar cambios" : "Crear tarea")}</form>`,
  );
}
function formularioObra() {
  modal(
    "Añadir a tu biblioteca",
    `<form id="work-form"><div class="form-grid">${field("Título", "title", "", 'required maxlength="150"')}<label class="field">Formato<select name="type">${Object.entries(
      types,
    )
      .map(([v, l]) => `<option value="${v}">${l}</option>`)
      .join(
        "",
      )}</select></label><label class="field full">Sinopsis<textarea name="synopsis" maxlength="8000"></textarea></label>${field("Géneros", "genres", "", 'maxlength="250"')}${field("Unidades totales (capítulos o episodios)", "totalUnits", 12, 'type="number" min="1" max="100000" required')}${field("Minutos por unidad / sesión de juego", "minutesPerUnit", 15, 'type="number" min="1" max="600" required')}<label class="check"><input type="checkbox" name="quickSession">Videojuego apto para partidas rápidas</label></div><p class="help">Para videojuegos, la historia principal se consulta por título y la sesión mínima se indica en minutos. En series y anime, se usan episodios. En lecturas, capítulos.</p>${actions("Añadir obra")}</form>`,
  );
  mostrarUnidadesObra();
}
function mostrarUnidadesObra() {
  const form = $("#work-form");
  form.elements.totalUnits.closest("label").hidden = form.elements.type.value === "GAME";
}
function formularioProgreso(id) {
  const p = S.catalog.find((obra) => obra.id === Number(id));
  const w = p.work, juego = w.type === "GAME", episodios = ["ANIME", "SERIES"].includes(w.type);
  const lectura = ["BOOK", "MANGA", "COMIC"].includes(w.type);
  const unidad = juego ? "hora" : episodios ? "episodio" : lectura ? "capítulo" : "película";
  const etiqueta = juego ? "Horas jugadas" : episodios ? "Episodios vistos" : lectura ? "Capítulos leídos" : "Películas vistas";
  const objetivo = juego ? w.horasHistoriaPrincipal : w.totalUnits;
  const meta = juego ? (objetivo ? `Historia principal · ≈ ${objetivo.toLocaleString("es-ES")} h` : "")
    : w.type === "MOVIE" ? (w.minutesPerUnit ? `${w.minutesPerUnit} min` : "") : `${w.totalUnits} ${units(w.type)}`;
  const cabecera = `<header class="obra-cabecera"><div class="obra-miniatura">${icon(juego ? "game" : lectura ? "book" : "play")}<img src="/api/covers/${w.id}" data-cover alt=""></div>
    <div><span class="obra-tipo">${types[w.type]}</span><h2 id="titulo-modal">${esc(w.title)}</h2>${meta ? `<small>${meta}</small>` : ""}</div></header>`;
  const estados = [["PENDING", "timer"], ["IN_PROGRESS", "play"], ["PAUSED", "pause"], ["COMPLETED", "check"]];
  const numeroOpcional = 'type="number" min="0" max="10000" placeholder="Sin indicar"';
  const oculto = (nombre, valor) => `<input type="hidden" name="${nombre}" value="${esc(valor)}">`;
  modal(esc(w.title), `<form id="progress-form" data-id="${p.id}" data-juego="${juego}" data-objetivo="${objetivo || 0}">
    <fieldset class="obra-estados"><legend>Estado</legend><div>${estados.map(([v, i]) =>
      `<label><input type="radio" name="state" value="${v}" ${p.state === v ? "checked" : ""}><span>${icon(i)}${states[v]}</span></label>`).join("")}</div></fieldset>
    <section class="obra-contador"><label for="cantidad-progreso">${etiqueta}</label>
      <div class="obra-pasos"><div class="obra-cifra"><input id="cantidad-progreso" type="number" name="currentUnits" min="0" max="${juego ? 100000 : w.totalUnits}" step="${juego ? "0.1" : "1"}" value="${p.currentUnits}" required><span>${juego ? "h" : "de " + w.totalUnits}</span></div>
      <div class="obra-botones"><button type="button" data-paso="-1" aria-label="Restar 1 ${unidad}">−</button><button type="button" data-paso="1">${w.type === "MOVIE" && w.totalUnits === 1 ? "Marcar como vista" : `+1 ${unidad}`}</button></div></div>
      ${objetivo ? `<progress max="100" aria-label="Progreso de la obra"></progress><output aria-live="polite"></output>` : ""}
    </section>
    <details class="obra-detalles"><summary>Detalles de seguimiento</summary><div class="form-grid">
      ${episodios ? field("Temporada actual", "season", p.season || "", numeroOpcional) : oculto("season", p.season)}
      ${["MANGA","COMIC"].includes(w.type) ? field("Tomo actual", "volume", p.volume || "", numeroOpcional) : oculto("volume", p.volume)}
      ${field("Plataforma", "platform", p.platform, 'maxlength="150" placeholder="Opcional"')}</div></details>
    ${actions("Guardar progreso")}</form>
    <details class="obra-detalles obra-opciones"><summary>Más sobre esta obra</summary>
      ${w.synopsis ? `<p class="obra-sinopsis">${esc(w.synopsis)}</p>` : ""}
      <div class="form-actions"><button data-action="classify" data-id="${p.id}">✧ Sugerir etiquetas con IA</button><button class="danger" data-action="delete-work" data-id="${p.id}">Retirar de biblioteca</button></div>
      <p class="oracle-text" id="classification" hidden></p></details>`, cabecera);
  actualizarEditorProgreso($("#progress-form"));
}

function actualizarEditorProgreso(form, cambio) {
  const cantidad = form.elements.currentUnits, estado = form.elements.state;
  const juego = form.dataset.juego === "true", objetivo = Number(form.dataset.objetivo);
  if (cambio === "state" && estado.value === "COMPLETED" && !juego) cantidad.value = cantidad.max;
  const valor = cantidad.valueAsNumber;
  if (cambio === "currentUnits" && cantidad.validity.valid) {
    if (!juego && valor === objetivo) estado.value = "COMPLETED";
    else if ((!juego && estado.value === "COMPLETED") || (estado.value === "PENDING" && valor > 0))
      estado.value = valor ? "IN_PROGRESS" : "PENDING";
  }
  form.querySelector('[data-paso="-1"]').disabled = !(valor > 0);
  form.querySelector('[data-paso="1"]').disabled = valor >= Number(cantidad.max);
  if (objetivo) {
    const porcentaje = estado.value === "COMPLETED" ? 100 : Math.min(100, Math.max(0, Math.round((valor || 0) * 100 / objetivo)));
    form.querySelector("progress").value = porcentaje;
    form.querySelector("output").textContent = `${porcentaje} % ${juego ? "del tiempo estimado" : "completado"}`;
  }
}
function buscarObrasExternas() {
  modal(
    "Descubre tu próxima historia",
    `<form id="external-search"><div class="form-grid"><label class="field">Catálogo<select name="source"><option value="VIDEOJUEGOS">Videojuegos</option><option value="JIKAN_ANIME">Anime</option><option value="JIKAN_MANGA">Manga</option><option value="CINE_SERIES">Series</option><option value="CINE_PELICULAS">Películas</option></select></label>${field("Título", "q", "", 'minlength="2" maxlength="100" placeholder="Buscar por título"')}</div><div class="form-actions"><button type="button" data-action="close-modal">Cerrar</button><button class="primary" type="submit">Buscar</button></div></form><div class="search-results" id="external-results" aria-label="Resultados del catálogo" tabindex="0"></div>`,
  );
  cargarCatalogoExterno();
}
function buscarAmigo() {
  modal(
    "Encuentra a tus compañeros",
    `<form id="friend-search">${field("Nombre de usuario", "q", "", 'required minlength="2" maxlength="40"')}${actions("Buscar")}</form><div id="friend-results"></div>`,
  );
}

let consultaCatalogo = 0;
async function cargarCatalogoExterno() {
  const turno = ++consultaCatalogo;
  const form = $("#external-search");
  const panel = $("#external-results");
  const parametros = new URLSearchParams({ source: form.elements.source.value, q: form.elements.q.value.trim() });
  $("#modal-error").textContent = "";
  panel.textContent = "Cargando títulos…";
  panel.scrollTop = 0;
  try {
    const lista = await api("/catalog/search?" + parametros);
    if (turno !== consultaCatalogo || !panel.isConnected) return;
    panel.innerHTML = lista.map(fichaExterna).join("") || "<p>No se encontraron títulos.</p>";
  } catch (error) {
    if (turno === consultaCatalogo && panel.isConnected) panel.textContent = error.message;
  }
}

function fichaExterna(obra) {
  const guardada = S.catalog.some(({work}) => work.source === obra.source && String(work.externalId) === String(obra.id));
  let duracion = "Duración sin estimación";
  if (["STEAM", "IGDB"].includes(obra.source)) {
    if (obra.horasHistoriaPrincipal) duracion = `Historia principal · ≈ ${obra.horasHistoriaPrincipal.toLocaleString("es-ES")} h`;
  } else if (obra.source.endsWith("_MANGA")) {
    duracion = obra.unidades ? `${obra.unidades} capítulos · ≈ ${(obra.unidades / 6).toLocaleString("es-ES", {maximumFractionDigits: 1})} h de lectura` : "≈ 10 min de lectura por capítulo";
  } else if (obra.minutos) {
    duracion = obra.source === "CINE_PELICULAS" ? `${obra.minutos} min`
      : `${obra.unidades ? obra.unidades + " episodios · " : ""}${obra.minutos} min por episodio`;
  } else if (obra.unidades) {
    duracion = `${obra.unidades} episodios · Duración sin estimación`;
  }
  return `<div class="search-result">${obra.imagen ? `<img src="${esc(obra.imagen)}" alt="" loading="lazy" referrerpolicy="no-referrer">` : ""}<span>${esc(obra.title)}${obra.anio ? ` (${esc(obra.anio)})` : ""}<small>${esc(duracion)}</small></span><button type="button" class="small lime" data-action="import-work" data-id="${esc(obra.id)}" data-source="${esc(obra.source)}" ${guardada ? "disabled" : ""}>${guardada ? "Añadida" : "Añadir"}</button></div>`;
}
function confirmarAccion(title, text, action, id, extra = "") {
  modal(
    title,
    `<p>${esc(text)}</p><div class="form-actions"><button data-action="close-modal">Cancelar</button><button class="primary" data-action="${action}" data-id="${id}" ${extra}>Confirmar</button></div>`,
  );
}
