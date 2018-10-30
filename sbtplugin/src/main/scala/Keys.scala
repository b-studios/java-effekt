package effekt
package plugin

import sbt._
import java.io.File

object EffektKeys extends BaseEffektKeys

class BaseEffektKeys {
  final val effektClassDirectory = taskKey[File]("The folder containing the classfiles to be instrumented")
  final val effektTargetDirectory = taskKey[File]("The folder the instrumented classfiles should be written to")
  final val effektDependencyClassPath = taskKey[Seq[File]]("Paths to dependency jar files that are not going to be instrumented but necessary for analysis")
  final val effektInstrument = taskKey[Unit]("Perform instrumentation on compiled class files")
}