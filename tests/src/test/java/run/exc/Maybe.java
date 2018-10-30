package run.exc;

import effekt.*;
import run.Lists;

import java.util.Optional;

public interface Maybe<R> extends Exc, Handler<R, Optional<R>> {

    default Optional<R> pure(R r) { return Optional.of(r); }

    default <A> A raise(String msg) throws Effects {
        return discard(() -> Optional.empty());
    }

    static <R> Optional<R> handle(Effectful<Exc, R> prog) throws Effects {
        Maybe<R> a = new Maybe<R>() {};
        return a.handle(() -> prog.apply(a));
    }
}