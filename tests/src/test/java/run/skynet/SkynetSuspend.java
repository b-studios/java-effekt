package run.skynet;

import effekt.*;

import run.fibers.*;

// skynet benchmark:
//    https://github.com/atemerev/skynet
//
// every fiber suspends once before returning the result.
public class SkynetSuspend implements Runnable {

    static long skynet(Suspendable fiber, long num, int size, int div) throws Effects {
        if (size == 1) {
            fiber.suspend();
            return num;
        }

        Fiber<Long>[] children = new Fiber[div];
        long sum = 0L;
        for (int i = 0; i < div; i++) {
            long subNum = num + i * (size / div);
            children[i] =  Fiber.create(f -> skynet(f, subNum, size / div, div));
        }

        // start all children
        for (Fiber<Long> c : children) {
            c.resume();
        }
        // wake up and get result
        for (Fiber<Long> c : children) {
            c.resume();
            sum += c.result();
        }

        fiber.suspend();
        return sum;
    }

    public void run() {
        Effekt.run(() -> {
            Fiber<Long> fiber = Fiber.create(f -> skynet(f, 0, 1_000_000, 10));
            fiber.resume();
            fiber.resume();
            assert fiber.result() == 499999500000L;
            return null;
        });
    }
}
