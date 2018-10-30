package run.select;

import effekt.*;
import run.Lists;

import java.util.List;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NQueens implements Runnable {

    static int N = 10;

    static int tries = 0;

    public static List<Pos> nqueens(int n, Select<Integer> sel) throws Effects {
        final List<Integer> ys = range(1, n);

        List<Pos> queens = Lists.empty();

        // here it is actually necessary not to use `for (int x : ys)`
        // otherwise we don't backtrack enough.
        for (int x = 1; x <= n; x++) {
            // this anf is required ATM! Causes problems with OPALs AI and stackmap generation.
            int y = sel.select(available(x, queens, ys));
            Pos pos = new Pos(x, y);
            tries += 1;
            queens = Lists.cons(pos, queens);
        }
        return queens;
    }

    private static List<Integer> available(int x, List<Pos> queens, List<Integer> ys) {
        Stream<Integer> is = ys.stream().filter(y -> {
            for (Pos q : queens) {
                if (attack(x, y, q)) {
                    return false;
                }
            }
            return true;
        });

        return Lists.fromStream(is);
    }

    private static boolean attack(int x, int y, Pos q2) {
        return x == q2.x || y == q2.y || Math.abs(x - q2.x) == Math.abs(y - q2.y);
    }

    // creates a range including the last index
    private static List<Integer> range(int from, int to) {
        return Lists.fromStream(IntStream.range(from, to + 1).boxed());
    }

    public void run() {
        Effekt.run(() -> {
            List<List<Pos>> result = SelectAll.<List<Pos>, Integer> handle(s -> nqueens(NQueens.N, s));
            return null;
        });
    }
}



class Pos {
    final int x;
    final int y;
    Pos(int x, int y) { this.x = x; this.y = y; }

    @Override
    public String toString() {
        return "Pos(" + x + ", " + y + ")";
    }
}
