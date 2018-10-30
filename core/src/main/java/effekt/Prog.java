package effekt;

public interface Prog<A> {
    A apply() throws Effects;
}