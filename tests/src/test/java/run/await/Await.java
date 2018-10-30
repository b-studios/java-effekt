package run.await;

import effekt.*;
import scala.concurrent.duration.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public interface Await {

    <A> A await(Awaiting<A> body) throws Effects;

    boolean fork() throws Effects;

    default int forkN(int n) throws Effects {
        if (n <= 1)
            return 0;
        else if (fork())
            return n - 1;
        else
            return forkN(n - 1);
    }

    // polling based implementation
    default <A> A await(Future<A> f) throws Effects {
        do { yield(); } while (!f.isDone());
        A result = null;
        try {
            result = f.get();
        } catch (InterruptedException e) {
        } catch (ExecutionException e) {
        }
        return result;
    }

    default void exit() throws Effects {
        await(k -> {});
    }

    // yield control to another fiber
    default void yield() throws Effects {
        await(k -> k.apply(null));
    }

    // busy waiting
    default void sleepUntil(Deadline alarm) throws Effects {
        do { yield(); } while (!alarm.isOverdue());
    }

    default void sleep(FiniteDuration d) throws Effects {
        sleepUntil(Deadline.now().$plus(d));
    }
}