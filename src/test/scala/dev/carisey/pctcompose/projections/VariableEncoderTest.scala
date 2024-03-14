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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import Variable.*
import VariableEncoder.given
class VariableEncoderTest extends AnyFlatSpec with Matchers {
  behavior of "VariableEncoder"

  it should "encode a complex object" in {
    val aVariable = List(
      Parent("foo", List(Child("1", 10), Child("2", 12))),
      Parent("bar", List.empty)
    )

    aVariable.encode shouldEqual Items(
      x = List(
        Fields(
          x = Map(
            "name" -> ValueString(x = "foo"),
            "children" -> Items(
              x = List(
                Fields(x = Map("name" -> ValueString(x = "1"), "age" -> ValueIntegral(x = 10L))),
                Fields(x = Map("name" -> ValueString(x = "2"), "age" -> ValueIntegral(x = 12L)))
              )
            )
          )
        ),
        Fields(x = Map("name" -> ValueString(x = "bar"), "children" -> Items(x = List())))
      )
    )
  }
}

final case class Child(name: String, age: Int)
final case class Parent(name: String, children: List[Child])
