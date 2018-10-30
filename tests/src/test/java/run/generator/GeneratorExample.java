package run.generator;

import effekt.*;
import run.amb.*;

public class GeneratorExample implements Runnable {

    void numbers(Generator<Integer> gen, int to) throws Effects {
        int n = 0;
        while (n <= to) {
            gen.yield(n++);
        }
    }

    void numbersFlip(Generator<Integer> gen, Amb amb, int to) throws Effects {
        int n = 0;
        while (n <= to) {
            int next = amb.flip() ? n : n * -1;
            n++;
            gen.yield(next);
        }
    }

    void countTo10() throws Effects {
        EffectfulIterator<Integer> it = Iterate.handle(i -> { numbers(i, 10); return null; });

        System.out.println("After handle");

        while (it.hasNext()) {
            System.out.println(it.next());
        }
    }

    void flipCount() throws Effects {
        AmbList.handle(amb -> {
            EffectfulIterator<Integer> it = Iterate.handle(i -> { numbers(i, 10); return null; });

            System.out.println(it.next());        // 0
            System.out.println(it.next());        // 1
            if (amb.flip()) {
                System.out.println("true");
                System.out.println(it.next());    // 2
                System.out.println(it.next());    // 3
            } else {
                System.out.println("false");

                // since `it` is mutated outside of the scope of the ambient handler state.
                System.out.println(it.next());    // 4
            }

            return null;
        });
    }

    void flipCountInside() throws Effects {
        AmbList.handle(amb -> {
            EffectfulIterator<Integer> it = Iterate.handle(i -> { numbersFlip(i, amb, 2); return null; });

            while (it.hasNext()) {
                System.out.println(it.next());
            }
            return null;
        });
    }

    public void run() {
        Effekt.run(() -> {
            System.out.println("--- countTo10 ---");
            countTo10();

            System.out.println("--- flipCount ---");
            flipCount();

            System.out.println("--- flipCountInside ---");
            flipCountInside();

            return null;
        });
    }

}