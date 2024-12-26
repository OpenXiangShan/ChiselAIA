package aia


import chisel3._
import org.chipsalliance.cde.config.{Field, Parameters}



case object IMSICParameKey extends Field[IMSICParameters]

case class IMSICParameters
(
  HasTEEIMSIC: Boolean = false
)

//trait HasIMSICParameters extends IMSICParameters
trait HasIMSICParameters {
  implicit val p: Parameters

  val IMSICParam = p(IMSICParameKey)
  val GHasTEEIMSIC = IMSICParam.HasTEEIMSIC
}

