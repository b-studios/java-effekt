package effekt;

import effekt.stateful.Stateful;

public interface StatefulHandler<R, Res, State> extends Handler<R, Res>, Stateful<State> {

    Res pure(R r) throws Effects;

    default Res handle(Prog<R> prog, State init) throws Effects {
        importState(init);
        return Effekt.pushPrompt(this, () -> pure(prog.apply()));
    }

    // on stateful handlers
    //   use(k -> ... k.resume(a) ...) should be equivalent to
    //   useStateful( (k, s) -> ... k.resume(a, s) ... )
    default <A> A useStateful(effekt.stateful.CPS<A, Res, State> body) throws Effects {
        final State before = exportState();
        return Effekt.withSubcontinuation(this, k ->
            body.apply((a, s) -> Effekt.pushPrompt(this, () -> {
                    importState(s);
                    return k.resume(a);
                }),
                before));
    }

    default <A> A use(CPS<A, Res> body) throws Effects {
        final State before = exportState();
        return Effekt.withSubcontinuation(this, k ->
            body.apply(a ->
                Effekt.pushPrompt(this, () -> {
                    importState(before);
                    return k.resume(a);
                })));
    }

    // only for compatibility:
    default <A> A useOnce(effekt.stateful.CPS<A, Res, State> body) throws Effects {
        return useStateful(body);
    }
}