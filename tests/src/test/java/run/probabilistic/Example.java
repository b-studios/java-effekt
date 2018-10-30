package run.probabilistic;

import effekt.*;

import java.util.List;
import run.Lists;

public class Example implements Runnable {

    static boolean falsePositive(Prob prob) throws Effects {
        boolean sick = prob.bernoulli(0.01);
        boolean positive = false;

        if (sick) {
            positive = prob.bernoulli(0.99);
        } else {
            positive = prob.bernoulli(0.1);
        }
        prob.guard(positive);

        return sick;
    }

    public void run() {
        Effekt.run(() -> {
            System.out.println(ProbHandler.handle(Example::falsePositive));
            return null;
        });
    }
}

class FlipOnce {
    static String flipOnce(Prob prob) throws Effects {
        if (prob.flip()) {
            return "Caught it";
        } else {
            return prob.fail();
        }
    }
}

class Drunkflip {
    static String flipCoin(Prob prob) throws Effects {
        if (prob.flip()) {
            if (prob.flip()) {
                return "Heads";
            } else {
                return "Tails";
            }
        } else {
            return prob.fail();
        }
    }
}

class ProbHandler<R> implements Prob, StatefulHandler<R, List<Weighted<R>>, Double> {

    double weight = 1.0;

    public List<Weighted<R>> pure(R r) throws Effects {
        return Lists.singleton(new Weighted(weight, r));
    }

    public <A> A fail() throws Effects {
        return discard(() -> Lists.empty());
    }

    public boolean flip() throws Effects {
        return use(k ->
            Lists.concat(k.resume(false), k.resume(true)));
    }

    public void factor(double p) throws Effects {
        weight *= p;
    }

    static <R> List<Weighted<R>> handle(Effectful<Prob, R> prog) throws Effects {
        ProbHandler<R> a = new ProbHandler<R>() {};
        return a.handle(() -> prog.apply(a), 1.0);
    }

    public Double exportState() { return weight; }
    public void importState(Double state) { weight = state; }
}


class Weighted<T> {
    final double prob;
    final T value;

    Weighted(double prob, T value) {
        this.prob = prob; this.value = value;
    }

    public String toString() {
        return "Weighted(" + value.toString() + " @ " + Double.toString(prob) + ")";
    }
}
