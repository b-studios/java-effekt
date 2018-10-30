package run.select;

import effekt.*;
import java.util.List;
import java.util.Optional;

public class Backtracking implements Runnable {
    public void run() {
        Effekt.run(() -> {
            NQueens.tries = 0;
            Optional<List<Pos>> result = SelectFirst.<List<Pos>, Integer> handle(s -> NQueens.nqueens(9, s));
            System.out.println("Overall, tried " + NQueens.tries + " y-positions");
            System.out.println(result.get());
            return null;
        });
    }
}