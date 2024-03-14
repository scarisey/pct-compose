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
import fastparse.*
import Variable.*

class ProjectedExpressionTest extends AnyFlatSpec with Matchers {

  "Evaluate projected expression" should "give a primitive value" in {
    import ProjectedExpression.*
    import Variable.*

    evaluate(ABool(true), ValueIntegral(42)).safe() shouldEqual Some(true)
    evaluate(AString("foo"), ValueIntegral(42)).safe() shouldEqual Some("foo")
    evaluate(ANumber(3), ValueIntegral(42)).safe() shouldEqual Some(3)
    evaluate(AFieldValue("foo"), Fields(Map("foo" -> ValueString("bar")))).safe() shouldEqual Some("bar")
    evaluate(FieldPath("foo",AFieldValue("bar")), Fields(Map("foo" -> Fields(Map("bar"->ValueIntegral(42)))))).safe() shouldEqual Some(42)
    val x = evaluate(
      FilterArrayValue("foo", "name", AString("foo2")),
      Fields(
        Map(
          "foo" ->
            Items(
              List(
                Fields(Map("name" -> ValueString("foo1"))),
                Fields(Map("name" -> ValueString("foo2"))),
                Fields(Map("name" -> ValueString("foo3")))
              )
            )
        )
      )
    )
    pprint.pprintln(x)
    x.safe() shouldEqual Some("foo2")
    evaluate(
      FilterArrayPath("foo", "name", AString("foo2"),AFieldValue("age")),
      Fields(
        Map(
          "foo" ->
            Items(
              List(
                Fields(Map("name" -> ValueString("foo1"),"age" -> ValueIntegral(23))),
                Fields(Map("name" -> ValueString("foo2"),"age" -> ValueIntegral(36))),
                Fields(Map("name" -> ValueString("foo3"),"age" -> ValueIntegral(42)))
              )
            )
        )
      )
    ).safe() shouldEqual Some(36)
  }

  "Parsing and evaluating" should "give primitive values" in {
    val variables = Fields(
      Map(
        "foo" ->
          Items(
            List(
              Fields(Map("name" -> ValueString("foo1"), "age" -> ValueIntegral(23))),
              Fields(Map("name" -> ValueString("foo2"), "age" -> ValueIntegral(36))),
              Fields(Map("name" -> ValueString("foo3"), "age" -> ValueIntegral(42)))
            )
          )
      )
    )
    val parsed = parse("""foo[name="foo2"].age""", Parser.expr(_)).get.value
    ProjectedExpression.evaluate(parsed, variables).safe() shouldEqual Some(36)
  }
}
