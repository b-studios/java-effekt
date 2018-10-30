package run.select;

import effekt.*;
import run.Lists;

import java.util.List;
import java.util.Optional;

public interface SelectAll<R, A> extends Select<A>, Handler<R, List<R>> {

    default List<R> pure(R r) throws Effects { return Lists.singleton(r); }

    default A select(List<A> choices) throws Effects {
        return use(k -> {
            List<R> result = Lists.empty();

            for (A choice : choices) {
                result = Lists.concat(result, k.resume(choice));
            }

            return result;
        });
    }

    public static <R, A> List<R> handle(Effectful<Select<A>, R> prog) throws Effects {
        SelectAll<R, A> a = new SelectAll<R, A>() {};
        return a.handle(() -> prog.apply(a));
    }
}