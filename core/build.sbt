import effekt.plugin.EffektPlugin.effektDefaultSettings

// the Effekt library (incl. Runtime)
// we apply the effekt instrumentation also for the core library
// since Handler and StatefulHandler both need to be instrumented.
lazy val root = project
  .in(file("."))
  .settings(effektDefaultSettings) // AOT settings
  .enablePlugins(effekt.plugin.EffektPlugin)
  .settings(effektSettings)
  .settings(
    moduleName := "effekt-core",
    name := "effekt-core"
  )

lazy val effektSettings = Seq(
  scalaVersion := "2.12.4",
  organization := "de.b-studios",
  version := "0.1.2-SNAPSHOT"
)