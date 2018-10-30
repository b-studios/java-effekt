package run.coroutines;

import effekt.*;

@FunctionalInterface
public interface CoroutineBody<In, Out, Arg, Result> {
    Result run(Arg arg, Yield<In, Out> y) throws Effects;
}