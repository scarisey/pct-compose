package dev.carisey.pctcompose

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import dev.carisey.pctcompose.Deployment.extractTags
import io.github.iltotore.iron.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import PctCompose.descriptionCodec
import dev.carisey.pctcompose.LxcTemplate.*

object DeploymentTest {
  def readContainersJson(): Description = readFromStream(
    Thread.currentThread().getContextClassLoader.getResourceAsStream("containers.json")
  )
}
class DeploymentTest extends AnyFlatSpec with Matchers {
  "extractTags" should "return parsed tags from pct config tags output" in {
    val line = "tags: a_tag;another_tag;g-h;k-v;machin.bidule;name-inputs;template-test_template;version-file"
    extractTags(line) shouldEqual Map(
      "name" -> "inputs",
      "version" -> "file",
      "g" -> "h",
      "template" -> "test_template",
      "k" -> "v"
    )
  }

  "zipContainersWithTags" should "combine corresponding container with its tags, if there is" in {
    val tags = List(
      "1" ->
        Map(
          "g" -> "h",
          "k" -> "v",
          "name" -> "host1",
          "template" -> "test_template",
          "version" -> "file"
        ),
      "2" -> Map(
        "name" -> "host2",
        "template" -> "test_template",
        "version" -> "latest"
      )
    )
    val description: Description = DeploymentTest.readContainersJson()
    Deployment.zipContainersWithTags(description, tags) shouldEqual List(
      Deployed(
        id = "1",
        container = Container(
          hostname = "host1",
          volumes = Set("data"),
          cores = 4,
          memory = 2048,
          diskSize = 8,
          services = Set("ssh1"),
          template = File(file = "/var/lib/vz/template/cache/foo.tar.xz")
        ),
        version = "file"
      ),
      Deployed(
        id = "2",
        container = Container(
          hostname = "host2",
          volumes = Set("data", "config"),
          cores = 1,
          memory = 512,
          diskSize = 24,
          services = Set("ssh2", "webhook"),
          template = Github(file = "bar.tar.xz", repo = "scarisey/my-proxmox", tag = "latest")
        ),
        version = "latest"
      )
    )
  }

  "zipContainersWithTags" should "return missing containers too" in {
    val tags = List.empty
    val description: Description = DeploymentTest.readContainersJson()
    Deployment.zipContainersWithTags(description, tags) shouldEqual List(
      NotDeployed(
        container = Container(
          hostname = "host1",
          volumes = Set("data"),
          cores = 4,
          memory = 2048,
          diskSize = 8,
          services = Set("ssh1"),
          template = File(file = "/var/lib/vz/template/cache/foo.tar.xz")
        )
      ),
      NotDeployed(
        container = Container(
          hostname = "host2",
          volumes = Set("data", "config"),
          cores = 1,
          memory = 512,
          diskSize = 24,
          services = Set("ssh2", "webhook"),
          template = Github(file = "bar.tar.xz", repo = "scarisey/my-proxmox", tag = "latest")
        )
      )
    )

  }
}
