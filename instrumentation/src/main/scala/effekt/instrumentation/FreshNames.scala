package effekt.instrumentation

import scala.collection.mutable

case class FreshNames() {
  val usage = mutable.HashMap.empty[String, Int]

  def apply(name: String): String = {
    val used = usage.getOrElse(name, 0)
    usage.put(name, used + 1)
    s"${name}${used + 1}"
  }
}

case class FreshIds() {
  var last = 0;

  def apply(): Int = {
    val res = last
    last += 1
    res
  }
}