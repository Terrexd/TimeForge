# TimeForge — Sistema Gamificado de Productividad y Ocio Digital

TimeForge es una aplicación web *Desktop-First* diseñada para combatir la procrastinación y la parálisis por elección. Transforma la gestión de tareas personales combinando un temporizador de enfoque, una economía virtual basada en la puntualidad y un catálogo de ocio digital (*backlog*) asistido por Inteligencia Artificial ("El Oráculo")[cite: 4].

---

## 📌 Resumen del Proyecto

* **Ciclo:** Desarrollo de Aplicaciones Multiplataforma (DAM)[cite: 4].
* **Autor:** Javier Valencia Miranda[cite: 4].
* **Materia:** Proyecto Intermodular / Análisis y Diseño de Software[cite: 4].
* **Versión:** 1.0 (Fase de Documentación y Diseño Inicial)[cite: 4].
* **Prototipo Interactivo:** [Acceso al prototipo en Figma / Web](https://clink-epic-93348728.figma.site)[cite: 4].

---

## 🎮 ¿Cómo Funciona TimeForge?

1. **Gestión de Tareas:** El usuario planifica sus obligaciones con tiempos estimados de resolución[cite: 4].
2. **Temporizador de Enfoque:** El cronómetro mide el tiempo real invertido[cite: 4].
3. **Recompensas Virtuales:** Cumplir los plazos otorga monedas de oro en la billetera virtual según la puntualidad demostrada[cite: 4].
4. **Ruleta de Descanso (Gacha):** Las monedas se canjean para desbloquear pausas delimitadas (15 min, 30 min o 60 min)[cite: 4].
5. **El Oráculo (IA):** Un algoritmo ponderado selecciona la obra ideal del backlog y una IA contextual justifica la elección según el esfuerzo previo[cite: 4].

---

## 🛠️ Stack Tecnológico Previsto

| Capa / Elemento | Tecnología Seleccionada |
| :--- | :--- |
| **Lenguaje Backend** | Java (JDK 17+)[cite: 4] |
| **Framework Base** | Spring Boot / Maven[cite: 4] |
| **Base de Datos** | MySQL (Persistencia relacional ACID)[cite: 4] |
| **Frontend / Interfaz** | HTML5, CSS3, JavaScript (o React) — *Dark Gaming Dashboard*[cite: 4] |
| **Control de Versiones** | Git + GitHub[cite: 4] |
| **Testing** | JUnit 5[cite: 4] |
| **APIs Externas** | Google Gemini / OpenAI (IA), RAWG y Jikan (Catálogos multimedia)[cite: 4] |

---

## 📁 Estructura del Repositorio y Documentación

La memoria técnica del proyecto se encuentra estructurada en el directorio `/docs`:

* 📄 **[`docs/analisis-diseno.md`](docs/analisis-diseno.md):** Identificación general, justificación, objetivos del sistema y alcance funcional[cite: 4].
* 📋 **[`docs/requisitos.md`](docs/requisitos.md):** Catálogo de requisitos funcionales (RF-01 a RF-24) y requisitos no funcionales (RNF-01 a RNF-07)[cite: 4].
* 🔄 **[`docs/casos-uso.md`](docs/casos-uso.md):** Reglas de negocio (RN-01 a RN-09), matriz de casos de uso y especificaciones completas (CU-04 y CU-07)[cite: 4].
* 🗄️ **[`docs/modelo-datos/`](docs/modelo-datos/):** Esquema relacional normalizado, catálogo de atributos, relaciones y [diagrama E/R](docs/modelo-datos/er-diagrama.png)[cite: 4, 6].
* 🏛️ **[`docs/arquitectura.md`](docs/arquitectura.md):** Desacoplamiento por capas (Presentación, Negocio, DAO/Acceso a datos y MySQL)[cite: 4].
* 🧩 **[`docs/uml/`](docs/uml/):** Modelado orientado a objetos y [diagrama de clases inicial](docs/uml/diagrama-clases.png)[cite: 4, 5].
* 🖥️ **[`docs/wireframes/`](docs/wireframes/):** Capturas del prototipo de alta fidelidad y [diagrama de flujo de navegación](docs/wireframes/navegacion-flujo.png)[cite: 4].
* 🤖 **[`docs/ia-registro.md`](docs/ia-registro.md):** Registro de trazabilidad y evaluación crítica del uso de herramientas de Inteligencia Artificial[cite: 4].
