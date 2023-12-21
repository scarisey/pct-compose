package dev.carisey.pctcompose
import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*
import org.scalatest.matchers.should.Matchers
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import NetworkAddresses.*

class NetworkAddressesTest extends AnyFlatSpec with Matchers {

  "maskBits" should "return a valid mask" in {
    maskBits("192.168.1.1/19".refine) shouldBe Array(
      1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    )
  }
  "inversedMaskBits" should "return a valid inversed mask" in {
    inversedMaskBits("192.168.1.1/19".refine) shouldBe Array(
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1
    )
  }
  "bitsToInts" should "return a valid conversion of bit array to IP int array" in {
    bitsToInts(
      Array(
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
      )
    ) shouldBe Array(255, 255, 224, 0)
    Array(
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1
    ).toInts() shouldBe Array(0, 0, 31, 255)
  }

  "networkAddress" should "give an array int IP of network" in {
    networkAddress("192.168.1.1/19".refine) shouldBe Array(192, 168, 0, 0)
  }

  "broadcastAddress" should "give an array int IP of broadcast" in {
    broadcastAdress("192.168.1.1/19".refine) shouldBe Array(192, 168, 31, 255)
  }

  "nextIp" should "give always give a valid IP" in {
    nextIP("192.168.1.1") shouldBe Some("192.168.1.2")
    nextIP("255.255.255.255") shouldBe None
    nextIP("255.255.255.254") shouldBe Some("255.255.255.255")
    nextIP("255.255.254.255") shouldBe Some("255.255.255.0")
    nextIP("254.255.255.255") shouldBe Some("255.0.0.0")
    nextIP("255.255.0.255") shouldBe Some("255.255.1.0")
    nextIP("255.255.0.254") shouldBe Some("255.255.0.255")
  }

  "availableIPs" should "give a stream of valid IPs for given network" in {
    availableIPs("192.168.1.1/19").take(6) shouldBe List(
      "192.168.0.1",
      "192.168.0.2",
      "192.168.0.3",
      "192.168.0.4",
      "192.168.0.5",
      "192.168.0.6"
    )

    availableIPs("192.168.1.1/32").toList shouldBe List.empty[IP]
    availableIPs("192.168.1.1/24").size shouldBe 254
  }
}
