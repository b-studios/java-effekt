package run.exc;

import effekt.*;

public interface Exc {
    <A> A raise(String msg) throws Effects;
    default <A> A raise() throws Effects {
        return raise(null);
    }
}