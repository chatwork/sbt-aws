import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
import sbtrelease._

val sonatypeURL = "https://oss.sonatype.org/service/local/repositories/"

def updateReadmeFile(version: String, readme: String): Unit = {
  val readmeFile = file(readme)
  val newReadme = Predef.augmentString(IO.read(readmeFile)).lines.map { line =>
    val matchReleaseOrSnapshot = line.contains("SNAPSHOT") == version.contains("SNAPSHOT")
    if (line.startsWith("addSbtPlugin") && matchReleaseOrSnapshot) {
      line.replaceAll("\"\\d\\.\\d\\.\\d(-SNAPSHOT)?\"\\)$", "\"" + version + "\")")
    } else line
  }.mkString("", "\n", "\n")
  IO.write(readmeFile, newReadme)
}


val updateReadme = { state: State =>
  val extracted = Project.extract(state)
  val scalaV = extracted get scalaBinaryVersion
  val v = extracted get version
  val org = extracted get organization
  val n = extracted get name
  val snapshotOrRelease = if (extracted get isSnapshot) "snapshots" else "releases"
  val readmeFiles = Seq(
    "README.md",
    "sbt-aws-cfn/README.md",
    "sbt-aws-eb/README.md",
    "sbt-aws-s3/README.md",
    "sbt-aws-s3-resolver/README.md"
  )
  readmeFiles.foreach(readme => updateReadmeFile(v, readme))
  val git = new Git(extracted get baseDirectory)
  readmeFiles.foreach{ readme => 
    git.add(readme) ! state.log 
    git.commit("update " + readme) ! state.log
  }
  "git diff HEAD^" ! state.log
  state
}

commands += Command.command("updateReadme")(updateReadme)

val updateReadmeProcess: ReleaseStep = updateReadme

releaseCrossBuild := true

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  updateReadmeProcess,
  tagRelease,
  ReleaseStep(
    action = { state =>
      val extracted = Project extract state
      extracted.runAggregated(PgpKeys.publishSigned in Global in extracted.get(thisProjectRef), state)
    },
    enableCrossBuild = true
  ),
  setNextVersion,
  commitNextVersion,
  updateReadmeProcess,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
  pushChanges
)
