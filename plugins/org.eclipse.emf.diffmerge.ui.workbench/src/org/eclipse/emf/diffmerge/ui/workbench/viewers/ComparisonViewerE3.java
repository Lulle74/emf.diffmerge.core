/**
 * <copyright>
 * 
 * Copyright (c) 2010-2017 Thales Global Services S.A.S and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Thales Global Services S.A.S. - initial API and implementation
 *    Stephane Bouchet (Intel Corporation) - Bug #442492 : hide number of differences in the UI
 *    Stephane Bouchet (Intel Corporation) - Bug #489274 : added API viewers creation methods
 *    Jeremy Aubry (Obeo) - Bug #500417 : Cannot call a merge with a given selection programmatically
 * 
 * </copyright>
 */
package org.eclipse.emf.diffmerge.ui.workbench.viewers;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.diffmerge.ui.EMFDiffMergeUIPlugin;
import org.eclipse.emf.diffmerge.ui.Messages;
import org.eclipse.emf.diffmerge.ui.specification.IComparisonMethod;
import org.eclipse.emf.diffmerge.ui.util.MiscUtil;
import org.eclipse.emf.diffmerge.ui.viewers.ComparisonViewer;
import org.eclipse.emf.diffmerge.ui.viewers.EMFDiffNode;
import org.eclipse.emf.diffmerge.ui.viewers.HeaderViewer;
import org.eclipse.emf.diffmerge.ui.workbench.setup.ComparisonSetupManagerE3;
import org.eclipse.emf.diffmerge.ui.workbench.setup.EMFDiffMergeEditorInput;
import org.eclipse.emf.edit.ui.action.RedoAction;
import org.eclipse.emf.edit.ui.action.UndoAction;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

/**
 * A Viewer for comparisons which is composed of six sub-viewers that show the scopes being
 * compared, a synthesis of the differences, features of the selected element, and the contents
 * of the selected feature in each scope.
 * Input: EMFDiffNode ; Elements: IMatch | IDifference.
 * @author Olivier Constant
 */
public class ComparisonViewerE3 extends ComparisonViewer implements IWorkbenchComparisonViewer
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
	 */
	public ComparisonViewerE3( Composite parent_p )
	{
		this( parent_p, null );
	}

	/**
	 * Constructor
	 * @param parent_p a non-null composite
	 * @param actionBars_p optional action bars
	 */
	public ComparisonViewerE3( Composite parent_p, IActionBars actionBars_p )
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

	@Override
	public void refreshTools()
	{
		super.refreshTools();
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

	/**
	 * Create context menus for the given viewer
	 * @param viewer_p a non-null viewer
	 * @param useLocalSelectionProvider_p whether the selection provider of the viewer must be used
	 * @return a potentially null menu manager for the context menus
	 */
	@Override
	protected MenuManager createViewerContextMenus( HeaderViewer<?> viewer_p,
			boolean useLocalSelectionProvider_p )
	{
		MenuManager result = super.createViewerContextMenus( viewer_p, useLocalSelectionProvider_p );
		ISelectionProvider selectionProvider = useLocalSelectionProvider_p ? viewer_p.getInnerViewer()
				: getMultiViewerSelectionProvider();
		// External contributions
		if ( acceptContextMenuAdditions( viewer_p ) ) {
			IWorkbenchPartSite site = getSite();
			if ( site != null ) {
				result.add( new GroupMarker( IWorkbenchActionConstants.MB_ADDITIONS ) );
				site.registerContextMenu( result, selectionProvider );
			}
		}
		return result;
	}

	@Override
	protected Item createItemRestart( Menu context_p )
	{
		MenuItem result = (MenuItem)super.createItemRestart( context_p );

		addPropertyChangeListener( new IPropertyChangeListener() {
			/**
			 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
			 */
			@Override
			public void propertyChange( PropertyChangeEvent event_p )
			{
				if ( PROPERTY_CURRENT_INPUT.equals( event_p.getProperty() ) ) {
					boolean enable = false;
					EMFDiffNode input = getInput();
					if ( input instanceof EMFDiffNodeE3 && !result.isDisposed() )
						enable = ((EMFDiffNodeE3)input).getEditorInput() != null;
					result.setEnabled( enable );
				}
			}
		} );
		return result;
	}

	/**
	 * Restart the comparison via a GUI
	 */
	@Override
	protected void restart()
	{
		final EMFDiffNode input = getInput();
		IEditorInput rawEditorInput = input instanceof EMFDiffNodeE3 ? ((EMFDiffNodeE3)input).getEditorInput() : null;
		if ( input != null && rawEditorInput instanceof EMFDiffMergeEditorInput ) {
			final EMFDiffMergeEditorInput editorInput = (EMFDiffMergeEditorInput)rawEditorInput;
			ComparisonSetupManagerE3 manager = (ComparisonSetupManagerE3)EMFDiffMergeUIPlugin.getDefault().getSetupManager();
			boolean confirmed = manager.updateEditorInputWithUI( getShell(), editorInput );
			if ( confirmed ) {
				final IComparisonMethod method = editorInput.getComparisonMethod();
				method.setVerbose( false );
				Job job = new Job( Messages.ComparisonViewer_RestartInProgress ) {
					/**
					 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
					 */
					@Override
					protected IStatus run( final IProgressMonitor monitor_p )
					{
						MiscUtil.executeAndForget( getEditingDomain(), new Runnable() {
							/**
							 * @see java.lang.Runnable#run()
							 */
							public void run()
							{
								input.setReferenceRole( method.getTwoWayReferenceRole() );
								boolean leftEditable = method.getModelScopeDefinition( input.getRoleForSide( true ) ).isEditable();
								boolean rightEditable = method.getModelScopeDefinition( input.getRoleForSide( false ) ).isEditable();
								input.setEditionPossible( leftEditable, true );
								input.setEditionPossible( rightEditable, false );
								input.getUIComparison().clear();
								input.getActualComparison().compute( method.getMatchPolicy(), method.getDiffPolicy(), method.getMergePolicy(), monitor_p );
								input.getCategoryManager().update();
							}
						} );
						Display.getDefault().syncExec( new Runnable() {
							/**
							 * @see java.lang.Runnable#run()
							 */
							public void run()
							{
								firePropertyChangeEvent( PROPERTY_CURRENT_INPUT, null );
								refresh();
							}
						} );
						editorInput.checkInconsistency( input.getActualComparison() );
						return Status.OK_STATUS;
					}
				};
				job.setUser( true );
				job.schedule();
			}
		}
	}

}
