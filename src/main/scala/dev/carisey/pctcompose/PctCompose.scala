package dev.carisey.pctcompose

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import dev.carisey.pctcompose.LxcTemplate.Github
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.constraint.collection.ForAll
import io.github.iltotore.iron.constraint.numeric.Greater
import io.github.iltotore.iron.jsoniter.given
import mainargs.*
import requests.RequestAuth.Bearer

import java.util.UUID
import scala.util.Try
import scala.util.chaining.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import dev.carisey.pctcompose.LxcTemplate.File

object PctCompose {
  implicit object PathRead extends TokensReader.Simple[os.Path] {
    def shortName = "path"
    def read(strs: Seq[String]) = Right(os.Path(strs.head, os.pwd))
  }
  given descriptionCodec: JsonValueCodec[Description] = JsonCodecMaker.make

  @main
  def parse(
      @arg(short = 'd', name = "descriptor", doc = "Descriptor in JSON format")
      descriptor: os.Path = os.pwd / "containers.json"
  ): Unit = {
    val description: Description = readFromArray(os.read.bytes(descriptor))
    pprint.pprintln(description)
  }

  @main
  def update(
      @arg(short = 'd', name = "descriptor", doc = "Descriptor in JSON format")
      descriptor: os.Path = os.pwd / "containers.json",
      @arg(short = 'h', name = "hostname", doc = "Update only container with given hostname")
      containerHostname: Option[String],
      @arg(short = 's', name = "secret", doc = "Github secret")
      secret: Option[os.Path]
  ): Unit = {
    implicit val description: Description = readFromArray(os.read.bytes(descriptor))
    val containers = description.createContainerParams()

    makeDirs(description)
    flushPrerouting()

    val deployments = Deployment
      .getActualDeployment(description)
      .filter { deployment => containerHostname.forall(deployment.container.hostname == _) }

    val clean = Future.traverse(deployments.collect { case d: Deployed => d }) { deployment =>
      Future {
        pprint.pprintln(s"Stop container ${deployment.id.toString()}")
        Try(OsCommand("pct", "stop", deployment.id.toString()).execute())
        pprint.pprintln(s"Destroy container ${deployment.id.toString()}")
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
          case template: Github =>
            secret.fold(pprint.pprintln("No secret was given so downloading assets is skipped."))(
              Assets.downloadAsset(template, _)
            )
          case _ => ()
      }
      .map { case (c, _) =>
        Future {
          pprint.pprintln(s"Creating container ${c.id}")
          OsCommand.execute(c.toPctCreateArgs(scala.util.Random.nextLong()))
          OsCommand("pct", "start", c.id.toString()).execute()
        }
      }

    Await.ready(Future.sequence(deploy), Duration.Inf)

    updatePrerouting(containers)

  }

  @main
  def restoreFw(
      @arg(short = 'd', name = "descriptor", doc = "Descriptor in JSON format")
      descriptor: os.Path = os.pwd / "containers.json"
  ): Unit = {
    val description: Description = readFromArray(os.read.bytes(descriptor))
    flushPrerouting()
    updatePrerouting(description.createContainerParams())
  }

  @main
  def status(): Unit = {
    val description: Description = readFromArray(os.read.bytes(os.pwd / "containers.json"))
    pprint.pprintln(Deployment.getActualDeployment(description))
  }

  private def flushPrerouting(): Unit = {
    pprint.pprintln("Flush prerouting")
    OsCommand.iptables("-t", "nat", "--flush", "PREROUTING").execute()
  }

  private def updatePrerouting(containers: List[CreateContainerParams]): Unit = {
    pprint.pprintln("Update prerouting")
    val iptables = Array("iptables")
    OsCommand.execute(containers.flatMap(_.toIpTablesArgs))
    OsCommand("iptables-save").execute()
  }

  private def makeDirs(description: Description): Unit = {
    pprint.pprintln(description.mkDirs())
    OsCommand.execute(description.mkDirs())
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)
}
