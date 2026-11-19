import mill._
import scalalib._
import $file.`rocket-chip`.common
import $file.`rocket-chip`.cde.common
import $file.`rocket-chip`.hardfloat.common

object v {
  val scala = "2.13.18"
  val chiselCrossVersions = Map(
    "xiangshan" -> (
      mvn"cc.xiangshan::chisel:7.16.0-xiangshan-2",
      mvn"cc.xiangshan:::chisel-plugin:7.16.0-xiangshan-2"
    ),
    "upstream" -> (
      mvn"org.chipsalliance::chisel:7.16.0",
      mvn"org.chipsalliance:::chisel-plugin:7.16.0"
    )
  )
  def chiselKeys = chiselCrossVersions.keys.toSeq
}

val pwd = os.Path(sys.env("MILL_WORKSPACE_ROOT"))

trait HasChisel extends SbtModule with Cross.Module[String] {
  def chiselModule: Option[ScalaModule] = None
  def chiselPluginJar: T[Option[PathRef]] = None
  def chiselIvy: Option[Dep] = Some(v.chiselCrossVersions(crossValue)._1)
  def chiselPluginIvy: Option[Dep] = Some(v.chiselCrossVersions(crossValue)._2)
  def sourcecodeIvy = mvn"com.lihaoyi::sourcecode:0.4.4"
  override def scalaVersion = v.scala
  override def scalacOptions = super.scalacOptions() ++
    Agg("-language:reflectiveCalls", "-Ymacro-annotations", "-Ytasty-reader")
  override def ivyDeps = super.ivyDeps() ++ Agg(chiselIvy.get) ++ Agg(sourcecodeIvy)
  override def scalacPluginIvyDeps = super.scalacPluginIvyDeps() ++ Agg(chiselPluginIvy.get)
}

trait Utility extends SbtModule with HasChisel {
  override def millSourcePath = pwd / "Utility"
  override def moduleDeps = super.moduleDeps ++ Seq(rocketchip(crossValue))
}

trait RocketChip extends build_.`rocket-chip`.common.RocketChipModule with HasChisel {
  def scalaVersion: T[String] = T(v.scala)
  override def millSourcePath = pwd / "rocket-chip"
  def macrosModule = macros(crossValue)
  def hardfloatModule = hardfloat(crossValue)
  def cdeModule = cde(crossValue)
  def mainargsIvy = mvn"com.lihaoyi::mainargs:0.7.0"
  def json4sJacksonIvy = mvn"org.json4s::json4s-jackson:4.0.7"

  trait Macros extends build_.`rocket-chip`.common.MacrosModule with HasChisel {
    def scalaVersion: T[String] = T(v.scala)
    def scalaReflectIvy = mvn"org.scala-lang:scala-reflect:${v.scala}"
  }

  object macros extends Cross[Macros](v.chiselKeys)

  trait HardFloat extends build_.`rocket-chip`.hardfloat.common.HardfloatModule with HasChisel {
    def scalaVersion: T[String] = T(v.scala)
    override def millSourcePath = pwd / "rocket-chip" / "hardfloat" / "hardfloat"
  }

  object hardfloat extends Cross[HardFloat](v.chiselKeys)

  trait CDE extends build_.`rocket-chip`.cde.common.CDEModule with Cross.Module[String] {
    def scalaVersion: T[String] = T(v.scala)
    override def millSourcePath = pwd / "rocket-chip" / "cde" / "cde"
  }

  object cde extends Cross[CDE](v.chiselKeys)
}

object rocketchip extends Cross[RocketChip](v.chiselKeys)

object utility extends Cross[Utility](v.chiselKeys)

trait ChiselAIA extends SbtModule with Cross.Module[String] { m =>
  override def millSourcePath = pwd
  override def scalaVersion = v.scala
  override def scalacOptions = Seq(
    "-language:reflectiveCalls",
    "-deprecation",
    "-feature",
    "-Xcheckinit",
  )
  override def ivyDeps = Agg(v.chiselCrossVersions(crossValue)._1)
  override def scalacPluginIvyDeps = Agg(v.chiselCrossVersions(crossValue)._2)
  override def moduleDeps = super.moduleDeps ++ Seq(
    rocketchip(crossValue),
    utility(crossValue),
  )
  def rocketModule = rocketchip(crossValue)
  def utilityModule = utility(crossValue)
}

trait TLAIA extends ChiselAIA { def mainClass = Some("aia.TLAIA") }
object TLAIA extends Cross[TLAIA](v.chiselKeys)

trait AXI4AIA extends ChiselAIA { def mainClass = Some("aia.AXI4AIA") }
object AXI4AIA extends Cross[AXI4AIA](v.chiselKeys)
