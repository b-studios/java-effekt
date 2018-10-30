package effekt

import java.io.{ ByteArrayInputStream, InputStream }

import org.opalj.ba.CodeElement
import org.opalj.br._
import org.opalj.br.instructions._
import org.opalj.da.ClassFileReader

package object instrumentation {

  type ClassDB = Map[String, Array[Byte]]

  object annotationTypes {
    object AlreadyInstrumented {
      val Type = ObjectType("effekt/instrumentation/annotations/AlreadyInstrumented")
    }
    object DontInstrument {
      val Type = ObjectType("effekt/instrumentation/annotations/DontInstrument")
    }
  }

  object Effects {
    val Type = ObjectType("effekt/Effects")

    def throwReturn = code(GETSTATIC(Type, "RETURN", Type), ATHROW)
  }

  // UTILS
  def debug(msg: String) = println(msg)

  def code(els: CodeElement[AnyRef]*) = els

  def dump(input: => InputStream, name: String): Unit =
    org.opalj.io.writeAndOpen(
      ClassFileReader.ClassFile(() => input).head.toXHTML(None),
      name, ".html")

  def dump(bytecode: Array[Byte], name: String): Unit =
    dump(new ByteArrayInputStream(bytecode), name)

  // load and return a default value of given type
  def returnDefaultValue(typ: Type) = typ match {
    case VoidType =>
      code(RETURN)
    case ft: FieldType =>
      code(LoadConstantInstruction.defaultValue(ft), ReturnInstruction(ft))
  }

  import org.opalj.collection.immutable.{ RefArray => RA }
  // re-export
  type RefArray[T] = RA[T]
  object RefArray {
    def apply[T <: AnyRef](vs: T*): RefArray[T] = RA.mapFrom(vs)(identity)
    def empty = RA.empty
  }

  implicit class SeqOps[T <: AnyRef](seq: Seq[T]) {
    // converts between a scala Seq and an OPAL RefArray
    def toRefArray = RA.mapFrom(seq)(identity)
  }
}