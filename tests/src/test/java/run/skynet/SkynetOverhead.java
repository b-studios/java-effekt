package run.skynet;

import effekt.*;

// skynet benchmark:
//    https://github.com/atemerev/skynet
//
// not suspending so not using algebraic effects at all.
public class SkynetOverhead implements Runnable {

    static long skynet(long num, int size, int div) throws Effects {
        if (size == 1)
            return num;

        Effectful<Void, Long>[] children = new Effectful[div];
        long sum = 0L;
        for (int i = 0; i < div; i++) {
            long subNum = num + i * (size / div);
            children[i] = v -> skynet(subNum, size / div, div);
        }
        for (int i = 0; i < div; i ++) {
            sum += children[i].apply(null);
        }
        return sum;
    }

    public void run() {
        Effekt.run(() -> {
            long result = skynet(0, 1_000_000, 10);
            assert result == 499999500000L;
            return null;
        });
    }
}