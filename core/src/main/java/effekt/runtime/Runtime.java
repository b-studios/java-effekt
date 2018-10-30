package effekt.runtime;

import effekt.*;
import effekt.instrumentation.annotations.DontInstrument;
import scala.Tuple2;

// CPS Runtime

@DontInstrument
public class Runtime {

    private static SplitSeq stack = EmptyCont$.MODULE$;
    private static Object result = null;

    private static Throwable lastException = null;

    private static Frame lastFrame = null;

    public static void push(Frame frame) {
        pushLastFrame();
        lastFrame = frame;
    }
    public static void pop() { if (lastFrame != null) lastFrame = null; else stack = stack.tail(); }

    public static Object result() throws Throwable {
        if (lastException == null)
            return result;
        else
            throw lastException;
    }

    public static void returnWith(Object r) {
        result = r; lastException = null;
    }

    // clean up stack in case an effectful operation throws an exception
    public static void onThrow(Throwable t) throws Throwable {
        if (lastFrame != null) lastFrame = null;
        else stack = stack.tail();
        throw t;
    }

    public static <A> A run(Prog<A> prog) {
        stack = EmptyCont$.MODULE$;
        lastFrame = prog::apply;
        trampoline();

        if (lastException != null) {
            throw new RuntimeException("Exception during effectful execution", lastException);
        }

        return (A) result;
    }


    // Delimited Control Operators

    public static <A> A pushPrompt(Prompt<A> prompt, Prog<A> prog) throws Effects {
        pushLastFrame();
        stack = stack.pushPrompt(prompt);
        return prog.apply();
    }

    private static void pushLastFrame() {
        if (lastFrame != null) {
            stack = stack.push(lastFrame);
            lastFrame = null;
        }
    }

    public static <A, R> A withSubcontinuation(Prompt<R> prompt, effekt.CPS<A, R> body) throws Effects {
        pushLastFrame();
        final Tuple2<Segment, SplitSeq> res = stack.splitAt(prompt);
        final Segment init = res._1;
        final SplitSeq rest = res._2;

        lastFrame = null;
        stack = rest;
        // The following only works with `Config.pushInitialEntrypoint`, since otherwise the callsite will
        // not typecheck since it still expects a value of type A.
        //
        //     return (A) body.apply(new Subcont<>(init));
        body.apply(new Subcont<>(init));
        return null;
    }

    private static void trampoline() {
        while (lastFrame != null || stack.nonEmpty()) {
            try {
                if (lastFrame != null) {
                    Frame f = lastFrame;
                    lastFrame = null;
                    f.enter();
                } else {
                    final Tuple2<Frame, SplitSeq> res = stack.pop();
                    stack = res._2;
                    res._1.enter();
                }
            }
            catch (Effects e) {}
            catch (Throwable t) {
                lastException = t;
            }
        }
    }

    @DontInstrument
    final static class Subcont<A, R> implements Continuation<A, R> {
        final Segment init;
        Subcont(Segment init) { this.init = init; }

        public R resume(A value) throws Effects {
            pushLastFrame();
            stack = init.prependTo(stack);
            Effekt.returnWith(value);
            return null;
        }
    }
}