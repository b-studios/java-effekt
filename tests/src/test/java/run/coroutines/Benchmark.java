package run.coroutines;

import effekt.*;
import run.generator.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

// Up to now we already discovered three different implementations of Pull vs Push
// (1) pipes example: the continuation of the other end is stored in handler state
// (2) coroutines: the continuation is store in a mutable reference cell
// (3) parsers: the continuation is returned in an immutable container (PushParser)
//
// Example translated from http://drops.dagstuhl.de/opus/volltexte/2018/9208/
public class Benchmark implements Runnable {

    public <A> void bucket(A[] b, Yield<?, A> y) throws Effects {
        for (int i = 0; i < b.length; i++) {
            y.yield(b[i]);
        }
    }

    public <A> Void buckets(A[][] t, Yield<?, A> y) throws Effects {
        int i = 0;
        while (i < t.length) {
            bucket(t[i], y);
            i++;
        }
        return null;
    }

    public <A> void bucketGenerator(A[] b, Generator<A> gen) throws Effects {
        for (int i = 0; i < b.length; i++) {
            gen.yield(b[i]);
        }
    }

    public <A> Void bucketsGenerator(A[][] t, Generator<A> gen) throws Effects {
        int i = 0;
        while (i < t.length) {
            bucketGenerator(t[i], gen);
            i++;
        }
        return null;
    }


    //
    // Direct DelimCC implementation
    //

    private Prog<Void> nextCont = null;
    private long nextValue = 0;
    public void bucketDelimCC(Long[] b, Prompt<Void> p) throws Effects {
        for (int i = 0; i < b.length; i++) {
            long curr = b[i];
            Effekt.withSubcontinuation(p, k -> {
                nextCont = () -> {
                    nextCont = null;
                    nextValue = curr;
                    return Effekt.pushPrompt(p, k);
                };
                return null;
            });
        }
    }

    public Void bucketsDelimCC(Long[][] t, Prompt<Void> p) throws Effects {
        int i = 0;
        while (i < t.length) {
            bucketDelimCC(t[i], p);
            i++;
        }
        return null;
    }


    private static int NOUTER = 500;
    private static int NINNER = 500;

    private static int ITERATIONS = 10;

    private static Long[][] DATA;

    {
        DATA = new Long[NOUTER][NINNER];
        final Random rnd = ThreadLocalRandom.current();
        for (int i = 0; i < NOUTER; i++) {
            // build inner bucket
            Long[] inner = new Long[NINNER];
            for (int j = 0; j < NINNER; j++) {
                inner[j] = rnd.nextLong();
            }
            DATA[i] = inner;
        }
    }

    public long findMaxIterator() {
        long max = Long.MIN_VALUE;
        BucketsIterator<Long> it = new BucketsIterator(DATA);
        while (it.hasNext()) {
            long next = it.next();
            if (next > max) { max = next; }
        }
        return max;
    }

    long findMaxCoroutines() {
        return Effekt.run(() -> {
            long max = Long.MIN_VALUE;
            Coroutine<Void, Long, Void> co = Coroutine.call(this::buckets, DATA);
            do {
                // this exposes a bug with OPAL's stackmap generation.
                long next = co.value();
                if (next > max) { max = next; }
            } while(co.resume());
            return max;
        });
    }

    long findMaxGenerators() {
        return Effekt.run(() -> {
            long max = Long.MIN_VALUE;
            EffectfulIterator<Long> it = Iterate.handle(i -> { bucketsGenerator(DATA, i); return null; });
            while (it.hasNext()) {
                long next = it.next();
                if (next > max) { max = next; }
            }
            return max;
        });
    }

    long findMaxDelimCC() {
        return Effekt.run(() -> {
            long max = Long.MIN_VALUE;
            Prompt<Void> p = new Prompt<Void>() {};
            Effekt.pushPrompt(p, () -> bucketsDelimCC(DATA, p));

            while (nextCont != null) {
                nextCont.apply();
                if (nextValue > max) { max = nextValue; }
            }
            return max;
        });
    }

    public void runIterators() {
//        for (int i = 0; i < ITERATIONS; i++)
        findMaxIterator();
    }
    public void run() {
//        for (int i = 0; i < ITERATIONS; i++)
//        findMaxGenerators();
        findMaxDelimCC();
//        findMaxCoroutines();
    }
}
