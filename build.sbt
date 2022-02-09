enablePlugins(JavaAppPackaging)

val Versions = new {
  val http4s = "0.23.7"
  // Make sure to match the circe version with the one provided by http4s.
  val circe = "0.14.1"
  val pureconfig = "0.17.1"
  val doobie = "1.0.0-RC1"
  val flyway = "8.4.0"
  val logback = "1.2.10"
  val scalaLogging = "3.9.4"
}

lazy val scalafmtSettings = Seq(
  scalafmtOnCompile := true
)

lazy val herokuSettings = Seq(
  Compile / herokuAppName := "food-survey-backend",
  Compile / herokuJdkVersion := "11",
  Compile / herokuProcessTypes := Map(
    "web" -> "target/universal/stage/bin/japanese-food-survey-backend"
  )
)

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-source:future",
  "-Ykind-projector:underscores"
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "japanese-food-survey-backend",
    version := "0.1.0",
    scalaVersion := "3.1.0",
    scalacOptions ++= compilerOptions,
    scalafmtSettings,
    herokuSettings,
    libraryDependencies :=
      libraryDependencies.value
        // Remove scala-compiler added by Heroku sbt plugin.
        .filterNot { x =>
          x.organization == "org.scala-lang" && x.name == "scala-compiler"
        } ++ Seq(
        "org.http4s"                 %% "http4s-blaze-server" % Versions.http4s,
        "org.http4s"                 %% "http4s-dsl"          % Versions.http4s,
        "org.http4s"                 %% "http4s-circe"        % Versions.http4s,
        "com.github.pureconfig"      %% "pureconfig-core"     % Versions.pureconfig,
        "org.tpolecat"               %% "doobie-postgres"     % Versions.doobie,
        "org.tpolecat"               %% "doobie-hikari"       % Versions.doobie,
        "org.flywaydb"                % "flyway-core"         % Versions.flyway,
        "ch.qos.logback"              % "logback-classic"     % Versions.logback,
        "com.typesafe.scala-logging" %% "scala-logging"       % Versions.scalaLogging
      )
  )
