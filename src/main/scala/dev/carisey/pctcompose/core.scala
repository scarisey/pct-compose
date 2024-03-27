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

import dev.carisey.pctcompose.LxcTemplate.Github
import dev.carisey.pctcompose.OsCommand.*
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.constraint.numeric.Greater

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
type StorageName = String :| Match["""^[a-zA-Z0-9_\.\-]+$"""]
type BridgeName = String :| Match["""^[a-zA-Z0-9_\.\-]+$"""]
type Permissions = String :| Match["""^(0?[0-7]{2}|[0-7]{3})$"""]
type Path = String :| Match["""^\/{1}.*$"""]
type RelativePath = String :| Match["""^\..+[^\/]$"""]
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
    storage:StorageName="local",
    services: Set[ServiceName],
    template: LxcTemplate
)
final case class Network(cidr: CIDR, gateway: IP, bridge:BridgeName="vmbr0") {
  lazy val cidrNotation: String = s"/${cidr.split('/')(1)}"
}
final case class Projection(
    template: RelativePath,
    target: Path
)
final case class Description(
    name: TemplateName,
    containers: List[Container],
    volumes: Set[Volume],
    services: Set[Service],
    dns: Set[IP],
    templatesDir: Path,
    network: Network,
    projections: List[Projection] = List.empty
) {
  implicit val self: Description = this
  def projectedVars(): List[Projected] = containers
    .zip(NetworkAddresses.availableIPs(network.cidr).filter(_ != network.gateway))
    .zipWithIndex
    .map { case ((c, computedIP), index) =>
      Projected(
        id = (100 + index).refine,
        template = c.template.localPath,
        hostname = c.hostname,
        cores = c.cores,
        memory = c.memory,
        ip = computedIP.refine,
        cidrNotation = network.cidrNotation,
        gateway = network.gateway,
        bridge=network.bridge,
        storage=c.storage,
        diskSize = c.diskSize,
        volumes = volumes.filter(v => c.volumes.contains(v.name)),
        services = services.filter(s => c.services.contains(s.name)),
        dns = dns,
        tags = Set(s"template-${name}", s"name-${c.hostname}", s"version-${c.template.version}")
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
final case class Projected(
    id: Int :| Positive,
    template: String,
    hostname: Hostname,
    cores: Cores,
    memory: Memory,
    ip: IP,
    cidrNotation: String,
    gateway: IP,
    bridge:BridgeName,
    diskSize: DiskSize,
    storage:StorageName,
    volumes: Set[Volume],
    services: Set[Service],
    dns: Set[IP],
    tags: Set[String],
    arch: String = "amd64",
    ostype: String = "nixos",
    cmode: String = "console",
    swap: Int = 0
) {
  val net0: String = s"name=eth0,bridge=${bridge},gw=${gateway},ip=${ip}${cidrNotation},type=veth"
  val mps: List[String] = volumes.map(v => s"${v.hostPath},mp=${v.mountPath}").toList
  def toPctCreateArgs(seed: Long): OsCommand = pctCreate(
    (List(
      id.toString(),
      template,
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
      s"${storage}:${diskSize}",
      "--features",
      "keyctl=1,nesting=1,fuse=1",
      "--tags",
      tags.mkString(";")
    ) ++ dns.toList.flatMap(x => List("-nameserver", x))
      ++ mps.zipWithIndex.flatMap((arg, i) => Array(s"-mp${i}", arg)))*
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
          bridge,
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
