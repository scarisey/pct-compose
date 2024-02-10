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
