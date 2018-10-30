package run.amb;

import effekt.*;
import run.Lists;

import java.util.List;

public interface AmbList<R> extends Amb, Handler<R, List<R>> {

    default List<R> pure(R r) {
        return Lists.singleton(r);
    }

    default boolean flip() throws Effects {
        return use(k -> {
            List<R> xs = k.resume(true);
            List<R> ys = k.resume(false);
            return Lists.concat(xs, ys);
        });
    }

    static <R> List<R> handle(Effectful<Amb, R> prog) throws Effects {
        AmbList<R> a = new AmbList<R>() {};
        return a.handle(() -> prog.apply(a));
    }
}