import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "socketplay"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    "com.typesafe.akka" % "akka-agent_2.10" % "2.1.4",
    "io.spray" % "spray-can" % "1.1.0",
    "io.spray" % "spray-httpx" % "1.1.0",
    "org.mongodb" %% "casbah" % "2.5.0"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
		 
      resolvers += (
          "Local Maven Repository" at "file:///"+Path.userHome.absolutePath+"/.m2/repository"
      ),
      resolvers += ("spray repo" at "http://repo.spray.io")
      
  )

}
