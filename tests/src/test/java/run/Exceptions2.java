package run;

import effekt.*;

public class Exceptions2 implements Runnable {

    private boolean fail = false;

    public int stackMapTest() throws Effects {
        try {
            System.out.println("before");
            effectOp();
            System.out.println("after");
        } catch (RuntimeException e) {
            System.out.println("handling");
            return 13;
        }
    return 42;
    }

    public void effectOp() throws Effects {
        System.out.println("failing: " + fail);
        if (fail) throw new RuntimeException("failed");
    }

    public void run() {
        Effekt.run(() ->  {
            fail = false;
            System.out.println(stackMapTest());

            fail = true;
            System.out.println(stackMapTest());
            return null;
        });
    }
}
