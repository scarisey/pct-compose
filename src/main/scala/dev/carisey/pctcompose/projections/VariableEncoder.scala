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
