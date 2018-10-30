package run.state;

import effekt.*;

public class SimpleRead implements Runnable {

    public static void readOnce(State<Integer> s) throws Effects {
        System.out.println(s.get());
    }

    public void run() {
        Effekt.run(() -> {
            StateFun.handle(42, s -> {
                readOnce(s);
                return null;
            });
            return null;
        });
    }
}