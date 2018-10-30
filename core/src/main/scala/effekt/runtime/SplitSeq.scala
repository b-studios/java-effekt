package effekt
package runtime

import effekt.stateful.Stateful

sealed trait SplitSeq {
  def isEmpty: Boolean
  def nonEmpty = !isEmpty
  def head: Frame
  def tail: SplitSeq
  def pop: (Frame, SplitSeq)
  def push(f: Frame): SplitSeq = f :: this // FramesCont(f, Nil, this) // surprisingly this is faster for the state benchmark
  def pushPrompt(p: Any) = PromptCont(p, this)
  def ::(f: Frame): SplitSeq = FramesCont(f, Nil, this)
  def :::(other: Segment): SplitSeq = other prependTo this

  def splitAt(p: Any): (Segment, SplitSeq) = splitAtAux(p, EmptySegment)

  // helper method with aggregator
  def splitAtAux(p: Any, seg: Segment): (Segment, SplitSeq)
}
case object EmptyCont extends SplitSeq {
  final val isEmpty = true
  final def head: Frame = sys error "No head on EmptyCont"
  final def tail: SplitSeq = sys error "No tail on EmptyCont"
  final def pop: (Frame, SplitSeq) = sys error "Can't pop EmptyCont"
  final def splitAtAux(p: Any, seg: Segment): (Segment, SplitSeq) = sys error s"Prompt not found $p in $seg"
}

// frame :: frames ::: rest
case class FramesCont(final val frame: Frame, final val frames: List[Frame], final val rest: SplitSeq) extends SplitSeq {
  final val isEmpty = false
  final def head: Frame = frame
  final def tail: SplitSeq = pop._2
  final def pop: (Frame, SplitSeq) = {
    if (frames.isEmpty)
      (frame, rest)
    else
      (frame, FramesCont(frames.head, frames.tail, rest))
  }
  override final def ::(f: Frame): SplitSeq = FramesCont(f, frame :: frames, rest)
  final def splitAtAux(p: Any, seg: Segment): (Segment, SplitSeq) =
    rest.splitAtAux(p, FramesSegment(frame, frames, seg))
}

case class PromptCont(
  final val p: Any,
  final val rest: SplitSeq
) extends SplitSeq {

  final val isEmpty = rest.isEmpty
  final def head: Frame = rest.head
  final def tail: SplitSeq = rest.tail
  final def pop: (Frame, SplitSeq) = rest.pop

  final def splitAtAux(p2: Any, seg: Segment): (Segment, SplitSeq) = {

    // we always save the current state at the point of capture in the segment
    if (p2.asInstanceOf[AnyRef] eq p.asInstanceOf[AnyRef]) {
      (seg, rest)
    } else {
      val currState = p match {
        case s: Stateful[x] => s.exportState()
        case _ => null
      }
      rest.splitAtAux(p2, PromptSegment(p, currState, seg))
    }
  }
}

// sub continuations / stack segments
// mirrors the stack, and so is in reverse order. allows easy access to the state
// stored in the current prompt
sealed trait Segment {
  def prependTo(stack: SplitSeq): SplitSeq
}
case object EmptySegment extends Segment {
  def prependTo(stack: SplitSeq): SplitSeq = stack
}
case class FramesSegment(final val frame: Frame, final val frames: List[Frame], final val init: Segment) extends Segment {
  def prependTo(stack: SplitSeq): SplitSeq = init prependTo FramesCont(frame, frames, stack)
}
case class PromptSegment(
  final val prompt: Any,
  final val state: Any,
  final val init: Segment) extends Segment {
  def prependTo(stack: SplitSeq): SplitSeq = {
    // restore state as of the time of capture
    prompt match {
      case s: Stateful[Any] => s.importState(state)
      case _ =>
    }
    init prependTo PromptCont(prompt, stack)
  }
}
