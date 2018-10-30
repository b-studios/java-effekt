package effekt.instrumentation

import java.net.URL

import org.opalj.ai._
import org.opalj.ai.domain.l0.TypeCheckingDomain
import org.opalj.ai.domain.l1.{ DefaultDomainWithCFGAndDefUse }
import org.opalj.br._
import org.opalj.br.analyses.Project
import org.opalj.br.instructions._
import org.opalj.br.cfg.CFGFactory

/**
 * Simple method analysis, extracts locals and operands at given PCs
 *
 * Potential optimization that could obviously be implemented using
 * OPALs current support:
 *   - don't store statically known constants (or null)
 *   - don't store aliased references (refId attribute of ReferenceValue)
 *   - don't store aliased values (through reference equality of domain values)
 *
 * However, all of this optimizations will only buy us a little bit of
 * memory usage and maybe a few load and store instructions.
 */
case class MethodAnalysis(ctx: MethodContext) { outer =>

  import ctx._

  lazy val lastLocal = (Set(0) ++ allLocals).max

  // play it safe since it might be Computational Type 2
  // TODO this is strange, since lastLocal should already do that.
  lazy val firstFreeLocal = lastLocal + 2

  // can contain null values, if no type yet for a local
  def localsAt(n: Int) = aiResult.localsArray(n).map(toFieldType)
  def operandsAt(n: Int) = aiResult.operandsArray(n).map(toFieldType)
  def aliveAt(n: Int) = aiResult.liveVariables(n).iterator.toArray.toList // instead of code.liveVariables

  // IMPLEMENTATION DETAILS

  // collects all locals that are ever written or read to (including those
  // 2nd parts of double and longs)
  private lazy val allLocals = body.iterator.collect {
    case StoreLocalVariableInstruction(typ, index) => (typ, index)
    case LoadLocalVariableInstruction(typ, index) => (typ, index)
  }.flatMap {
    case (typ, index) if typ.operandSize == 2 => Set(index, index + 1)
    case (typ, index) => Set(index)
  }

  private lazy val argumentLocals = (0 until m.actualArgumentsCount).toSet

  // some abstract interpretation to know operand stack and locals
  val domain = new DefaultDomainWithCFGAndDefUse(p, m)
  private lazy val aiResult: AIResult {
    val domain: outer.domain.type
  } = BaseAI(m, domain)
  import domain._

  lazy val (predecessorPCs, finalPCs, cfJoins) = body.predecessorPCs(p.classHierarchy)
  lazy val cfg = bbCFG

  // also look at org.opalj.br.FieldType
  // probably better to convert to FieldType!
  private def toFieldType(d: DomainValue): FieldType = d.fieldType

  implicit class DomainValueOps(self: DomainValue) {
    def size: Int = compType.operandSize

    def compType: ComputationalType =
      if (self == null || self == TheIllegalValue)
        null
      else self.computationalType

    def fieldType: FieldType = compType match {
      case ComputationalTypeDouble => DoubleType
      case ComputationalTypeFloat => FloatType
      case ComputationalTypeInt => IntegerType
      case ComputationalTypeLong => LongType
      case ComputationalTypeReference => self.asDomainReferenceValue.valueType.get

      // ??? ReturnAdresses, e.g. effectful finally clauses ???
      case null => null
    }

    def aliases(other: DomainValue) = (self, other) match {
      case (a : ReferenceValue, b : ReferenceValue) => a.refId == b.refId
      case (a : IntegerRange, b : IntegerRange) => a eq b
      case _ => false
    }
  }
}