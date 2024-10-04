import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  private val excludedPackages: Seq[String] = Seq(
    "<empty>",
    "Reverse.*",
    "uk.gov.hmrc.BuildInfo",
    "app.*",
    "prod.*",
    ".*Routes.*",
    "testOnly.*",
    "testOnlyDoNotUseInAppConf.*",
    "uk.gov.hmrc.pensionschemereturnsipp.config.AppConfig",
    "uk.gov.hmrc.pensionschemereturnsipp.config.Module",
    "uk.gov.hmrc.pensionschemereturnsipp.models.*",
    "uk.gov.hmrc.pensionschemereturnsipp.utils.*",
    "uk.gov.hmrc.pensionschemereturnsipp.auth.*",
    "uk.gov.hmrc.pensionschemereturnsipp.validators.*"
  )

  val settings: Seq[Setting[?]] = Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 60, // TODO -> needs to be increased!
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
