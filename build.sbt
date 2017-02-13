name := "akka-streams-custom-stages"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies ++= {
  val akkaV = "2.4.17"
  val akka = "com.typesafe.akka"
  Seq(
    akka %% "akka-actor" % akkaV,
    akka %% "akka-slf4j" % akkaV,
    akka %% "akka-stream" % akkaV,
    akka %% "akka-stream-testkit" % akkaV,
    akka %% "akka-testkit" % akkaV
  )
}