function modal(title, html) {
  $("#modal-content").innerHTML =
    `<h2>${title}</h2>${html}<p class="error" id="modal-error" role="alert"></p>`;
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
  const p = S.catalog.find((p) => p.id === Number(id)),
    w = p.work;
  modal(
    esc(w.title),
    `<form id="progress-form" data-id="${p.id}"><div class="form-grid"><label class="field">Estado<select name="state">${["PENDING", "IN_PROGRESS", "PAUSED", "COMPLETED"].map((v) => `<option value="${v}" ${p.state === v ? "selected" : ""}>${states[v]}</option>`).join("")}</select></label>${field(w.type === "GAME" ? "Horas jugadas" : "Progreso (" + units(w.type) + ") · total " + w.totalUnits, "currentUnits", p.currentUnits, `type="number" min="0" max="${w.type === "GAME" ? 100000 : w.totalUnits}" step="${w.type === "GAME" ? "0.1" : "1"}" required`)}${field("Tomo / volumen actual", "volume", p.volume, 'type="number" min="0" max="10000"')}${field("Temporada actual", "season", p.season, 'type="number" min="0" max="10000"')}${field("Plataforma", "platform", p.platform, 'maxlength="150"')}</div><p class="help">${esc(w.synopsis || "Sin sinopsis registrada.")}</p>${actions("Guardar progreso")}</form><div class="form-actions"><button data-action="classify" data-id="${p.id}">✧ Sugerir etiquetas con IA</button><button class="danger" data-action="delete-work" data-id="${p.id}">Retirar de biblioteca</button></div><p class="oracle-text" id="classification" hidden></p>`,
  );
}
function buscarObrasExternas() {
  modal(
    "Descubre tu próxima historia",
    `<form id="external-search"><div class="form-grid"><label class="field">Catálogo<select name="source"><option value="VIDEOJUEGOS">Videojuegos</option><option value="JIKAN_ANIME">Anime</option><option value="JIKAN_MANGA">Manga</option></select></label>${field("Título", "q", "", 'required minlength="2" maxlength="100"')}</div>${actions("Buscar")}</form><div class="search-results" id="external-results"></div>`,
  );
}
function buscarAmigo() {
  modal(
    "Encuentra a tus compañeros",
    `<form id="friend-search">${field("Nombre de usuario", "q", "", 'required minlength="2" maxlength="40"')}${actions("Buscar")}</form><div id="friend-results"></div>`,
  );
}
function confirmarAccion(title, text, action, id, extra = "") {
  modal(
    title,
    `<p>${esc(text)}</p><div class="form-actions"><button data-action="close-modal">Cancelar</button><button class="primary" data-action="${action}" data-id="${id}" ${extra}>Confirmar</button></div>`,
  );
}
