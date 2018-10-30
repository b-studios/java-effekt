package effekt
package instrumentation

case class Config(
  bubbleSemantics: Boolean = false,       // requires BubbleRuntime
  pushInitialEntrypoint: Boolean = false, // requires CPSRuntime
  optimizePureCalls: Boolean = false      // requires CPSLocalRuntime or bubbleSemantics
)