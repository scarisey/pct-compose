package dev.carisey.pctcompose

import dev.carisey.pctcompose.LxcTemplate.Github
import dev.carisey.pctcompose.OsCommand.*
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.constraint.numeric.Greater

import java.util.UUID

type OwnerId = Int :| Interval.Closed[0, 65535]
type IP = String :| Match["""^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"""]
type CIDR = String :|
  Match["""^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\/([1-9]|[1-2][0-9]|3[0-2])$"""]
type Port = Int :| Interval.Closed[0, 65535]
type Cores = Int :| Interval.Closed[1, 64]
type Memory = Int :| Interval.Closed[512, 65536]
type DiskSize = Int :| Interval.Closed[8, 65536]
type Hostname = String :|
  Match["""^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]*[a-zA-Z0-9])\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\-]*[A-Za-z0-9])$"""]
type ServiceName = String :| LettersLowerCase
type VolumeName = String :| LettersLowerCase
type Permissions = String :| Match["""^(0?[0-7]{2}|[0-7]{3})$"""]
type Path = String :| Match["""^\/{1}.*$"""]
type TemplateName = String :| Match["""^\w+$"""]
type Version = String :| Match["""^[a-zA-Z0-9_\.]+$"""]
sealed trait LxcTemplate {
  val version: Version
}
object LxcTemplate {
  final case class File(file: Path) extends LxcTemplate {
    val version = "file".refine
  }
  final case class Github(file: String, repo: String, tag: String) extends LxcTemplate {
    val urlSuffix = tag match
      case "latest" => s"${repo}/releases/latest"
      case _        => s"${repo}/releases/tags/${tag}"
    val version = tag.refine
  }
  extension (t: LxcTemplate) {
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
    diskSize: DiskSize,
    services: Set[ServiceName],
    template: LxcTemplate
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
        diskSize = c.diskSize,
        volumes = volumes.filter(v => c.volumes.contains(v.name)),
        services = services.filter(s => c.services.contains(s.name)),
        dns = dns,
        tags = List(s"template-${name}", s"name-${c.hostname}", s"version-${c.template.version}")
      )
    }
  def mkDirs(): OsCommands =
    volumes.toList.flatMap(v =>
      List(
        OsCommand("mkdir", "-p", v.hostPath),
        OsCommand("chown", "-R", s"${v.uid}:${v.gid}", v.hostPath),
        OsCommand("chmod", "-R", v.perms, v.hostPath)
      )
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
    diskSize: DiskSize,
    volumes: Set[Volume],
    services: Set[Service],
    dns: Set[IP],
    tags: List[String]
) {
  val arch: String = "amd64"
  val ostype: String = "nixos"
  val cmode: String = "console"
  val swap: Int = 0
  val net0: String = s"name=eth0,bridge=vmbr1,gw=${gateway},ip=${ip}${cidrNotation},type=veth"
  val mps: List[String] = volumes.map(v => s"${v.hostPath},mp=${v.mountPath}").toList
  def toPctCreateArgs(seed: Long): OsCommand = pctCreate(
    (List(
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
      RandomPassword(seed).get(),
      "-net0",
      net0,
      "--rootfs",
      s"local:${diskSize}",
      "--features",
      "keyctl=1,nesting=1,fuse=1",
      "--tags",
      tags.mkString(";")
    ) ++ dns.toList.flatMap(x => List("-nameserver", x))
      ++ mps.zipWithIndex.flatMap((arg, i) => Array(s"-mp${i}", arg))): _*
  )

  lazy val toIpTablesArgs: OsCommands =
    services
      .map(s =>
        OsCommand.iptables(
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
      .toList
}
final case class GithubAsset(id: Long, name: String, url: String)
final case class GithubRelease(assets: List[GithubAsset]) {
  def filter(name: String): Option[GithubAsset] = assets.find(_.name == name)
}

final case class RandomPassword(seed: Long) {
  def get(): String = {
    scala.util.Random.setSeed(seed)
    List.fill(24) { scala.util.Random.nextPrintableChar() }.mkString
  }
}
