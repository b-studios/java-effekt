package effekt

import org.scalatest._

import sbt.io._
import sbt.io.syntax._
import java.io.{ ByteArrayOutputStream, File, PrintStream }

class InstrumentationTests extends FunSpec with Matchers {
  import InstrumentationTests._

  runCheckFiles()

  def runCheckFiles(exclude: List[String] = Nil) = {

    describe(s"All test files in folder 'run'") {
      for (check <- checkFiles.get;
        if exclude forall { ex => !(check.getCanonicalPath contains ex) }) {

        val relative = IO.relativizeFile(srcFolder, check)
          .getOrElse(sys error s"can't find relative paths to ${check}")

        val className = relative.toString.replace("/", ".").stripSuffix(".check")

        it(className) {
          val result = runClass(className)
          val expected = IO.read(check)
          result shouldBe expected
        }
      }
    }
  }
}
object InstrumentationTests {

  val srcFolder = BuildInfo.test_javaSource
  val debugFolder = BuildInfo.target / "instrumented"
  val checkFiles = (srcFolder / "run") ** "*.check"

  def runClass(name: String): String = {
    val runnableClass = this.getClass.getClassLoader.loadClass(name)

    val oldOut = System.out
    val mockedOut = new ByteArrayOutputStream()
    System.setOut(new PrintStream(mockedOut))

    try {
      runnableClass.newInstance.asInstanceOf[Runnable].run()
    } finally System.setOut(oldOut)

    mockedOut.flush()
    new String(mockedOut.toByteArray)
  }
}
