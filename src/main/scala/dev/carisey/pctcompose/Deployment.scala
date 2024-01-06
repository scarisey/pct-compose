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

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
sealed trait Deployment {
  val container: Container
}
final case class NotDeployed(container: Container) extends Deployment
final case class Deployed(id: String, container: Container, version: Version) extends Deployment
object Deployment {

  private val tagPattern = """([a-zA-Z0-9\._]+)-([a-zA-Z0-9\._]+)""".r
  def extractTags(line: String): Map[String, String] = {
    tagPattern
      .findAllMatchIn(line)
      .toList
      .map { case tagPattern(key, value) =>
        key -> value
      }
      .toMap
  }
  def zipContainersWithTags(description: Description, tags: List[(String, Map[String, String])]): List[Deployment] =
    description.containers.map { c =>
      (tags
        .flatMap { case (id, kv) =>
          for {
            template <- kv.get("template")
            if template == description.name
            name <- kv.get("name")
            if c.hostname == name
            actualVersion <- kv.get("version")
          } yield Deployed(id, c, actualVersion.refine)
        })
        .headOption
        .getOrElse(NotDeployed(c))
    }

  def getActualDeployment(description: Description): List[Deployment] = {
    val cmds = List(
      OsCommand("pct", "list"),
      OsCommand("cut", "-d", " ", "-f1"),
      OsCommand("tail", "-n", "+2")
    )

    val containerIds = OsCommand.execute(cmds)
    println(containerIds)

    val configTags = containerIds
      .map(id =>
        id -> OsCommand
          .execute(
            OsCommand("pct", "config", id)
              .map(xs => xs.filter(_.contains("tags:")))
          )
          .headOption
          .getOrElse("")
      )
      .map((id, tags) => id -> extractTags(tags))
    zipContainersWithTags(description, configTags)
  }

}
