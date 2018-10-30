package effekt.stateful;

import effekt.Effects;

public interface Continuation<A, B, S> {
    B resume(A value, S state) throws Effects;
}