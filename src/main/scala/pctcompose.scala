//> using scala 3
//> using packaging.output pct-compose
//> using dep com.lihaoyi::mainargs::0.5.4
//> using dep com.lihaoyi::os-lib::0.9.2
//> using dep com.lihaoyi::pprint::0.8.1
//> using dep com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core::2.25.0
//> using dep com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros::2.25.0
//> using dep io.github.iltotore::iron::2.3.0
//> using dep io.github.iltotore::iron-scalacheck::2.3.0
//> using dep io.github.iltotore::iron-jsoniter::2.3.0
//> using dep com.lihaoyi::requests::0.8.0
//> using testFramework org.scalatest.tools.Framework
//> using test.dep org.scalatest::scalatest::3.2.17
//> using test.dep org.scalacheck::scalacheck::1.17.0

import mainargs._
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.jsoniter.given
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import io.github.iltotore.iron.constraint.numeric.Greater
import io.github.iltotore.iron.constraint.collection.ForAll
import java.util.UUID
import scala.collection.mutable.ArrayBuilder
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import requests.RequestAuth.Bearer
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.chaining.*
import Template.Github

type OwnerId = Int :| Interval.Closed[0, 65535]
type IP = String :| Match["""^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"""]
type CIDR = String :|
  Match["""^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\/([1-9]|[1-2][0-9]|3[0-2])$"""]
type Port = Int :| Interval.Closed[0, 65535]
type Cores = Int :| Interval.Closed[1, 64]
type Memory = Int :| Interval.Closed[512, 65536]
type Hostname = String :|
  Match["""^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]*[a-zA-Z0-9])\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\-]*[A-Za-z0-9])$"""]
type ServiceName = String :| LettersLowerCase
type VolumeName = String :| LettersLowerCase
type Permissions = String :| Match["""^(0?[0-7]{2}|[0-7]{3})$"""]
type Path = String :| Match["""^\/{1}.*$"""]
type TemplateName = String :| Match["""^\w+$"""]
sealed trait Template {
  val version: String
}
object Template {
  final case class File(file: Path) extends Template {
    val version = "file"
  }
  final case class Github(file: String, repo: String, tag: String) extends Template {
    val url = tag match
      case "latest" => s"${repo}/releases/latest"
      case _        => s"${repo}/releases/tags/${tag}"
    val version = tag
  }
  extension (t: Template) {
    def localPath(using d: Description): Path = t match
      case File(file)              => file
      case Github(file, repo, tag) => s"${d.templatesDir}/$file".refine

  }
}
final case class Nat(localPort: Port, remotePort: Port)
final case class Service(name: ServiceName, nat: Nat)
final case class Volume(name: VolumeName, hostPath: Path, mountPath: Path, uid: OwnerId, gid: OwnerId, perms: Permissions)
final case class Container(
    hostname: Hostname,
    volumes: Set[VolumeName],
    cores: Cores,
    memory: Memory,
    services: Set[ServiceName],
    template: Template
)
final case class Network(cidr: CIDR, gateway: IP) {
  lazy val cidrNotation: String = s"/${cidr.split('/')(1)}"
}
final case class Description(
    name: TemplateName,
    containers: List[Container],
    volumes: Set[Volume],
    services: Set[Service],
    dns: Set[IP],
    templatesDir: Path,
    network: Network
) {
  implicit val self: Description = this
  def createContainerParams(): List[CreateContainerParams] = containers
    .zip(NetworkAddresses.availableIPs(network.cidr).filter(_ != network.gateway))
    .zipWithIndex
    .map { case ((c, computedIP), index) =>
      CreateContainerParams(
        id = (100 + index).refine,
        template = c.template.localPath,
        hostname = c.hostname,
        cores = c.cores,
        memory = c.memory,
        ip = computedIP.refine,
        cidrNotation = network.cidrNotation,
        gateway = network.gateway,
        volumes = volumes.filter(v => c.volumes.contains(v.name)),
        services = services.filter(s => c.services.contains(s.name)),
        dns = dns,
        tags = List(s"name-${name}", s"version-${c.template.version}")
      )
    }
  def mkDirs(): List[List[String]] =
    volumes.toList.flatMap(v =>
      List(List("mkdir", "-p", v.hostPath), List("chown", "-R", s"${v.uid}:${v.gid}", v.hostPath), List("chmod", "-R", v.perms, v.hostPath))
    )
}
final case class CreateContainerParams(
    id: Int :| Positive,
    template: String,
    hostname: Hostname,
    cores: Cores,
    memory: Memory,
    ip: IP,
    cidrNotation: String,
    gateway: IP,
    volumes: Set[Volume],
    services: Set[Service],
    dns: Set[IP],
    tags: List[String]
) {
  val arch: String = "amd64"
  val ostype: String = "nixos"
  val cmode: String = "console"
  val swap: Int = 0
  val password: String = UUID.randomUUID().toString()
  val net0: String = s"name=eth0,bridge=vmbr1,gw=${gateway},ip=${ip}${cidrNotation},type=veth"
  val mps: Array[String] = volumes.map(v => s"${v.hostPath},mp=${v.mountPath}").toArray
  lazy val toPctCreateArgs: Array[String] = Array(
    id.toString(),
    template.toString(),
    "-arch",
    arch,
    "-ostype",
    ostype,
    "-hostname",
    hostname,
    "-cores",
    cores.toString(),
    "-memory",
    memory.toString(),
    "-cmode",
    cmode,
    "-swap",
    swap.toString(),
    "-password",
    password,
    "-net0",
    net0,
    "--rootfs",
    "local:8",
    "--features",
    "keyctl=1,nesting=1,fuse=1",
    "--tags",
    tags.mkString(";")
  ) ++ dns.toArray.flatMap(x => List("-nameserver", x))
    ++ mps.zipWithIndex.flatMap((arg, i) => Array(s"-mp${i}", arg))

  lazy val toIpTablesArgs: Set[Array[String]] =
    services.map(s =>
      Array(
        "-t",
        "nat",
        "-A",
        "PREROUTING",
        "-i",
        "vmbr0",
        "-p",
        "tcp",
        "--dport",
        s.nat.remotePort.toString(),
        "-j",
        "DNAT",
        "--to",
        s"${ip}:${s.nat.localPort}"
      )
    )
}
final case class GithubAsset(id: Long, name: String, url: String)
final case class GithubRelease(assets: List[GithubAsset]) {
  def filter(name: String): Option[GithubAsset] = assets.find(_.name == name)
}

object Main {
  implicit object PathRead extends TokensReader.Simple[os.Path] {
    def shortName = "path"
    def read(strs: Seq[String]) = Right(os.Path(strs.head, os.pwd))
  }
  given descriptionCodec: JsonValueCodec[Description] = JsonCodecMaker.make
  given releaseCodec: JsonValueCodec[GithubRelease] = JsonCodecMaker.make
  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global

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
    val description: Description = readFromArray(os.read.bytes(descriptor))
    val containers = description.createContainerParams()

    secret.fold(pprint.pprintln("No secret was given so downloading assets is skipped."))(
      downloadAssets(descriptor, _)
    )

    makeDirs(description)
    flushPrerouting()

    val pctCreate = Array("pct", "create")
    containers
      .filter(c => containerHostname.forall(c.hostname == _))
      .foreach(c => {
        pprint.pprintln(s"Stop container ${c.id.toString()}")
        Try { os.proc("pct", "stop", c.id.toString()).call(stdout = os.Inherit) }.fold(e => pprint.pprintln(e), c => pprint.pprintln(c))
        pprint.pprintln(s"Destroy container ${c.id.toString()}")
        Try { os.proc("pct", "destroy", c.id.toString(), "--destroy-unreferenced-disks", "--force", "--purge").call(stdout = os.Inherit) }
          .fold(e => pprint.pprintln(e), c => pprint.pprintln(c))
        pprint.pprintln(s"Creating container ${c.id}")
        pprint.pprintln(os.proc((pctCreate ++ c.toPctCreateArgs)).call(stdout = os.Inherit))
      })

    updatePrerouting(containers)

    containers
      .filter(c => containerHostname.forall(c.hostname == _))
      .foreach(c => os.proc("pct", "start", c.id.toString()).call(stdout = os.Inherit))
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
  def downloadAssets(
      @arg(short = 'd', name = "descriptor", doc = "Descriptor in JSON format")
      descriptor: os.Path = os.pwd / "containers.json",
      @arg(short = 's', name = "secret", doc = "Github secret")
      secret: os.Path
  ): Unit = {

    implicit val description: Description = readFromArray(os.read.bytes(descriptor))
    val token = Bearer(os.read.lines(secret).head)

    pprint.pprintln("Prepare to download assets: ")
    val urls = description.containers
      .map(_.template)
      .collect { case t: Template.Github => t }
      .map(_.tap(t => pprint.pprintln(t)))
    val res =
      for {
        releases <- Future.traverse(urls) { template =>
          {
            val url = template.url
            val file = template.file
            val localPath = template.localPath
            pprint.pprintln(s"Get release for $file ...")
            Future(requests.get(url = s"https://api.github.com/repos/${url}", auth = token).text())
              .map(readFromString[GithubRelease](_))
              .map(_.tap(x => pprint.pprintln(x)))
              .map(
                _.filter(file)
                  .map(asset => (localPath, asset))
                  .tap {
                    case None => pprint.pprintln(s"$file cannot be found at $url")
                    case _    => ()
                  }
              )
              .recover { case e: Throwable => pprint.pprintln(e); throw e }
          }
        }
        _ = releases.flatten.foreach { (localPath, release) =>
          {
            val tempPath = os.Path(s"${localPath}-${UUID.randomUUID().toString()}")
            pprint.pprintln(s"Download ${release.name} into ${tempPath}")
            requests
              .get(url = release.url, auth = token, headers = Map("Accept" -> "application/octet-stream"))
              .writeBytesTo(os.write.outputStream(tempPath))
            pprint.pprintln(s"Move ${tempPath} into ${localPath}")
            os.move.over(tempPath, os.Path(localPath.toString()))
          }
        }
      } yield ()

    Await.ready(res, Duration.Inf) // TODO set a timeout on param

  }

  private def flushPrerouting(): Unit = {
    pprint.pprintln("Flush prerouting")
    os.proc("iptables", "-t", "nat", "--flush", "PREROUTING").call(stdout = os.Inherit)
  }

  private def updatePrerouting(containers: List[CreateContainerParams]): Unit = {
    pprint.pprintln("Update prerouting")
    val iptables = Array("iptables")
    containers.flatMap(_.toIpTablesArgs).foreach(argsForContainer => os.proc(iptables ++ argsForContainer).call(stdout = os.Inherit))
    os.proc("iptables-save").call(stdout = os.Inherit)
  }

  private def makeDirs(description: Description): Unit = {
    pprint.pprintln(description.mkDirs())
    description.mkDirs().foreach(mkdir => os.proc(mkdir).call(stdout = os.Inherit))
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)
}
