package run;

import effekt.*;

public class Subtyping implements Runnable {

    void effectful(Foo f) throws Effects {
        System.out.println(f.foo());
    }

    void pure(Bar b) throws Effects {
        System.out.println(b.foo());
    }

    public void run() {
        Foo f = new Foo();
        Bar b = new Bar();

        Effekt.run(() -> {
            effectful(f);
            effectful(b);
            // pure(f); // won't typecheck
            pure(b);
            return null;
        });
    }

}

class Foo {
    int foo() throws Effects { return 43; }
}
class Bar extends Foo {
    @Override
    int foo() { return 42; }
}