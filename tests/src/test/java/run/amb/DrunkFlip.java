package run.amb;

import effekt.*;
import run.exc.*;

import java.util.List;
import java.util.Optional;

public class DrunkFlip implements Runnable {

    public static String drunkFlip(Amb a, Exc e) throws Effects {

        // trying to flip a coin
        if (a.flip()) {
            return a.flip() ? "Heads" : "Tails";
        } else {
            return e.raise("dropped it");
        }
    }

    public void run() {
        List<Optional<String>> res1 = Effekt.run(() ->
            AmbList.handle(a -> Maybe.handle(e -> drunkFlip(a, e))));

        Optional<List<String>> res2 = Effekt.run(() ->
            Maybe.handle(e -> AmbList.handle(a -> drunkFlip(a, e))));

        System.out.println(res1);
        System.out.println(res2);
    }
}
