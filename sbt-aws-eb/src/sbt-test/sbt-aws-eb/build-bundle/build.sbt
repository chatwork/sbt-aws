
ebBundleTargetFiles in aws := {
  val base = baseDirectory.value
  Seq(
    (base / "Dockerfile", "Dockerfile")
  )
}

ebBundleFileName in aws := "test.zip"
