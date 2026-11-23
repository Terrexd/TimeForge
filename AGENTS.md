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
- **Ejecución.** Java 21 y Spring Boot con Maven. H2 guarda los datos locales en `.data/` y existe un perfil MySQL. En PowerShell, `.\mvnw.cmd test` ejecuta las pruebas y `.\mvnw.cmd clean package` genera el JAR que abre `iniciar.bat`.
- **Referencias.** El README explica la puesta en marcha y sigue una tarea desde el formulario hasta la base de datos. Las reglas funcionales están en el [documento de requisitos](docs/requisitos.txt).

## Decisiones y comprobaciones vigentes

**Monedas.** RF-06 y RN-02 fijan la base en dos monedas por minuto **estimado**. Terminar antes o justo a tiempo da el 100 % si se han cronometrado al menos cinco minutos. Por ejemplo, una tarea estimada en 30 minutos y terminada en 20 da 60 monedas, sujeto al límite diario. Completarla manualmente sin cronómetro o en menos de cinco minutos da cero. Por cada bloque completo de un 5 % de retraso se descuentan dos puntos porcentuales de la recompensa base, con un mínimo del 20 %. El límite por tareas es de 1.500 monedas diarias.

**Biblioteca de videojuegos.** El recorrido es **Biblioteca de ocio → Buscar online → Videojuegos → Añadir**. El buscador también permite filtrar por título. Hay una sola opción de videojuegos. El servidor consulta IGDB cuando dispone de sus credenciales de Twitch y recurre a la tienda pública de Steam si faltan o falla la búsqueda. RAWG se retiró. Las fichas anteriores conservan el progreso y pueden recuperar portadas desde Steam. Las horas jugadas y las horas estimadas de campaña se muestran por separado. Cuando falta una duración en IGDB, se usa la estimación local de HowLongToBeat por título. Los títulos ambiguos o desconocidos muestran «Sin estimación».

## Explorador de ocio

El explorador carga sugerencias al abrirse y al cambiar entre videojuegos, anime, manga, series y películas. El listado se desplaza dentro de la ventana y permite añadir varias obras seguidas. Las filas muestran la portada disponible y la duración correspondiente al medio. Para manga se estima la lectura a diez minutos por capítulo. Se incorporó Cinemeta para cine y series y se separaron los identificadores de AniList y Jikan para importar la ficha seleccionada.

Pasaron 38 pruebas. En Chrome con el JAR actualizado y una base temporal se añadieron cinco obras, una por categoría, sin cerrar el explorador. Se comprobaron la carga de imágenes, la duración, el desplazamiento en una pantalla de 390 píxeles y la búsqueda de películas por título. Las fichas sin información suficiente indican que falta la estimación.

## Editor de progreso

Se rediseñó el editor de progreso de la biblioteca. La cabecera muestra el formato, el título y la portada disponible. Se puede cambiar de estado, sumar o restar una unidad y escribir el progreso exacto. La barra y el porcentaje responden a los cambios antes de guardar. Las series y el anime muestran temporada, y el manga y los cómics muestran tomo. Los detalles secundarios y la sinopsis se despliegan bajo demanda.

Pasaron 38 pruebas y se actualizó el JAR de iniciar.bat. En Chrome se verificaron el avance de una serie de siete episodios, completar y cancelar cambios, guardar la temporada y la plataforma, el tomo de un manga y el botón de película vista. Un videojuego conservó 35,5 horas al marcarlo completado. Se comprobó el diseño en escritorio y a 390 píxeles de ancho.

## Eliminación desde la biblioteca

Cada tarjeta de la biblioteca tiene una papelera junto al título. Se retiró el botón de eliminación del editor de progreso. La confirmación compacta indica el título seleccionado y la pérdida del progreso de esa obra. Cancelar recibe el foco inicial y Escape cierra la ventana.

Pasaron 38 pruebas y se actualizó el JAR. En Chrome, con dos obras de prueba, se verificó que cancelar o pulsar Escape conserva ambas y que confirmar en móvil envía una sola petición y elimina únicamente la seleccionada.

## Última tarea

Las tarjetas de anime, series, manga, cómics y libros permiten sumar o restar una unidad directamente. El servidor aplica el paso al progreso guardado y conserva temporada, tomo y plataforma. Los botones se desactivan en los límites. Actualizar progreso sigue abriendo el editor completo.

Pasaron 39 pruebas y se actualizó el JAR de iniciar.bat. En Chrome, con una biblioteca temporal, Cowboy Bebop pasó de 3 a 4 de 26 episodios y volvió a 3 desde la tarjeta en móvil. Se verificaron el porcentaje, la persistencia y la apertura del editor con el contador actualizado.
