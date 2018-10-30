package run.state;

import run.amb.*;
import effekt.*;

public class AmbientState implements Runnable {

    Void ex1(CrazyState s, Amb a) throws Effects {
        if (a.flip()) {
            s.put(1);
        }
        System.out.println(s.get());
        return null;
    }

    Void ex2(CrazyState s) throws Effects {
        System.out.println(s.get()); //=> 0
        s.foo();
        System.out.println(s.get()); //=> 2  | 2
        s.put(42);
        System.out.println(s.get()); //=> 42 | 42
        return null;
    }

    Void ex3(CrazyState s) throws Effects {
        System.out.println(s.get()); //=> 0
        s.bar();
        System.out.println(s.get()); //=> 3  | 43
        s.put(42);
        System.out.println(s.get()); //=> 42 | 42
        return null;
    }

    public void run() {
        System.out.println("--- ex1 ---");
        Effekt.run(() -> AmbList.handle(a -> MyState.handle(0, s -> ex1(s, a))));
        Effekt.run(() -> MyState.handle(0, s -> AmbList.handle(a -> ex1(s, a))));

        System.out.println("--- ex2 ---");
        Effekt.run(() -> MyState.handle(0, s -> ex2(s)));

        System.out.println("--- ex3 ---");
        Effekt.run(() -> MyState.handle(0, s -> ex3(s)));
    }
}
interface CrazyState extends State<Integer> {
    void foo() throws Effects;
    void bar() throws Effects;
}
class MyState<R> implements CrazyState, StatefulHandler<R, R, Integer> {

    Integer state;

    public R pure(R r) { return r; }

    public Integer get() throws Effects { return state; }
    public void put(Integer s2) throws Effects {  state = s2; }
    public void foo() throws Effects {
        state = 2;
        use(k -> {
            k.resume(null);
            state = 13; // doesn't affect the continuation call since the state will be restored.
            k.resume(null);
            return null;
        });
    }

    public void bar() throws Effects {
        state = 2;
        useStateful( (k, s) -> {
            k.resume(null, state += 1);
            k.resume(null, state += 1);
            return null;
        });
    }

    static <R> R handle(Integer init, Effectful<CrazyState, R> prog) throws Effects {
        MyState<R> s = new MyState<R>() {};
        return s.handle(() -> prog.apply(s), init);
    }

    public Integer exportState() { return state; }
    public void importState(Integer newState) { state = newState; }
}