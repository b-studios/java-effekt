package run.select;

import effekt.*;

import java.util.List;

public interface Select<A> {
    A select(List<A> choices) throws Effects;
}