package run.state;

import effekt.*;

interface State<S> {
    S get() throws Effects;
    void put(S s) throws Effects;
}