package effekt
package instrumentation

import org.opalj.br._
import org.opalj.br.instructions._
import org.opalj.ba._
import org.opalj.collection.mutable.Locals

/**
 * Manages state around entry points
 */
case class InstrumentEntryPoint(
    ctx: MethodContext,
    analyser: MethodAnalysis) {

  import ctx._

  // First store all operands, then all locals
  trait State {

    // name of the entry point
    lazy val name: String = fresh(s"${m.name}$$entrypoint$$")

    // the operand stack (in reverse order, bottom first)
    def operands: List[FieldType]
    def locals: Locals[FieldType]
    def alive: List[Int]

    // types of all operands and locals that need to be stored
    def types: RefArray[FieldType] = (operands ++ alive.map(locals.apply)).toRefArray

    def debug = println(s"Operands: $operands\nLocals: $locals\nAlive: $alive\nTypes: $types")

    // first index in instrumented method that is used by an closure argument
    lazy val firstArgIndex = 0 // if (m.isStatic) 0 else 1

    // now saving locals is just pushing them on the operand stack and
    // invoking closure creation
    // the operands are already on the operand stack
    def saveState = alive.flatMap { local =>
      code (LoadLocalVariableInstruction(locals(local), local))
    }

    def restoreState = restoreOperands ++ loadLocals ++ restoreLocals

    // compute indices that will be used to store the current state
    lazy val (operandIndices, firstLocal) = indices(operands.toList, firstArgIndex)
    lazy val (localIndices, _) = indices(alive.map(locals.apply), firstLocal)

    def restoreOperands = (operands zip operandIndices).flatMap { case (operand, index) =>
      code (LoadLocalVariableInstruction(operand, index))
    }

    def loadLocals = (alive zip localIndices).flatMap { case (local, index) =>
      code (LoadLocalVariableInstruction(locals(local), index))
    }

    def restoreLocals = alive.reverse.flatMap { local =>
      code (StoreLocalVariableInstruction(locals(local), local))
    }

    // helper function that computes indices based on the size (1 or 2 registers) of
    // the given fieldtypes.
    def indices(ts: List[FieldType], startFrom: Int = 0): (List[Int], Int) = {
      val (is, next) = ts.foldLeft[(List[Int], Int)]((Nil, startFrom)) {
        case ((rest, next), el) => (next :: rest, next + el.operandSize)
      }
      (is.reverse, next)
    }

    // push
    def pushFun = Frame.createClosure(m.classFile.thisType, name, types) ++ Effekt.push
  }

  // Reads the locals from the method descriptor.
  // Operand stack is empty
  object EntryState extends State {

    val parameters = m.descriptor.parameterTypes
    def operands: List[FieldType] = Nil

    val locals: Locals[FieldType] =
      if (m.isStatic)
        Locals(parameters.toIndexedSeq)
      else
        Locals((m.classFile.thisType +: parameters).toIndexedSeq)

    def alive: List[Int] =
      if (m.isStatic)
        (0 until m.descriptor.parametersCount).toList
      else // now we also need to store `this` in the closure again
        (0 to m.descriptor.parametersCount).toList
  }

  trait LocalState extends State {

    // information needed for all entrypoint related utilities
    val ep: EntryPoint
    val firstTempLocal: Int

    // provided methods:
    def beforeCall: Seq[CodeElement[AnyRef]]
    def afterCall(returnValue: Boolean): Seq[CodeElement[AnyRef]]

    def beforeTailCall: Seq[CodeElement[AnyRef]]
    def afterTailCall(returnValue: Boolean): Seq[CodeElement[AnyRef]]


    // === Called Effectful Function Information ===
    val callPos = ep.callPos
    lazy val effectOp  = body.instructions(callPos).asInvocationInstruction
    lazy val effectRet = effectOp.methodDescriptor.returnType
    lazy val isVoid = effectRet.isVoidType
    lazy val nextPos = body.pcOfNextInstruction(callPos)


    // === Locals and Operands Information ===

    // Since we get operand stack information AFTER the call, we need
    // to ignore the result on top of the stack, which is NOT on top
    // of the stack at point of storing.
    //
    // we also reverse the operands since they will be stored in the closure
    // bottom to top.
    lazy val operandsWithResult = analyser.operandsAt(nextPos)
    lazy val operands = (if (isVoid) operandsWithResult else operandsWithResult.tail).reverse.toList
    lazy val locals = analyser.localsAt(nextPos)
    lazy val alive  = analyser.aliveAt(nextPos)
    lazy val callOperands: List[FieldType] = {
      val params = effectOp.methodDescriptor.parameterTypes.toList
      if (effectOp.isInstanceMethod) {
        List(ObjectType.Object) ++ params
      } else {
        params
      }
    }

    // === Operand indexes into temporary registers ===
    type OperandMap = List[(FieldType, Int)]
    lazy val (callOpIndices, firstOperandIndex) = indices(callOperands, firstTempLocal)
    lazy val (restOpIndices, tmpResultIndex)    = indices(operands, firstOperandIndex)

    lazy val callOperandsMap: OperandMap = callOperands zip callOpIndices
    lazy val restOperandsMap: OperandMap = operands zip restOpIndices
    lazy val resultMap: OperandMap       = if (isVoid) Nil else List((effectRet.asFieldType, tmpResultIndex))


    // === Result Handling ===
    lazy val ignoreResult =
      if (effectRet.isVoidType) code ()
      else if (effectRet.computationalType.operandSize == 2) code (POP2)
      else code (POP)


    // === Exception Handling ===

    // this is only needed since we don't explicitly push the initial entrypoint
    def beginHandling = code(TRY(Symbol(s"EH${name}")))
    def endHandling(returnValue: Boolean) =
      code(TRYEND(Symbol(s"EH${name}")), CATCH(Symbol(s"EH${name}"), -1, Some(ObjectType.Throwable))) ++
      Effekt.onThrow ++ maybeReturnDefault(returnValue)


    // === Save and Restore Call Operands ===
    def save(m: OperandMap) = m.reverse.flatMap {
      case (op, idx) => saveAt(op, idx)
    }
    def restore(m: OperandMap) = m.flatMap {
      case (op, idx) => restoreFrom(op, idx)
    }

    def saveCallOperands = save(callOperandsMap)
    def restoreCallOperands = restore(callOperandsMap)

    def saveRestOperands = save(restOperandsMap)
    def restoreRestOperands = restore(restOperandsMap)

    def saveResult = save(resultMap)
    def restoreResult = restore(resultMap)

    // index relative to stack index
    def saveAt(t: FieldType, index: Int) =
      code(StoreLocalVariableInstruction(t, index))

    def restoreFrom(t: FieldType, index: Int) =
      code(LoadLocalVariableInstruction(t, index))


    def maybeReturn(returnValue: Boolean) =
      if (returnValue) code (ReturnInstruction(m.returnType)) else code (RETURN)

    def maybeReturnDefault(returnValue: Boolean) =
      if (returnValue) returnDefaultValue(m.returnType) else code (RETURN)

  }

  case class CPSState(ep: EntryPoint, firstTempLocal: Int) extends LocalState {

    def beforeCall =
      saveCallOperands ++
      saveState ++
      pushFun ++
      restoreCallOperands ++
      beginHandling

    // now that we don't use exceptions anymore (but default values) the
    // previous call NEEDS to be effectful. We just return.
    def afterCall(returnValue: Boolean) =
        maybeReturnDefault(returnValue) ++
        endHandling(returnValue) ++
      code (ep.label) ++
        restoreState ++
        Effekt.result(effectRet)

    def beforeTailCall = code()
    def afterTailCall(returnValue: Boolean) = code()
  }

  // resumes in place
  case class CPSInPlaceState(ep: EntryPoint, firstTempLocal: Int) extends LocalState {

    def beforeCall =
      // (1) save the full operand stack
      saveCallOperands ++
      saveRestOperands ++
      // (2) only restore the part that should be closed over and save it in the closure
      restoreRestOperands ++
      saveState ++
      pushFun ++
      // (3) restore the full operand stack to allow in-place resumption
      restoreRestOperands ++
      restoreCallOperands ++
      beginHandling

    def afterCall(returnValue: Boolean) =
        Effekt.isEffectful ++
        code (IFEQ(popLabel)) ++
        // (4) was effectful, so return to trampoline
        maybeReturnDefault(returnValue) ++
        endHandling(returnValue) ++
      code (popLabel) ++
        // (5) remove stack frame that has been pushed, but is not needed
        Effekt.pop ++
        code (GOTO(resumeLabel)) ++
      code (ep.label) ++
        restoreState ++
        Effekt.result(effectRet) ++
      code (resumeLabel)

    def beforeTailCall = Effekt.beforeEffect
    def afterTailCall(returnValue: Boolean) =
      if (returnValue) code() else
        Effekt.isPure ++
          code (IFEQ(exit)) ++
          Effekt.returnWith(effectRet) ++
        code (exit) ++
          code (RETURN)

    lazy val popLabel = Symbol(ep.label.name ++ "Pop")
    lazy val resumeLabel = Symbol(ep.label.name ++ "Resume")
    lazy val exit = Symbol(fresh.apply("exit"))
  }

  case class BubbleSemanticsState(ep: EntryPoint, firstTempLocal: Int) extends LocalState {

    def beforeCall = Effekt.beforeEffect

    // now that we don't use exceptions anymore (but default values) the
    // previous call NEEDS to be effectful. We just return.
    def afterCall(returnValue: Boolean) =
        Effekt.isEffectful ++
        code (IFEQ(resumeLabel)) ++
        ignoreResult ++
        saveState ++
        pushFun ++
        maybeReturnDefault(returnValue) ++
      code (ep.label) ++
        restoreState ++
        Effekt.result(effectRet) ++
      code (resumeLabel)

    def beforeTailCall = Effekt.beforeEffect

    def afterTailCall(returnValue: Boolean) =
      if (returnValue) code() else
        Effekt.isPure ++
          code (IFEQ(exit)) ++
          Effekt.returnWith(effectRet) ++
        code (exit) ++
          code (RETURN)

    lazy val resumeLabel = Symbol(ep.label.name ++ "Resume")
    lazy val exit = Symbol(fresh.apply("exit"))
  }
}