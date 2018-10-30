package run.fibers;

import effekt.*;

public class Example implements Runnable {

    static void user1(Scheduler s) throws Effects {
        s.fork(() -> {
            System.out.println("Hello from fork");
            s.yield();
            System.out.println("Hello from fork 2");
        });
        System.out.println("Hello from main");
        s.yield();
        System.out.println("Hello from main 2");
        s.yield();
        System.out.println("Hello from main 3");
    }

    static void user2() throws Effects {
        Fiber f = Fiber.create(fiber -> {
            System.out.println("Hello from fiber");
            fiber.suspend();
            System.out.println("back again");
            return 42;
        });

        f.resume();
        System.out.println("In our main thread");
        f.resume();
        System.out.println("Again in our main thread");
        System.out.println("Fiber is done: " + f.isDone());
        System.out.println("Result is: " + f.result());
    }

    public void run() {
        Effekt.run(() -> {
            Example.user2();
            return null;
        });
        System.out.println("---");
        Effekt.run(() -> {
            Scheduler.handle(s -> {
                Example.user1(s);
                return null;
            });
            return null;
        });
    }
}