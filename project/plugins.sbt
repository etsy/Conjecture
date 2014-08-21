resolvers += Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

resolvers ++= Seq(
      "David's Snapshots" at "http://davidharcombe.github.io/snapshots",
      "David's Releases" at "http://davidharcombe.github.io/releases"
)

addSbtPlugin("com.gramercysoftware" % "sbt-multi-publish" % "1.0.1")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.8.3")

addSbtPlugin("no.arktekk.sbt" % "aether-deploy" % "0.9")
