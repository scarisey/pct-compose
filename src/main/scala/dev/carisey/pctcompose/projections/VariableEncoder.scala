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

import dev.carisey.pctcompose.projections
import io.github.iltotore.iron.{:|, Constraint, IronType, RefinedTypeOps}
import magnolia1.*
import scala.deriving.Mirror

trait VariableEncoder[T] {
  extension (x: T) def encode: Variable
}

object VariableEncoder extends AutoDerivation[VariableEncoder] {
  given VariableEncoder[Int] = Variable.ValueIntegral(_)
  given VariableEncoder[String] = Variable.ValueString(_)
  given VariableEncoder[Boolean] = Variable.ValueBool(_)
  override def join[T](ctx: CaseClass[VariableEncoder, T]): VariableEncoder[T] = value =>
    Variable.Fields(
      ctx.params.map { param => param.label -> param.typeclass.encode(param.deref(value)) }.toMap
    )
  override def split[T](ctx: SealedTrait[VariableEncoder, T]): VariableEncoder[T] = value =>
    ctx.choose(value) { sub => sub.typeclass.encode(sub.value) }
  given [T](using enc: VariableEncoder[T]): VariableEncoder[List[T]] = xs => Variable.Items(xs.map { x => enc.encode(x) }.toList)
  given [T](using enc: VariableEncoder[T]): VariableEncoder[Set[T]] = xs => Variable.Items(xs.map { x => enc.encode(x) }.toList)
  given [T](using enc: VariableEncoder[T]): VariableEncoder[Map[String,T]] = (xs:Map[String,T]) => Variable.Fields(xs.map { (k,v) =>(k, enc.encode(v)) })
  given [A, C](using encoder: VariableEncoder[A]): VariableEncoder[A :| C] = encoder.asInstanceOf[VariableEncoder[A :| C]]
  
}
