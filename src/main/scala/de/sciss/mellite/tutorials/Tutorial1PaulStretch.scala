package de.sciss.mellite.tutorials

import scala.concurrent.Future

object Tutorial1PaulStretch extends Tutorial {
  def main(args: Array[String]): Unit =
    startMellite()

  def started(): Future[Unit] =
    for {
      _ <- delay()
      _ <- selectMenu(mainFrame, "File", "New", "Workspace...")
      _ <- snapWindow(mainFrame, "main-frame")
    } yield ()
}
