package effekt.stateful;

public interface Stateful<S> {
    S exportState();
    void importState(S state);
}