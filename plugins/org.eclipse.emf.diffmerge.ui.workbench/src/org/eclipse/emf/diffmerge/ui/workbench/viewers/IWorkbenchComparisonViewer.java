package org.eclipse.emf.diffmerge.ui.workbench.viewers;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;

public interface IWorkbenchComparisonViewer
{
	IWorkbenchPage getPage();

	IWorkbenchPartSite getSite();

	/**
	 * Set up the undo/redo mechanism
	 */
	void setupUndoRedo();

	/**
	 * Refresh the tools of the viewer
	 */
	void refreshTools();
}
