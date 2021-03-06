/*
 * Copyright 2013 Eike Kettner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import _root_.sbtassembly.Plugin.AssemblyKeys
import sbt._
import Keys._
import Dependencies._
import sbt.ScmInfo
import scala.Some
import sbtassembly.Plugin._
import AssemblyKeys._

object Resolvers {
  val eknet = "eknet.org" at "https://eknet.org/maven2"
}
object Version {
  val bouncyCastle = "1.46"
  val slf4j = "1.7.2"
  val logback = "1.0.9"
  val scalaTest = "1.9.1"
  val grizzled = "0.6.9"
  val scala = "2.9.2"
  val servlet = "3.0.1"
  val publet = "1.2.0"
  val scue = "0.2.0"
}

object Dependencies {
  val publetQuartz = "org.eknet.publet.quartz" %% "publet-quartz" % "0.1.0"  exclude("org.restlet.jse", "org.restlet.ext.fileupload") exclude("org.restlet.jse", "org.restlet")
  val publetAppDev = "org.eknet.publet" %% "publet-app" % Version.publet  exclude("org.restlet.jse", "org.restlet.ext.fileupload") exclude("org.restlet.jse", "org.restlet")
  val publetAppPlugin = publetAppDev % "publet"  exclude("org.restlet.jse", "org.restlet.ext.fileupload") exclude("org.restlet.jse", "org.restlet")
  val publetQuartzPlugin = publetQuartz % "publet" exclude("org.restlet.jse", "org.restlet.ext.fileupload") exclude("org.restlet.jse", "org.restlet")

  val providedDeps = Seq(
    "org.eknet.publet" %% "publet-auth" % Version.publet exclude("org.restlet.jse", "org.restlet.ext.fileupload") exclude("org.restlet.jse", "org.restlet"),
    "org.eknet.publet" %% "publet-web" % Version.publet exclude("org.restlet.jse", "org.restlet.ext.fileupload") exclude("org.restlet.jse", "org.restlet"),
    "org.eknet.publet" %% "publet-webeditor" % Version.publet exclude("org.restlet.jse", "org.restlet.ext.fileupload") exclude("org.restlet.jse", "org.restlet"),
    "org.eknet.publet" %% "publet-ext" % Version.publet exclude("org.restlet.jse", "org.restlet.ext.fileupload") exclude("org.restlet.jse", "org.restlet"),
    "org.eknet.scue" %% "scue" % Version.scue,
    "org.eknet.publet" %% "publet-ext" % Version.publet exclude("org.restlet.jse", "org.restlet.ext.fileupload") exclude("org.restlet.jse", "org.restlet"),
    "org.slf4j" % "jcl-over-slf4j" % Version.slf4j,
    "org.scalatest" %% "scalatest" % Version.scalaTest,
    "org.clapper" %% "grizzled-slf4j" % Version.grizzled exclude("org.slf4j", "slf4j-api"),
    "org.bouncycastle" % "bcprov-jdk16" % Version.bouncyCastle,
    "org.bouncycastle" % "bcmail-jdk16" % Version.bouncyCastle,
//    "javax.servlet" % "javax.servlet-api" % Version.servlet,
    publetQuartz
  ) map (_ % "provided")

  val testDeps = Seq(
    "org.slf4j" % "slf4j-simple" % Version.slf4j
  ) map (_ % "test")

}

// Root Module 

object RootBuild extends Build {
  import org.eknet.publet.sbt.PubletPlugin

  lazy val root = Project(
    id = "publet-sharry",
    base = file("."),
    settings = buildSettings
  )

  lazy val runner = Project(
    id = "publet-runner",
    base = file("runner"),
    settings = Project.defaultSettings ++ Seq(
      name := "publet-runner",
      libraryDependencies ++= Seq(publetAppDev, publetQuartz)
    )
  ) dependsOn (root)

  val exludedFiles = Set(
    "javax.servlet-3.0.0.v201112011016.jar"
  )
  def isExcluded(n: String) = exludedFiles contains (n)

  val deps = Seq(publetAppPlugin, publetQuartzPlugin) ++ providedDeps ++ testDeps

  val buildSettings = Project.defaultSettings ++ assemblySettings ++ ReflectPlugin.allSettings ++ Seq(
    name := "publet-sharry",
    ReflectPlugin.reflectPackage := "org.eknet.publet.sharry",
    sourceGenerators in Compile <+= ReflectPlugin.reflect,
    assembleArtifact in packageScala := false,
    excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
      cp filter { f => isExcluded(f.data.getName) }
    },
    libraryDependencies ++= deps
  ) ++ PubletPlugin.publetSettings

  override lazy val settings = super.settings ++ Seq(
    version := "0.2.0-SNAPSHOT",
    organization := "org.eknet.publet",
    licenses := Seq(("ASL2", new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))),
    scmInfo := Some(ScmInfo(new URL("https://eknet.org/gitr/?r=eike/publet-sharry.git"), "scm:git:https://eknet.org/git/eike/publet-sharry.git")),
    scalaVersion := Version.scala,
    exportJars := true,
    publishMavenStyle := true,
    publishTo := Some("eknet-maven2" at "https://eknet.org/maven2"),
    scalacOptions ++= Seq("-unchecked", "-deprecation"),
    resolvers += Resolvers.eknet,
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    pomIncludeRepository := (_ => false),

    // see https://jira.codehaus.org/browse/JETTY-1493
    ivyXML := <dependency org="org.eclipse.jetty.orbit" name="javax.servlet" rev="3.0.0.v201112011016">
         <artifact name="javax.servlet" type="orbit" ext="jar"/>
       </dependency>

  )
}


