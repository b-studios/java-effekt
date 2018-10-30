package effekt;

/**
 * TODO add a family of interfaces:
 *   Effectful             () -> void
 *   EffectfulIn<A>        A  -> void
 *   EffectfulOut<B>       () -> B
 *   EffectfulInOut<A, B>  A  -> B
 *
 * Same for CPS and StatefulCPS
 */
public interface Effectful<A, B> {
    B apply(A a) throws Effects;
}