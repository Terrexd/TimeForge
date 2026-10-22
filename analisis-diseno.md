# 📖 Análisis y Diseño General del Sistema

> **TimeForge** — Documento de identificación, justificación, objetivos del sistema y definición del alcance funcional del proyecto.

---

## 📑 Contenido
1. [6.1. Identificación del Proyecto](#61-identificación-del-proyecto)[cite: 1]
2. [6.2. Descripción General](#62-descripción-general)[cite: 1]
   * [6.2.1. Descripción de la aplicación](#621-descripción-de-la-aplicación)[cite: 1]
   * [6.2.2. Problema o necesidad](#622-problema-o-necesidad)[cite: 1]
   * [6.2.3. Objetivos](#623-objetivos)[cite: 1]
   * [6.2.4. Usuarios objetivo](#624-usuarios-objetivo)[cite: 1]
3. [6.3. Alcance del Proyecto](#63-alcance-del-proyecto)[cite: 1]
   * [Funcionalidades dentro del alcance](#-dentro-del-alcance)[cite: 1]
   * [Funcionalidades fuera del alcance](#-fuera-del-alcance)[cite: 1]

---

## 6.1. Identificación del Proyecto

| Parámetro | Detalle |
| :--- | :--- |
| **Nombre del proyecto** | **TimeForge** |
| **Autor** | Javier Valencia Miranda |
| **Ciclo formativo** | Desarrollo de Aplicaciones Multiplataforma (DAM) |
| **Versión del documento** | `v1.0` |
| **Fecha de emisión** | 05/10/2026 |

### 📝 Descripción Breve de la Aplicación
Software de gestión de la productividad personal donde el usuario registra y completa sus tareas para obtener monedas virtuales según su puntualidad. Este saldo se utiliza para desbloquear descansos temporizados, en los cuales un sistema de selección ponderada asistido por Inteligencia Artificial elige automáticamente qué videojuego, lectura o serie pendiente disfrutar durante ese intervalo de tiempo.

---

## 6.2. Descripción General

### 6.2.1. Descripción de la Aplicación

#### 📌 ¿Qué es?
Una aplicación de gestión de tiempo, productividad personal y administración de ocio digital. Combina un planificador de tareas con seguimiento activo y un catálogo estructurado de obras pendientes de consumir (*backlog*).

#### 🎯 ¿Para qué sirve?
Organiza las obligaciones diarias y regula los periodos de descanso mediante un ciclo de incentivos gamificado:

```text
┌─────────────────────────┐
│ 1. Registrar Tarea      │ ─── Asignación de tiempo estimado de resolución
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ 2. Completar a Tiempo   │ ─── Obtención de monedas virtuales por puntualidad
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ 3. Canjear Descanso     │ ─── Desbloqueo de pausas temporizadas con monedas
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ 4. Ruleta + Oráculo IA  │ ─── Selección óptima de la obra según tiempo disponible
└─────────────────────────┘


👥 ¿Quién la utilizará?
Usuarios que necesiten compaginar sus responsabilidades diarias (académicas, laborales o personales) con su ocio digital y deseen optimizar la distribución de sus tiempos.

6.2.2. Problema o NecesidadEn la actualidad es habitual acumular listas masivas de obras pendientes de entretenimiento (videojuegos, mangas, cómics, series, animes o películas). Al disponer de un periodo de descanso, el usuario suele invertir más tiempo navegando entre menús o decidiendo qué consumir que disfrutando de la propia pausa.
TimeForge aborda esta problemática unificando el control del tiempo productivo con la curación automática del descanso, reduciendo la fricción cognitiva previa al ocio.

6.2.3. Objetivos🏆 Objetivo GeneralGestionar el tiempo de forma integral integrando un planificador de tareas con un catálogo de ocio pendiente, motivando al usuario a finalizar sus obligaciones a tiempo mediante un sistema de economía virtual y recompensas.

  🎯 Objetivos EspecíficosControlar el tiempo real de las tareas: Desarrollar un temporizador interactivo que compute el tiempo invertido frente a la estimación inicial, liquidando la recompensa en monedas según la puntualidad o aplicando penalizaciones progresivas ante retrasos.   Gestionar el catálogo de pendientes (backlog): Diseñar un esquema relacional estructurado para almacenar videojuegos (horas, plataformas), lecturas (tomos, capítulos) y series/anime (temporadas, episodios).   Crear la ruleta de descansos (gacha): Implementar un algoritmo que consuma saldo virtual y seleccione aleatoriamente obras del catálogo cuyos formatos encajen con la duración de la pausa adquirida.   Integrar Inteligencia Artificial:Generar justificaciones contextuales y personalizadas sobre la obra asignada en función del esfuerzo previo realizado (El Oráculo).   Clasificar e inferir etiquetas de temática, densidad y ritmo a partir de la sinopsis de los títulos.   Bloquear el ocio sin saldo: Asegurar mediante reglas de negocio que ningún usuario pueda iniciar descansos asistidos si no ha obtenido saldo mediante el cumplimiento de tareas.   Misiones y funciones online: Diseñar un sistema de retos dinámicos semanales para reactivar obras pausadas y habilitar tiradas conjuntas entre amigos sobre títulos compartidos.

6.2.4. Usuarios Objetivo
┌───────────────────────────────────────┐   ┌───────────────────────────────────────┐
│     Usuario Estándar (Productivo)     │   │      Root / Administrador Sistema     │
├───────────────────────────────────────┤   ├───────────────────────────────────────┤
│ • Estudiantes, opositores, empleados  │   │ • Supervisión técnica y base de datos │
│ • Gestión de tareas y cronómetros     │   │ • Gestión y suspensión de cuentas     │
│ • Administración de backlog personal  │   │ • Auditoría de cuotas y costes de IA  │
│ • Uso de ruleta, misiones y social    │   │ • Monitorización de estabilidad       │
└───────────────────────────────────────┘   └───────────────────────────────────────┘

Usuario estándar (Estudiante / Opositor / Trabajador): Actor principal del sistema. Acumula obligaciones diarias y dispone de un catálogo de entretenimiento pendiente. Utiliza la herramienta para cronometrar tareas, actualizar su progreso multimedia, desbloquear descansos y participar en funciones cooperativas.
Root / Administrador del sistema: Perfil con privilegios avanzados orientado al mantenimiento. Supervisa la consistencia de la base de datos, administra las cuentas de usuario y controla las cuotas y consumo de peticiones hacia las APIs externas de Inteligencia Artificial

6.3. Alcance del Proyecto✅
  Dentro del Alcance⏱️ Gestión de tareas y control de tiempoCreación, edición, consulta y borrado de tareas con asignación de prioridades y categorías.   Temporizador integrado con estados de inicio, pausa y finalización.   Algoritmo de liquidación de recompensas por eficiencia temporal.

  📚 Gestión de backlog multimediaCatálogo unificado para videojuegos, mangas/cómics y series/animes.   Control de estados de obra (Pendiente, En curso, En pausa, Completado).   Registro de avance granular según el formato (episodios, tomos, capítulos u horas).

  🎰 Economía de ocio y ruletaBilletera virtual persistente con registro de transacciones.   Sistema de tiradas estructurado en modalidades fijas de descanso.   Algoritmo ponderado de exclusión por duración de formato.   Bloqueo estricto de descansos ante saldo insuficiente.

  🧠 Integración con Inteligencia Artificial (DAIA)"El Oráculo": Motor de justificaciones textuales automáticas basadas en la tarea previa y la obra asignada[cite: 1].Etiquetado temático y categorización de ritmo a partir del análisis semántico de sinopsis.

  🌐 Módulo social y misionesSistema de contactos y solicitudes de amistad[cite: 1].Mecánica de tirada cooperativa basada en la intersección de catálogos pendientes entre dos usuarios[cite: 1].Generación de misiones dinámicas periódicas con reinicio semanal.

🗄️ Persistencia y cuentasRegistro de usuarios con contraseñas cifradas y control de sesiones seguras[cite: 1].Almacenamiento persistente en base de datos relacional MySQL bajo transacciones ACID.

❌ Fuera del Alcance
Funcionalidad Excluida	Justificación Técnica
Bloqueo a nivel de sistema operativo	
Se delega en avisos y notificaciones visuales/sonoras en el navegador para evitar dependencias nativas o privilegios de administrador del SO[cite: 1].

Escaneo por cámara y OCR	
No se desarrollará reconocimiento óptico de portadas físicas para no sobredimensionar el alcance del frontend[cite: 1].

Pasarelas de pago y dinero real	
La economía se gestiona exclusivamente mediante monedas virtuales ganadas por productividad, sin monetización real[cite: 1].

Chat en tiempo real integrado	
Se descarta la mensajería instantánea interna para mantener el enfoque de concentración y evitar distracciones en la plataforma[cite: 1].
