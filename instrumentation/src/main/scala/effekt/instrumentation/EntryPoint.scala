package effekt.instrumentation

import org.opalj.br.PC

// the label of the entrypoint AFTER the effectful call at position
// `callPos`.
case class EntryPoint(callPos: PC, index: Int) {
    def label = Symbol(s"EP$index")
}