import sbt.*

object AppDependencies {

  private val bootstrapVersion = "9.13.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "org.typelevel"                 %% "cats-core"                  % "2.12.0",
    "com.networknt"                 %  "json-schema-validator"      % "1.5.1" exclude("org.slf4j", "slf4j-api"),
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.17.2",
    "com.beachape"                  %% "enumeratum-play-json"       % "1.8.1",
    "io.scalaland"                  %% "chimney"                    % "1.3.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-test-play-30"     % bootstrapVersion,
    "org.scalatestplus"             %% "scalacheck-1-17"            % "3.2.18.0",
    "org.scalacheck"                %% "scalacheck"                 % "1.18.0",
    "org.scalatestplus"             %% "mockito-4-11"               % "3.2.18.0",
    "com.softwaremill.diffx"        %% "diffx-scalatest-should"     % "0.9.0",
    "com.vladsch.flexmark"          % "flexmark-all"                % "0.64.8",
  ).map(_ % Test)
}
