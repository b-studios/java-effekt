package effekt
package plugin

import effekt.instrumentation._
import sbt._
import sbt.io.Path
import sbt.Keys._
import java.io.File
import org.opalj.br.analyses.Project

object EffektPlugin extends AutoPlugin {

  object autoImport extends BaseEffektKeys
  import autoImport._

  def effektDefaultSettings: Seq[Setting[_]] = Seq(
    effektClassDirectory := (classDirectory in Compile).value,

    effektTargetDirectory := effektClassDirectory.value,

    effektDependencyClassPath := (dependencyClasspath in Compile).value.map {
      cp => cp.data
    },

    effektInstrument := {

      val logger = sLog.value

      logger.info("Running instrumentation")


      val input = effektClassDirectory.value
      val output = effektTargetDirectory.value
      val dependencies = effektDependencyClassPath.value

      val p = InstrumentProject.makeProject(Seq(input), dependencies)

      InstrumentProject.classFiles(p, { case (url, name, code) =>
        val classFile = targetClassFile(url, name)
        logger.info(s"Generated code for ${name}")
        IO.write(classFile, code)
      })

      logger.info("Completed instrumentation")

      /**
       * Computes the new classfile path in `output` from the old
       * full path `source` and the new (unqualified) classname `simpleName`.
       */
      def targetClassFile(source: java.net.URL, simpleName: String): File =
        new File(basepathInTarget(source), s"$simpleName.class")

      /**
       * /some/input/foo/bar.txt
       *   ->
       * /other/output/foo
       */
      def basepathInTarget(source: java.net.URL): File = {
        val sourceFile = IO.asFile(source)
        val relative = IO.relativizeFile(input, sourceFile)
          .getOrElse(sys error s"can't find relative paths to ${sourceFile}")

        output / relative.toPath.getParent.toString
      }
    }
  )

}