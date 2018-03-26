# emf.diffmerge.core
EMF Diff/Merge project repository (emf.diffmerge.core)

Now what's this?
I am on a team developing (or rather, migrating from an Eclipse 3.x Workbench-based app to) an E4 Application. We have been using EMF Diff/Merge 
successfully for years, but we need a version of Diff/Merge that is independent of the old 3.x workbench. 

What has been done in this fork:
* org.eclipse.emf.diffmerge.ui is independent of 3.x workbench
* org.eclipse.emf.diffmerge.ui is independent of "org.eclipse.emf.edit.ui"  (which is really mandatory, to get rid of 3.x stuff)
* org.eclipse.emf.diffmerge.ui.workbench (new bundle): Have refactored/moved the 3.x dependent stuff into here (plus the stuff depending on emf.edit.ui)

Some notes of what has been done to achieve this:
* A "drop-in" replacement of all ISharedImages icons (not available on e4). ALL relevant icons are therefore bundled directly
in the "Diffmerge UI" plugin...maybe not the nicest solution, but will work of course on both E3/E4 platform.
* When running with the new "Diffmerge UI Workbench" plugin, some service-like classes have their implementation changed during runtime. See the Activator.start() in the new plugin. (I did it like this, to attempt to keep the API as similar as possible, and with as small as effort as possible.) 
* Some refactorings+separations led to a class with orig name in original plugin, plus a new class, typically with "..E3.java" suffix. Not very nice naming pattern, but I could not come up with better.
* I have a feeling that a few (revised) class hierarchies may not be optimal yet. For instance, the hierarchy of AbstractComparisonViewer feels a bit "odd". 

But is this enough? Well, if you seek "total independence" of the 3.x workbench, I suggest you have a look on my two other projects:

* https://github.com/Lulle74/eclipse.platform.team  (a fork. The version of "org.eclipse.compare" here uses the "Imported Packages" approach to all things "org.eclipse.ui*")
* https://github.com/Lulle74/Eclipse3-Workbench-Mock-Facade (A stipped-down, compiling "mock impl" of all packaged needed by the forked version of "org.eclipse.compare" )

Using this project along with the two others described above, we can now run a pure E4 app with EMF Diff/Merge.
