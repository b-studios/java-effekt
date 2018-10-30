package run.select;

import effekt.Effekt;

import java.util.List;

public class EnumerateAll implements Runnable {
    public void run() {
        Effekt.run(() -> {
            NQueens.tries = 0;
            List<List<Pos>> result = SelectAll.<List<Pos>, Integer> handle(s -> NQueens.nqueens(9, s));
            System.out.println("Overall, tried " + NQueens.tries + " y-positions");
            System.out.println(result.size() + " results");
            return null;
        });
    }
}