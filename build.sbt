lazy val root = project
  .in(file("."))
  .settings(noPublishSettings)
  .aggregate(instrumentation, sbtplugin)

lazy val effekt = "effekt"
lazy val effektVersion = "0.1.2-SNAPSHOT"

lazy val opalVersion = "2.0.1"

lazy val effektSettings = Seq(
  scalaVersion := "2.12.4",
  organization := "de.b-studios",
  version := effektVersion
)

// the instrumentation component and java agent
lazy val instrumentation = project
  .in(file("instrumentation"))
  .settings(effektSettings)
  .settings(instrumentationSettings)
  .settings(javaAgentSettings)

// the ahead of time sbt plugin
lazy val sbtplugin = project
  .in(file("sbtplugin"))
  .settings(effektSettings)
  .settings(sbtpluginSettings)
  .dependsOn(instrumentation)


lazy val instrumentationSettings = Seq(

  moduleName := s"${effekt}-instrumentation",
  name := s"${effekt}-instrumentation",

  libraryDependencies ++= Seq(
    "de.opal-project" % "common_2.12" % opalVersion,
    "de.opal-project" % "bytecode-representation_2.12" % opalVersion,
    "de.opal-project" % "bytecode-creator_2.12" % opalVersion,
    "de.opal-project" % "bytecode-assembler_2.12" % opalVersion,
    "de.opal-project" % "abstract-interpretation-framework_2.12" % opalVersion
  )
)

lazy val sbtpluginSettings = Seq(
  sbtPlugin := true,
  moduleName := s"${effekt}-sbtplugin",
  name := s"${effekt}-sbtplugin"
)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val javaAgentSettings = Seq(
  test in assembly := {},
  packageOptions in assembly += javaAgentManifest,
  packageOptions in ThisBuild += javaAgentManifest,
  packageOptions += javaAgentManifest
)

lazy val javaAgentManifest = Package.ManifestAttributes(
  "Premain-Class" -> "effekt.instrumentation.JavaAgent",
  "Agent-Class"   -> "effekt.instrumentation.JavaAgent",
  "Can-Retransform-Classes" -> "true",
  "Can-Redefine-Classes" -> "true"
)
