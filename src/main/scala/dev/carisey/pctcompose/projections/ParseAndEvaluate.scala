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

import cats.data.{OptionT, Writer}
import cats.implicits.given
import dev.carisey.pctcompose.Projected

type Log[T] = Writer[List[String], T]
type Eval[T] = OptionT[Log, T]
def liftOption[T](log: String, t: Option[T]): Eval[T] = OptionT(Writer(List(log), t))

extension [T](v: Eval[T])
  def safe(): Option[T] = v.value.run._2
extension [T](t:T)
  def lift(log:String):Eval[T] = OptionT(Writer(List(log),Some(t)))
object ParseAndEvaluate {
  val replaceProjection = """\$\{([^\$\{\}]*)\}""".r
  def apply(file: String, projections: List[Projected]): String = {
    val encodedProjections: Variable = Projections(projections).encode
    val lines = scala.io.Source.fromFile(file).getLines()
    lines
      .map { line =>
        ParseAndEvaluate.replaceProjection.replaceAllIn(line, {
          case ParseAndEvaluate.replaceProjection(pj)=>
            val parsed = fastparse.parse(pj, Parser.expr(_)).get.value
            val (logs, evaluated) = ProjectedExpression.evaluate(parsed, encodedProjections).value.run
            evaluated.fold(throw IllegalStateException(logs.mkString("\n")))(x=>x.toString)
        })
      }
      .mkString("\n")
  }
}

final case class Projections(projections: List[Projected]) derives VariableEncoder
object Projections {
  given VariableEncoder[Projected] = VariableEncoder.derived //FIXME why I have to do this ?
}
