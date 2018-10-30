package effekt.instrumentation

import java.io.File
import java.lang.ref.SoftReference
import java.net.URL

import com.typesafe.config.{ Config => OpalConfig }
import org.opalj.ba.toDA
import org.opalj.bc.Assembler
import org.opalj.br.analyses.Project
import org.opalj.br.{ BaseConfig, ClassFile }
import org.opalj.br.reader.{ BytecodeInstructionsCache, Java8FrameworkWithCaching, Java9FrameworkWithInvokedynamicSupportAndCaching, Java9LibraryFramework }
import org.opalj.log.{ ConsoleOPALLogger, GlobalLogContext, LogContext, OPALLogger }

object InstrumentProject {

  def apply(p: Project[URL], cfg: Config): ClassDB =
    p.allProjectClassFiles.flatMap { cf => InstrumentedClass(cf, p, cfg) }.map { instr =>
      val name = instr.thisType.toJava
      (name -> assemble(instr))
    }.toMap

  def apply(projectFolder: String, cfg: Config): ClassDB =
    apply(new File(projectFolder), cfg)

  def apply(projectFolder: File, cfg: Config): ClassDB =
    apply(makeProject(Seq(projectFolder), Nil), cfg)

  def apply(projectFolder: File, libraries: Seq[File], cfg: Config): ClassDB =
    apply(makeProject(Seq(projectFolder), libraries), cfg)

  def apply(projectFolder: Seq[File], libraries: Seq[File], cfg: Config = Config()): ClassDB =
    apply(makeProject(projectFolder, libraries), cfg)

  // TODO use p.parForeachProjectClassFile()
  def classFiles(p: Project[URL], resultAlg: Result, cfg: Config = Config()): Unit =
    p.projectClassFilesWithSources.foreach { case (cf, url) =>
      InstrumentedClass(cf, p, cfg).map { instr =>
        val name  = instr.thisType.simpleName
        resultAlg(url, name, assemble(instr))
      }
    }

  def assemble(cf: ClassFile): Array[Byte] = Assembler(toDA(cf))

  // (source url of original classfile, simpleName, code)
  type Result = (URL, String, Array[Byte]) => Unit

  /**
   * Adapted from https://bitbucket.org/delors/opal/src/1dbf23095ea272b50c9daf51d23921f4b2bec4c1/OPAL/br/src/main/scala/org/opalj/br/analyses/Project.scala?at=develop
   */
  lazy val JavaLibraryClassFileReader: Java9LibraryFramework.type = Java9LibraryFramework

  private val errorLogger = new ConsoleOPALLogger(true, org.opalj.log.Error)

  OPALLogger.updateLogger(GlobalLogContext, errorLogger)

  def JavaClassFileReader(
      theLogContext: LogContext = GlobalLogContext,
      theConfig:     OpalConfig = BaseConfig
  ): Java8FrameworkWithCaching = {
      // The following makes use of early initializers
      class ConfiguredFramework extends {
          override implicit val logContext: LogContext = theLogContext
          override implicit val config: OpalConfig = theConfig
      } with Java8FrameworkWithCaching(cache)
      new ConfiguredFramework
  }

  @volatile private[this] var theCache: SoftReference[BytecodeInstructionsCache] = {
      new SoftReference(new BytecodeInstructionsCache)
  }
  private[this] def cache: BytecodeInstructionsCache = {
      var cache = theCache.get
      if (cache == null) {
          this.synchronized {
              cache = theCache.get
              if (cache == null) {
                  cache = new BytecodeInstructionsCache
                  theCache = new SoftReference(cache)
              }
          }
      }
      cache
  }

  def makeProject(projectFolders: Seq[File], libraries: Seq[File]): Project[URL] = {
    val p = Project(
        JavaClassFileReader().AllClassFiles(projectFolders),
        JavaLibraryClassFileReader.AllClassFiles(libraries),
        libraryClassFilesAreInterfacesOnly = true,
        virtualClassFiles = Traversable.empty
    )

    OPALLogger.updateLogger(p.logContext, errorLogger)

    p
  }
}
