package run.probabilistic;

import effekt.*;

public interface Prob {
    boolean flip() throws Effects;
    <A> A fail() throws Effects;
    void factor(double p) throws Effects;

    // could also be the primitive effect op and `flip = bernoulli(0.5)`
    default boolean bernoulli(double p) throws Effects {
        if (flip()) {
            factor(p);
            return true;
        } else {
            factor(1 - p);
            return false;
        }
    }

    default void guard(boolean b) throws Effects {
        if (!b) fail();
    }
}