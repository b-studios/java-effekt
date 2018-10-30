package run.coroutines;

import effekt.*;

public interface Yield<In, Out> {
    In yield(Out out) throws Effects;

    default In yield() throws Effects {
        return yield(null);
    }
}