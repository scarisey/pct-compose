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
import mainargs.*
import requests.RequestAuth.Bearer
import scala.util.chaining.*
import java.util.UUID
import scala.util.Try

object Assets {

  implicit object PathRead extends TokensReader.Simple[os.Path] {
    def shortName = "path"
    def read(strs: Seq[String]) = Right(os.Path(strs.head, os.pwd))
  }

  given releaseCodec: JsonValueCodec[GithubRelease] = JsonCodecMaker.make

  def downloadAsset(template: LxcTemplate.Github, secret: os.Path)(implicit description: Description): Unit = {
    val token = Bearer(os.read.lines(secret).head)
    val url = template.urlSuffix
    val file = template.file
    val localPath = template.localPath
    println(s"Get release for $file ...")
    val res = Try(requests.get(url = s"https://api.github.com/repos/${url}", auth = token).text())
      .map(readFromString[GithubRelease](_))
      .map(
        _.filter(file)
          .map(asset => (localPath, asset))
          .tap {
            case None => println(s"$file cannot be found at $url")
            case _    => ()
          }
      )
      .recover { case e: Throwable => pprint.pprintln(e); throw e }
      .collect {
        case Some((localPath, release)) => {
          val tempPath = os.Path(s"${localPath}-${UUID.randomUUID().toString()}")
          println(s"Download ${release.name} into ${tempPath}")
          requests
            .get(url = release.url, auth = token, headers = Map("Accept" -> "application/octet-stream"))
            .writeBytesTo(os.write.outputStream(tempPath))
          println(s"Move ${tempPath} into ${localPath}")
          os.move.over(tempPath, os.Path(localPath.toString()))
        }
      }
  }
}
