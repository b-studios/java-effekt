package run.coroutines;

import effekt.*;
import run.amb.*;

import java.util.Iterator;
import java.util.List;

// Up to now we already discovered three different implementations of Pull vs Push
// (1) pipes example: the continuation of the other end is stored in handler state
// (2) coroutines: the continuation is store in a mutable reference cell
// (3) parsers: the continuation is returned in an immutable container (PushParser)
//
// Example translated from http://drops.dagstuhl.de/opus/volltexte/2018/9208/
public class Example implements Runnable {

    public Void random(Amb a, Yield<?, Integer> y) throws Effects {
        y.yield(0);

        if (a.flip()) {
            y.yield(1);
        } else {
            y.yield(2);
        }

        y.yield(3);
        return null;
    }

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

    public void run() {

        // [[4, 3, 5, 7], [8], [], [1, 3, 7]]
        Integer[][] data = {
                {4, 3, 5, 7}, {8}, {}, {1, 3, 7}
        };

        Effekt.run(() -> {
            Coroutine<Void, Integer, Void> co = Coroutine.call(this::buckets, data);

            do {
                System.out.println(co.value());
            } while(co.resume());
            return null;
        });

        System.out.println("---");

        Effekt.run(() -> {
            Coroutine<Void, Integer, Void> co = Coroutine.call(this::buckets, data);

            System.out.println(co.value()); // 4
            co.resume();
            System.out.println(co.value()); // 3

            Coroutine<Void, Integer, Void> co2 = co.snapshot();

            co.resume();
            System.out.println(co.value()); // 5

            System.out.println(co2.value()); // 3
            co2.resume();
            System.out.println(co2.value()); // 5

            System.out.println(co.value()); // 5
            return null;
        });

        System.out.println("---");

        Effekt.run(() -> {
            AmbList.handle(a -> {
                Coroutine<Void, Integer, Void> co = Coroutine.call(this::random, a);
                do {
                    System.out.println(co.value());
                } while (co.resume());
                return null;
            });
            return null;
        });

        System.out.println("--- iterators ---");
        BucketsIterator<Integer> it = new BucketsIterator(data);
        while (it.hasNext()) {
            System.out.println(it.next());
        }
    }

}

class BucketIterator<A> implements Iterator<A> {
    List<A> b;
    int i = 0;
    BucketIterator(List<A> bucket) { this.b = bucket; }
    public boolean hasNext() { return i < b.size(); }
    public A next() { return b.get(i++); }
}

// inlined manually
class BucketsIterator<A> implements Iterator<A> {

    final A[][] t;
    int i = 0;
    int j = 0;

    BucketsIterator(A[][] buckets) { this.t = buckets; }

    public boolean hasNext() {
        if (i >= t.length) return false;
        if (j < t[i].length) return true;
        i++;
        j = 0;
        return hasNext();
    }
    public A next() {
        if (!hasNext()) throw new RuntimeException("No more elements");
        return t[i][j++];
    }
}
