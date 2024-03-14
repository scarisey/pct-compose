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

import dev.carisey.pctcompose.projections.Variable.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class VariableTest extends AnyFlatSpec with Matchers {

  behavior of "Variable"

  it should "getValue" in {
    ValueIntegral(10).getValue().safe() shouldEqual Some(10)
    ValueString("foo").getValue().safe() shouldEqual Some("foo")
    ValueBool(true).getValue().safe() shouldEqual Some(true)
    Fields(Map("foo" -> ValueString("bar"))).getValue().value.run shouldEqual (List("Get value for Fields(Map(foo -> ValueString(bar)))"), None)
    Items(List.empty).getValue().value.run shouldEqual (List("Get value for Items(List())"), None)
  }

  it should "get value of an attribute" in {
    List(
      ValueIntegral(10),
      ValueString("foo"),
      ValueBool(true),
      Fields(Map("foo" -> ValueString("bar"))),
      Items(List.empty)
    ).map(_.getAttr("foo").safe()) should contain theSameElementsAs List(None, None, None, Some(ValueString("bar")), None)
  }

  it should "get an array of an object" in {
    List(
      ValueIntegral(10),
      ValueString("foo"),
      ValueBool(true),
      Fields(Map("foo" -> ValueString("bar"))),
      Fields(Map("foo" -> Items(List(ValueIntegral(42))))),
      Items(List.empty)
    ).map(_.getArrayAttr("foo").safe()) should contain theSameElementsAs List(None, None, None, None, Some(Items(List(ValueIntegral(42)))), None)
  }

}
