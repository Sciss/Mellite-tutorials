lazy val root = project.in(file("."))
  .enablePlugins(ParadoxPlugin)
  .settings(
    name := "Mellite tutorial - Paul Stretch",
    paradoxProperties /* in Paradox */ ++= Map(
      "image.base_url"       -> "assets/images",
      "github.base_url"      -> "https://github.com/Sciss/Mellite-website",
      "snip.sp_tut.base_dir" -> s"${baseDirectory.value}/snippets/src/main/scala/de/sciss/soundprocesses/tutorial"
    ),
  )

/*
val snippets = (project in file("snippets"))
  // .dependsOn(lMellite)
  .settings(
    name := s"$baseName-Snippets"
  )
*/

