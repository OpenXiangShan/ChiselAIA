import mill._
import scalalib._
import $file.`rocket-chip`.common
import $file.`rocket-chip`.cde.common
import $file.`rocket-chip`.hardfloat.build

val defaultScalaVersion = "2.13.14"
def defaultVersions = Map(
  "chisel"        -> ivy"org.chipsalliance::chisel:6.5.0",
  "chisel-plugin" -> ivy"org.chipsalliance:::chisel-plugin:6.5.0",
  "chiseltest"    -> ivy"edu.berkeley.cs::chiseltest:6.0.0"
)

trait HasChisel extends SbtModule {
  def chiselModule: Option[ScalaModule] = None
  def chiselPluginJar: T[Option[PathRef]] = None
  def chiselIvy: Option[Dep] = Some(defaultVersions("chisel"))
  def chiselPluginIvy: Option[Dep] = Some(defaultVersions("chisel-plugin"))
  override def scalaVersion = defaultScalaVersion
  override def scalacOptions = super.scalacOptions() ++
    Agg("-language:reflectiveCalls", "-Ymacro-annotations", "-Ytasty-reader")
  override def ivyDeps = super.ivyDeps() ++ Agg(chiselIvy.get)
  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(chiselPluginIvy.get)
}

object xsutils extends SbtModule with HasChisel {
  override def ivyDeps = Agg(defaultVersions("chisel"))
  override def millSourcePath = os.pwd / "xs-utils"
  override def moduleDeps = super.moduleDeps ++ Seq(rocketchip)
}

object rocketchip
extends millbuild.`rocket-chip`.common.RocketChipModule
with HasChisel {
  def scalaVersion: T[String] = T(defaultScalaVersion)
  override def millSourcePath = os.pwd / "rocket-chip"
  def macrosModule = macros
  def hardfloatModule = hardfloat
  def cdeModule = cde
  def mainargsIvy = ivy"com.lihaoyi::mainargs:0.7.0"
  def json4sJacksonIvy = ivy"org.json4s::json4s-jackson:4.0.7"

  object macros extends Macros
  trait Macros extends millbuild.`rocket-chip`.common.MacrosModule with SbtModule {
    def scalaVersion: T[String] = T(defaultScalaVersion)
    def scalaReflectIvy = ivy"org.scala-lang:scala-reflect:${defaultScalaVersion}"
  }

  object hardfloat extends millbuild.`rocket-chip`.hardfloat.common.HardfloatModule with HasChisel {
    def scalaVersion: T[String] = T(defaultScalaVersion)
    override def millSourcePath = os.pwd / "rocket-chip" / "hardfloat" / "hardfloat"
  }

  object cde extends millbuild.`rocket-chip`.cde.common.CDEModule with ScalaModule {
    def scalaVersion: T[String] = T(defaultScalaVersion)
    override def millSourcePath = os.pwd / "rocket-chip" / "cde" / "cde"
  }
}

object OpenAIA extends SbtModule { m =>
  override def millSourcePath = os.pwd
  override def scalaVersion = defaultScalaVersion
  override def scalacOptions = Seq(
    "-language:reflectiveCalls",
    "-deprecation",
    "-feature",
    "-Xcheckinit",
  )
  override def ivyDeps = Agg(defaultVersions("chisel"))
  override def scalacPluginIvyDeps = Agg(defaultVersions("chisel-plugin"))
  override def moduleDeps = super.moduleDeps ++ Seq(
    rocketchip,
    xsutils,
  )
  def rocketModule = rocketchip
}
