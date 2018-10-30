package effekt.runtime;

import effekt.Effects;
import effekt.instrumentation.annotations.DontInstrument;

@DontInstrument
public interface Frame {
    void enter() throws Effects, Throwable;
}