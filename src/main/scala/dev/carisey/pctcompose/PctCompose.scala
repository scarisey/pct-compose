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

package dev.carisey.pctcompose

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import dev.carisey.pctcompose.LxcTemplate.{File, Github}
import dev.carisey.pctcompose.projections.VariableEncoder.*
import dev.carisey.pctcompose.projections.{ParseAndEvaluate, VariableEncoder}
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.constraint.collection.ForAll
import io.github.iltotore.iron.constraint.numeric.Greater
import io.github.iltotore.iron.jsoniter.given
import mainargs.*
import requests.RequestAuth.Bearer

import java.nio.file.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.Try
import scala.util.chaining.*

object PctCompose {
  implicit object PathRead extends TokensReader.Simple[os.Path] {
    def shortName = "path"
    def read(strs: Seq[String]) = Right(os.Path(strs.head, os.pwd))
  }
  given descriptionCodec: JsonValueCodec[Description] = JsonCodecMaker.make

  @main(doc = "Show the interpretation of the JSON descriptor.")
  def parse(
      @arg(short = 'd', name = "descriptor", doc = "Descriptor in JSON format")
      descriptor: os.Path = os.pwd / "containers.json"
  ): Unit = {
    val description: Description = readFromArray(os.read.bytes(descriptor))
    pprint.pprintln(description)
  }

  @main(doc = "Interpolate descriptor variables.")
  def project(
      @arg(short = 'd', name = "descriptor", doc = "Descriptor in JSON format")
      descriptor: os.Path = os.pwd / "containers.json",
      @arg(short = 'h', name = "hostname", doc = "Update only container with given hostname")
      containerHostname: Option[String],
      @arg(short = 's', name = "show-variables", doc = "Show all variables")
      showVariables: Boolean = false,
      @arg(short = 'r', name = "dry-run", doc = "Print the resulting projection without writing it.")
      dryRun: Boolean = false
  ): Unit = {
    val description: Description = readFromArray(os.read.bytes(descriptor))
    val projections = description.projectedVars().filter(p => containerHostname.forall(_ == p.hostname))

    if (showVariables) {
      pprint.pprintln(projections)
    }
    if(dryRun) {
      pprint.pprintln(
        description.projections.map(_.template).map(file => ParseAndEvaluate(file, projections))
      )
    }else{
      description.projections.map{projection=>
        val evaluated = ParseAndEvaluate(projection.template,projections)
        val pathToFile:Path = java.nio.file.Paths.get(projection.target)
        pathToFile.toFile.getParentFile.mkdirs()
        Files.deleteIfExists(pathToFile)
        val output = Files.createFile(pathToFile)
        Files.writeString(output,evaluated)
      }
    }
  }

  @main(doc = "Update all the LXC containers described in the JSON descriptor.")
  def update(
      @arg(short = 'd', name = "descriptor", doc = "Descriptor in JSON format")
      descriptor: os.Path = os.pwd / "containers.json",
      @arg(short = 'h', name = "hostname", doc = "Update only container with given hostname")
      containerHostname: Option[String],
      @arg(short = 's', name = "secret", doc = "Github secret")
      secret: Option[os.Path]
  ): Unit = {
    implicit val description: Description = readFromArray(os.read.bytes(descriptor))
    val containers = description.projectedVars()

    makeDirs(description)
    flushPrerouting()

    val deployments = Deployment
      .getActualDeployment(description)
      .filter { deployment => containerHostname.forall(deployment.container.hostname == _) }

    val clean = Future.traverse(deployments.collect { case d: Deployed => d }) { deployment =>
      Future {
        println(s"Stop container ${deployment.id.toString()}")
        Try(OsCommand("pct", "stop", deployment.id.toString()).execute())
        println(s"Destroy container ${deployment.id.toString()}")
        Try(OsCommand("pct", "destroy", deployment.id.toString(), "--destroy-unreferenced-disks", "--force", "--purge").execute())
      }
    }

    Await.ready(clean, Duration.Inf)

    val deploy = deployments
      .flatMap { deployment => // join with pct create params
        containers.find(_.hostname == deployment.container.hostname).map((_, deployment))
      }
      .filter {
        case (_, d: NotDeployed)                                                                      => true
        case (_, Deployed(_, c, _)) if c.template.version == "latest" || c.template.version == "file" => true
        case (_, Deployed(_, c, actualVersion)) if c.template.version == actualVersion                => false
        case _                                                                                        => true
      }
      .tapEach { case (_, deployment) =>
        deployment.container.template match
          case template: Github => Assets.downloadAsset(template, secret)
          case _ => ()
      }
      .map { case (c, _) =>
        Future {
          println(s"Creating container ${c.id}")
          OsCommand.execute(c.toPctCreateArgs(scala.util.Random.nextLong()))
          OsCommand("pct", "start", c.id.toString()).execute()
        }
      }

    Await.ready(Future.sequence(deploy), Duration.Inf)

    updatePrerouting(containers)

  }

  @main(doc = "Flush and reaaply the firewall rules computed from the JSON descriptor.")
  def restoreFw(
      @arg(short = 'd', name = "descriptor", doc = "Descriptor in JSON format")
      descriptor: os.Path = os.pwd / "containers.json"
  ): Unit = {
    val description: Description = readFromArray(os.read.bytes(descriptor))
    flushPrerouting()
    updatePrerouting(description.projectedVars())
  }

  @main(doc = "Show the status of LXC containers.")
  def status(
      @arg(short = 'd', name = "descriptor", doc = "Descriptor in JSON format")
      descriptor: os.Path = os.pwd / "containers.json"
  ): Unit = {
    val description: Description = readFromArray(os.read.bytes(descriptor))
    pprint.pprintln(Deployment.getActualDeployment(description))
  }

  private def flushPrerouting(): Unit = {
    println("Flush prerouting")
    OsCommand.iptables("-t", "nat", "--flush", "PREROUTING").execute()
  }

  private def updatePrerouting(containers: List[Projected]): Unit = {
    println("Update prerouting")
    val iptables = Array("iptables")
    OsCommand.execute(containers.flatMap(_.toIpTablesArgs))
    OsCommand("iptables-save").execute()
  }

  private def makeDirs(description: Description): Unit = {
    description.mkDirs().foreach(cmd => pprint.pprintln(cmd.line.mkString(" ")))
    OsCommand.execute(description.mkDirs())
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)
}
