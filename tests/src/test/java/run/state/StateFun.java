package run.state;

import effekt.*;

public interface StateFun<R, S> extends State<S>, Handler<R, Effectful<S, R>> {
    default Effectful<S, R> pure(R r) { return s -> r; }

    default S get() throws Effects {
        return useOnce(k -> s -> k.resume(s).apply(s));
    }

    default void put(S s2) throws Effects {
        useOnce(k -> s -> k.resume(null).apply(s2));
    }

    static <R, S> R handle(S init, Effectful<State<S>, R> prog) throws Effects {
        StateFun<R, S> s = new StateFun<R, S>() {};
        return s.handle(() -> prog.apply(s)).apply(init);
    }
}