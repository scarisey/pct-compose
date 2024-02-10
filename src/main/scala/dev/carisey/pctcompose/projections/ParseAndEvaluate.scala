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
