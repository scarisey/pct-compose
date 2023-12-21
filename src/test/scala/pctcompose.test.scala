import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*
import io.github.iltotore.iron.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import Main.descriptionCodec
import os.remove
import Template.Github

class ContainersTest extends AnyFlatSpec with should.Matchers {

  "Description with Github template" should "give proper destination to local path where to download file" in {
    implicit val description: Description = readFromArray(os.read.bytes(os.pwd / "containers.json"))

    description.containers.map(_.template).collect { case c: Github => c.localPath } shouldEqual List(
      "/var/lib/vz/template/cache/bar.tar.xz"
    )

  }

  "Description" should "generate ContainersParam" in {
    val description = readFromArray(os.read.bytes(os.pwd / "containers.json"))
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
        tags = List("name-test_template", "version-file")
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
        tags = List("name-test_template", "version-latest")
      )
    )

  }

  "Description" should "generate pct create params" in {
    val description = readFromArray(os.read.bytes(os.pwd / "containers.json"))
    val pctCreateParams = description.createContainerParams().map(_.toPctCreateArgs)
    val removePasswordIndexes = pctCreateParams
      .map(_.foldLeft((List.empty[String], false)) {
        case ((acc, false), x) if x == "-password" => (acc, true)
        case ((acc, false), x)                     => (x :: acc, false)
        case ((acc, true), x)                      => (acc, false)
      }._1.reverse)
    removePasswordIndexes(0).filterNot(_.contains("password")) should contain theSameElementsAs Array(
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
      "-net0",
      "name=eth0,bridge=vmbr1,gw=192.168.1.1,ip=192.168.1.2/24,type=veth",
      "--rootfs",
      "local:8",
      "--features",
      "keyctl=1,nesting=1,fuse=1",
      "-nameserver",
      "8.8.8.8",
      "-nameserver",
      "8.8.4.4",
      "-nameserver",
      "1.1.1.1",
      "-mp0",
      "/var/lib/vz/data,mp=/data",
      "--tags",
      "name-test_template;version-file"
    )
    removePasswordIndexes(1).filterNot(_.contains("password")) should contain theSameElementsAs Array(
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
      "-net0",
      "name=eth0,bridge=vmbr1,gw=192.168.1.1,ip=192.168.1.3/24,type=veth",
      "--rootfs",
      "local:8",
      "--features",
      "keyctl=1,nesting=1,fuse=1",
      "-nameserver",
      "8.8.8.8",
      "-nameserver",
      "8.8.4.4",
      "-nameserver",
      "1.1.1.1",
      "-mp0",
      "/var/lib/vz/data,mp=/data",
      "-mp1",
      "/var/lib/vz/config,mp=/var/lib/config",
      "--tags",
      "name-test_template;version-latest"
    )
  }

  "Description" should "generate iptables params" in {
    val description = readFromArray(os.read.bytes(os.pwd / "containers.json"))
    val iptablesParams = description.createContainerParams().map(_.toIpTablesArgs)
    iptablesParams should have size 2
    iptablesParams(0) should contain theSameElementsAs Set(
      Array("-t", "nat", "-A", "PREROUTING", "-i", "vmbr0", "-p", "tcp", "--dport", "2022", "-j", "DNAT", "--to", "192.168.1.2:22")
    )
    iptablesParams(1) should contain theSameElementsAs Set(
      Array("-t", "nat", "-A", "PREROUTING", "-i", "vmbr0", "-p", "tcp", "--dport", "3022", "-j", "DNAT", "--to", "192.168.1.3:22"),
      Array("-t", "nat", "-A", "PREROUTING", "-i", "vmbr0", "-p", "tcp", "--dport", "8080", "-j", "DNAT", "--to", "192.168.1.3:8080")
    )

  }

  "Description" should "generate mkdir commands" in {
    val description = readFromArray(os.read.bytes(os.pwd / "containers.json"))
    val mkdirs = description.mkDirs()

    mkdirs should contain theSameElementsAs List(
      List("mkdir", "-p", "/var/lib/vz/data"),
      List("chown", "-R", "1000:1000", "/var/lib/vz/data"),
      List("chmod", "-R", "022", "/var/lib/vz/data"),
      List("mkdir", "-p", "/var/lib/vz/config"),
      List("chown", "-R", "180:180", "/var/lib/vz/config"),
      List("chmod", "-R", "764", "/var/lib/vz/config")
    )
  }

  "GithubTemplate" should "give url to download asset" in {
    val templateLatest = Template.Github("bar.tar.xz", "owner/repo", "latest")
    templateLatest.url shouldEqual "owner/repo/releases/latest"

    val templateTag = Template.Github("bar.tar.xz", "owner/repo", "v1.0")
    templateTag.url shouldEqual "owner/repo/releases/tags/v1.0"
  }

}
