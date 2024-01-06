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

package dev.carisey.pctcompose

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class OsCommandTest extends AnyFlatSpec with Matchers {

  "OsCommand.map" should "preserve composition" in {
    val f: List[String] => List[String] = xs => List(s"${xs.mkString} bar")
    val g: List[String] => List[String] = xs => List(s"${xs.mkString} baz")
    OsCommand.execute(OsCommand("echo", "foo").map(g.compose(f))) shouldEqual OsCommand.execute(
      OsCommand("echo", "foo").map(f).map(g)
    )
  }

  behavior of "OsCommand.execute"
  it should "return a list of String" in {
    OsCommand.execute(OsCommand("echo", "Hello world!\nI'm a developer.")) shouldEqual List("Hello world!", "I'm a developer.")
  }

  it should "return a composition of each command" in {
    OsCommand.execute(
      List(
        OsCommand("printf", "HEADERS\nfoo fuu\nbar baz\n").andPrint(),
        OsCommand("cut", "-d", " ", "-f1").andPrint(),
        OsCommand("sed", "s/o/a/g").andPrint(),
        OsCommand("tail", "-n", "+2").andPrint()
      )
    ) shouldEqual List("faa", "bar")
  }

  "OsCommand.forEach" should "return the sequence of commands results" in {
    OsCommand.forEach(
      List(
        OsCommand("echo", "foo"),
        OsCommand("echo", "bar")
      )
    ) shouldEqual List("foo", "bar")
  }
}
