package run.state;

import effekt.*;

public class SilentCountDown implements Runnable {

    int N = 10_000;

    public static void countDown(State<Integer> s) throws Effects {
        while (s.get() > 0) {
            s.put(s.get() - 1);
        }
    }

    public void run() {
        Effekt.run(() -> {
            StateFun.handle(N, s -> { countDown(s); return null; });
            return null;
        });
    }
}