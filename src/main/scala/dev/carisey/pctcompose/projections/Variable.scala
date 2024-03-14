/*
 * Copyright (C) 2023  dev.carisey
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
