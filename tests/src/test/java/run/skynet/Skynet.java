package run.skynet;

import effekt.*;

import run.fibers.*;

// skynet benchmark:
//    https://github.com/atemerev/skynet
//
// not using effects at all
public class Skynet implements Runnable {

    static long skynet(long num, int size, int div) throws Effects {
        if (size == 1)
            return num;

        Fiber<Long>[] children = new Fiber[div];
        long sum = 0L;
        for (int i = 0; i < div; i++) {
            long subNum = num + i * (size / div);
            children[i] =  Fiber.create(fiber -> skynet(subNum, size / div, div));
        }
        for (Fiber<Long> c : children) {
            c.resume();
            sum += c.result();
        }
        return sum;
    }

    public void run() {
        Effekt.run(() -> {
            Fiber<Long> f = Fiber.create(fiber -> skynet(0, 1_000_000, 10));
            f.resume();
            assert f.result() == 499999500000L;
            return null;
        });
    }
}