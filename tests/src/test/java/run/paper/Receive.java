package run.paper;

import effekt.Effects;

public interface Receive<A> {
    A receive() throws Effects;
}