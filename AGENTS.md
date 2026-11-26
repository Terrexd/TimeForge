# TimeForge · Memoria del proyecto

## Cómo trabajar en cada petición

1. Lee este archivo al empezar cada petición del usuario. Comprueba el estado actual con `git status` y consulta los requisitos o el código que afecten a la tarea.
2. Mantén el MVC sencillo, con nombres en español y explicaciones que permitan seguir el código. Si el usuario señala un fallo, comprueba el recorrido que ya utiliza.
3. Antes de responder, **reescribe este archivo** para reflejar la petición, el resultado, las comprobaciones y lo que quede pendiente. Conserva las decisiones que sigan vigentes y sustituye la información desactualizada. Si la petición es una consulta, recoge su conclusión en el resumen.
4. **Haz un commit cada vez que termines una tarea.** Incluye los cambios de esa tarea y la memoria actualizada. Escribe el título en español y resume el cambio concreto. Usa el cuerpo cuando ayude a explicar el motivo o cómo lo comprobaste. Verifica el commit con `git log -1` y comunica su identificador al usuario.

## Qué hace TimeForge

TimeForge es el proyecto intermodular de DAM de Javier Valencia Miranda. El usuario registra tareas, las cronometra y gana monedas para canjear descansos de 15, 30 o 60 minutos. La ruleta elige una obra de su biblioteca para ese descanso. El proyecto incluye cuentas, billetera, catálogo multimedia, recomendaciones del Oráculo, misiones, logros y funciones entre amigos.

## Dónde trabajar

- **Vista.** El HTML, los estilos y el JavaScript están en `src/main/resources/vista`. Los formularios recogen los datos, los eventos envían las peticiones y las pantallas muestran el resultado.
- **Controladores y modelo.** En `src/main/java/es/timeforge`, cada controlador recibe las peticiones de su módulo. Dentro de `modelo`, las entidades representan los datos, los servicios aplican las reglas, el repositorio guarda y consulta, y las integraciones conectan con servicios externos. La reorganización al español conservó las rutas y los campos usados por la API y la base de datos.
- **Ejecución.** Java 21/27 y Spring Boot con Maven. H2 guarda los datos locales en `.data/` y existe un perfil MySQL. En PowerShell, `.\mvnw.cmd test` ejecuta las pruebas y `.\mvnw.cmd clean package` genera el JAR que abre `iniciar.bat`.
- **Referencias.** El README explica la puesta en marcha y sigue una tarea desde el formulario hasta la base de datos. Las reglas funcionales están en el [documento de requisitos](docs/requisitos.txt).

## Decisiones y comprobaciones vigentes

**Lectura del código para la defensa.** El README sigue el alta y la edición de tareas, el borrado con confirmación y la incorporación y retirada de obras. En la vista, `guardarTarea` distingue el alta de la edición. Los controladores y servicios usan los mismos nombres para operaciones como `crearTarea`, `eliminarTarea` y `eliminarObra`. En el catálogo se distingue la ficha compartida del progreso personal que se borra. Se ejecutaron las 40 pruebas de Maven y se comprobó la sintaxis de `eventos.js` con Node.

**Monedas.** RF-06 y RN-02 fijan la base en dos monedas por minuto **estimado**. Terminar antes o justo a tiempo da el 100 % si se han cronometrado al menos cinco minutos. Por ejemplo, una tarea estimada en 30 minutos y terminada en 20 da 60 monedas, sujeto al límite diario. Completarla manualmente sin cronómetro o en menos de cinco minutos da cero. Por cada bloque completo de un 5 % de retraso se descuentan dos puntos porcentuales de la recompensa base, con un mínimo del 20 %. El límite por tareas es de 1.500 monedas diarias.

**Biblioteca de videojuegos.** El recorrido es **Biblioteca de ocio → Buscar online → Videojuegos → Añadir**. El buscador también permite filtrar por título. Hay una sola opción de videojuegos. El servidor consulta IGDB cuando dispone de sus credenciales de Twitch y recurre a la tienda pública de Steam si faltan o falla la búsqueda. RAWG se retiró. Las fichas anteriores conservan el progreso y pueden recuperar portadas desde Steam. Las horas jugadas y las horas estimadas de campaña se muestran por separado. Cuando falta una duración en IGDB, se usa la estimación local de HowLongToBeat por título. Los títulos ambiguos o desconocidos muestran «Sin estimación».

## Explorador de ocio

El explorador carga sugerencias al abrirse y al cambiar entre videojuegos, anime, manga, series y películas. El listado se desplaza dentro de la ventana y permite añadir varias obras seguidas. Las filas muestran la portada disponible y la duración correspondiente al medio. Para manga se estima la lectura a diez minutos por capítulo. Se incorporó Cinemeta para cine y series y se integró AniList con GraphQL como respaldo transparente ante caídas o límites de Jikan para importar fichas seleccionadas.

## Editor de progreso

Se rediseñó el editor de progreso de la biblioteca. La cabecera muestra el formato, el título y la portada disponible. Se puede cambiar de estado, sumar o restar una unidad y escribir el progreso exacto. La barra y el porcentaje responden a los cambios antes de guardar. Las series y el anime muestran temporada, y el manga y los cómics muestran tomo. Los detalles secundarios y la sinopsis se despliegan bajo demanda.

## Eliminación y controles directos en biblioteca

Cada tarjeta de la biblioteca tiene una papelera junto al título con confirmación compacta que previene borrados accidentales. Las tarjetas de anime, series, manga, cómics y libros permiten sumar o restar una unidad directamente mediante botones `+` y `-`, sincronizando con el servidor de forma escalonada mientras conservan temporada, tomo y plataforma.

## Pausa y reanudación de descansos

Se incorporó la posibilidad de pausar y reanudar los descansos activos. Al pausar, se congelan los segundos restantes en el servidor y la interfaz entra en estado pausado (mostrando etiqueta `EN PAUSA`, temporizador en ámbar y botón `▷ Reanudar descanso`). Al reanudar, se recalcula la fecha de fin a partir del tiempo restante exacto. El banner superior global también permite pausar o reanudar el descanso desde cualquier sección de la aplicación sin perder tiempo ni monedas.

## Migración de esquema en descansos

Tras incorporar los campos de pausa, la tabla `rest_session` de la base de datos local H2 requirió la migración explícita de las columnas `paused` (booleano con valor por defecto false) y `remaining_seconds` (entero con valor por defecto 0). Se agregaron las anotaciones `@Column(columnDefinition = ...)` en `Descanso.java` y se validó con una prueba automatizada autenticada que todos los endpoints del sistema (`/me`, `/tasks`, `/catalog`, `/rests`, `/missions`, `/friends`, `/wallet`) responden correctamente con código 200 OK.

## Auditoría general y estado del proyecto

Se ha auditado el cumplimiento de los 24 requisitos funcionales (RF-01 a RF-24) y las 9 reglas de negocio (RN-01 a RN-09):
1. **Módulo 1 (Cuentas y Perfil):** Registro BCrypt, autenticación de sesión, CSRF y edición de perfil al 100 %.
2. **Módulo 2 (Tareas y Recompensas):** CRUD de tareas, cronómetro en vivo persistente, cálculo de monedas con penalización progresiva y métricas de desviación temporal al 100 %.
3. **Módulo 3 (Catálogo Multimedia):** Biblioteca con estados, control escalonado unitario, búsqueda remota unificada y tolerancia a fallos de API externa al 100 %.
4. **Módulo 4 (Billetera y Ruleta de Descansos):** Canje de descansos (15/30/60 min), pausa/reanudación de descansos, ruleta ponderada probabilística con filtros y temporizador regresivo de descanso al 100 %.
5. **Módulo 5 (El Oráculo IA):** Justificación de recomendaciones, etiquetado de ritmo y sinopsis con fallback local sin conexión al 100 %.
6. **Módulo 6 (Social, Misiones y Logros):** Misiones semanales dinámicas, insignias automáticas, gestión de amigos y giros cooperativos de ruleta al 100 %.
7. **Módulo 7 (Administración):** Gestión de usuarios (suspender/reactivar/eliminar) y métricas de consumo/latencia de IA al 100 %.

## Recarga de monedas para pruebas

A petición del usuario para probar los descansos y la ruleta, se abonaron 2.000 monedas en la cuenta `admin` (contraseña `admin`), registrando la transacción en el historial de movimientos de su billetera. Saldo verificado en la base de datos local H2.
