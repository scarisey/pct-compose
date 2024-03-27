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

import dev.carisey.pctcompose.projections.Parser
import dev.carisey.pctcompose.projections.ProjectedExpression.*
import fastparse.*
import fastparse.Parsed.Success
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ParserTest extends AnyFlatSpec with Matchers {
  behavior of "parser"

  it should "parse a simple expressions" in {
    parse("\"foo\"", Parser.string(using _)) shouldEqual Success(value = AString(value = "foo"), index = 5)

    parse("aSingleField", Parser.expr(using _)) shouldEqual Success(value = AFieldValue(name = "aSingleField"), index = 12)
    parse("anObject.aField", Parser.expr(using _)) shouldEqual Success(
      value = FieldPath(name = "anObject", next = AFieldValue(name = "aField")),
      index = 15
    )
    parse("anArray[name=\"foo\"]", Parser.expr(using _)) shouldEqual
      Success(
        value = FilterArrayValue(
          name = "anArray",
          attributeFiltered = "name",
          expectedValue = AString(value = "foo")
        ),
        index = 19
      )
    parse("anArray[isRightHanded=true]", Parser.expr(using _)) shouldEqual
      Success(
        value = FilterArrayValue(
          name = "anArray",
          attributeFiltered = "isRightHanded",
          expectedValue = ABool(value = true)
        ),
        index = 27
      )
    parse("anArray[age=42]", Parser.expr(using _)) shouldEqual
      Success(
        value = FilterArrayValue(
          name = "anArray",
          attributeFiltered = "age",
          expectedValue = ANumber(42)
        ),
        index = 15
      )

    parse("anArray[weight=7.5E1]", Parser.expr(using _)) shouldEqual
      Success(
        value = FilterArrayValue(
          name = "anArray",
          attributeFiltered = "weight",
          expectedValue = ANumber(75)
        ),
        index = 21
      )
    parse("anObject.anArray[name=\"foo\"].aField", Parser.expr(using _)) shouldEqual
      Success(
        value = FieldPath(
          name = "anObject",
          next = FilterArrayPath(
            name = "anArray",
            attributeFiltered = "name",
            expectedValue = AString(value = "foo"),
            next = AFieldValue(name = "aField")
          )
        ),
        index = 35
      )

  }
}
