package run.generator;

import effekt.*;

public interface Generator<A> {
    void yield(A a) throws Effects;
}