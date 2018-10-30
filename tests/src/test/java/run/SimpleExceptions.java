package run;

import effekt.*;

class MyException extends RuntimeException {}

public class SimpleExceptions implements Runnable {

    public static int dangerous() throws Effects {
        throw new MyException();
    }

    public static int effectOp1() throws Effects {
        System.out.println("Before dangerous");
        int n = dangerous();
        System.out.println("After dangerous");
        return n;
    }

    public static int effectOp2() throws Effects {
        int n = 0;
        try {
            n = effectOp1();
        } catch (MyException e) {
            System.out.println("got it");
            n = 42;
        }
        return n;
    }

    public void run() {
        Effekt.run(() ->  {
          System.out.println(effectOp2());
          return null;
        });
    }
}
