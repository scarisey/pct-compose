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
    parse("\"foo\"", Parser.string(_)) shouldEqual Success(value = AString(value = "foo"), index = 5)

    parse("aSingleField", Parser.expr(_)) shouldEqual Success(value = AFieldValue(name = "aSingleField"), index = 12)
    parse("anObject.aField", Parser.expr(_)) shouldEqual Success(
      value = FieldPath(name = "anObject", next = AFieldValue(name = "aField")),
      index = 15
    )
    parse("anArray[name=\"foo\"]", Parser.expr(_)) shouldEqual
      Success(
        value = FilterArrayValue(
          name = "anArray",
          attributeFiltered = "name",
          expectedValue = AString(value = "foo")
        ),
        index = 19
      )
    parse("anArray[isRightHanded=true]", Parser.expr(_)) shouldEqual
      Success(
        value = FilterArrayValue(
          name = "anArray",
          attributeFiltered = "isRightHanded",
          expectedValue = ABool(value = true)
        ),
        index = 27
      )
    parse("anArray[age=42]", Parser.expr(_)) shouldEqual
      Success(
        value = FilterArrayValue(
          name = "anArray",
          attributeFiltered = "age",
          expectedValue = ANumber(42)
        ),
        index = 15
      )

    parse("anArray[weight=7.5E1]", Parser.expr(_)) shouldEqual
      Success(
        value = FilterArrayValue(
          name = "anArray",
          attributeFiltered = "weight",
          expectedValue = ANumber(75)
        ),
        index = 21
      )
    parse("anObject.anArray[name=\"foo\"].aField", Parser.expr(_)) shouldEqual
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
