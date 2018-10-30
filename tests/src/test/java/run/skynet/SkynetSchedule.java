package run.skynet;

import effekt.*;
import run.fibers.*;

// skynet benchmark:
//    https://github.com/atemerev/skynet
//
// not using fibers, but cooperative multitasking with a scheduler
//
// we capture too large parts of the stack in this implementation.
// fibers only capture "their" stack not everything up to the scheduler.
//
// we also perform "busy waiting" which is pretty slow since it captures the
// continuation on every wait cycle ...
public class SkynetSchedule implements Runnable {

    long sum = 0L;
    int returned = 0;

    long skynet(Scheduler s, long num, int size, int div) throws Effects {
        sum = 0L; returned = 0;

        if (size == 1) {
            return num;
        }

        for (int i = 0; i < div; i++) {
            int id = i;
            s.fork(() -> {
                long subNum = num + id * (size / div);
                long res = new SkynetSchedule().skynet(s, subNum, size / div, div);
                returned++;
                sum += res;
            });
        }

        while (returned < div) { s.yield(); }
        s.yield();
        return sum;
    }

    public void run() {
        Effekt.run(() -> {
            return Scheduler.handle(s -> {
                long result = new SkynetSchedule().skynet(s, 0, 1_000_000, 10);
                assert result == 499999500000L;
                System.out.println(result);
                return null;
            });
        });
    }
}
