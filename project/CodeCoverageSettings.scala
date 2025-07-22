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
    "uk.gov.hmrc.pensionschemereturnsipp.config.Binders",
    "uk.gov.hmrc.pensionschemereturnsipp.config.Crypto",
    "uk.gov.hmrc.pensionschemereturnsipp.config.CryptoImpl",
    "uk.gov.hmrc.pensionschemereturnsipp.config.Module",
    "uk.gov.hmrc.pensionschemereturnsipp.models.*"
  )

  val settings: Seq[Setting[?]] = Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
