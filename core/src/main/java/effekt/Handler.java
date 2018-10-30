package effekt;

public interface Handler<R, Res> extends Prompt<Res> {

    Res pure(R r) throws Effects;

    default Res handle(Prog<R> prog) throws Effects {
        return Effekt.pushPrompt(this, () -> pure(prog.apply()));
    }

    default <A> A use(CPS<A, Res> body) throws Effects {
        return Effekt.withSubcontinuation(this, k ->
            body.apply(a ->
                Effekt.pushPrompt(this, () ->
                    k.resume(a))));
    }

    // only for compatibility:
    default <A> A useOnce(CPS<A, Res> body) throws Effects {
        return use(body);
    }

    default <A> A discard(Prog<Res> body) throws Effects {
        return use(k -> body.apply());
    }

    static <R, E, H extends Handler<R, E>> E handle(H h, Effectful<H, R> prog) throws Effects {
        return Effekt.pushPrompt(h, () -> h.pure(prog.apply(h)));
    }
}