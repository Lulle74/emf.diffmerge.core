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

import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.diffmerge.api.Role;
import org.eclipse.emf.diffmerge.ui.EMFDiffMergeUIPlugin;
import org.eclipse.emf.diffmerge.ui.EMFDiffMergeUIPlugin.ImageID;
import org.eclipse.emf.diffmerge.ui.Messages;
import org.eclipse.emf.diffmerge.ui.setup.ComparisonSetupManager;
import org.eclipse.emf.diffmerge.ui.specification.IComparisonMethod;
import org.eclipse.emf.diffmerge.ui.specification.IModelScopeDefinition;
import org.eclipse.emf.diffmerge.ui.viewers.ComparisonViewer;
import org.eclipse.emf.diffmerge.ui.viewers.EMFDiffNode;
import org.eclipse.emf.diffmerge.ui.viewers.HeaderViewer;
import org.eclipse.emf.diffmerge.ui.viewers.SelectionBridge;
import org.eclipse.emf.diffmerge.ui.workbench.setup.ComparisonSetupManagerE3;
import org.eclipse.emf.diffmerge.ui.workbench.setup.EMFDiffMergeEditorInput;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.ui.action.RedoAction;
import org.eclipse.emf.edit.ui.action.UndoAction;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.menus.IMenuService;

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
	   * Return whether the viewer belongs to a workbench window,
	   * assuming the current thread is the UI thread
	   */
	@Override
	protected boolean isInWorkbenchWindow()
	{
		Shell ownShell = getShell();
		IWorkbench workbench = PlatformUI.getWorkbench();
		if ( workbench != null && !workbench.isClosing() ) {
			for ( IWorkbenchWindow window : workbench.getWorkbenchWindows() ) {
				if ( ownShell == window.getShell() ) {
					return true;
				}
			}
		}
		return false;
	}
	
	 /**
	   * Set up the selection provider
	   */
	@Override
	protected void setupSelectionProvider()
	{
		final IWorkbenchSite site = getSite();
		if ( site != null ) {
			_selectionBridgeToOutside = new SelectionBridge() {
				/**
				 * @see org.eclipse.emf.diffmerge.ui.viewers.SelectionBridge#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
				 */
				@Override
				public void selectionChanged( SelectionChangedEvent event_p )
				{
					if ( _isExternallySynced && isInternalSelectionProvider( event_p.getSelectionProvider() ) ) {
						super.selectionChanged( event_p );
					}
				}
			};
			getMultiViewerSelectionProvider().addSelectionChangedListener( _selectionBridgeToOutside );
			site.setSelectionProvider( _selectionBridgeToOutside );
			// Eclipse 4.x compatibility layer workaround: selection changed event propagation
			ISelectionChangedListener selectionChangedListener = new ISelectionChangedListener() {
				/**
				 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
				 */
				@Override
				public void selectionChanged( SelectionChangedEvent event_p )
				{
					// Force propagation to selection listeners through the selection service
					IWorkbenchWindow window = site.getWorkbenchWindow();
					if ( window != null && !window.getWorkbench().isClosing() ) {
						ISelectionService service = window.getSelectionService();
						if ( service instanceof ISelectionChangedListener ) {
							((ISelectionChangedListener)service).selectionChanged( event_p );
						}
					}
				}
			};
			_selectionBridgeToOutside.addSelectionChangedListener( selectionChangedListener );
		}
	}

	@Override
	protected ActionContributionItem createItemRestart( IContributionManager context_p )
	{
		ActionContributionItem result = super.createItemRestart( context_p );
		IAction action = result.getAction();
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
					if ( input instanceof EMFDiffNodeE3 ) {
						enable = ((EMFDiffNodeE3)input).getEditorInput() != null;
					}
					action.setEnabled( enable );
				}
			}
		} );
		return result;
	}

	/**
	   * Return the current undo command, if any
	   * @return a singleton array in case of success that may contain null, an empty array otherwise
	   */
	@Override
	protected Object[] getUndoCommand()
	{
		Object[] result = new Object[0];
		EMFDiffNode input = getInput();
		if ( input != null ) {
			IUndoContext undoContext = input.getUndoContext();
			if ( undoContext != null ) {
				IOperationHistory opHistory = PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
				result = new Object[] { opHistory.getUndoOperation( undoContext ) };
			}
			else {
				EditingDomain domain = input.getEditingDomain();
				if ( domain != null ) {
					result = new Object[] { getEditingDomain().getCommandStack().getUndoCommand() };
				}
			}
		}
		return result;
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
	   * Return the workbench part site of this viewer, if any,
	   * assuming the current thread is the UI thread
	   * @return a potentially null site
	   */
	@Override
	public IWorkbenchPartSite getSite()
	{
		IWorkbenchPartSite result = null;
		if ( isInWorkbenchWindow() ) {
			try {
				IWorkbenchPage page = getPage();
				IWorkbenchSite site = page.getActivePart().getSite();
				if ( site instanceof IWorkbenchPartSite ) {
					result = (IWorkbenchPartSite)site;
				}
			}
			catch ( Exception e ) {
				// Just proceed
			}
		}
		return result;
	}
	
	@Override
	protected void inputChanged( Object input_p, Object oldInput_p )
	{
		super.inputChanged( input_p, oldInput_p );
		if ( _undoAction != null ) {
			_undoAction.setEditingDomain( getEditingDomain() );
			_undoAction.update();
		}
		if ( _redoAction != null ) {
			_redoAction.setEditingDomain( getEditingDomain() );
			_redoAction.update();
		}
		if ( _actionBars != null ) {
			_actionBars.updateActionBars();
		}
	}
	
	@Override
	protected void handleDispose()
	{
		if ( _actionBars != null ) {
			_actionBars.clearGlobalActionHandlers();
			_actionBars = null;
		}
		_undoAction = null;
		_redoAction = null;
	}

	@Override
	public void refreshTools()
	{
		super.refreshTools();
		if ( _undoAction != null ) {
			_undoAction.update();
		}
		if ( _redoAction != null ) {
			_redoAction.update();
		}
		if ( _actionBars != null ) {
			try {
				_actionBars.updateActionBars();
			}
			catch ( NullPointerException e ) {
				// E4 bug when setInput is called on an already open editor:
				// NPE at org.eclipse.e4.ui.workbench.renderers.swt.HandledContributionItem.canExecuteItem
				// Give up and proceed.
			}
		}
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
				if ( getEditingDomain() != null ) {
					super.update();
				}
			}
		};
		_undoAction.setImageDescriptor( getImageDescriptor( ImageID.UNDO ) );
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
				if ( getEditingDomain() != null ) {
					super.update();
				}
			}
		};
		_redoAction.setImageDescriptor( getImageDescriptor( ImageID.REDO ) );
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
	    if (acceptContextMenuAdditions(viewer_p)) {
	      IWorkbenchPartSite site = getSite();
	      if (site != null) {
	        result.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
	        site.registerContextMenu(result, selectionProvider);
	      }
	    }
		return result;
	}
	
	@Override
	protected void setupToolsDetails( ToolBar toolbar_p )
	{
		ToolBarManager toolbarManager = new ToolBarManager(toolbar_p);
	    createItemOpenDedicated(toolbarManager);
	    // Integrate contributions
	    IMenuService menuService = (IMenuService)PlatformUI.getWorkbench().getService(IMenuService.class);
	    menuService.populateContributionManager(toolbarManager, LOCATION_TOOLBAR_DETAILS);
	    // Drop-down menu
	    toolbarManager.add(new Separator(LOCATION_TOOLBAR_GROUP_MENU));
	    setupMenuDetails(toolbarManager);
	    toolbarManager.update(true);
	}
	
	@Override
	protected void setupToolsSynthesis( ToolBar toolbar_p )
	{
		ToolBarManager toolbarManager = new ToolBarManager(toolbar_p);
	    toolbarManager.add(new Separator(LOCATION_TOOLBAR_GROUP_CONSISTENCY));
	    createItemInconsistency(toolbarManager);
	    // Next / Previous
	    toolbarManager.add(new Separator(LOCATION_TOOLBAR_GROUP_NAVIGATION));
	    createItemNavigationNext(toolbarManager);
	    createItemNavigationPrevious(toolbarManager);
	    // Expand / Collapse
	    toolbarManager.add(new Separator(LOCATION_TOOLBAR_GROUP_EXPANSION));
	    createItemExpand(toolbarManager);
	    createItemCollapse(toolbarManager);
	    // Filters and sync
	    toolbarManager.add(new Separator(LOCATION_TOOLBAR_GROUP_FILTERING));
	    createItemFilter(toolbarManager);
	    // Integrate contributions
	    if (acceptToolBarAdditions(_viewerSynthesisMain)) {
	      IMenuService menuService = (IMenuService)PlatformUI.getWorkbench().getService(IMenuService.class);
	      menuService.populateContributionManager(toolbarManager, LOCATION_TOOLBAR_SYNTHESIS);
	    }
	    // Drop-down menu
	    toolbarManager.add(new Separator(LOCATION_TOOLBAR_GROUP_MENU));
	    setupMenuSynthesis(toolbarManager);
	    toolbarManager.update(true);
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
			EMFDiffMergeEditorInput editorInput = (EMFDiffMergeEditorInput)rawEditorInput;
			IComparisonMethod origMethod = editorInput.getComparisonMethod();
			IModelScopeDefinition originalTargetScopeDef = origMethod.getModelScopeDefinition( Role.TARGET );
			ComparisonSetupManager manager = EMFDiffMergeUIPlugin.getDefault().getSetupManager();
			boolean confirmed = manager instanceof ComparisonSetupManagerE3? ((ComparisonSetupManagerE3)manager).updateEditorInputWithUI( getShell(), editorInput ) : false;
			if ( confirmed ) {
				IModelScopeDefinition newTargetScopeDef = editorInput.getComparisonMethod().getModelScopeDefinition( Role.TARGET );
				boolean sidesSwapped = newTargetScopeDef != originalTargetScopeDef;
				Job job = new RestartJob( editorInput, sidesSwapped );
				job.setUser( true );
				job.schedule();
			}
		}
	}
	
	/**
	   * The job that executes the non-interactive part of "comparison restart".
	   */
	  protected class RestartJob extends Job {
	    /** The non-null updated editor input */
	    protected final EMFDiffMergeEditorInput _editorInput;
	    /** Whether sides have been swapped during editor input update */
	    protected final boolean _sidesSwapped;
	    /**
	     * Constructor
	     * @param editorInput_p the non-null updated editor input
	     * @param sidesSwapped_p whether sides have been swapped during editor input update
	     */
	    protected RestartJob(EMFDiffMergeEditorInput editorInput_p, boolean sidesSwapped_p) {
	      super(Messages.ComparisonViewer_RestartInProgress);
	      _editorInput = editorInput_p;
	      _sidesSwapped = sidesSwapped_p;
	    }
	    /**
	     * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	     */
	    @Override
	    protected IStatus run(IProgressMonitor monitor_p) {
	      final EMFDiffNode diffNode = _editorInput.getCompareResult();
	      // Set GUI to read-only mode
	      final boolean editionPossibleLeft = diffNode.isEditionPossible(true);
	      final boolean editionPossibleRight = diffNode.isEditionPossible(false);
	      Display.getDefault().syncExec(new Runnable() {
	        /**
	         * @see java.lang.Runnable#run()
	         */
	        @Override
			public void run() {
	          diffNode.setEditionPossible(false, true);
	          diffNode.setEditionPossible(false, false);
	          refreshTools();
	        }
	      });
	      // Isolate from resource set to avoid unwanted interactions with listeners
	      Resource comparisonResource = diffNode.getUIComparison().eResource();
	      ResourceSet rs = comparisonResource == null? null: comparisonResource.getResourceSet();
	      if (rs != null) {
	        rs.getResources().remove(comparisonResource);
	      }
	      // Main behavior
	      restart(monitor_p);
	      // Re-integrate in resource set
	      if (rs != null) {
	        rs.getResources().add(comparisonResource);
	      }
	      // Dismiss GUI read-only mode and refresh
	      Display.getDefault().syncExec(new Runnable() {
	        /**
	         * @see java.lang.Runnable#run()
	         */
	        @Override
			public void run() {
	          diffNode.setEditionPossible(editionPossibleLeft, true);
	          diffNode.setEditionPossible(editionPossibleRight, false);
	          firePropertyChangeEvent(PROPERTY_CURRENT_INPUT, null);
	          refresh();
	        }
	      });
	      // Display inconsistency warning message if needed
	      _editorInput.checkInconsistency(diffNode.getActualComparison());
	      return Status.OK_STATUS;
	    }
	    /**
	     * The main restart behavior
	     * @param monitor_p a non-null progress monitor
	     */
	    protected void restart(IProgressMonitor monitor_p) {
	      IComparisonMethod newMethod = _editorInput.getComparisonMethod();
	      newMethod.setVerbose(false);
	      EMFDiffNode diffNode = _editorInput.getCompareResult();
	      diffNode.getUIComparison().clear();
	      if (_sidesSwapped) {
	        diffNode.setLeftRole(diffNode.getRoleForSide(false));
	        diffNode.getActualComparison().swapScopes();
	      }
	      diffNode.setReferenceRole(newMethod.getTwoWayReferenceRole());
	      diffNode.setDrivingRole(newMethod.getTwoWayReferenceRole());
	      boolean leftEditable = newMethod.getModelScopeDefinition(
	          diffNode.getRoleForSide(true)).isEditable();
	      boolean rightEditable = newMethod.getModelScopeDefinition(
	          diffNode.getRoleForSide(false)).isEditable();
	      diffNode.setEditionPossible(leftEditable, true);
	      diffNode.setEditionPossible(rightEditable, false);
	      diffNode.getActualComparison().compute(
	          newMethod.getMatchPolicy(), newMethod.getDiffPolicy(),
	          newMethod.getMergePolicy(), monitor_p);
	      diffNode.getCategoryManager().update();
	    }
	  }

}
