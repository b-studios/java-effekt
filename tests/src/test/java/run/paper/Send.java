package run.paper;

import effekt.Effects;

public interface Send<A> {
    void send(A value) throws Effects;
}