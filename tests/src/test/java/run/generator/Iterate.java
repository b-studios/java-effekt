package run.generator;

import effekt.*;
import effekt.stateful.Stateful;

import java.util.function.Consumer;

// this implementation performs the computational effect on `next()`
public class Iterate<A, R> implements Generator<A>, Handler<R, EffectfulIterator<A>> {


    private Prog<A> nextValue = null;

    private final EffectfulIterator<A> it = new EffectfulIterator<A>() {

        // maybe run on `hasNext` and cache result?
        public boolean hasNext() { return nextValue != null; }
        public A next() throws Effects {
            Prog<A> cont = nextValue;
            nextValue = null;
            return cont.apply();
        }
    };

    public EffectfulIterator<A> pure(R r) { return it; }

    public void yield(A a) throws Effects {
        useOnce(continuation -> {
            nextValue = () -> {
                continuation.resume(null);
                return a;
            };
            return it;
        });
    }

    public static <A, R> EffectfulIterator<A> handle(Effectful<Generator<A>, R> prog) throws Effects {
        Iterate<A, R> it = new Iterate<A, R>();
        return it.handle(() -> prog.apply(it));
    }

    public Prog<A> exportState() { return nextValue; }
    public void importState(Prog<A> state) { nextValue = state; }
}