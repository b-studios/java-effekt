package run;

import effekt.*;
import effekt.Effekt;

public class Simple implements Runnable {

     public static int effectOp1() throws Effects { return 42; }

     public static int effectOp2(int foo) throws Effects {
         int i = 0;
         System.out.println(i + foo);
         i += effectOp1();
         System.out.println(i);
         return i;
     }

    public void run() {
        Effekt.run(() ->  {
          int i = 0;
          System.out.println(i);
          i += effectOp2(5);
          System.out.println(i);
          return null;
        });
    }
}
