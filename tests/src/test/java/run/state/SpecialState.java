package run.state;

import effekt.*;

public class SpecialState<R, S> implements State<S>, StatefulHandler<R, R, S> {

    S state;

    public R pure(R r) { return r; }

    public S get() throws Effects { return state; }

    public void put(S s2) throws Effects { state = s2; }

    static <R, S> R handle(S init, Effectful<State<S>, R> prog) throws Effects {
        SpecialState<R, S> s = new SpecialState<R, S>() {};
        return s.handle(() -> prog.apply(s), init);
    }

    public S exportState() { return state; }
    public void importState(S newState) { state = newState; }
}