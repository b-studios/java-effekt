package effekt
package instrumentation

import annotationTypes.{ AlreadyInstrumented, DontInstrument }

import org.opalj.br._
import org.opalj.br.analyses.Project
import org.opalj.ba._
import org.opalj.collection.immutable.UShortPair

import java.net.URL

object InstrumentedClass {

  // TODO factor this information to a better place
  val classfileVersion = UShortPair(0, 52)

  def apply(cf: ClassFile, p: Project[URL], cfg: Config): Option[ClassFile] = {

    if (shouldBeIgnored(cf))
      return None

    // symgen for new fun classes
    val fresh = FreshNames()

    def toTemplate[T](m: METHOD[T]): MethodTemplate =
      m.result(classfileVersion, cf.thisType)._1

    var instrumented = false

    val (ms, eps) = cf.methods.map { m =>
      val ctx   = MethodContext(m, p, fresh, cfg)
      val instr = InstrumentMethod(ctx)
      if (!instr.needsInstrumentation) {
        (List(m.copy()), Nil)
      } else {
        instrumented = true
        val InstrumentMethod.Result(stub, entrypoints) = instr.instrument
        (List(stub), entrypoints.map(toTemplate))
      }
    }.unzip match {
      case (ms, eps) => (ms.flatten, eps.flatten)
    }

    if (!instrumented)
      return None

    Some(cf.copy(
      version = classfileVersion,
      methods = (ms ++ eps).toRefArray,
      interfaceTypes = cf.interfaceTypes ++ Seq(Frame.Type),
      attributes = cf.attributes :+ alreadyInstrumented
    ))
  }

  def shouldBeIgnored(cf: ClassFile): Boolean = {
    val annos = cf.annotations.map { _.annotationType }
    annos.contains(AlreadyInstrumented.Type) || annos.contains(DontInstrument.Type)
  }

  lazy val alreadyInstrumented: AnnotationTable =
    RuntimeVisibleAnnotationTable(RefArray(Annotation(AlreadyInstrumented.Type)))
}