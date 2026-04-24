import mill._
import scalalib._
import $file.`rocket-chip`.common
import $file.`rocket-chip`.cde.common
import $file.`rocket-chip`.hardfloat.common

val defaultScalaVersion = "2.13.14"
val pwd = os.Path(sys.env("MILL_WORKSPACE_ROOT"))
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

object utility extends SbtModule with HasChisel {
  override def ivyDeps = Agg(defaultVersions("chisel"))
  override def millSourcePath = pwd / "Utility"
  override def moduleDeps = super.moduleDeps ++ Seq(rocketchip)
}

object rocketchip
extends $file.`rocket-chip`.common.RocketChipModule
with HasChisel {
  def scalaVersion: T[String] = T(defaultScalaVersion)
  override def millSourcePath = pwd / "rocket-chip"
  def macrosModule = macros
  def hardfloatModule = hardfloat
  def cdeModule = cde
  def mainargsIvy = ivy"com.lihaoyi::mainargs:0.7.0"
  def json4sJacksonIvy = ivy"org.json4s::json4s-jackson:4.0.7"

  object macros extends Macros
  trait Macros extends $file.`rocket-chip`.common.MacrosModule with SbtModule {
    def scalaVersion: T[String] = T(defaultScalaVersion)
    def scalaReflectIvy = ivy"org.scala-lang:scala-reflect:${defaultScalaVersion}"
  }

  object hardfloat extends $file.`rocket-chip`.hardfloat.common.HardfloatModule with HasChisel {
    def scalaVersion: T[String] = T(defaultScalaVersion)
    override def millSourcePath = pwd / "rocket-chip" / "hardfloat" / "hardfloat"
  }

  object cde extends $file.`rocket-chip`.cde.common.CDEModule with ScalaModule {
    def scalaVersion: T[String] = T(defaultScalaVersion)
    override def millSourcePath = pwd / "rocket-chip" / "cde" / "cde"
  }
}

class ChiselAIA extends SbtModule { m =>
  override def millSourcePath = pwd
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
    utility,
  )
  def rocketModule = rocketchip
}
object TLAIA extends ChiselAIA { def mainClass = Some("aia.TLAIA") }
object AXI4AIA extends ChiselAIA { def mainClass = Some("aia.AXI4AIA") }
