package dev.carisey.pctcompose.projections

import cats.data.{OptionT, Writer, WriterT}
import cats.implicits.given

enum Variable {
  case ValueIntegral(x: Long) extends Variable
  case ValueString(x: String) extends Variable
  case ValueBool(x: Boolean) extends Variable
  case Fields(x: Map[String, Variable]) extends Variable
  case Items(x: List[Variable]) extends Variable
}

object Variable {
  extension (v: Variable)
    def getArrayAttr(name: String): Eval[Items] = v match
      case Fields(x) => liftOption(s"Get array ${name} for ${v}", x.get(name).collect { case items @ Items(_) => items })
      case _         => liftOption(s"No array found in field ${name} for ${v}", None)

    def getAttr(name: String): Eval[Variable] = liftOption(
      s"Get attribute ${name} for ${v}",
      v match
        case Fields(x) => x.get(name)
        case _         => None
    )

    def getValue(): Eval[String | Boolean | Long] = liftOption(
      s"Get value for ${v}",
      v match
        case ValueIntegral(x) => Some(x)
        case ValueString(x)   => Some(x)
        case ValueBool(x)     => Some(x)
        case Fields(x)        => None
        case Items(x)         => None
    )

  extension (v: Eval[Variable])
    def getArrayAttr(name: String): Eval[Items] = v.flatMap(_.getArrayAttr(name))

    def getAttr(name: String): Eval[Variable] = v.flatMap(_.getAttr(name))

    def getValue(): Eval[String | Boolean | Long] = v.flatMap(_.getValue())

}
