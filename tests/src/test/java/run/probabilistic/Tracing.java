package run.probabilistic;

import effekt.*;
import run.Lists;
import run.amb.Amb;
import scala.Serializable;

import java.util.Collections;
import java.util.List;

public class Tracing<R> implements Amb, Handler<R, R>, Runnable, Serializable {

    // use mutable state for now.
    final List<SamplePoint<R>> pts = Lists.empty();

    public R pure(R res) throws Effects {

        // ok some very specialized sampling:
        //   We are trying to find a result which is == 1
        if ((Integer) res != 1) {
            Collections.shuffle(pts);

            // find first samplePoint that is not exhausted
            for (SamplePoint<R> pt : pts) {
                if (pt.choices.size() < 2) {
                    boolean alternative = !pt.choices.get(0);
                    // mark as having tried that one
                    pt.choices.add(alternative);
                    return pt.k.resume(alternative);
                }
            }
            throw new RuntimeException("Could not find samples to produce expected result");
        } else {
            System.out.println("Found 1 with samples:\n    " + pts);
            return res;
        }
    }

    public boolean flip() throws Effects {
        return use(k -> {
            boolean choice = Math.random() > 0.5;
            pts.add(new SamplePoint<R>(Lists.singleton(choice), k));
            return k.resume(choice);
        });
    }

    static <R> R handle(Effectful<Amb, R> prog) throws Effects {
        final Tracing<R> a = new Tracing<R>() {};
        return a.handle(() -> prog.apply(a));
    }

    public void run() {
        for (int i = 0; i < 10; i ++) {
            Integer res = Effekt.run(() ->
                    Tracing.handle(a -> {
                        if (a.flip()) {
                            if (a.flip()) {
                                return 1;
                            } else {
                                return 2;
                            }
                        } else {
                            if (a.flip()) {
                                return 3;
                            } else {
                                return 4;
                            }
                        }
                    })
            );
        }
    }
}

class SamplePoint<R> implements Serializable {
    final List<Boolean> choices;

    final Continuation<Boolean, R> k;

    public SamplePoint(List<Boolean> choices, Continuation<Boolean, R> k) {
        this.choices = choices; this.k = k;
    }
}
