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
import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*
import io.github.iltotore.iron.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import PctCompose.descriptionCodec
import LxcTemplate.Github

object CoreTest {
  def readContainersJson(): Description = readFromStream(
    Thread.currentThread().getContextClassLoader.getResourceAsStream("containers.json")
  )
}
class CoreTest extends AnyFlatSpec with should.Matchers {
  import CoreTest.readContainersJson

  "Description with Github template" should "give proper destination to local path where to download file" in {
    implicit val description: Description = readContainersJson()

    description.containers.map(_.template).collect { case c: Github => c.localPath } shouldEqual List(
      "/var/lib/vz/template/cache/bar.tar.xz"
    )

  }

  "Description" should "generate ContainersParam" in {
    val description: Description = readContainersJson()
    val containerParams = description.createContainerParams()
    containerParams.map(_.ip).toSet should have size 2
    containerParams.map(_.id).toSet should have size 2

    containerParams should contain theSameElementsAs List(
      CreateContainerParams(
        id = 100,
        template = "/var/lib/vz/template/cache/foo.tar.xz",
        hostname = "host1",
        cores = 4,
        memory = 2048,
        ip = "192.168.1.2",
        cidrNotation = "/24",
        gateway = "192.168.1.1",
        diskSize = 8,
        volumes = Set(
          Volume(
            name = "data",
            hostPath = "/var/lib/vz/data",
            mountPath = "/data",
            uid = 1000,
            gid = 1000,
            perms = "022"
          )
        ),
        services = Set(Service(name = "ssh1", nat = Nat(localPort = 22, remotePort = 2022))),
        dns = Set("8.8.8.8", "8.8.4.4", "1.1.1.1"),
        tags = List("template-test_template", "name-host1", "version-file")
      ),
      CreateContainerParams(
        id = 101,
        template = "/var/lib/vz/template/cache/bar.tar.xz",
        hostname = "host2",
        cores = 1,
        memory = 512,
        ip = "192.168.1.3",
        cidrNotation = "/24",
        gateway = "192.168.1.1",
        diskSize = 24,
        volumes = Set(
          Volume(
            name = "data",
            hostPath = "/var/lib/vz/data",
            mountPath = "/data",
            uid = 1000,
            gid = 1000,
            perms = "022"
          ),
          Volume(
            name = "config",
            hostPath = "/var/lib/vz/config",
            mountPath = "/var/lib/config",
            uid = 180,
            gid = 180,
            perms = "764"
          )
        ),
        services = Set(
          Service(name = "ssh2", nat = Nat(localPort = 22, remotePort = 3022)),
          Service(name = "webhook", nat = Nat(localPort = 8080, remotePort = 8080))
        ),
        dns = Set("8.8.8.8", "8.8.4.4", "1.1.1.1"),
        tags = List("template-test_template", "name-host2", "version-latest")
      )
    )

  }

  "Description" should "generate pct create params" in {
    val description: Description = readContainersJson()
    val pctCreateParams = description.createContainerParams().map(_.toPctCreateArgs(42))
    pctCreateParams should contain theSameElementsAs List(
      OsCommand(
        line = List(
          "pct",
          "create",
          "100",
          "/var/lib/vz/template/cache/foo.tar.xz",
          "-arch",
          "amd64",
          "-ostype",
          "nixos",
          "-hostname",
          "host1",
          "-cores",
          "4",
          "-memory",
          "2048",
          "-cmode",
          "console",
          "-swap",
          "0",
          "-password",
          "i2=u#d\"?zb-g-1)UEkb`)\"El",
          "-net0",
          "name=eth0,bridge=vmbr1,gw=192.168.1.1,ip=192.168.1.2/24,type=veth",
          "--rootfs",
          "local:8",
          "--features",
          "keyctl=1,nesting=1,fuse=1",
          "--tags",
          "template-test_template;name-host1;version-file",
          "-nameserver",
          "8.8.8.8",
          "-nameserver",
          "8.8.4.4",
          "-nameserver",
          "1.1.1.1",
          "-mp0",
          "/var/lib/vz/data,mp=/data"
        )
      ),
      OsCommand(
        line = List(
          "pct",
          "create",
          "101",
          "/var/lib/vz/template/cache/bar.tar.xz",
          "-arch",
          "amd64",
          "-ostype",
          "nixos",
          "-hostname",
          "host2",
          "-cores",
          "1",
          "-memory",
          "512",
          "-cmode",
          "console",
          "-swap",
          "0",
          "-password",
          "i2=u#d\"?zb-g-1)UEkb`)\"El",
          "-net0",
          "name=eth0,bridge=vmbr1,gw=192.168.1.1,ip=192.168.1.3/24,type=veth",
          "--rootfs",
          "local:24",
          "--features",
          "keyctl=1,nesting=1,fuse=1",
          "--tags",
          "template-test_template;name-host2;version-latest",
          "-nameserver",
          "8.8.8.8",
          "-nameserver",
          "8.8.4.4",
          "-nameserver",
          "1.1.1.1",
          "-mp0",
          "/var/lib/vz/data,mp=/data",
          "-mp1",
          "/var/lib/vz/config,mp=/var/lib/config"
        )
      )
    )
  }

  "Description" should "generate iptables params" in {
    val description: Description = readContainersJson()
    val iptablesParams = description.createContainerParams().map(_.toIpTablesArgs)
    iptablesParams should have size 2
    iptablesParams(0) should contain theSameElementsAs List(
      OsCommand(
        "iptables",
        "-t",
        "nat",
        "-A",
        "PREROUTING",
        "-i",
        "vmbr0",
        "-p",
        "tcp",
        "--dport",
        "2022",
        "-j",
        "DNAT",
        "--to",
        "192.168.1.2:22"
      )
    )
    iptablesParams(1) should contain theSameElementsAs List(
      OsCommand(
        "iptables",
        "-t",
        "nat",
        "-A",
        "PREROUTING",
        "-i",
        "vmbr0",
        "-p",
        "tcp",
        "--dport",
        "3022",
        "-j",
        "DNAT",
        "--to",
        "192.168.1.3:22"
      ),
      OsCommand(
        "iptables",
        "-t",
        "nat",
        "-A",
        "PREROUTING",
        "-i",
        "vmbr0",
        "-p",
        "tcp",
        "--dport",
        "8080",
        "-j",
        "DNAT",
        "--to",
        "192.168.1.3:8080"
      )
    )

  }

  "Description" should "generate mkdir commands" in {
    val description: Description = readContainersJson()
    val mkdirs = description.mkDirs()

    mkdirs should contain theSameElementsAs List(
      OsCommand("mkdir", "-p", "/var/lib/vz/data"),
      OsCommand("chown", "-R", "1000:1000", "/var/lib/vz/data"),
      OsCommand("chmod", "-R", "022", "/var/lib/vz/data"),
      OsCommand("mkdir", "-p", "/var/lib/vz/config"),
      OsCommand("chown", "-R", "180:180", "/var/lib/vz/config"),
      OsCommand("chmod", "-R", "764", "/var/lib/vz/config")
    )
  }

  "GithubTemplate" should "give url to download asset" in {
    val templateLatest = LxcTemplate.Github("bar.tar.xz", "owner/repo", "latest")
    templateLatest.urlSuffix shouldEqual "owner/repo/releases/latest"

    val templateTag = LxcTemplate.Github("bar.tar.xz", "owner/repo", "v1.0")
    templateTag.urlSuffix shouldEqual "owner/repo/releases/tags/v1.0"
  }

}
