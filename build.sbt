lazy val root = project
  .in(file("."))
  .settings(noPublishSettings)
  .aggregate(instrumentation, sbtplugin)

lazy val opalVersion = "2.0.1"

lazy val effektSettings = Seq(
  scalaVersion := "2.12.4",
  organization := "de.b-studios",
  version := "0.1.2-SNAPSHOT"
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

  moduleName := "effekt-instrumentation",
  name := "effekt-instrumentation",

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
  moduleName := "effekt-sbtplugin",
  name := "effekt-sbtplugin"
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
