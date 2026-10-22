# 📋 Especificación de Requisitos del Sistema

> **TimeForge** — Documento de especificación técnica de requisitos funcionales y no funcionales conforme al modelo de ingeniería de software para ciclo formativo DAM.

---

## 📑 Índice de Módulos
1. [Módulo 1: Gestión de Usuarios y Autenticación](#-módulo-1-gestión-de-usuarios-y-autenticación)
2. [Módulo 2: Productividad, Tareas y Control de Tiempo](#-módulo-2-productividad-tareas-y-control-de-tiempo)
3. [Módulo 3: Catálogo Multimedia y Backlog](#-módulo-3-catálogo-multimedia-y-backlog)
4. [Módulo 4: Economía de Ocio y Ruleta (Gacha)](#-módulo-4-economía-de-ocio-y-ruleta-gacha)
5. [Módulo 5: Inteligencia Artificial (DAIA)](#-módulo-5-inteligencia-artificial-daia)
6. [Módulo 6: Misiones, Logros y Funciones Online](#-módulo-6-misiones-logros-y-funciones-online)
7. [Requisitos No Funcionales (RNF)](#-requisitos-no-funcionales-rnf)

---

## 6.4. Requisitos Funcionales (RF)

### 👤 Módulo 1: Gestión de Usuarios y Autenticación

| Código | Requisito | Prioridad |
| :---: | :--- | :---: |
| **RF-01** | **Registrar usuario** | `Alta` |
| **RF-02** | **Iniciar y cerrar sesión** | `Alta` |
| **RF-03** | **Gestionar perfil de usuario** | `Media` |

* **`RF-01` — Registrar usuario**  
  El sistema permitirá registrar una nueva cuenta de usuario indicando un nombre de usuario único, correo electrónico y contraseña.
* **`RF-02` — Iniciar y cerrar sesión**  
  El sistema permitirá a los usuarios identificarse con sus credenciales para acceder a sus datos personales y cerrar su sesión activa de forma segura.
* **`RF-03` — Gestionar perfil de usuario**  
  El sistema permitirá modificar los datos básicos del perfil (nombre para mostrar, avatar y descripción personal).

---

### ⏱️ Módulo 2: Productividad, Tareas y Control de Tiempo

| Código | Requisito | Prioridad |
| :---: | :--- | :---: |
| **RF-04** | **Crear y gestionar tareas** | `Alta` |
| **RF-05** | **Controlar tiempo de ejecución con temporizador** | `Alta` |
| **RF-06** | **Liquidar recompensa por eficiencia temporal** | `Alta` |
| **RF-07** | **Consultar historial de tareas y métricas** | `Media` |

* **`RF-04` — Crear y gestionar tareas**  
  El sistema permitirá dar de alta, modificar y eliminar tareas, asignándoles título, descripción opcional, categoría (estudio, trabajo, personal), nivel de prioridad y una estimación de tiempo en minutos.
* **`RF-05` — Controlar tiempo de ejecución con temporizador**  
  El sistema dispondrá de un temporizador integrado que permitirá iniciar, pausar y marcar como finalizada una tarea, computando el tiempo real invertido.
* **`RF-06` — Liquidar recompensa por eficiencia temporal**  
  El sistema calculará automáticamente la recompensa base a razón de **2 monedas virtuales por cada minuto estimado** de la tarea.
  * Si la tarea se finaliza dentro del plazo estimado (o antes), asignará el **100%** de la recompensa.
  * Si el tiempo real supera al previsto, aplicará una reducción progresiva garantizando siempre un suelo mínimo del **20%**.
  * Para tareas completadas manualmente sin pasar por el cronómetro o con una duración real inferior a 5 minutos, la recompensa será de **0 monedas**.
* **`RF-07` — Consultar historial de tareas y métricas**  
  El sistema permitirá revisar el listado de tareas completadas junto con las métricas de desviación temporal (tiempo estimado frente a tiempo real).

---

### 📚 Módulo 3: Catálogo Multimedia y Backlog

| Código | Requisito | Prioridad |
| :---: | :--- | :---: |
| **RF-08** | **Registrar obras en el catálogo personal** | `Alta` |
| **RF-09** | **Gestionar progreso segmentado** | `Alta` |
| **RF-10** | **Controlar estados de las obras** | `Alta` |
| **RF-11** | **Filtrar y buscar en el backlog** | `Media` |

* **`RF-08` — Registrar obras en el catálogo personal**  
  El sistema permitirá dar de alta videojuegos, mangas/cómics y series/animes indicando título, tipo de obra, géneros y sinopsis.
* **`RF-09` — Gestionar progreso segmentado**  
  El sistema permitirá registrar y actualizar el progreso concreto de cada obra según su naturaleza: horas y plataformas en videojuegos; tomos y capítulos en lecturas; temporadas y episodios en series/animes.
* **`RF-10` — Controlar estados de las obras**  
  El sistema permitirá asignar y cambiar el estado de cualquier título del catálogo entre: `Pendiente`, `En curso`, `En pausa` y `Completado`.
* **`RF-11` — Filtrar y buscar en el backlog**  
  El sistema permitirá buscar y sincronizar títulos de videojuegos, manga y anime consumiendo APIs externas de catálogo (RAWG y Jikan), persistiendo los datos básicos y portadas localmente para agilizar la gestión del backlog.

---

### 🎰 Módulo 4: Economía de Ocio y Ruleta (Gacha)

| Código | Requisito | Prioridad |
| :---: | :--- | :---: |
| **RF-12** | **Consultar balance de la billetera virtual** | `Alta` |
| **RF-13** | **Canjear monedas por pausas de descanso** | `Alta` |
| **RF-14** | **Seleccionar obra mediante ruleta ponderada** | `Alta` |
| **RF-15** | **Bloquear descansos por saldo insuficiente** | `Alta` |
| **RF-16** | **Temporizar la sesión de descanso** | `Media` |

* **`RF-12` — Consultar balance de la billetera virtual**  
  El sistema mostrará en todo momento el saldo de monedas acumuladas obtenidas mediante el cumplimiento de tareas y misiones.
* **`RF-13` — Canjear monedas por pausas de descanso**  
  El sistema permitirá gastar monedas para desbloquear descansos temporizados estructurados en tres modalidades fijas e inmutables:
  * **Pausa corta:** 15 min — 80 monedas
  * **Pausa media:** 30 min — 150 monedas
  * **Pausa extendida:** 60 min — 280 monedas
* **`RF-14` — Seleccionar obra mediante ruleta ponderada**  
  Al canjear un descanso, el sistema elegirá de forma aleatoria ponderada una obra del catálogo del usuario cuyo formato encaje en el tiempo del descanso adquirido, priorizando aquellas con mayor tiempo de inactividad o próximas a terminarse.
* **`RF-15` — Bloquear descansos por saldo insuficiente**  
  El sistema impedirá activar tiradas en la ruleta o iniciar sesiones de descanso asistidas si el usuario no cuenta con las monedas necesarias.
* **`RF-16` — Temporizar la sesión de descanso**  
  El sistema activará una cuenta atrás durante el descanso y emitirá un aviso visual o sonoro cuando la pausa haya finalizado.

---

### 🧠 Módulo 5: Inteligencia Artificial (DAIA)

| Código | Requisito | Prioridad |
| :---: | :--- | :---: |
| **RF-17** | **Generar justificación contextual ("El Oráculo")** | `Alta` |
| **RF-18** | **Clasificar automáticamente etiquetas y ritmo** | `Media` |

* **`RF-17` — Generar justificación contextual del descanso ("El Oráculo")**  
  El sistema generará un texto explicativo breve al salir la tirada de la ruleta, motivando la recomendación en función de la tarea que el usuario acaba de finalizar y la duración del descanso.
* **`RF-18` — Clasificar automáticamente etiquetas y ritmo**  
  El sistema analizará mediante inteligencia artificial la sinopsis de una obra para inferir y sugerir etiquetas de temática y densidad/ritmo narrativo sin que el usuario deba introducirlas manualmente.

---

### 🌐 Módulo 6: Misiones, Logros y Funciones Online

| Código | Requisito | Prioridad |
| :---: | :--- | :---: |
| **RF-19** | **Generar misiones dinámicas periódicas** | `Media` |
| **RF-20** | **Desbloquear logros del sistema** | `Media` |
| **RF-21** | **Gestionar lista de amigos** | `Baja` |
| **RF-22** | **Ejecutar tirada cooperativa** | `Baja` |
| **RF-23** | **Administrar usuarios y cuentas** | `Alta` |
| **RF-24** | **Monitorizar consumo y rendimiento de IA** | `Media` |

* **`RF-19` — Generar misiones dinámicas periódicas**  
  El sistema generará semanalmente retos automáticos basados en el catálogo del usuario (por ejemplo, avanzar cierta cantidad de capítulos o retomar un título pausado) para otorgar bonificaciones de monedas al cumplirlos.
* **`RF-20` — Desbloquear logros del sistema**  
  El sistema evaluará de forma automática el cumplimiento de hitos (constancia de estudio, finalización de sagas completas, rachas de puntualidad) y desbloqueará insignias en el perfil.
* **`RF-21` — Gestionar lista de amigos**  
  El sistema permitirá buscar otros usuarios, enviar y aceptar solicitudes de amistad y consultar perfiles públicos de contactos.
* **`RF-22` — Ejecutar tirada cooperativa**  
  El sistema permitirá a dos usuarios amigos iniciar una tirada compartida que calcule la intersección de sus catálogos pendientes y elija una obra común para jugar o ver juntos.
* **`RF-23` — Administrar usuarios y cuentas**  
  El sistema permitirá a perfiles con rol Administrador suspender, reactivar o dar de baja cuentas de usuario ante comportamientos anómalos o incidencias técnicas.
* **`RF-24` — Monitorizar consumo y rendimiento de IA**  
  El sistema registrará internamente el volumen de peticiones enviadas al servicio de Inteligencia Artificial, sus tiempos de respuesta y los errores generados para controlar cuotas y costes operativos.

---

## 6.5. Requisitos No Funcionales (RNF)

| Identificador | Categoría | Criterio de Aceptación |
| :--- | :--- | :--- |
| **RNF-01** | 🔒 Seguridad | Contraseñas protegidas mediante algoritmo hash **BCrypt**. |
| **RNF-02** | ⚡ Rendimiento | Ruleta y búsquedas en catálogo local responden en **< 1,5 segundos**. |
| **RNF-03** | 🛡️ Disponibilidad | Timeout de IA a los **5 segundos** con fallback automático a texto local. |
| **RNF-04** | 🎯 Usabilidad | Iniciar temporizador o consultar saldo en **máximo 2 clics**. |
| **RNF-05** | 💎 Integridad | Transacciones de balance gestionadas con soporte **ACID**. |
| **RNF-06** | 🖥️ Compatibilidad | Arquitectura **Desktop-first** para Chrome/Firefox/Edge en Win/Linux/macOS. |
| **RNF-07** | 🧱 Mantenibilidad | Arquitectura desacoplada en **4 capas (MVC + DAO)**. |

### Detalle de Requisitos No Funcionales

#### 🔒 RNF-01 — Seguridad (Protección de contraseñas)
* **Descripción:** Las contraseñas de los usuarios se guardarán siempre cifradas mediante algoritmos de *hash* (como BCrypt), nunca en texto plano.
* **Justificación:** Imprescindible para proteger la privacidad de las cuentas de usuario ante posibles filtraciones de la base de datos.

#### ⚡ RNF-02 — Rendimiento (Respuesta rápida de la ruleta y catálogo)
* **Descripción:** La ruleta de selección ponderada y las búsquedas en el catálogo local responderán en menos de 1,5 segundos. La justificación textual del Oráculo (IA) cargará de manera asíncrona para no demorar la entrega de la obra seleccionada.
* **Justificación:** Garantiza inmediatez al usuario al solicitar una pausa sin retener la interfaz mientras el modelo de lenguaje procesa el texto.

#### 🛡️ RNF-03 — Disponibilidad (Tolerancia a fallos de la IA)
* **Descripción:** Si la conexión con la API de Inteligencia Artificial supera el tiempo límite de 5 segundos (*timeout*) o devuelve un error, la aplicación asignará el descanso de inmediato mostrando un mensaje genérico predeterminado en el frontend.
* **Justificación:** Evita que la caída de un servicio externo interrumpa la experiencia principal del usuario.

#### 🎯 RNF-04 — Usabilidad (Facilidad y rapidez de uso)
* **Descripción:** La interfaz será clara e intuitiva, permitiendo iniciar el temporizador de una tarea o consultar el saldo de monedas con un máximo de dos clics desde la pantalla principal.
* **Justificación:** Una app de productividad debe ser ágil para que no dé pereza usarla en el día a día.

#### 💎 RNF-05 — Integridad (Transacciones seguras de monedas)
* **Descripción:** El cobro y la entrega de monedas se gestionarán de forma atómica en la base de datos para asegurar que el saldo no se duplique ni se pierda.
* **Justificación:** Evita errores en la billetera virtual al ganar o gastar monedas de descanso.

#### 🖥️ RNF-06 — Compatibilidad (Arquitectura Desktop-First)
* **Descripción:** La aplicación web estará diseñada y optimizada con un enfoque prioritario para ordenadores de escritorio (*Desktop-first*), garantizando una visualización uniforme en navegadores web modernos bajo entornos Windows, Linux y macOS.
* **Justificación:** Asegura la compatibilidad multiplataforma exigida en el ciclo formativo sin requerir compilaciones nativas dependientes del sistema operativo.

#### 🧱 RNF-07 — Mantenibilidad (Código ordenado por capas)
* **Descripción:** El proyecto se programará separando claramente la vista (interfaz), la lógica de negocio y el acceso a la base de datos.
* **Justificación:** Facilita encontrar fallos, hacer pruebas y añadir mejoras a futuro sin tener que reescribir todo el código.
