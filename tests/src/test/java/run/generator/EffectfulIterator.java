package run.generator;

import effekt.*;

// Note: the java.lang.Iterator interface does not declare Effects to be thrown.
public interface EffectfulIterator<A> {
    boolean hasNext() throws Effects;
    A next() throws Effects;
}