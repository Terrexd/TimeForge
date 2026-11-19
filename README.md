# TimeForge

TimeForge convierte el tiempo dedicado a tus tareas en monedas que puedes gastar en descansos. Durante cada descanso, la ruleta elige una obra de tu biblioteca. Proyecto intermodular de DAM de Javier Valencia Miranda.

## Ejecutar el proyecto

Con un JDK 21 o superior, abre PowerShell en la carpeta del proyecto y ejecuta:

```powershell
.\mvnw.cmd spring-boot:run
```

Abre [TimeForge en tu navegador](http://localhost:8080). La primera ejecución descarga las dependencias de Maven. Por defecto se utiliza H2 y los datos se guardan en `.data/`. El perfil `mysql` permite usar MySQL con los ajustes de `application-mysql.properties`.

## Añadir videojuegos desde RAWG

Obtén tu clave en [RAWG](https://rawg.io/apidocs). En la carpeta de `iniciar.bat`, crea o abre el archivo `.env` y guarda una línea con tu clave, sin comillas. Git ignora este archivo.

```properties
RAWG_API_KEY=tu_clave_aqui
```

Cierra el servidor si está abierto y vuelve a ejecutar `iniciar.bat`. En **Biblioteca de ocio → Buscar online**, selecciona **Videojuegos · RAWG**, escribe el título y pulsa **Buscar**. Pulsa **Añadir** junto al resultado para guardarlo en tu biblioteca.

## Dónde está cada cosa

| Parte | Carpeta | Qué encontrarás |
| --- | --- | --- |
| Vista | [resources/vista](src/main/resources/vista) | La página de entrada, los estilos y el JavaScript que dibuja las pantallas y recoge los clics. |
| Controlador | [controlador](src/main/java/es/timeforge/controlador) | Un controlador por tema. Recibe una petición como «crear tarea» y devuelve la respuesta a la vista. |
| Modelo | [modelo](src/main/java/es/timeforge/modelo) | Los datos y las reglas de TimeForge. En `entidades` están las clases como Tarea y Usuario. En `servicios` se calcula la recompensa y se decide cuándo se puede iniciar un descanso. |
| Persistencia e integraciones | Dentro del modelo | `repositorio` lee y guarda datos. `integraciones` conecta con el catálogo externo y el Oráculo. |

[AplicacionTimeForge.java](src/main/java/es/timeforge/AplicacionTimeForge.java) arranca el servidor. La carpeta [configuracion](src/main/java/es/timeforge/configuracion) contiene la seguridad y la creación del administrador inicial.

## Sigue una tarea desde el formulario hasta la base de datos

1. Abre [formularios.js](src/main/resources/vista/formularios.js) y busca `formularioTarea`. Ahí se muestran los campos que rellenas al crear una tarea.
2. Al pulsar **Crear tarea**, [eventos.js](src/main/resources/vista/eventos.js) recoge el envío de `task-form` y hace una petición `POST /api/tasks`.
3. En [ControladorTareas.java](src/main/java/es/timeforge/controlador/ControladorTareas.java), `crearTarea` recibe los datos validados, identifica al usuario y llama al servicio.
4. En [ServicioTareas.java](src/main/java/es/timeforge/modelo/servicios/ServicioTareas.java), `crear` construye una [Tarea](src/main/java/es/timeforge/modelo/entidades/Tarea.java), le asigna el usuario y pide al [Repositorio](src/main/java/es/timeforge/modelo/repositorio/Repositorio.java) que la guarde.
5. La vista vuelve a cargar las tareas. [pantallas.js](src/main/resources/vista/pantallas.js) dibuja la lista actualizada.

Si quieres cambiar **qué se ve**, empieza por la vista. Si quieres cambiar **cuántas monedas se ganan**, abre [ReglasRecompensa.java](src/main/java/es/timeforge/modelo/servicios/ReglasRecompensa.java). Las rutas y los campos que intercambian el navegador y el servidor conservan sus nombres originales, como `/api/tasks` y `estimatedMinutes`.

## Pruebas y requisitos

Para ejecutar las [pruebas](src/test/java/es/timeforge) usa `.\mvnw.cmd test`. Para generar el archivo que utiliza `iniciar.bat`, ejecuta `.\mvnw.cmd clean package`.

Consulta el [documento de análisis y requisitos](docs/requisitos.txt) y el [prototipo de la interfaz](https://clink-epic-93348728.figma.site).
