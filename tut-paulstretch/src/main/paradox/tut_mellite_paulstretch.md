# Mllt1 - Paul Stretch

This tutorial shows you how to develop a basic workspace in Mellite. We will be writing a non-realtime signal
processing program ( _FScape_ ) and wire it up with a simple user interface ( _Widget_ ). We use as example the
_Paul Stretch_ algorithm, named after its creator Nasca Octavian Paul and originally
@link:[a standalone software](http://hypermammut.sourceforge.net/paulstretch/) { open=new }
to perform "extreme" time stretching of sounds and additional spectral transformations. Paul has since released
@link:[a public domain version](https://github.com/paulnasca/paulstretch_python) { open=new } of the stripped down
stretching algorithm written in Python. The idea is very simple but remarkably effective: An input sound is stretched 
by taking "segments" (windows) overlapping in time, calculating their spectra (Fourier transform), randomising 
the phase information, resynthesising the time domains segments and putting them back together with an "overlap-add" 
operation, using a larger stepping size than in the analysis. This is illustrated in the following diagram provided 
by Paul:

![Original Paul Stretch Algorithm](.../paulstretch_steps.png)

## Creating a New Workspace

Assuming that you have downloaded and launched Mellite, the first thing to do is create a new _workspace_.

@@@ note

A workspace in Mellite is the document that contains all objects belonging to a project. Technically, a workspace is 
a database on the file system, taking the form of a directory ending in `.mllt` by convention. You can think of the 
workspace as a PD or Max project, for example their main patch and the related abstractions, or as a SuperCollider
`.scd` document. In contrast to PD, Max, and SuperCollider, manipulations in a workspace are always performed directly 
on the database, so there is no "unsaved" document state and no explicit save operation.

@@@

To create a new workspace, choose _File → New → Workspace…_ from the main window's menu,
and in the subsequent dialog select the default option _Durable_, 
after which you are prompted to choose the name and location of the workspace on your file system:

![Menu Item for New Workspace](.../tut-paulstretch-menu-new-workspace.png)

![Dialog for New Workspace](.../tut-paulstretch-dlg-new-workspace.png)

![File-Chooser for New Workspace](.../tut-paulstretch-filechooser-new-workspace.png)

For example, we could name the workspace `PaulStretchTutorial` and save it inside the documents directory in your
home directory.

@@@ note { title=Tip }

Because all operations are performed on the file system, choosing a location on an SSD is preferable
over a traditional hard disk.

@@@

The top element of a workspace is a so-called _Folder_. It is simply a list of elements, and you can nest folders
inside folders to obtain a tree-like structure. You can think of a folder as a PD or Max patch, only that objects
have no visual position within the patch but are just itemised as a list. Since we just created a new workspace,
that list is initially empty. You can add new objects by clicking on the plus-shaped button and choosing
from the popup menu _Composition → FScape_:

![Popup for New Object](.../tut-paulstretch-popup-folder-new-object.png)

This is followed by prompting a name for the new object. This is like naming a variable---since you may have multiple
_FScape_ programs, you distinguish them by name. Let's just use the default name "FScape" for now, and confirm the
dialog:
 
![Popup for New Object](.../tut-paulstretch-new-fscape-name.png)

You now see a new object in the main workspace window. It is an empty program, in order to write our signal
processing code, we have to select it with the mouse or cursor keys and click on the eye-shaped button to open its
editor:

![Popup for New Object](.../tut-paulstretch-fscape-in-folder.png)

## Giving it a Spin

The code editor in Mellite is provided by a light-weight component modelled loosely
after the popular IntelliJ IDEA software---much less powerful, but sharing some of its visual cues and keyboard 
shortcuts.

@@@ note { title=Tip }

If you are new to this editor, it is advised that you take a look at the keyboard shortcuts, which can be found
in the menu _Help → Keyboard Shortcuts_ in Mellite's main window.

@@@

Before we type any code ourselves, we begin by using a built-in example of an FScape program. It generates and
plots a sine wave. To get the code of this example, select the _Examples → Plot Sine_ menu item:

![Popup for New Object](.../tut-paulstretch-fscape-plot-sine-code.png)

Like all code in Mellite, it is written in the
@link:[Scala programming language](https://www.scala-lang.org/) { open=new }, although we use different domain
specific abstractions depending on the type of object. Here we use the signal processing toolkit FScape with its
own set of available elements and rules. For example, as opposed to a "fully fledged" Scala program, we can
write FScape code here directly on the "top level" without wrapping it inside an `object` or `class`, and also we
do not need `import` statements for the commonly used symbols.

As we go through the code, there will be extra boxes explaining bits of the
Scala language to you. __If you are already familiar with Scala, you can simply skip these boxes.__
Here is that example code again in text form:

```scala
val sr  = 44100.0
val sig = SinOsc(440 / sr)
Plot1D(sig, 500)
```

@@@ note { title=Scala }

You can place a statement in each line without having to use a terminal character such as `;`.
You only need a semicolon if you want to place multiple statements in the same line. Values and expressions
can be assigned to variables in the form of `val name = expression`. Conventionally, you would use "camel-case"
names for variables, beginning with a lower-case character.

@@@

Since FScape calculates "as fast as possible" without being tied to real-time sound output, there is no given
@link:[sampling rate](https://en.wikipedia.org/wiki/Sampling_(signal_processing)#Sampling_rate), the number of digital
samples per second in the sound signal. If a program generates sound
"from scratch", you would normally define a suitable sampling rate, or if a program transforms an input sound file,
you would normally use that sound file's existing sampling rate. Common sampling rates are 44.1 kHz (44100 Hz) and
48 kHz (48000 Hz). The first line, `val sr = 44100.0` defines a variable for the sampling rate that we can then use
subsequently.

The second line `val sig = SinOsc(440 / sr)` creates a so-called
@link:[unit generator](https://en.wikipedia.org/wiki/Unit_generator) (UGen), in this case a sine oscillator. Whereas in
real-time systems, you typically specify its frequency in Hertz, for example using `[osc~ 440]` in PD, `[cycle~ 440]`
in Max, or `SinOsc.ar(440)` in SuperCollider, in FScape you give the __normalized frequency__, which means that you
divide the nominal frequency in Hertz by the sampling rate---precisely, because FScape does not know what the
sampling rate is. For the same reason, there is no distinction between audio-rate and control-rate as in SuperCollider.

The third line `Plot1D(sig, 500)` introduces a unit generator that plots the first 500 values of the sine oscillator
when the program is run.

@@@ note

`Plot1D` is really just a tool for debugging, as it currently blocks the further execution
of the program. In the future, visualisations will be much better integrated into FScape.

@@@

To _run_ this program, we need to understand the buttons at the bottom of the editor window, namely
_Apply_, _Compile_, and _Render_. Code snippets in Mellite exists always in two forms:
__Source code__ and __compiled program__ (Scala is a compiled language). Furthermore, when you edit code in the text
editor, the changes only exist in a temporary __buffer__ and have to be explicitly applied, as if the updated source 
code was "saved". Thus if you tried to close the editor window now, you would see a dialog warning you that you have
"unsaved" changes. The _Compile_ button can be used to __test__ whether the current buffer (editor content) is a
valid program. When you click that button, it should take a moment to compile and return with either a green
mark (program is valid) or a red mark (errors). In the case of errors, these are also printed to the log window.
You rarely need to use the _Compile_ button, because the editor automatically test-compiles your code in the
background as you edit, indicating the state on the right-hand side of the window. The tiny green square in the
screenshot above indicates that the program is valid. If you introduce errors, these will be indicated by red
marks. For example, if we left out the second argument of `Plot1D` and just wrote `Plot1D(sig)`, we would see a
compilation error:

![Compilation Error in FScape Code Editor](.../tut-paulstretch-fscape-plot-sine-error.png)

The error reads something like

> error: not enough arguments for method apply: (in: GE, size: GE, label: String)Plot1D in object Plot1D

We can undo the last edit using <kbd>ctrl</kbd>-<kbd>Z</kbd> (Mac: <kbd>cmd</kbd>-<kbd>Z</kbd>).
If the program is valid, we can save both source code and the compiled program by clicking on the _Apply_ button.
When this button is disabled (shown in faint colours), it means the current buffer has been saved.

@@@ warning {title=Bug }

Sometimes the _Apply_ button is not shown disabled even after saving.

@@@

@@@ note

It is currently not possible to save just the source code if the program is invalid. This will be fixed in a future
version. As a work-around, you can place a block comment `/* ... */` around your code to be able to save and exit
the editor while your program is still invalid (the compiled program will be empty in this case).

@@@

Finally, to run the program, we press the _Render_ button. It is important to understand that this will run the
__last saved compiled program__, so if you forgot to save you might be actually running a previously
saved version, not the one corresponding to the current buffer! The output should be the plot window showing the
first 500 sample values of the sine oscillator:

![Compilation Error in FScape Code Editor](.../tut-paulstretch-fscape-plot-sine-output.png)

As you can see, the signal range is between -1.0 and +1.0, corresponding with the approach of most other signal
processing software, including PD, Max, and SuperCollider.

## Reading and Writing Audio Files

Now we are going to write the first program that transforms an existing audio file. The program will begin
with a UGen that reads the existing (input) audio file, and it will end with a UGen that writes a new (output)
audio file. First, let us find an audio file for testing. Here is a two seconds bell-like sound placed in the
public domain:

<audio controls>
  <source src="337048_131348kaonayabell.wav" type="audio/wav">
</audio>

You can right-click on the sound player element in the browser and select _Save Audio As_ in the popup menu
to obtain a local copy of this sound. Note that the file format is uncompressed WAV!

@@@ note

While Mellite can export compressed formats such as mp3 or FLAC, it currently only handles uncompressed or 'PCM'
formats as input, typically AIFF or WAV files. If you have mp3, FLAC or Ogg files, you must convert them to be able
to use them in Mellite.

@@@

The easiest way to get the input audio file, is to add it first to the main workspace folder, and from there
make the connection to the attribute map. Therefore, go back to the main workspace window, press the plus-shaped
button again, and this time select _Audio File_. A file chooser opens, navigate to the WAV file downloaded, select
it and press _Open_. A second file chooser opens, prompting you:

> Choose Artifact Base Location

Because this is the first "external resource" you add to the workspace, Mellite wants to know what your base
directory for audio files is. Every audio file you add is relative to one of these locations (directories). Just
stick to the default suggestion here by simply confirming with _Open_ again. Two new elements should have been
added now to your workspace folder, the location and the audio file itself:

![Compilation Error in FScape Code Editor](.../tut-paulstretch-audio-file-in-folder.png)

If you select the audio file and press the eye-shaped button, a viewer opens with the sonogram of the sound and
a transport bar to play back the sound:

![Compilation Error in FScape Code Editor](.../tut-paulstretch-audio-file-view.png)

Before you can hear the sound when pressing the play-button, the audio system must be started, by pressing
the _Boot_ button in Mellite's main window. If you haven't done yet, you must make sure that SuperCollider is
configured in the preferences, as every real-time sound production goes through SuperCollider. In Mellite's
preferences, select the _Audio_ tab, and edit the _SuperCollider (scsynth)_ setting if necessary. On Linux, it
can usually remain on the default _<SC_HOME>_, but on Mac and Windows you must point it explicitly to the
`scynth` or `scsynth.exe` program. Once you manage to boot the audio system and press the play button in the
audio file viewer, you should hear the playback and see the output meter moving in the main window:

----

Let us now return to the FScape code editor. The transform consists of a "normalization", i.e. we apply the maximum 
gain so that the signal range does not exceed -1.0 to +1.0, making the signal maximally loud while avoiding digital 
clipping. In the first step, let's measure the maximum amplitude of the input signal:

```scala
val in        = AudioFileIn("in")
val chanMax   = RunningMax(in.abs).last
chanMax .poll("chan-max")   // print the amplitude of each channel
val totalMax  = Reduce.max(chanMax)
totalMax.poll("total-max")  // print the maximum input amplitude
```

Before we can run the program, we must actually provide the input audio file location. Note how
`AudioFileIn` uses a string parameter---`"in"`---to refer to this location.
This is a __key__ into the __attribute map__ of the FScape object, where the actual file location will be
looked up when running the program. This way, we avoid having to hard-code file paths in the program, and later
we can link this key to a user interface component as well.

@@@ note { title=Scala }

String literals in Scala have to be enclosed in double quotes `"..."`, just as in Java or SuperCollider.
Anther similarity are line comments which begin with double forward slashes `// ...`. Comments are purely
informative to the reader, they are ignored by the compiler.

@@@

We can freely choose the key, but we must be careful that a corresponding entry actually exist. For example,
if we save and run (render) the above program, there will be an error:

> java.lang.RuntimeException: AudioFileIn missing attribute in
