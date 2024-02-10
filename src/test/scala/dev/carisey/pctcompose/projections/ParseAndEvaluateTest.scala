package dev.carisey.pctcompose.projections

import com.github.plokhotnyuk.jsoniter_scala.core.*
import dev.carisey.pctcompose.Description
import dev.carisey.pctcompose.PctCompose.descriptionCodec
import dev.carisey.pctcompose.projections.ParseAndEvaluateTest.readContainersWithProjectionsJson
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

object ParseAndEvaluateTest{
  def readContainersWithProjectionsJson(): Description = readFromStream(
    Thread.currentThread().getContextClassLoader.getResourceAsStream("containersWithProjections.json")
  )
}
class ParseAndEvaluateTest extends AnyFlatSpec with Matchers{
 "parse descriptor and evaluate projections" should "print projected files" in {
   val result = ParseAndEvaluate(Thread.currentThread().getContextClassLoader.getResource("templates/host1config").getFile,readContainersWithProjectionsJson().projectedVars())
   pprint.pprintln(result)
   result shouldEqual
     """foo:
       |  - host1:
       |    ipAddress: 192.168.1.2
       |  - host2:
       |    ipAddress: 192.168.1.3
       |    dataPath: /data""".stripMargin
 }
}
