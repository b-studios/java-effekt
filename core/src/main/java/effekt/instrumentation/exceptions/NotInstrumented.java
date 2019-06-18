package effekt.instrumentation.exceptions;

public class NotInstrumented extends RuntimeException {
    public static final NotInstrumented NOT_INSTRUMENTED = new NotInstrumented();
}