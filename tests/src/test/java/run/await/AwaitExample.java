package run.await;

import effekt.*;

import java.util.concurrent.*;

public class AwaitExample implements Runnable {

    void user1(Await a) throws Effects {
        System.out.println("hello 1");
        a.yield();
        System.out.println("world 1");
    }

    void user2(Await a) throws Effects {
        System.out.println("hello 2");
        a.yield();
        System.out.println("world 2");
        a.yield();
        System.out.println("and it goes on 2");
        a.yield();
        System.out.println("and it goes on and on 2");
    }

    Future<Integer> futureExample(Await a) throws Effects {

        final ExecutorService executor = Executors.newSingleThreadExecutor();

        return executor.submit(() -> {
            System.out.println("started future");
            Thread.sleep(1000);
            return 42;
        });
    }

    void forked(Await a) throws Effects {

        Future<Integer> f = futureExample(a);

        if (a.fork()) {
            user1(a);
            System.out.println(a.await(f) + 1);
        } else {
            user2(a);
        }
    }

    public void run() {
        Effekt.run(() -> {
            MutableAwait.handle(i -> { forked(i); return null; });

            return null;
        });
    }

}