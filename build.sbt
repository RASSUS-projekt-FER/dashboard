name := "dashboard"

version := "1.0"

organization := "hr.fer.rassus.dashboard" 

libraryDependencies ++= {
  val akkaVersion = "2.4.12"
  Seq(
    "com.typesafe.akka" %% "akka-actor"      % akkaVersion, 
    "com.typesafe.akka" %% "akka-http-core"  % "2.4.11", 
    "com.typesafe.akka" %% "akka-http-experimental"  % "2.4.11", 
    "com.typesafe.akka" %% "akka-http-spray-json-experimental"  % "2.4.11", 
    "com.typesafe.akka" %% "akka-slf4j"      % akkaVersion,
    "ch.qos.logback"    %  "logback-classic" % "1.1.3",
    "com.github.wookietreiber" %% "scala-chart" % "latest.integration"
  )
}

// Assembly settings
mainClass in assembly := Some("hr.fer.rassus.dashboard.Main")
