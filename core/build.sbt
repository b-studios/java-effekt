import effekt.plugin.EffektPlugin

lazy val effektName = "effekt"
lazy val effektVersion = "0.1.2-SNAPSHOT"

// the Effekt library (incl. Runtime)
// we apply the effekt instrumentation also for the core library
// since Handler and StatefulHandler both need to be instrumented.
lazy val root = project
  .in(file("."))
  .enablePlugins(EffektPlugin)
  .settings(EffektPlugin.effektDefaultSettings)
  .settings(EffektPlugin.instrumentAfterCompile)
  .settings(
    moduleName := s"${effektName}-core",
    name := s"${effektName}-core",
    organization := "de.b-studios",
    version := effektVersion,
    scalaVersion := "2.12.4"
  )
