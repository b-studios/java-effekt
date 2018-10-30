package run.await;

import effekt.*;

public interface Awaiting<A> {
    void apply(Effectful<A, Void> k) throws Effects;
}