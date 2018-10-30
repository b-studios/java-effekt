package run.fibers;

import effekt.*;

public interface Suspendable {
    void suspend() throws Effects;
}