package run.fibers;

import effekt.*;

// A Fiber is something that can be resumed.
// Has been MUCH faster when directly storing the continuation instead of
// wrapping it into Prog. (about 8x)
public interface Fiber<A> {

    void resume() throws Effects;

    boolean isDone();
    A result();

    static <A> Fiber<A> create(Effectful<Suspendable, A> body) {
        // set up the communication channel between the two components
        final CanResume<A> fiber = new CanResume<A>();
        final CanSuspend<A> suspend = new CanSuspend<A>(fiber);

        // create first continuation by installing a ScheduledSuspendable handler
        fiber.next(() -> suspend.handle(() -> body.apply(suspend)));

        return fiber;
    }
}

class CanResume<A> implements Fiber<A> {

    // Result Handling
    // ===============

    private boolean done = false;
    private A res;

    public boolean isDone() { return done; }

    public A result() {
        if (!isDone())
            throw new UnsupportedOperationException("This fiber is not yet done, can't get result");

        return res;
    }

    void returnWith(A result) {
        this.res = result;
        done = true;
    }


    // Continuations
    // =============

    private Prog<A> k;

    public void resume() throws Effects {
        final Prog<A> cont = k;
        if (isDone())
            throw new UnsupportedOperationException("Can't resume this fiber anymore");

        cont.apply();
    }

    void next(Prog<A> k) { this.k = k; }
}

class CanSuspend<A> implements Suspendable, Handler<A, A> {

    private final CanResume<A> fiber;

    CanSuspend(CanResume<A> fiber) {
        this.fiber = fiber;
    }

    public A pure(A result) throws Effects {
        fiber.returnWith(result);
        return result;
    }

    public void suspend() throws Effects {
        use(k -> {
            fiber.next(k);
            return null;
        });
    }
}