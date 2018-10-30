package effekt.stateful;

import effekt.Effects;

public interface CPS<A, B, S> {
    B apply(Continuation<A, B, S> k, S state) throws Effects;
}