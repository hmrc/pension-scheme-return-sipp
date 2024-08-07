import sbt.*

object AppDependencies {

  private val bootstrapVersion = "8.5.0"
  private val hmrcMongoVersion = "1.9.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-30"         % hmrcMongoVersion,
    "org.typelevel"                 %% "cats-core"                  % "2.12.0",
    "com.networknt"                 % "json-schema-validator"       % "1.5.0" exclude("org.slf4j", "slf4j-api"),
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.17.2",
    "com.beachape"                  %% "enumeratum-play-json"       % "1.8.1",
    "io.scalaland"                  %% "chimney"                    % "1.3.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-test-play-30"     % bootstrapVersion        % Test,
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-test-play-30"    % hmrcMongoVersion        % Test,
    "org.scalatestplus"             %% "scalacheck-1-17"            % "3.2.18.0"              % Test,
    "org.scalacheck"                %% "scalacheck"                 % "1.18.0"                % Test,
    "org.mockito"                   %% "mockito-scala"              % "1.17.37"               % Test,
    "com.softwaremill.diffx"        %% "diffx-scalatest-should"     % "0.9.0"                 % Test,
    "com.vladsch.flexmark"          % "flexmark-all"                % "0.64.8"                % Test
  )
}
