package effekt.instrumentation

import org.opalj.br.{ Method, Code }
import org.opalj.br.analyses.Project
import java.net.URL

case class MethodContext(
  m: Method,
  p: Project[URL],
  fresh: FreshNames = FreshNames(),
  config: Config
) {
  implicit lazy val body: Code = m.body.get
}