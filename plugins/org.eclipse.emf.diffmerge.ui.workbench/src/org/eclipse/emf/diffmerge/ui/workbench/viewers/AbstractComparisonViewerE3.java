/**
 * <copyright>
 * 
 * Copyright (c) 2013-2017 Thales Global Services S.A.S.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Thales Global Services S.A.S. - initial API and implementation
 * 
 * </copyright>
 */
package org.eclipse.emf.diffmerge.ui.workbench.viewers;

import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.emf.diffmerge.ui.EMFDiffMergeUIPlugin;
import org.eclipse.emf.diffmerge.ui.viewers.AbstractComparisonViewer;
import org.eclipse.emf.diffmerge.ui.viewers.EMFDiffNode;
import org.eclipse.emf.edit.ui.action.RedoAction;
import org.eclipse.emf.edit.ui.action.UndoAction;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

/**
 * @author Erik Lundstrom
 */
public abstract class AbstractComparisonViewerE3 extends AbstractComparisonViewer implements IWorkbenchComparisonViewer
{
	/** The optional action bars */
	private IActionBars _actionBars;

	/** The (initially null) undo action */
	private UndoAction _undoAction;

	/** The (initially null) redo action */
	private RedoAction _redoAction;

	/**
	 * Constructor
	 * @param parent_p a non-null composite
	 * @param actionBars_p optional action bars
	 */
	public AbstractComparisonViewerE3( Composite parent_p, IActionBars actionBars_p )
	{
		super( parent_p );
		_actionBars = actionBars_p;
		setupUndoRedo();
	}

	/**
	 * Return the workbench page of this viewer, if any
	 * @return a potentially null page
	 */
	@Override
	public IWorkbenchPage getPage()
	{
		IWorkbenchPage result = null;
		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			result = window.getActivePage();
		}
		catch ( Exception e ) {
			// Just proceed
		}
		return result;
	}

	/**
	 * Return the workbench part site of this viewer, if any
	 * @return a potentially null site
	 */
	@Override
	public IWorkbenchPartSite getSite()
	{
		IWorkbenchPartSite result = null;
		try {
			IWorkbenchPage page = getPage();
			IWorkbenchSite site = page.getActivePart().getSite();
			if ( site instanceof IWorkbenchPartSite )
				result = (IWorkbenchPartSite)site;
		}
		catch ( Exception e ) {
			// Just proceed
		}
		return result;
	}

	/**
	 * Dispose this viewer as a reaction to the disposal of its control
	 */
	@Override
	protected void handleDispose()
	{
		if ( _actionBars != null )
			_actionBars.clearGlobalActionHandlers();
		_undoAction = null;
		_redoAction = null;

		super.handleDispose();
	}


	/**
	 * @see org.eclipse.jface.viewers.Viewer#inputChanged(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void inputChanged( Object input_p, Object oldInput_p )
	{
		if ( oldInput_p instanceof ICompareInput )
			((ICompareInput)oldInput_p).removeCompareInputChangeListener( this );
		if ( _undoAction != null ) {
			_undoAction.setEditingDomain( getEditingDomain() );
			_undoAction.update();
		}
		if ( _redoAction != null ) {
			_redoAction.setEditingDomain( getEditingDomain() );
			_redoAction.update();
		}
		if ( _actionBars != null )
			_actionBars.updateActionBars();
		if ( input_p instanceof EMFDiffNode ) {
			EMFDiffNode node = (EMFDiffNode)input_p;
			registerCategories( node );
			node.updateDifferenceNumbers();
			node.getCategoryManager().setDefaultConfiguration();
		}
		if ( input_p instanceof ICompareInput ) {
			final ICompareInput compareInput = (ICompareInput)input_p;
			compareInput.addCompareInputChangeListener( this );
			getControl().addDisposeListener( new DisposeListener() {
				/**
				 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
				 */
				@Override
				public void widgetDisposed( DisposeEvent e_p )
				{
					compareInput.removeCompareInputChangeListener( AbstractComparisonViewerE3.this );
				}
			} );
		}
		firePropertyChangeEvent( PROPERTY_CURRENT_INPUT, null );
	}

	/**
	 * Refresh the tools of the viewer
	 */
	@Override
	public void refreshTools()
	{
		if ( _undoAction != null )
			_undoAction.update();
		if ( _redoAction != null )
			_redoAction.update();
		if ( _actionBars != null )
			_actionBars.updateActionBars();
	}

	/**
	 * Set up the undo/redo mechanism
	 */
	@Override
	public void setupUndoRedo()
	{
		// Undo
		_undoAction = new UndoAction( null ) {
			/**
			 * @see org.eclipse.emf.edit.ui.action.UndoAction#run()
			 */
			@Override
			public void run()
			{
				undoRedo( true );
			}

			/**
			 * @see org.eclipse.emf.edit.ui.action.UndoAction#update()
			 */
			@Override
			public void update()
			{
				if ( getEditingDomain() != null )
					super.update();
			}
		};
		_undoAction.setImageDescriptor( EMFDiffMergeUIPlugin.getDefault().getImageDescriptor( EMFDiffMergeUIPlugin.ImageID.UNDO ) );
		// Redo
		_redoAction = new RedoAction() {
			/**
			 * @see org.eclipse.emf.edit.ui.action.RedoAction#run()
			 */
			@Override
			public void run()
			{
				undoRedo( false );
			}

			/**
			 * @see org.eclipse.emf.edit.ui.action.RedoAction#update()
			 */
			@Override
			public void update()
			{
				if ( getEditingDomain() != null )
					super.update();
			}
		};
		_redoAction.setImageDescriptor( EMFDiffMergeUIPlugin.getDefault().getImageDescriptor( EMFDiffMergeUIPlugin.ImageID.REDO ) );
		if ( _actionBars != null ) {
			_actionBars.setGlobalActionHandler( ActionFactory.UNDO.getId(), _undoAction );
			_actionBars.setGlobalActionHandler( ActionFactory.REDO.getId(), _redoAction );
		}
	}

}
