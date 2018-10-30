package effekt
package instrumentation

import annotationTypes._

import org.opalj.Result
import org.opalj.br._
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.instructions._

/**
 * Instrumentation relevant analysis of a method
 *
 * In contrast to MethodAnalysis, this module focuses on
 * instrumentation related information, such as identifying
 * entrypoints.
 */
trait InstrumentationAnalysis {

  val ctx: MethodContext
  val analyser: MethodAnalysis

  import ctx._
  import analyser._

  // The local variables that can be used to store temporary results
  lazy val firstTempLocal = analyser.firstFreeLocal

  lazy val epPCs = body.flatMap {
    case PCAndInstruction(pc, is : MethodInvocationInstruction) if isEffectfulCall(is) => Some(pc)
    case _ => None
  }

  lazy val entrypoints = epPCs.filterNot(isTailCall).zipWithIndex.map { case (pc, idx) =>
    EntryPoint(pc, idx + 1)
  }

  lazy val needsInstrumentation: Boolean = {
    val res =
      //
      // not abstract or native code
      m.body.isDefined &&
      //
      // not flagged as DontInstrument
      !m.annotations
        .map {_.annotationType}
        .contains(DontInstrument.Type) &&
      //
      // not a generated bridge method (TODO maybe we don't need this due to other optimizations)
      // !m.hasFlags(ACC_BRIDGE.mask) &&
      //
      // marked as throwing effects
      (if (config.optimizePureCalls)
        throwsEffects(m)
       else
        selfOrSuperThrowsEffects(m)) &&
      //
      // at least one EP needs instrumentation
      (!config.optimizePureCalls || epPCs.exists(ep => !isTailCall(ep)))

//    println(s"${m.name} needs instrumentation: $res")
    res
  }

  def isTailCall(ep: PC): Boolean =
    nextReturn(next(ep)).isDefined

  def tailcalls: List[PC] = epPCs.filter(isTailCall).toList

  // PCs of return instructions, corresponding to tail effect calls
  def tailEffectCallReturns: List[PC] =
    epPCs.flatMap(ep => nextReturn(next(ep))).toList

  // returns PC of next return, if between the return PC and the given PC
  // are only noops
  // used to collect tail-effect-calls
  def nextReturn(pc: PC): Option[PC] = {
    val instr = instruction(pc)
    if (instr.isReturnInstruction)
      Some(pc)
    else if (instr.isCheckcast)
      nextReturn(next(pc))
    else
      None
  }

  def next(pc: PC): PC = body.pcOfNextInstruction(pc)
  def instruction(pc: PC): Instruction = body.instructions(pc)


  implicit class BasicBlockOps(self: BasicBlock) {
    // invariant: if a basic block is an entry point then the last
    // instruction is the effectful call.
    def isEntryPoint: Boolean = self.entryPointsIn.nonEmpty

    def entryPointsIn: List[PC] = {
      var pcs = List.empty[PC]
      self foreach { i => if (epPCs contains i) pcs = i :: pcs }
      pcs
    }

    def isStart: Boolean = self.startPC == 0
  }

  def isEffectfulCall(i: MethodInvocationInstruction): Boolean = {

    val callee = getCallee(i)

    // XXX JDK is missing in the classpath, hence the special case
//    if (callee.isEmpty && !i.declaringClass.toJava.startsWith("java.")) {
//      val name = i.declaringClass.toJava + "." + i.name
//      sys error s"Can't determine callee for '$name', maybe the instrumentation analysis doesn't have all necessary class files?"
//    }

    callee.exists(selfOrSuperThrowsEffects)
  }

  def throwsEffects(m: Method) =
    m.exceptionTable.toList
       .flatMap {_.exceptions}
       .contains {Effects.Type}

  def selfOrSuperThrowsEffects(m: Method) =
    throwsEffects(m) || superMethods(m).exists(throwsEffects)

  lazy val cf: ClassFile = m.classFile

  // TODO should be done, once per class, not per method!
  lazy val superClasses: Seq[ClassFile] = superClasses(cf)

  private def superClasses(cf: ClassFile): Seq[ClassFile] =
     for {
      cls <- cf.interfaceTypes.toSeq ++ cf.superclassType.toSeq
      cf  <- p.classFile(cls)
    } yield cf

  // methods potentially overridden by given method
  private lazy val superMethods: Seq[Method] = superMethods(m)

  private def superMethods(m: Method): Seq[Method] =
    superClasses(m.classFile).flatMap { c => c.findMethod(m.name, m.descriptor) }

  // also see: https://bitbucket.org/snippets/delors/oegdqG/opal-110-resolving-call-targets-of
  private def getCallee(i: MethodInvocationInstruction): List[Method] = i match {
    case i @ INVOKEVIRTUAL(dc, name, descr) => {
       p.instanceCall(m.classFile.thisType, dc, name, descr).toList ++
       p.resolveMethodReference(i).toList
    }

    case i @ INVOKESTATIC(dc, isInterface, name, descr) => {
      p.staticCall(i).toList
    }

    // TODO what about constructor calls (the following is for super-calls)
    case INVOKESPECIAL(dc, isInterface, name, descr) =>
      p.specialCall(dc, m.classFile.thisType, isInterface, name, descr).toList

    case i @ INVOKEINTERFACE(dc, name, descr) => {
      p.interfaceCall(dc, name, descr).toList ++
      // since we appearently can't find methods after erasure
      p.resolveInterfaceMethodReference(i)
    }

    case _ => sys error s"$i is not a function call"
  }

  private implicit def result2Option[A](r: Result[A]): Option[A] =
    if (r.isEmpty) None else Some(r.value)
}