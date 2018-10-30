package run.fibers;

import effekt.*;

import java.util.LinkedList;
import java.util.Queue;

public class Scheduler implements Handler<Void, Void> {

    private Queue<Prog> tasks = new LinkedList<>();

    public Void pure(Void r) throws Effects { return null; }

    public boolean fork() throws Effects {
        return use(k -> {
            tasks.add(() -> k.resume(true));
            // this is a tailresumption, could be optimized
            return k.resume(false);
        });
    }

    public void fork(Task t) throws Effects {
        if (fork()) { t.run(); discard(() -> null); }
    }

    // Since we only run on one thread, we also need yield in the scheduler
    // to allow cooperative multitasking
    public void yield() throws Effects {
        useOnce(k -> { tasks.add(k); return null; });
    }

    // we can't run the scheduler in pure since the continuation that contains
    // the call to pure might be discarded by one fork, while another one
    // is still alive.
    public void run() throws Effects {
        while (!tasks.isEmpty())
            tasks.remove().apply();
    }

    public static Void handle(Effectful<Scheduler, Void> prog) throws Effects {
        Scheduler it = new Scheduler();
        it.handle(() -> prog.apply(it));

        // this is safe since all tasks container the scheduler prompt marker
        it.run();
        return null;
    }
}