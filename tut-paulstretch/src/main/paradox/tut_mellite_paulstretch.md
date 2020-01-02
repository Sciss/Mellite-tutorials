# Mllt1 - Paul Stretch

This tutorial shows you how to develop a basic workspace in Mellite. We will be writing a non-realtime signal
processing program ( _FScape_ ) and wire it up with a simple user interface ( _Widget_ ). We use as example the
well known _Paul Stretch_ algorithm, named after its creator Nasca Octavian Paul and originally
@link:[a standalone software](http://hypermammut.sourceforge.net/paulstretch/) { open=new }
to perform "extreme" time stretching of sounds and additional spectral transformations. Paul has since released
@link:[a public domain version](https://github.com/paulnasca/paulstretch_python) { open=new } of the stripped down
stretching algorithm written in Python. The idea is very simple but remarkably effective: An input sound is stretched 
by taking "segments" (windows) of overlapping time slices, calculating their spectra (Fourier transform), randomising 
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

To create a new workspace, choose _File > New > Workspace ..._, and in the dialog select the default option _Durable_, 
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
from the popup menu:

![Popup for New Object](.../tut-paulstretch-popup-folder-new-object.png)

This is followed by prompting a name for the new object. This is like naming a variable—since you may have multiple
_FScape_ programs, you distinguish them by name. Let's just use the default "FScape" for now.
