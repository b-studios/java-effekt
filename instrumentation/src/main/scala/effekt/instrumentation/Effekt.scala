package effekt
package instrumentation

import org.opalj.br._
import org.opalj.br.instructions._
import MethodDescriptor._

object Effekt {

  val Type = ObjectType("effekt/Effekt")

  // assumes the frame is on top of the operand stack
  def push = code (
    INVOKESTATIC(Type, false, "push", MethodDescriptor(RefArray(Frame.Type), VoidType))
  )

  def onThrow = code (
    INVOKESTATIC(Type, false, "onThrow", MethodDescriptor(RefArray(ObjectType.Throwable), VoidType))
  )

  def pop = code (
    INVOKESTATIC(Type, false, "pop", NoArgsAndReturnVoid)
  )

  def beforeEffect = code (
    INVOKESTATIC(Type, false, "beforeEffect", NoArgsAndReturnVoid)
  )

  def isEffectful = code (
    INVOKESTATIC(Type, false, "isEffectful", JustReturnsBoolean)
  )

  def isPure = code (
    INVOKESTATIC(Type, false, "isPure", JustReturnsBoolean)
  )

  // the result of the method is currently top on the stack
  def returnWith(retType: Type) =
    if (retType.isVoidType) code(
      INVOKESTATIC(Type, false, "returnVoid", NoArgsAndReturnVoid)
    ) else code (
      if (retType.computationalType.operandSize == 2) DUP2 else DUP,
      INVOKESTATIC(Type, false, "returnWith", MethodDescriptor(toComputational(retType), VoidType))
    )

  def result(typ: Type) =
    if (typ.isVoidType)
      code (INVOKESTATIC(Type, false, "resultVoid", NoArgsAndReturnVoid))
    else
      apply("result", typ.asFieldType)()

  // use like:
  //   stack("get", IntegerType)() for GlobalStack.get(): Int
  def apply(name: String, retType: FieldType)(argTypes: FieldType*) = (retType, toComputational(retType)) match {
    case (t : ReferenceType, ct) =>
      code (INVOKESTATIC(Type, false, name, MethodDescriptor(argTypes.toRefArray, ObjectType.Object)), CHECKCAST(t))
    case (t, ct) =>
      code (INVOKESTATIC(Type, false, name + ct.toJVMTypeName, MethodDescriptor(argTypes.toRefArray, ct)))
  }

  private def toComputational(t: Type): FieldType =
    t.computationalType match {
      case ComputationalTypeDouble => DoubleType
      case ComputationalTypeFloat => FloatType
      case ComputationalTypeInt => IntegerType
      case ComputationalTypeLong => LongType
      case ComputationalTypeReference => ObjectType.Object
      // TODO support ReturnAddress
      case _ => ???
    }
}