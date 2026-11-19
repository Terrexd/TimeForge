package es.timeforge.modelo.servicios;

public class ErrorNegocio extends RuntimeException {
    public final int status;

    public ErrorNegocio(String message) {
        this(400, message);
    }

    public ErrorNegocio(int status, String message) {
        super(message);
        this.status = status;
    }

    public static void require(boolean condition, String message) {
        if (!condition)
            throw new ErrorNegocio(message);
    }
}
