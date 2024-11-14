import scala.collection.Seq

name := "kamran-data-pipeline"
version := "0.1"
scalaVersion := "2.13.15"

val akkaDependencies = Seq(
  "akka-actor",
  "akka-discovery",
  "akka-protobuf-v3",
  "akka-stream",
  "akka-testkit",
  "akka-slf4j",
  "akka-stream-testkit"
).map("com.typesafe.akka" %% _ % "2.6.20") ++ Seq("akka-http", "akka-http-core").map(
  "com.typesafe.akka" %% _ % "10.2.10"
)

lazy val root = (project in file("."))
  .aggregate(dataEmitter) // Include the data-emitter module
  .settings(
    libraryDependencies ++=
      akkaDependencies ++
        Seq(
          "io.circe" %% "circe-core" % "0.14.1",
          "io.circe" %% "circe-generic" % "0.14.1",
          "io.circe" %% "circe-parser" % "0.14.1",
          "de.heikoseeberger" %% "akka-http-circe" % "1.38.2"
        )
  )

lazy val dataEmitter = Project("data-emitter", file("data-emitter"))
  .settings(
    name := "data-emitter",
    scalaVersion := "2.13.8",
    libraryDependencies ++=
      akkaDependencies ++ Seq(
        "io.circe" %% "circe-core" % "0.14.1",
        "io.circe" %% "circe-generic" % "0.14.1",
        "io.circe" %% "circe-parser" % "0.14.1",
        "de.heikoseeberger" %% "akka-http-circe" % "1.38.2"
      )
  )
