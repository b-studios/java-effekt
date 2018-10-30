package effekt;

public interface Continuation<A, B> extends Prog<B> {

    B resume(A value) throws Effects;

    default B apply() throws Effects {
        return resume(null);
    }
}