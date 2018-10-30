package run;

import effekt.*;

public class Locals implements Runnable {


    public static int longToInt(long n) throws Effects {
        return (int) n;
    }

    public static double floatToDouble(float f) throws Effects {
        return (double) f;
    }

    public static int manyArgs(int n, long m, float x, double y, boolean b) throws Effects {
        int foo = longToInt(m);
        // just use all of them here
        if (b)
            System.out.println(x + y);
        else
            System.out.println(n + m);

        return foo;
    }

    public int manyArgsInstance(int n, long m, float x, double y, boolean b) throws Effects {
        int foo = longToInt(m);
        if (b)
            System.out.println(x + y);
        else
            System.out.println(n + m);

        return foo;
    }

    public static double manyOperands() throws Effects {
        return sumThem(1.0f, 1.0, 2.0f, 2.0, 3.0f, floatToDouble(3.0f));
    }

    public static double manyOperandsInstance() throws Effects {
        return sumThem(1.0f, 1.0, 2.0f, 2.0, 3.0f, floatToDouble(3.0f));
    }

    public static double manyLocalsAndOperands(int n, long m, float x, double y, boolean b) throws Effects {
        return sumThem(n, m, x, y, b ? 1 : 2, floatToDouble(3.0f));
    }

    public static double manyLocalsAndOperands2(int n, long m, float x, double y, boolean b) throws Effects {
        return sumThem(n, m, x, y, b ? 1 : 2, floatToDouble(3.0f)) +
               sumThem(n, m, x, y, b ? 1 : 2, floatToDouble(3.0f));
    }

    public static double sumThem(float f1, double d1, float f2, double d2, float f3, double d3) {
        return f1 + d1 + f2 + d2 + f3 + d3;
    }

    public void run() {
        Effekt.run(() ->  {
          manyArgs(42, 100L, 1.42F, 3.2, false);
          new Locals().manyArgsInstance(42, 100L, 1.42F, 3.2, true);
          System.out.println(manyOperands());
          System.out.println(new Locals().manyOperandsInstance());
          System.out.println(manyLocalsAndOperands(42, 100L, 1.42F, 3.2, false));
          return null;
        });
    }
}
