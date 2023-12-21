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
