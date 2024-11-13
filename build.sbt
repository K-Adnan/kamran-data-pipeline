name := "kamran-data-pipeline"
version := "0.1"
scalaVersion := "2.13.15"

lazy val root = (project in file("."))
  .aggregate(dataEmitter)  // Include the data-emitter module
  .settings(
    name := "kamran-data-pipeline"
  )

lazy val dataEmitter = Project("data-emitter", file("data-emitter"))
  .settings(
    name := "data-emitter",
    scalaVersion := "2.13.8"  // Use your preferred Scala version
  )