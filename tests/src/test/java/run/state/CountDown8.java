package run.state;

import effekt.*;
import run.amb.*;

public class CountDown8 implements Runnable {

    int N = 1_000;

    public static boolean countDown(
        State<Integer> s1,
        State<Integer> s2,
        State<Integer> s3,
        State<Integer> s4,
        State<Integer> s5,
        State<Integer> s6,
        State<Integer> s7,
        State<Integer> s8,
        Amb amb
    ) throws Effects {
        while (s1.get() > 0) {
            s1.put(s1.get() - 1);
            s2.put(s2.get() - 1);
            s3.put(s3.get() - 1);
            s4.put(s4.get() - 1);
            s5.put(s5.get() - 1);
            s6.put(s6.get() - 1);
            s7.put(s7.get() - 1);
            s8.put(s8.get() - 1);
        }
        return amb.flip();
    }

    public void run() {
        int init = N;
        Effekt.run(() ->
             AmbList.handle(a ->
             SpecialState.handle(init, s1 ->
             SpecialState.handle(init, s2 ->
             SpecialState.handle(init, s3 ->
             SpecialState.handle(init, s4 ->
             SpecialState.handle(init, s5 ->
             SpecialState.handle(init, s6 ->
             SpecialState.handle(init, s7 ->
             SpecialState.handle(init, s8 ->
                countDown(s1, s2, s3, s4, s5, s6, s7, s8, a)))))))))));
    }
}