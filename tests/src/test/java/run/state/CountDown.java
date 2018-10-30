package run.state;

import effekt.*;

public class CountDown implements Runnable {

    public static void countDown(State<Integer> s) throws Effects {
        System.out.println("Start: " + s.get());
        while (s.get() > 0) {
            System.out.println("At: " + s.get());
            s.put(s.get() - 1);
        }
        System.out.println("Done: " + s.get());
    }

    public void run() {
        Effekt.run(() -> {
            StateFun.handle(10, s -> { countDown(s); return null; });
            return null;
        });
    }
}