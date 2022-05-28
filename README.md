# Mellite-tutorials

[![Build Status](https://travis-ci.org/Sciss/Mellite-tutorials.svg?branch=master)](https://travis-ci.org/Sciss/Mellite-tutorials)

## statement

A project that intends to develop reproducible code and screenshots for tutorials for
[Mellite](https://sciss.de/mellite). This project (C)opyright 2019–2022 by Hanns Holger Rutz. 
All rights reserved. This project is released under the 
[GNU Affero General Public License](https://raw.github.com/Sciss/Mellite-tutorials/master/LICENSE) v3+ 
and comes with absolutely no warranties. To contact the author, send an email to
`contact at sciss.de`.

## requirements / installation

This project compiles using [sbt](http://www.scala-sbt.org/).

## running / settings

For taking the reproducible screenshots, I use the following settings:

- desktop is Gnome wit Adwaita dark and 1920x1080 resolution
- jdk is 11
- code font is 'Noto Mono' 15pt, no stretch, 110% line spacing
- look and feel is Submin Light

The screenshots are produced by running `TutorialPaulStretchShots`; make sure to either delete
the old screenshots or set `overwriteSnaps` to `true`.

Converting DOT files

- `dot -Tpng tut-paulstretch-diamond-problem.dot -o src/main/paradox/assets/images/tut-paulstretch-diamond-problem.png`

For creating the final workspace, run `MkPaulStretchWorkspace`.

## to-do

- use the snippet function of paradox to validate the source code embedded in the markdown documents.
