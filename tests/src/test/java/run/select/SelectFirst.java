package run.select;

import effekt.*;
import java.util.List;
import java.util.Optional;

public interface SelectFirst<R, A> extends Select<A>, Handler<R, Optional<R>> {

    default Optional<R> pure(R r) throws Effects { return Optional.of(r); }

    default A select(List<A> choices) throws Effects {
        return use(k -> {
            for (A choice : choices) {
                Optional<R> res = k.resume(choice);
                if (res.isPresent()) {
                    return res;
                }
            }
            return Optional.empty();
        });
    }

    static <R, A> Optional<R> handle(Effectful<Select<A>, R> prog) throws Effects {
        SelectFirst<R, A> a = new SelectFirst<R, A>() {};
        return a.handle(() -> prog.apply(a));
    }
}