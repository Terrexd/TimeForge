# 🔄 Reglas de Negocio, Actores y Casos de Uso

> **TimeForge** — Especificación del comportamiento del sistema, actores involucrados, matriz de casos de uso y tratamiento exhaustivo de casos límite.

---

## 📑 Contenido
1. [6.6. Reglas de Negocio (RN)](#66-reglas-de-negocio-rn)
2. [6.7. Actores y Matriz de Casos de Uso](#67-actores-y-casos-de-uso)
3. [6.8. Especificación Detallada de Casos de Uso](#68-especificación-de-casos-de-uso)
   * [CU-04: Ejecutar tarea con temporizador y liquidar recompensa](#cu-04---ejecutar-tarea-con-temporizador-y-liquidar-recompensa)
   * [CU-07: Canjear descanso en la ruleta (Gacha)](#cu-07---canjear-descanso-en-la-ruleta-gacha)
4. [6.9. Casos Límite y Situaciones Excepcionales](#69-casos-límite-y-situaciones-excepcionales)

---

## 6.6. Reglas de Negocio (RN)

| Identificador | Regla Operativa | Ámbito de Aplicación |
| :---: | :--- | :--- |
| **RN-01** | **Bloqueo de descansos sin saldo** | Economía virtual / Ruleta |
| **RN-02** | **Liquidación de monedas y control antiabuso** | Gamificación / Temporizador |
| **RN-03** | **Exclusividad de sesión de trabajo** | Temporizador de tareas |
| **RN-04** | **Incompatibilidad entre trabajo y descanso** | Control de sesiones |
| **RN-05** | **Filtro de catálogo por duración de descanso** | Algoritmo de ruleta |
| **RN-06** | **Catálogo común en tirada cooperativa** | Módulo social |
| **RN-07** | **No retroactividad en misiones expiradas** | Sistema de retos |
| **RN-08** | **Inmutabilidad de los costes de descanso** | Configuración / Seguridad |
| **RN-09** | **Compatibilidad de obras con pausas de ocio** | Catálogo multimedia |

---

### Detalle de Reglas de Negocio

#### 🔒 RN-01 — Bloqueo de descansos sin saldo
Un usuario no podrá realizar tiradas en la ruleta ni iniciar sesiones de descanso asistidas si su saldo de monedas virtuales es inferior al coste de la pausa seleccionada.

#### ⚖️ RN-02 — Liquidación de monedas y control antiabuso
La recompensa base se calcula mediante la fórmula:
$$\text{Monedas Base} = \text{Minutos Estimados} \times 2$$

* **Puntualidad:** Si la tarea se completa dentro del tiempo estimado (o antes), se acredita el **100%** de las monedas base.
* **Penalización por retraso:** Si se supera el tiempo estimado, se aplica una deducción del **2% por cada 5% de tiempo excedido**, asegurando un **suelo mínimo garantizado del 20%** para premiar el esfuerzo final.
* **Control antiabuso estricto:** 
  * Las tareas con una duración real registrada inferior a **5 minutos** otorgan **0 monedas**.
  * Las tareas marcadas manualmente como completadas sin activar el temporizador computan como finalizadas pero otorgan **0 monedas**.
  * Se establece un límite máximo de **1.500 monedas diarias** acumulables mediante tareas estándar.

#### 🚫 RN-03 — Exclusividad de sesión de trabajo
Un usuario no podrá mantener dos temporizadores de tareas activos simultáneamente; deberá pausar o finalizar la tarea en curso antes de iniciar una nueva.

#### ⏸️ RN-04 — Incompatibilidad entre trabajo y descanso
Mientras un temporizador de descanso esté en marcha, el sistema bloqueará el inicio de cronómetros de tareas productivas hasta que expire la pausa o el usuario la cancele de forma explícita.

#### ⏱️ RN-05 — Filtro de catálogo por duración de descanso
La ruleta no podrá seleccionar una obra cuyo formato o duración media estimada supere el tiempo de descanso canjeado (por ejemplo, una sesión de 15 minutos jamás podrá asignar un capítulo de serie de 45 minutos).

#### 👥 RN-06 — Requisito de catálogo común en tirada cooperativa
Para iniciar una tirada conjunta entre dos usuarios amigos, ambos deben tener en común al menos una obra en estado `Pendiente` o `En curso`; de lo contrario, el sistema impedirá ejecutar la tirada.

#### 📅 RN-07 — No retroactividad en misiones expiradas
Las misiones dinámicas no completadas caducarán automáticamente al cierre del ciclo semanal (**domingos a las 23:59**) y no podrán volver a completarse ni reclamarse transcurrido dicho plazo.

#### 🛡️ RN-08 — Inmutabilidad de los costes de descanso
Los costes en monedas y la duración de las pausas en la ruleta son parámetros globales fijos del sistema y **no pueden ser modificados por el usuario desde los ajustes de configuración**:
* **Pausa corta:** 15 min $\rightarrow$ **80 monedas**
* **Pausa media:** 30 min $\rightarrow$ **150 monedas**
* **Pausa extendida:** 60 min $\rightarrow$ **280 monedas**

#### 🎮 RN-09 — Compatibilidad de obras con pausas de ocio
* **Series y Anime:** Una obra es apta si la duración media de su episodio es menor o igual al tiempo de la pausa.
* **Videojuegos:** Solo se seleccionarán títulos etiquetados para sesiones cortas (partidas rápidas, arcades o multijugador) en descansos de 15 o 30 minutos, reservando sesiones de 60 minutos para títulos con progresión narrativa.

---

## 6.7. Actores y Casos de Uso

### 🎭 Actores del Sistema

```text
       ┌───────────────┐
       │    Usuario    │ ─── Interactúa con tareas, backlog, ruleta y módulo social
       └───────────────┘
       ┌───────────────┐
       │ Administrador │ ─── Supervisa persistencia, gestión de cuentas y cuotas de API
       └───────────────┘
       ┌───────────────┐
       │ Servicio IA   │ ─── (Actor externo) Procesa contexto de Oráculo y sinopsis
       └───────────────┘
