import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.collection.*
import scala.annotation.nowarn

object NetworkAddresses {

  def maskBits(cidr: CIDR): Array[Int] = {
    val mask = Integer.parseInt(cidr.split('/')(1))
    Array
      .fill(mask)(1)
      .concat(Array.fill(32 - mask)(0))
  }
  def inversedMaskBits(cidr: CIDR): Array[Int] = {
    val mask = Integer.parseInt(cidr.split('/')(1))
    Array
      .fill(Integer.parseInt(cidr.split('/')(1)))(0)
      .concat(Array.fill(32 - mask)(1))
  }

  @nowarn
  def bitsToInts(bits: Array[Int]): Array[Int] =
    bits
      .foldLeft((0, List(List.empty[Int]))) { case ((index, last :: tail), x) =>
        if ((index + 1) % 8 == 0) (index + 1, List.empty :: (x :: last).reverse :: tail) else (index + 1, (x :: last) :: tail)
      }
      ._2
      .filter(_.nonEmpty)
      .reverse
      .map(xs => Integer.parseInt(xs.mkString, 2))
      .toArray

  extension (bits: Array[Int]) {
    def toInts(): Array[Int] = bitsToInts(bits)
  }

  def networkAddress(cidr: CIDR): Array[Int] = cidr
    .split('/')(0)
    .split('.')
    .map(Integer.parseInt)
    .zip(maskBits(cidr).toInts())
    .map { case (ipPart, maskPart) => ipPart & maskPart }

  def broadcastAdress(cidr: CIDR): Array[Int] = cidr
    .split('/')(0)
    .split('.')
    .map(Integer.parseInt)
    .zip(inversedMaskBits(cidr).toInts())
    .map { case (ipPart, maskPart) => ipPart | maskPart }

  def nextIP(ip: IP): Option[IP] = nextIP(ip.split('.').map(Integer.parseInt)).map(_.mkString(".").refine)

  def nextIP(ip: Array[Int]): Option[Array[Int]] = {
    val (next, isIt) = ip
      .foldRight((List.empty[Int], false)) {
        case (x, (acc, false)) =>
          if (x < 255) (x + 1 :: acc, true) else (0 :: acc, false)
        case (x, (acc, true)) => (x :: acc, true)
      }
    Option.when(isIt)(next.toArray)
  }

  def availableIPs(cidr: CIDR): Stream[IP] = {
    val start = NetworkAddresses.networkAddress(cidr)
    val end = NetworkAddresses.broadcastAdress(cidr)

    Stream
      .unfold(start) {
        case state if state.sameElements(end) => None
        case state =>
          nextIP(state).flatMap { next =>
            Option.when(!next.sameElements(end))((next, next))
          }
      }
      .map(_.mkString(".").refine)
  }

}
