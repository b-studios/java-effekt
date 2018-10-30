package run;

import effekt.*;

public class SimpleInstance implements Runnable {

     public int effectOp1() throws Effects { return 42; }

     public int effectOp2(int foo) throws Effects {
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
