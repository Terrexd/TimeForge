package es.timeforge.controlador;

import es.timeforge.modelo.servicios.ErrorNegocio;
import java.util.*;
import org.slf4j.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class ErroresApi {
    private static final Logger LOG = LoggerFactory.getLogger(ErroresApi.class);

    @ExceptionHandler(ErrorNegocio.class)
    ResponseEntity<?> business(ErrorNegocio e) {
        return ResponseEntity.status(e.status).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(x -> fields.put(x.getField(), x.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Revisa los campos obligatorios y sus límites", "fields", fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<?> invalid() {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Los datos recibidos no tienen un formato válido"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> duplicate() {
        return ResponseEntity.status(409).body(Map.of("message",
                "El registro ya existe o tiene datos incompatibles. Recarga y revisa los valores"));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<?> database(DataAccessException e) {
        LOG.error("Error de persistencia", e);
        return ResponseEntity.status(503)
                .body(Map.of("message", "La base de datos no está disponible. Reintenta la operación"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception e) {
        LOG.error("Error no previsto", e);
        return ResponseEntity.status(500)
                .body(Map.of("message", "No se pudo completar la operación. Reintenta en unos instantes"));
    }
}
