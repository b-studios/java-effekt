package run.amb;

import effekt.*;

import java.util.List;

public class Example implements Runnable {

    void drunkFlip() throws Effects {

        AmbList<Integer> a = new AmbList<Integer>() {};

        List<Integer> res = a.handle(() -> {
            System.out.println("Trying to flip a coin...");
            if (!a.flip()) {
                System.out.println("We dropped the coin");
                return -1;
            } else {
                System.out.println("We caught the coin");
                if (a.flip()) {
                    System.out.println("That's heads");
                    return 0;
                } else {
                    System.out.println("That's tails");
                    return 1;
                }
            }
        });

        System.out.println(res);
    }

    public void run() {
        Effekt.run(() -> {
            new Example().drunkFlip();
            return null;
        });
    }
}
