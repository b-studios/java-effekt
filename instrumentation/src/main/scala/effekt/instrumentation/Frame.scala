package effekt
package instrumentation

import org.opalj.br._
import org.opalj.br.instructions._
import MethodDescriptor._

object Frame {

  val QualifiedName = "effekt/runtime/Frame"
  val Type = ObjectType(QualifiedName)

  def createClosure(implClass: ObjectType, implName: String, closureArgs: RefArray[FieldType]) = {

    // for now we ALWAYS generate static methods and use static method handles
    // since `this` needs to be passed as argument anyways.
    val methodHandle =
      InvokeStaticMethodHandle(
        implClass,

        // TODO figure out how to determine this info
        false, // isInterface:      Boolean,
        implName,
        // closure arguments -> standard arguments -> return type
        MethodDescriptor(closureArgs, VoidType)
      )

    val methodType = NoArgsAndReturnVoid

    val bootstrap = BootstrapMethod(
      metafactoryHandle,
      RefArray(methodType, methodHandle, methodType))

    val closureType = MethodDescriptor(closureArgs, Type)

    code(
      DEFAULT_INVOKEDYNAMIC(bootstrap, "enter", closureType)
    )
  }

  // Method Handle for a metafactory call
  lazy val metafactoryHandle = {
    import ObjectType._
    InvokeStaticMethodHandle(
      LambdaMetafactory,
      false,
      "metafactory",
      MethodDescriptor(RefArray(MethodHandles$Lookup, String, MethodType, MethodType, MethodHandle, MethodType), CallSite)
    )
  }
}