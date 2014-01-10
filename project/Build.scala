import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "kakel"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      jdbc, anorm,
      "com.netflix.rxjava" % "rxjava-scala" % "0.15.1"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      // Add your own project settings here      
    )

}
