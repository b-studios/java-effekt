package run.coroutines;

import effekt.*;

// We use consistently across interfaces:
//
//   In: values that go into the coroutine
//   Out: values that come out of the coroutine
//   Result: the final result value of the coroutine
public interface Coroutine<In, Out, Result> {

    boolean resume(In in) throws Effects;
    default boolean resume() throws Effects {
        return resume(null);
    }

    boolean isDone();
    Out value();
    Result result();

    Coroutine<In, Out, Result> snapshot();

    static <In, Out, Arg, Result> Coroutine<In, Out, Result> call(CoroutineBody<In, Out, Arg, Result> body, Arg arg) throws Effects {
        final Yielder<In, Out, Result> yielder = new Yielder<>();
        final CoroutineInstance<In, Out, Result> instance = new CoroutineInstance<>(yielder, null);
        return instance.start(body, arg);
    }
}

class CoroutineInstance<In, Out, Result> implements Coroutine<In, Out, Result> {

    // used to communicate between Yielder and Instance
    private CoroutineState<In, Out, Result> state;

    private final Yielder<In, Out, Result> yielder;

    CoroutineInstance(Yielder<In, Out, Result> yielder, CoroutineState<In, Out, Result> state) {
        this.yielder = yielder;
        this.state = state;
    }

    // Result Handling
    // ===============
    public boolean isDone() {
        return state.state == CoroutineState.State.DONE;
    }

    public Result result() {
        if (!isDone())
            throw new UnsupportedOperationException("This coroutine is not yet done, can't get result");

        return state.result;
    }

    // Snapshotting
    // ============
    public Coroutine<In, Out, Result> snapshot() {
        return new CoroutineInstance(yielder, state);
    }

    // Continuations
    // =============
    public Out value() {
        if (isDone())
            throw new UnsupportedOperationException("Coroutine is done, doesn't have a value, but a result");

        return state.yieldValue;
    }

    public boolean resume(In in) throws Effects {
        if (isDone())
            throw new UnsupportedOperationException("Can't resume this coroutine anymore");

        yielder.state = state;
        state.k.resume(in);
        state = yielder.state;
        return !isDone();
    }

    <Arg> Coroutine<In, Out, Result> start(CoroutineBody<In, Out, Arg, Result> body, Arg arg) throws Effects {
        yielder.handle(() -> body.run(arg, yielder));
        state = yielder.state;
        return this;
    }


}

class Yielder<In, Out, Result> implements Yield<In, Out>, Handler<Result, Result> {

    public CoroutineState<In, Out, Result> state;

    public Result pure(Result result) throws Effects {
        state = CoroutineState.done(result);
        return result;
    }

    public In yield(Out out) throws Effects {
        return use(k -> {
            state = CoroutineState.paused(k, out);
            return null;
        });
    }
}

// Like "Free"
class CoroutineState<In, Out, Result> {

    // if we don't start it automatically, then we also need the state: "STARTED"
    enum State {
        PAUSED, DONE
    }

    final State state;
    final Result result;
    final Continuation<In, Result> k;
    final Out yieldValue;

    CoroutineState(State state, Continuation<In, Result> k, Out yieldValue, Result result) {
        this.state = state;
        this.result = result;
        this.k = k;
        this.yieldValue = yieldValue;
    }

    static <In, Out, Result> CoroutineState<In, Out, Result> paused(Continuation<In, Result> k, Out yieldValue) {
        return new CoroutineState<>(State.PAUSED, k, yieldValue, null);
    }

    static <In, Out, Result> CoroutineState<In, Out, Result> done(Result result) {
        return new CoroutineState<>(State.DONE, null, null, result);
    }
}
