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

//https://www.lihaoyi.com/post/BuildyourownProgrammingLanguagewithScala.html
package dev.carisey.pctcompose.projections

import dev.carisey.pctcompose.projections.ProjectedExpression.*
import dev.carisey.pctcompose.Projected
import fastparse.*
import fastparse.NoWhitespace.*


object Parser {

  def digits[$: P] = P(CharsWhileIn("0-9"))
  def fractional[$: P] = P("." ~ digits)
  def integral[$: P] = P("0" | CharIn("1-9") ~ digits.?)
  def exponent[$: P] = P(CharIn("eE") ~ CharIn("+\\-").? ~ digits)
  def number[$: P] = P(CharIn("+\\-").? ~ integral ~ fractional.? ~ exponent.?).!.map(x => ANumber(java.lang.Double.parseDouble(x)))

  def PTrue[$: P] = P(IgnoreCase("true")).!.map(_ => ABool(true))
  def PFalse[$: P] = P(IgnoreCase("false")).!.map(_ => ABool(false))
  def boolean[$: P] = P(PTrue | PFalse)

  def stringChars(c: Char) = c != '\"' && c != '\\'
  def strChars[$: P] = P( CharsWhile(stringChars) )
  def string[$: P] = P("\"" ~ strChars.rep.!.map(AString(_)) ~ "\"")

  def validChar[$: P] = P(CharIn("a-zA-Z"))
  def field[$: P] = P(validChar ~ (digits | validChar).rep).!

  def filterArray[$: P] = P((field) ~ "[" ~ (field) ~ "=" ~ (number | boolean | string) ~ "]")
  def filterArrayValue[$: P] = P(filterArray ~ End)
    .map { case (name, attribute, expected) => FilterArrayValue(name, attribute, expected) }
  def fieldValue[$: P] = P(field ~ End).!.map(x => AFieldValue(x))
  def filterArrayPath[$: P]: P[ProjectedExpression] = P(filterArray ~ "." ~ expr)
    .map { case (name, attribute, expected, expr) => FilterArrayPath(name, attribute, expected, expr) }
  def fieldPath[$: P]: P[ProjectedExpression] =
    P(((field) ~ "." ~ expr | fieldValue))
      .map {
        case fieldValue: AFieldValue                   => fieldValue
        case (name: String, expr: ProjectedExpression) => FieldPath(name, expr)
      }

  def expr[$: P] = P((fieldValue | filterArrayValue | filterArrayPath | fieldPath))
}
