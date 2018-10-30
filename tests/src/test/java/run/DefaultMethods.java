package run;

import effekt.*;

interface SomeInterface {
    void EFF() throws Effects;

    default int add(int l, float r) throws Effects {
        int x = l + 1;
        EFF();
        return x + (int) r;
    }
}

public class DefaultMethods implements Runnable, SomeInterface {

    public void EFF() throws Effects {}

    public void run() {
        Effekt.run(() ->  {
          System.out.println(new DefaultMethods().add(3, 4.0f));
          return null;
        });
    }
}
