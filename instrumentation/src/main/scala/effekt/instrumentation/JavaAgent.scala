package effekt
package instrumentation

import java.lang.instrument._
import java.security.ProtectionDomain

import InstrumentProject.{ JavaClassFileReader, JavaLibraryClassFileReader }
import org.opalj.br.analyses.Project
import java.net.URL
import java.io._
import java.nio.file.{ Files, Paths }

import scala.collection.mutable.WeakHashMap

object JavaAgent {
  def premain(args: String, instr: Instrumentation): Unit = {
    log("Installing Effekt JavaAgent")

    log(s"arguments: $args")

    try {
      instr.addTransformer(new EffektInstrumentor {
        def cfg = Config()
      })
    } catch {
      case t: Throwable =>
        println(t)
        t.printStackTrace()
    }
  }

  def agentmain(args: String, instr: Instrumentation): Unit =
    premain(args, instr)

  def log(msg: => String): Unit = ()
}

abstract class EffektInstrumentor extends ClassFileTransformer {

  def cfg: Config

  import JavaAgent.log

  type Classpath = String

  val SEP = File.pathSeparatorChar

  private[this] val cache = WeakHashMap.empty[Classpath, Project[URL]]

  // those packages don't use effekt (yet :) ), so we don't need to analyse them for instrumentation
  // TODO make this configurable via java agent parameters
  lazy val excludeList = Seq(
    "com/sun/",
    "java/",
    "javax/",
    "org/apache/",
    "sun/",
    "jsr166e/",
    "com/google/",
    "scala/",
    "org/opalj/",
    "play/",
    "org/joda/",
    "org/scalatest/",
    "org/scalatools/",
    "sbt/io/",
    "sbt/testing/",
    "org/scalactic/",
    "org/scalameter/"
  )

  // to speed up start of OPAL exclude the instrumentation jars
  // and dependencies from analysis
  lazy val excludeJars = Seq(
    "/de.opal-project",
    "/effekt-instrumentation",
    // this is very drastical since everything that is not local will be ignored for now.
    "/.ivy2/cache/"
  )

  lazy val reader = JavaClassFileReader()

  override def transform(loader: ClassLoader, className: String, cls: Class[_], domain: ProtectionDomain, data: Array[Byte]): Array[Byte] = try {

    if (className == null)
      return null

    if (excludeList exists { e => className startsWith e })
      return null

    val (cf, cfTime) = timed { reader.ClassFile(() => new ByteArrayInputStream(data)).head }

    if (InstrumentedClass shouldBeIgnored cf)
      return null

    log(s"instrumenting: $className reading it took ${cfTime}ms")

    // class path is huge. This probably takes ages, even if cached
    val cp = System.getProperty("java.class.path")

    // XXX is there a way to speed up the initial loading of OPAL?
    // this takes about 8s with full dependencies (which include OPAL itself...)
    val baseProject = cache.getOrElseUpdate(cp, {

      val libs = cp.split(SEP).filterNot(f => excludeJars exists { f contains _ }).map(f => new File(f))

      log(s"Including following paths in static analysis: ${libs mkString "\n"}")
      val (p, pTime) = timed { Project(
          Nil, // no project sources yet -- we are going to extend this project
          JavaLibraryClassFileReader.AllClassFiles(libs),
          libraryClassFilesAreInterfacesOnly = true,
          virtualClassFiles = Traversable.empty
      )}
      log(s"Initial project initialization took ${pTime}ms")
      p
    })

    // this takes around 1s for every single class!
    // - is there a faster way to do this in OPAL?
    // - why does OPAL need the URL here?
    // for now we just reuse the project
    // val (p, pTime) = timed { baseProject.extend(List((cf, new URL("file://" + className)))) }
    // log(s"loading classfile into project took ${pTime}ms")

    val (res, resTime) = timed { InstrumentedClass(cf, baseProject, cfg).map { InstrumentProject.assemble } }

    res.map { cf =>
      dumpClassfile(className + ".before.class", data)
      dumpClassfile(className + ".class", cf)
      cf
    }.orNull
  } catch {
    case t: Throwable =>
      println(t)
      t.printStackTrace()
      null
  }

  def dumpClassfile(fileName: String, cf: Array[Byte]) = {
    val f = new File("target/instrumented/" + fileName)
    f.getParentFile.mkdirs()
    val bw = new BufferedOutputStream(new FileOutputStream(f))
    try { bw.write(cf) } finally bw.close()
    log("Dumping classfile " + f.getAbsolutePath)
  }


  def timed[R](block: => R): (R, Long) = {
    val before = System.currentTimeMillis()
    val res = block
    val after = System.currentTimeMillis()
    (res, after - before)
  }
}
