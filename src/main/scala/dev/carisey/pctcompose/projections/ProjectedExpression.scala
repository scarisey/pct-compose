package dev.carisey.pctcompose.projections

import dev.carisey.pctcompose.Projected
import dev.carisey.pctcompose.projections.Variable.*
import cats.implicits.given

enum ProjectedExpression {
  case ANumber(value: Double)
  case ABool(value: Boolean)
  case AString(value: String)
  case FilterArrayPath(name: String, attributeFiltered: String, expectedValue: ProjectedExpression, next: ProjectedExpression)
  case FilterArrayValue(name: String, attributeFiltered: String, expectedValue: ProjectedExpression)
  case FieldPath(name: String, next: ProjectedExpression)
  case AFieldValue(name: String)
}
object ProjectedExpression {
  def evaluate(expression: ProjectedExpression, scope: Variable): Eval[Long | Boolean | String] = expression match
    case ANumber(value) => value.longValue.lift(s"ANumber $value in $expression") // FIXME improve parser to have ADT with integer vs double
    case ABool(value)   => value.lift(s"ABool $value in $expression")
    case AString(value) => value.lift(s"AString $value in $expression")
    case FilterArrayPath(name, attributeFiltered, expectedValue, next) =>
      for {
        expected <- evaluate( expectedValue, Variable.ValueBool(false) ) // Scope does not matter here since we should have AString|ANumber|...
        items <- scope.getArrayAttr(name)
        matchingItems <- items.x.traverse { variable => variable.getAttr(attributeFiltered).getValue().map((variable, _)) }
        evaluated <- liftOption(s"${expected} is in ${matchingItems.map(_._2)} ?", matchingItems.collectFirst { case (variable, actualValue) if expected == actualValue => variable })
        r <- evaluate(next, evaluated)
      } yield r
    case FilterArrayValue(name, attributeFiltered, expectedValue) =>
      for {
        expected <- evaluate( expectedValue, Variable.ValueBool(false) ) // Scope does not matter here since we should have AString|ANumber|...
        items <- scope.getArrayAttr(name)
        matchingItems <- items.x.traverse { variable => variable.getAttr(attributeFiltered).getValue().map((variable, _)) }
        evaluated <- liftOption(s"${expected} is in ${matchingItems.map(_._2)} ?", matchingItems.collectFirst { case (variable, actualValue) if expected == actualValue => variable })
        r <- evaluated.getAttr(attributeFiltered).getValue()
      } yield r
    case FieldPath(name, next) => scope.getAttr(name).flatMap(evaluate(next, _))
    case AFieldValue(name)     => scope.getAttr(name).getValue()
}
