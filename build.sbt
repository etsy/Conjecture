import sbt._

name := "conjecture"

version := "0.3.1-SNAPSHOT"

organization := "com.etsy"

scalaVersion := "2.11.11"
crossScalaVersions := Seq("2.11.11", "2.12.4")

scalacOptions ++= Seq("-unchecked", "-deprecation")

//Because some of our (legal!) java code confuses scaladoc, we must skip it for 2.12
//See: https://github.com/scala/bug/issues/10723
scalacOptions in (Compile, doc) += {if(scalaBinaryVersion.value == "2.12") "-no-java-comments" else ""}

javacOptions ++= Seq("-Xlint:none", "-source", "1.7", "-target", "1.7")

compileOrder := CompileOrder.JavaThenScala

resolvers ++= {
  Seq(
    "Concurrent Maven Repo" at "http://conjars.org/repo"
  )
}

libraryDependencies ++= Seq(
  "cascading" % "cascading-core" % "2.6.1",
  "cascading" % "cascading-local" % "2.6.1" exclude("com.google.guava", "guava"),
  "cascading" % "cascading-hadoop" % "2.6.1",
  "com.google.code.gson" % "gson" % "2.2.2",
  "com.twitter" %% "algebird-core" % "0.13.0" excludeAll ExclusionRule(organization="org.scala-lang", name="scala-library"),
  "com.twitter" %% "scalding-core" % "0.17.4" excludeAll ExclusionRule(organization="org.scala-lang", name="scala-library"),
  "commons-lang" % "commons-lang" % "2.4",
  "com.joestelmach" % "natty" % "0.7",
  "io.spray" %% "spray-json" % "1.3.2" excludeAll ExclusionRule(organization="org.scala-lang", name="scala-library"),
  "com.google.guava" % "guava" % "13.0.1",
  "org.apache.commons" % "commons-math3" % "3.2",
  "org.apache.hadoop" % "hadoop-common" % "2.5.0" excludeAll(
    ExclusionRule(organization="commons-daemon", name="commons-daemon"),
    ExclusionRule(organization="com.google.guava", name="guava")
    ),
  "org.apache.hadoop" % "hadoop-hdfs" % "2.5.0" excludeAll(
    ExclusionRule(organization="commons-daemon", name="commons-daemon"),
    ExclusionRule(organization="com.google.guava", name="guava")
    ),
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "net.sf.trove4j" % "trove4j" % "3.0.3",
  "com.novocode" % "junit-interface" % "0.10" % "test"
)

parallelExecution in Test := false

publishArtifact in Test := false

xerial.sbt.Sonatype.sonatypeSettings

publishTo := {
  if (System.getProperty("release") != null) {
    publishTo.value
  } else {
    val v = version.value
    val archivaURL = "http://ivy.etsycorp.com/repository"
    if (v.trim.endsWith("SNAPSHOT")) {
      Some("publish-snapshots" at (archivaURL + "/snapshots"))
    } else {
      Some("publish-releases"  at (archivaURL + "/internal"))
    }
  }
}

publishMavenStyle := true

overridePublishBothSettings

pomIncludeRepository := { x => false }

pomExtra := <url>https://github.com/etsy/Conjecture</url>
  <licenses>
    <license>
      <name>MIT License</name>
      <url>http://opensource.org/licenses/MIT</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:etsy/Conjecture.git</url>
    <connection>scm:git:git@github.com:etsy/Conjecture.git</connection>
  </scm>
  <developers>
    <developer>
      <id>jattenberg</id>
      <name>Josh Attenberg</name>
      <url>github.com/jattenberg</url>
    </developer>
    <developer>
      <id>rjhall</id>
      <name>Rob Hall</name>
      <url>github.com/rjhall</url>
    </developer>
  </developers>


pomIncludeRepository := { _ => false }

// Uncomment if you don't want to run all the tests before building assembly
// test in assembly := {}

// Janino includes a broken signature, and is not needed:
assemblyExcludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  val excludes = Set("jsp-api-2.1-6.1.14.jar", "jsp-2.1-6.1.14.jar",
    "jasper-compiler-5.5.12.jar", "janino-2.5.16.jar")
  cp filter { jar => excludes(jar.data.getName)}
}

// Some of these files have duplicates, let's ignore:
assemblyMergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
{
  case s if s.endsWith(".class") => MergeStrategy.last
  case s if s.endsWith("project.clj") => MergeStrategy.concat
  case s if s.endsWith(".html") => MergeStrategy.last
  case s if s.contains("servlet") => MergeStrategy.last
  case x => old(x)
}
}
