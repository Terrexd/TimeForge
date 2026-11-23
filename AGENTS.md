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

**RAWG.** El recorrido acordado es **Biblioteca de ocio → Buscar online → Videojuegos · RAWG → Buscar → Añadir**. Se retiró el formulario duplicado. Spring carga `RAWG_API_KEY` desde el entorno o desde el archivo local `.env` situado junto a `iniciar.bat`. La clave pertenece al servidor y Git ignora ese archivo.

**Biblioteca de videojuegos.** La duración de la campaña usa estimaciones locales de HowLongToBeat por título. Los títulos ambiguos o desconocidos muestran «Sin estimación». Las horas jugadas se registran aparte. Las portadas ausentes se recuperan al abrir la biblioteca desde RAWG o Steam y se guardan en la base de datos.

## Última comprobación de la biblioteca

Se actualizaron las tarjetas y el formulario de progreso para mostrar la portada y las horas de historia principal también en juegos ya guardados. Pasaron 35 pruebas. En Chrome, con el JAR de inicio y una base temporal, se comprobó que Cyberpunk 2077 muestra unas 26 h y una imagen real de Steam de 460 × 215 píxeles. La prueba de importación de RAWG sigue usando una respuesta simulada.

## Última tarea

Se revisó por qué aparecen varias carpetas en `.tools`. Las tres de nombre aleatorio contienen copias del proyecto y resultados de compilación. `commit-mvc-I2ZRK2` contiene `src` y `README.md`, y `m2` almacena dependencias de Maven. Git ignora `.tools/`. La consulta se resolvió inspeccionando los directorios y `.gitignore`.
