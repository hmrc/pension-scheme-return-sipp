import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.DefaultBuildSettings
import sbt.*

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.5.1"
ThisBuild / scalacOptions ++= Seq("-Werror", "-Wconf:src=routes/.*:s", "-Wconf:msg=Flag.*repeatedly:s")
ThisBuild / scalafmtOnCompile := true

lazy val microservice = Project("pension-scheme-return-sipp", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    RoutesKeys.routesImport ++= Seq(
      "uk.gov.hmrc.pensionschemereturnsipp.models._",
      "uk.gov.hmrc.pensionschemereturnsipp.models.JourneyType._",
      "uk.gov.hmrc.pensionschemereturnsipp.config.Binders._"
    ),
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
  )
  .settings(CodeCoverageSettings.settings)
  .settings(PlayKeys.playDefaultPort := 10704)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test)
