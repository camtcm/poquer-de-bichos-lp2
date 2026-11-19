val versionPekko     = "1.1.3"
val versionPekkoHttp = "1.1.0"

lazy val root = (project in file("."))
  .settings(
    name         := "poquer-de-bichos-avance2",
    version      := "0.5.0",
    scalaVersion := "3.3.6",
    scalacOptions ++= Seq("-deprecation", "-feature"),
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor-typed"  % versionPekko,
      "org.apache.pekko" %% "pekko-stream-typed" % versionPekko,
      "org.apache.pekko" %% "pekko-http"         % versionPekkoHttp,
      "com.lihaoyi"      %% "ujson"              % "4.1.0",
      "org.slf4j"         % "slf4j-simple"       % "2.0.16"
    ),
    assembly / mainClass       := Some("bichos.Main"),
    assembly / assemblyJarName := "poquer-de-bichos.jar",
    assembly / assemblyMergeStrategy := {
      case "reference.conf"                              => MergeStrategy.concat
      case ruta if ruta.endsWith("module-info.class")     => MergeStrategy.discard
      case ruta if ruta.startsWith("META-INF/services/") => MergeStrategy.concat
      case ruta if ruta.startsWith("META-INF/")          => MergeStrategy.discard
      case _                                             => MergeStrategy.first
    }
  )
