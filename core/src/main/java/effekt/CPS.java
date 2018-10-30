package effekt;

public interface CPS<A, B> {
    B apply(Continuation<A, B> k) throws Effects;
}