package effekt.instrumentation

import java.net.URL

import effekt._
import org.opalj.ba._
import org.opalj.br._
import org.opalj.br.analyses.Project
import org.opalj.br.instructions._

case class InstrumentMethod(ctx: MethodContext) extends InstrumentationAnalysis {

  import ctx._

  // instantiate other components
  lazy val analyser = MethodAnalysis(ctx)
  lazy val eps      = InstrumentEntryPoint(ctx, analyser)
  import eps._

  lazy val className = m.classFile.thisType.fqn

  lazy val instrument = {
    if (config.pushInitialEntrypoint) {
      InstrumentMethod.Result(
        pushingInitialEntryMethodStub,
        generateInitialEntrypointMethod +: generateEntrypointMethods)
    } else {
      InstrumentMethod.Result(
        initialEntryWithModifiedReturns,
        generateEntrypointMethods)
    }
  }

  def instrumentState: (EntryPoint, Int) => LocalState =
    if (config.bubbleSemantics) BubbleSemanticsState
    else if (config.optimizePureCalls) CPSInPlaceState
    else CPSState

  lazy val entrypointsState = entrypoints.map { ep => (ep, instrumentState(ep, firstTempLocal)) }

  // does not push a frame, but IS the initial entrypoint while maintaining the
  // original signature
  lazy val initialEntryWithModifiedReturns: MethodTemplate = {
    val lcode = generateInstrumentedBody(true)
    val (stub, _) = lcode.result(InstrumentedClass.classfileVersion, m)
    m.copy(body = Some(stub))
  }

  // just a stub pushing the initial entrypoint and immediately returning
  lazy val pushingInitialEntryMethodStub = {

    // load arguments, save in closure, push fun, throw
    val stubCode = EntryState.loadLocals ++ EntryState.pushFun ++ returnDefaultValue(m.descriptor.returnType)

    // should also be ok to use the original classfile version here, right?
    val (stub, _) = CODE(stubCode: _*)(InstrumentedClass.classfileVersion, m)
    m.copy(body = Some(stub))
  }

  lazy val generateInitialEntrypointMethod = {
    val lcode = generateInstrumentedBody(false)
    val flags = PRIVATE.STATIC.SYNTHETIC
    val descriptor = MethodDescriptor(EntryState.types, VoidType)

    // at the beginning of the initial entrypoint we don't need to do anything with
    // our state.
    //
    // there were no operands and the locals are already in place.
    METHOD(flags, EntryState.name, descriptor.toJVMDescriptor, lcode.result)
  }

  lazy val generateEntrypointMethods = for ((ep, state) <- entrypointsState) yield {
    val lcode = generateInstrumentedBody(false)
    val flags = PRIVATE.STATIC.SYNTHETIC
    val descriptor = MethodDescriptor(state.types, VoidType)

    // At the beginning of each individual entrypoint method add:
    // (1) restoration code
    // (2) unconditional jump to entrypoint
    //
    // OPAL's dead code elimination will reduce the method body to a minimum
    lcode.insert(0, InsertionPosition.At, code(GOTO(ep.label)))

    METHOD(flags, state.name, descriptor.toJVMDescriptor, lcode.result)
  }

  def generateInstrumentedBody(returnValue: Boolean): LabeledCode = {
    assert(m.body.isDefined, "Can only instrument concrete methods.")

    val lcode = LabeledCode(body)

    // add code before returns
    // add cast before reference return, just to make sure bytecode verification works
    def ret = (returnValue, m.returnType) match {
      case (true, t: ReferenceType) => code (CHECKCAST(t), ReturnInstruction(t))
      case (true, t)  => code (ReturnInstruction(t))
      case (false, _) => code (RETURN)
    }

    // for every return instruction in the *original* bytecode:
    for (PCAndInstruction(pc, ReturnInstruction(r)) <- body) {
      // for effectful tail calls:
      // - do not call returnWith (since the result is a dummy value)
      // - just replace with return
      //
      // this causes a problem if the effecful call turns out to be pure
      // and we optimize for pure calls. In that case the result wont be
      // stored...
      if (tailEffectCallReturns contains pc) {
        lcode.replace(pc, ret)
      } else {
        lcode.replace(pc, Effekt.returnWith(m.returnType) ++ ret)
      }
    }

    // mark entry points
    for ((ep, state) <- entrypointsState) {
      lcode.insert(ep.callPos, InsertionPosition.Before, state.beforeCall)
      lcode.insert(ep.callPos, InsertionPosition.After, state.afterCall(returnValue))
    }

    // deal with tail calls
    for (pc <- tailcalls) {
      val tailCallEP = EntryPoint(pc, -1)
      val instr = instrumentState(tailCallEP, firstTempLocal)

      lcode.insert(pc, InsertionPosition.Before, instr.beforeTailCall)
      lcode.insert(pc, InsertionPosition.After, instr.afterTailCall(returnValue))
    }

    lcode
  }

}
object InstrumentMethod {

  type BA = (Map[org.opalj.br.PC, AnyRef], List[String])

  case class Result(original: MethodTemplate, entrypoints: IndexedSeq[METHOD[BA]])
}