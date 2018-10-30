package effekt

import org.scalatest._

class RunSingle extends FunSpec with Matchers {

  import InstrumentationTests._

  describe("Running a single specified test") {
    runClass("run.state.CountDown")
  }
}