CREATE OR REPLACE VIEW tareas AS
SELECT id, owner_id AS usuario_id, title AS titulo, description AS descripcion,
       category AS categoria, priority AS prioridad, estimated_minutes AS minutos_estimados,
       coins AS monedas, elapsed_seconds AS segundos_transcurridos, timed AS cronometrada,
       CASE state WHEN 'PENDING' THEN 'Pendiente' WHEN 'RUNNING' THEN 'En curso'
                  WHEN 'PAUSED' THEN 'Pausada' WHEN 'COMPLETED' THEN 'Completada' ELSE concat('', state) END AS estado,
       created_at AS creada, started_at AS iniciada, completed_at AS completada FROM task;

CREATE OR REPLACE VIEW misiones AS
SELECT id, owner_id AS usuario_id, work_id AS obra_id, description AS descripcion,
       baseline AS punto_de_partida, target AS objetivo, progress AS progreso, reward AS recompensa,
       claimed AS cobrada, week_start AS inicio_de_semana, expires_at AS vencimiento FROM mission;

CREATE OR REPLACE VIEW usuarios AS
SELECT id, username AS usuario, display_name AS nombre_visible, email AS correo,
       avatar, bio AS biografia, CASE role WHEN 'ADMIN' THEN 'Administrador' WHEN 'USER' THEN 'Usuario' ELSE role END AS rol,
       active AS activo, deleted AS eliminado, xp AS experiencia, streak AS racha,
       last_active_day AS ultimo_dia_activo, created_at AS fecha_de_alta FROM tf_user;

CREATE OR REPLACE VIEW billeteras AS
SELECT id, owner_id AS usuario_id, balance AS saldo, updated_at AS actualizada FROM wallet;

CREATE OR REPLACE VIEW movimientos AS
SELECT id, wallet_id AS billetera_id, concept AS concepto, amount AS cantidad, created_at AS fecha FROM wallet_movement;

CREATE OR REPLACE VIEW obras AS
SELECT id, title AS titulo, canonical_title AS titulo_canonico,
       CASE type WHEN 'GAME' THEN 'Videojuego' WHEN 'BOOK' THEN 'Libro' WHEN 'MOVIE' THEN 'Película'
                 WHEN 'SERIES' THEN 'Serie' WHEN 'COMIC' THEN 'Cómic' WHEN 'MANGA' THEN 'Manga' WHEN 'ANIME' THEN 'Anime' ELSE concat('', type) END AS tipo,
       synopsis AS sinopsis, genres AS generos, tags AS etiquetas, pace AS ritmo, source AS fuente,
       external_id AS id_externo, total_units AS unidades_totales, minutes_per_unit AS minutos_por_unidad,
       quick_session AS sesion_corta, horas_campana_importadas FROM media_work;

CREATE OR REPLACE VIEW progresos AS
SELECT id, owner_id AS usuario_id, work_id AS obra_id,
       CASE state WHEN 'PENDING' THEN 'Pendiente' WHEN 'IN_PROGRESS' THEN 'En curso'
                  WHEN 'PAUSED' THEN 'En pausa' WHEN 'COMPLETED' THEN 'Completado' ELSE concat('', state) END AS estado,
       current_units AS unidades_actuales, volume AS tomo, season AS temporada, platform AS plataforma,
       last_interaction AS ultima_interaccion FROM media_progress;

CREATE OR REPLACE VIEW descansos AS
SELECT id, owner_id AS usuario_id, work_id AS obra_id, minutes AS minutos, cost AS coste, started_at AS inicio,
       ends_at AS fin, completed AS completado, cancelled AS cancelado, paused AS pausado,
       remaining_seconds AS segundos_restantes, partner_id AS companero_id FROM rest_session;

CREATE OR REPLACE VIEW insignias AS
SELECT id, owner_id AS usuario_id, code AS codigo, title AS titulo, created_at AS fecha FROM badge;

CREATE OR REPLACE VIEW amistades AS
SELECT id, sender_id AS remitente_id, receiver_id AS destinatario_id,
       CASE state WHEN 'PENDING' THEN 'Pendiente' WHEN 'ACCEPTED' THEN 'Aceptada' WHEN 'REJECTED' THEN 'Rechazada' ELSE concat('', state) END AS estado,
       created_at AS fecha FROM friendship;

CREATE OR REPLACE VIEW invitaciones AS
SELECT id, sender_id AS remitente_id, receiver_id AS destinatario_id, sender_name AS nombre_del_remitente,
       CASE state WHEN 'PENDING' THEN 'Pendiente' WHEN 'ACCEPTED' THEN 'Aceptada' WHEN 'REJECTED' THEN 'Rechazada'
                  WHEN 'EXPIRED' THEN 'Caducada' ELSE concat('', state) END AS estado,
       minutes AS minutos, cost AS coste, expires_at AS vencimiento FROM coop_invite;

CREATE OR REPLACE VIEW recomendaciones AS
SELECT id, rest_id AS descanso_id, previous_task_id AS tarea_anterior_id, text AS texto,
       source AS fuente, created_at AS fecha FROM ai_recommendation;

CREATE OR REPLACE VIEW llamadas_ia AS
SELECT id, operation AS operacion, CASE outcome WHEN 'DISABLED' THEN 'Desactivada' WHEN 'OK' THEN 'Correcta' WHEN 'ERROR' THEN 'Error' ELSE outcome END AS resultado, duration_ms AS duracion_ms,
       input_tokens AS tokens_de_entrada, output_tokens AS tokens_de_salida, created_at AS fecha FROM ai_call;
