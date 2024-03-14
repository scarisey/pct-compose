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
