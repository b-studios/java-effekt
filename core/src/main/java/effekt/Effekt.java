package effekt;

import effekt.instrumentation.annotations.DontInstrument;
import effekt.runtime.Runtime;

@DontInstrument
public final class Effekt extends Runtime {

    private Effekt() {}

    // --- Operations used by the instrumentation ---
    public static int resultI() throws Throwable { return (Integer) result(); }
    public static long resultJ() throws Throwable { return (Long) result(); }
    public static float resultF() throws Throwable { return (Float) result(); }
    public static double resultD() throws Throwable { return (Double) result(); }
    public static void resultVoid() throws Throwable { result(); }

    public static void returnWith(long result) { returnWith(Long.valueOf(result)); }
    public static void returnWith(int result) { returnWith(Integer.valueOf(result)); }
    public static void returnWith(float result) { returnWith(Float.valueOf(result)); }
    public static void returnWith(double result) { returnWith(Double.valueOf(result)); }
    public static void returnVoid() { returnWith(null); }
}