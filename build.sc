import mill._
import scalalib._
import $file.`rocket-chip`.common
import $file.`rocket-chip`.cde.common
import $file.`rocket-chip`.hardfloat.common

val defaultScalaVersion = "2.13.17"
def defaultVersions = Map(
  "chisel"        -> mvn"org.chipsalliance::chisel:7.13.0",
  "chisel-plugin" -> mvn"org.chipsalliance:::chisel-plugin:7.13.0",
)

val pwd = os.Path(sys.env("MILL_WORKSPACE_ROOT"))

trait HasChisel extends SbtModule {
  def chiselModule: Option[ScalaModule] = None
  def chiselPluginJar: T[Option[PathRef]] = None
  def chiselIvy: Option[Dep] = Some(defaultVersions("chisel"))
  def chiselPluginIvy: Option[Dep] = Some(defaultVersions("chisel-plugin"))
  def sourcecodeIvy = mvn"com.lihaoyi::sourcecode:0.4.4"
  override def scalaVersion = defaultScalaVersion
  override def scalacOptions = super.scalacOptions() ++
    Agg("-language:reflectiveCalls", "-Ymacro-annotations", "-Ytasty-reader")
  override def ivyDeps = super.ivyDeps() ++ Agg(chiselIvy.get) ++ Agg(sourcecodeIvy)
  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(chiselPluginIvy.get)
}

object utility extends SbtModule with HasChisel {
  override def ivyDeps = Agg(defaultVersions("chisel"))
  override def millSourcePath = pwd / "Utility"
  override def moduleDeps = super.moduleDeps ++ Seq(rocketchip)
}

object rocketchip
extends build_.`rocket-chip`.common.RocketChipModule
with HasChisel {
  def scalaVersion: T[String] = T(defaultScalaVersion)
  override def millSourcePath = pwd / "rocket-chip"
  def macrosModule = macros
  def hardfloatModule = hardfloat
  def cdeModule = cde
  def mainargsIvy = mvn"com.lihaoyi::mainargs:0.7.0"
  def json4sJacksonIvy = mvn"org.json4s::json4s-jackson:4.0.7"

  object macros extends Macros
  trait Macros extends build_.`rocket-chip`.common.MacrosModule with SbtModule {
    def scalaVersion: T[String] = T(defaultScalaVersion)
    def scalaReflectIvy = mvn"org.scala-lang:scala-reflect:${defaultScalaVersion}"
  }

  object hardfloat extends build_.`rocket-chip`.hardfloat.common.HardfloatModule with HasChisel {
    def scalaVersion: T[String] = T(defaultScalaVersion)
    override def millSourcePath = pwd / "rocket-chip" / "hardfloat" / "hardfloat"
  }

  object cde extends build_.`rocket-chip`.cde.common.CDEModule with ScalaModule {
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
