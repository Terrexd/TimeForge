const vista = document.querySelector("#vista");
const estado = document.querySelector("#estado");
const tabla = document.querySelector("#datos");
const paginas = document.querySelector("#paginas");
const anterior = document.querySelector("#anterior");
const siguiente = document.querySelector("#siguiente");
let pagina = 1;

function etiqueta(nombre) {
  const texto = nombre.toLowerCase().replaceAll("_", " ");
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

async function consultar(ruta) {
  const respuesta = await fetch("/api/admin/bbdd" + ruta, { cache: "no-store" });
  if (respuesta.status === 401 || respuesta.status === 403) {
    document.querySelector("#acceso").hidden = false;
    throw new Error("Para consultar los datos, entra en TimeForge con una cuenta de administración.");
  }
  if (!respuesta.ok) throw new Error("No se pudieron cargar los datos.");
  return respuesta.json();
}

async function cargar() {
  vista.disabled = true;
  paginas.hidden = true;
  tabla.replaceChildren();
  estado.textContent = "Cargando…";
  try {
    const datos = await consultar("/" + vista.value + "?pagina=" + pagina);
    const cabecera = tabla.createTHead().insertRow();
    for (const columna of datos.columnas) {
      const celda = document.createElement("th");
      celda.textContent = etiqueta(columna);
      cabecera.append(celda);
    }
    const cuerpo = tabla.createTBody();
    for (const fila of datos.filas) {
      const tr = cuerpo.insertRow();
      for (const valor of fila) {
        tr.insertCell().textContent = typeof valor === "boolean" ? (valor ? "Sí" : "No") : (valor ?? "—");
      }
    }
    estado.textContent = datos.filas.length ? "Página " + pagina : "Sin registros.";
    anterior.disabled = pagina === 1;
    siguiente.disabled = !datos.hayMas;
    paginas.hidden = anterior.disabled && siguiente.disabled;
  } catch (error) {
    estado.textContent = error.message;
  } finally {
    vista.disabled = false;
  }
}

async function abrir() {
  try {
    for (const nombre of await consultar("")) vista.add(new Option(etiqueta(nombre), nombre));
    document.querySelector("#selector").hidden = false;
    await cargar();
  } catch (error) {
    estado.textContent = error.message;
  }
}

vista.onchange = () => { pagina = 1; cargar(); };
anterior.onclick = () => { pagina--; cargar(); };
siguiente.onclick = () => { pagina++; cargar(); };
abrir();
