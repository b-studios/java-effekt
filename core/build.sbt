import effekt.plugin.EffektPlugin.effektDefaultSettings

lazy val effektName = "effekt"
lazy val effektVersion = "0.1.2-SNAPSHOT"

// the Effekt library (incl. Runtime)
// we apply the effekt instrumentation also for the core library
// since Handler and StatefulHandler both need to be instrumented.
lazy val root = project
  .in(file("."))
  .settings(effektDefaultSettings) // AOT settings
  .enablePlugins(effekt.plugin.EffektPlugin)
  .settings(
    moduleName := s"${effektName}-core",
    name := s"${effektName}-core",
    organization := "de.b-studios",
    version := effektVersion,
    scalaVersion := "2.12.4"
  )
