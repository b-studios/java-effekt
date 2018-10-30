package run.await;

import effekt.*;
import scala.concurrent.duration.*;

import java.util.LinkedList;
import java.util.Queue;

public class MutableAwait implements Await, Handler<Void, Void>{

    Queue<Prog<Void>> processes = new LinkedList<>();

    public Void pure(Void r) throws Effects {
        if (processes.isEmpty())
            return null;
        else
            return processes.remove().apply();
    }

    public <A> A await(Awaiting<A> body) throws Effects {
        return useOnce(k -> {
            body.apply(res -> {
                processes.add(() -> k.resume(res));
                return processes.remove().apply();
            });
            return null;
        });
    }

    public boolean fork() throws Effects {
        return use(k -> {
            processes.add(() -> k.resume(false));
            return k.resume(true);
        });
    }

    public static Void handle(Effectful<Await, Void> prog) throws Effects {
        MutableAwait it = new MutableAwait();
        return it.handle(() -> prog.apply(it));
    }
}