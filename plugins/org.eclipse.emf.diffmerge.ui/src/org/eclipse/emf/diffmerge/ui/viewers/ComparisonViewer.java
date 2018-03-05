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
package org.eclipse.emf.diffmerge.ui.viewers;

import static org.eclipse.emf.diffmerge.ui.util.UIUtil.itemAddDisposeListener;
import static org.eclipse.emf.diffmerge.ui.util.UIUtil.itemAddSelectionListener;
import static org.eclipse.emf.diffmerge.ui.util.UIUtil.itemCreate;
import static org.eclipse.emf.diffmerge.ui.util.UIUtil.itemGetSelection;
import static org.eclipse.emf.diffmerge.ui.util.UIUtil.itemSetSelection;
import static org.eclipse.emf.diffmerge.ui.util.UIUtil.itemSetText;
import static org.eclipse.emf.diffmerge.ui.util.UIUtil.itemSetToolTipText;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.INavigatable;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.Logger;
import org.eclipse.emf.diffmerge.api.IComparison;
import org.eclipse.emf.diffmerge.api.IMatch;
import org.eclipse.emf.diffmerge.api.Role;
import org.eclipse.emf.diffmerge.api.diff.IDifference;
import org.eclipse.emf.diffmerge.api.diff.IPresenceDifference;
import org.eclipse.emf.diffmerge.api.diff.IReferenceValuePresence;
import org.eclipse.emf.diffmerge.api.diff.IValuePresence;
import org.eclipse.emf.diffmerge.diffdata.EComparison;
import org.eclipse.emf.diffmerge.diffdata.EElementRelativePresence;
import org.eclipse.emf.diffmerge.diffdata.EMatch;
import org.eclipse.emf.diffmerge.diffdata.EMergeableDifference;
import org.eclipse.emf.diffmerge.diffdata.EValuePresence;
import org.eclipse.emf.diffmerge.structures.common.FArrayList;
import org.eclipse.emf.diffmerge.ui.EMFDiffMergeUIPlugin;
import org.eclipse.emf.diffmerge.ui.EMFDiffMergeUIPlugin.ImageID;
import org.eclipse.emf.diffmerge.ui.Messages;
import org.eclipse.emf.diffmerge.ui.diffuidata.ComparisonSelection;
import org.eclipse.emf.diffmerge.ui.diffuidata.MatchAndFeature;
import org.eclipse.emf.diffmerge.ui.diffuidata.impl.ComparisonSelectionImpl;
import org.eclipse.emf.diffmerge.ui.diffuidata.impl.MatchAndFeatureImpl;
import org.eclipse.emf.diffmerge.ui.log.CompareLogEvent;
import org.eclipse.emf.diffmerge.ui.log.DiffMergeLogger;
import org.eclipse.emf.diffmerge.ui.log.MergeLogEvent;
import org.eclipse.emf.diffmerge.ui.util.DelegatingLabelProvider;
import org.eclipse.emf.diffmerge.ui.util.DifferenceKind;
import org.eclipse.emf.diffmerge.ui.util.IProgressServiceProvider;
import org.eclipse.emf.diffmerge.ui.util.InconsistencyDialog;
import org.eclipse.emf.diffmerge.ui.util.UIUtil;
import org.eclipse.emf.diffmerge.ui.viewers.FeaturesViewer.FeaturesInput;
import org.eclipse.emf.diffmerge.ui.viewers.MergeImpactViewer.ImpactInput;
import org.eclipse.emf.diffmerge.ui.viewers.ValuesViewer.ValuesInput;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;



/**
 * A Viewer for comparisons which is composed of six sub-viewers that show the scopes being
 * compared, a synthesis of the differences, features of the selected element, and the contents
 * of the selected feature in each scope.
 * Input: EMFDiffNode ; Elements: IMatch | IDifference.
 * @author Olivier Constant
 */
public class ComparisonViewer extends AbstractComparisonViewer {
  
  /** The name of the "filtering state" property */
  public static final String PROPERTY_FILTERING = "PROPERTY_FILTERING"; //$NON-NLS-1$
  
  /** The name of the "delete left activation" property */
  public static final String PROPERTY_ACTIVATION_DELETE_LEFT = "PROPERTY_ACTIVATION_DELETE_LEFT"; //$NON-NLS-1$
  
  /** The name of the "delete right activation" property */
  public static final String PROPERTY_ACTIVATION_DELETE_RIGHT = "PROPERTY_ACTIVATION_DELETE_RIGHT"; //$NON-NLS-1$
  
  /** The name of the "merge to left activation" property */
  public static final String PROPERTY_ACTIVATION_MERGE_TO_LEFT = "PROPERTY_ACTIVATION_MERGE_TO_LEFT"; //$NON-NLS-1$
  
  /** The name of the "merge to right activation" property */
  public static final String PROPERTY_ACTIVATION_MERGE_TO_RIGHT = "PROPERTY_ACTIVATION_MERGE_TO_RIGHT"; //$NON-NLS-1$
  
  /** The name of the "ignore left activation" property */
  public static final String PROPERTY_ACTIVATION_IGNORE_LEFT = "PROPERTY_ACTIVATION_IGNORE_LEFT"; //$NON-NLS-1$
  
  /** The name of the "ignore right activation" property */
  public static final String PROPERTY_ACTIVATION_IGNORE_RIGHT = "PROPERTY_ACTIVATION_IGNORE_RIGHT"; //$NON-NLS-1$
  
  
  /** The synthesis model tree viewer */
  protected EnhancedComparisonTreeViewer _viewerSynthesisMain;
  
  /** The left model tree viewer */
  protected EnhancedComparisonSideViewer _viewerSynthesisLeft;
  
  /** The right model tree viewer */
  protected EnhancedComparisonSideViewer _viewerSynthesisRight;

  /** The features viewer */
  protected EnhancedFeaturesViewer _viewerFeatures;

  /** The left values viewer */
  protected EnhancedValuesViewer _viewerValuesLeft;
  
  /** The right values viewer */
  protected EnhancedValuesViewer _viewerValuesRight;
  
  /** A filter for move origins */
  protected ViewerFilter _filterMoveOrigins;
  
  /** A filter for unchanged elements */
  protected ViewerFilter _filterUnchangedElements;
  
  /** An alphanumeric sorter */
  protected ViewerComparator _sorterSynthesis;
  
  /** Whether the left and right trees are synchronized with the synthesis tree */
  protected boolean _isLeftRightSynced;
  
  /** The potentially null last selection */
  private ComparisonSelection _lastUserSelection;
  
  /** The non-null selection provider covering certain sub-viewers */
  protected SelectionBridge.SingleSource _multiViewerSelectionProvider;
  
  /** The non-null (after init) selection listener for filter items */
  protected FilterSelectionListener _filterSelectionListener;
  
  
  /**
   * Constructor
   * @param parent_p a non-null composite
   */
  public ComparisonViewer(Composite parent_p) {
    super(parent_p);
  }
  
  /**
   * Return whether context menus can be contributed via the usual ADDITIONS group
   * in the given viewer
   */
  protected boolean acceptContextMenuAdditions(Viewer viewer_p) {
    return true;
  }
  
  /**
   * Add differences to merge on the given match to the given list according
   * to the given criteria
   * @param toMerge_p a non-null, modifiable list
   * @param match_p a non-null match
   * @param destination_p a non-null role which is TARGET or REFEREBCE
   * @param incrementalMode_p whether optional deletions must be skipped
   */
  protected void addDifferencesToMerge(List<IDifference> toMerge_p, IMatch match_p,
      Role destination_p, boolean incrementalMode_p) {
    for (IDifference difference : match_p.getAllDifferences()) {
      if (!getInput().getCategoryManager().isFiltered(difference)) {
        if (!incrementalMode_p || difference instanceof IPresenceDifference &&
            ((IPresenceDifference)difference).getPresenceRole() != destination_p)
          toMerge_p.add(difference);
      }
    }
  }
  
  /**
   * Add differences to merge on the given match and its children to the given list according
   * to the given criteria
   * @param toMerge_p a non-null, modifiable list
   * @param match_p a non-null match
   * @param destination_p a non-null role which is TARGET or REFEREBCE
   * @param incrementalMode_p whether optional deletions must be skipped
   */
  protected void addDifferencesToMergeRec(List<IDifference> toMerge_p, IMatch match_p,
      Role destination_p, boolean incrementalMode_p) {
    addDifferencesToMerge(toMerge_p, match_p, destination_p, incrementalMode_p);
    for (IMatch child : getInput().getCategoryManager().getChildrenForMerge(match_p)) {
      addDifferencesToMergeRec(toMerge_p, child, destination_p, incrementalMode_p);
    }
  }
  
  /**
   * Convert the given structured selection to a comparison selection
   * @param selection_p a non-null selection
   * @return a non-null comparison selection
   */
  protected ComparisonSelection asComparisonSelection(IStructuredSelection selection_p) {
    Collection<IMatch> matches = new ArrayList<IMatch>();
    EComparison comparison = getComparison();
    if (comparison != null) {
      for (Object selected : selection_p.toArray()) {
        if (selected instanceof EObject) {
          EObject selectedElement = (EObject)selected;
          IMatch match = comparison.getMapping().getMatchFor(selectedElement, Role.TARGET);
          if (match == null)
            match = comparison.getMapping().getMatchFor(selectedElement, Role.REFERENCE);
          if (match != null)
            matches.add(match);
        }
      }
    }
    ComparisonSelection result = new ComparisonSelectionImpl(matches, null, getInput());
    return result;
  }
  
  /**
   * Return whether the given situation allows adding to the left
   * @param originKind_p a non-null kind
   */
  protected boolean canAddToTheLeft(DifferenceKind originKind_p) {
    final Collection<DifferenceKind> allowed = Arrays.asList(
        DifferenceKind.CONFLICT,
        DifferenceKind.MODIFIED,
        DifferenceKind.FROM_BOTH,
        DifferenceKind.FROM_RIGHT,
        DifferenceKind.FROM_RIGHT_ADD,
        DifferenceKind.FROM_LEFT_DEL);
    return allowed.contains(originKind_p);
  }
  
  /**
   * Return whether the given situation allows adding to the right
   * @param originKind_p a non-null kind
   */
  protected boolean canAddToTheRight(DifferenceKind originKind_p) {
    final Collection<DifferenceKind> allowed = Arrays.asList(
        DifferenceKind.CONFLICT,
        DifferenceKind.MODIFIED,
        DifferenceKind.FROM_BOTH,
        DifferenceKind.FROM_LEFT,
        DifferenceKind.FROM_LEFT_ADD,
        DifferenceKind.FROM_RIGHT_DEL);
    return allowed.contains(originKind_p);
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.viewers.AbstractComparisonViewer#compareInputChanged(org.eclipse.compare.structuremergeviewer.ICompareInput)
   */
  @Override
  public void compareInputChanged(final ICompareInput source_p) {
    BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
      /**
       * @see java.lang.Runnable#run()
       */
      public void run() {
        if (source_p instanceof EMFDiffNode) {
          EMFDiffNode node = (EMFDiffNode)source_p;
          boolean isFiltering = node.getCategoryManager().isUIMoreFilteringThanDefault();
          firePropertyChangeEvent(PROPERTY_FILTERING, Boolean.valueOf(isFiltering));
        }
        refresh();
      }
    });
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.viewers.AbstractComparisonViewer#createControls(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Composite createControls(Composite parent_p) {
    // Non-graphical instance variables
    initialize();
    // Main control
    SashForm result = new SashForm(parent_p, SWT.VERTICAL);
    // Upper and lower parts
    SashForm upperPart = createRowUpper(result);
    SashForm lowerPart = createRowLower(result);
    setupColumns(upperPart, lowerPart);
    result.setWeights(getDefaultRowWeights());
    // Tools: buttons and menus
    setupToolBars();
    return result;
  }
  
  /**
   * Create the "collapse all" item in the given context and return it
   * @param context_p a non-null object
   * @return a potentially null item
   */
  protected Item createItemCollapse(ToolBar context_p) {
    ToolItem result = new ToolItem(context_p, SWT.PUSH);
    result.setImage(EMFDiffMergeUIPlugin.getDefault().getImage(
        EMFDiffMergeUIPlugin.ImageID.COLLAPSEALL));
    result.setToolTipText(Messages.ComparisonViewer_CollapseTooltip);
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent event_p) {
        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
          /**
           * @see java.lang.Runnable#run()
           */
          public void run() {
            _viewerSynthesisMain.getInnerViewer().collapseAll();
          }
        });
      }
    });
    return result;
  }
  
  /**
   * Create the "delete" item for the given side in the given context and return it
   * @param context_p a non-null object
   * @param onLeft_p whether the side is left
   * @return a potentially null item
   */
  protected Item createItemDelete(ToolBar context_p, final boolean onLeft_p) {
    final ToolItem result = new ToolItem(context_p, SWT.PUSH);
    // Image
    result.setImage(EMFDiffMergeUIPlugin.getDefault().getImage(
        EMFDiffMergeUIPlugin.ImageID.DELETE));
    // Tool tip
    result.setToolTipText(onLeft_p? Messages.ComparisonViewer_DeleteLeftTooltip:
      Messages.ComparisonViewer_DeleteRightTooltip);
    result.setEnabled(false);
    // Activation
    addPropertyChangeListener(new IPropertyChangeListener() {
      /**
       * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
       */
      public void propertyChange(PropertyChangeEvent event_p) {
        if (onLeft_p && PROPERTY_ACTIVATION_DELETE_LEFT.equals(event_p.getProperty()) ||
            !onLeft_p && PROPERTY_ACTIVATION_DELETE_RIGHT.equals(event_p.getProperty())) {
          Object newValue = event_p.getNewValue();
          if (newValue instanceof Boolean)
            result.setEnabled(((Boolean)newValue).booleanValue());
        }
      }
    });
    // Selection
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent event_p) {
        merge(onLeft_p, false);
      }
    });
    return result;
  }
  
  /**
   * Create the "expand all" item in the given context and return it
   * @param context_p a non-null object
   * @return a potentially null item
   */
  protected Item createItemExpand(ToolBar context_p) {
    ToolItem result = new ToolItem(context_p, SWT.PUSH);
    result.setImage(EMFDiffMergeUIPlugin.getDefault().getImage(
        EMFDiffMergeUIPlugin.ImageID.EXPANDALL));
    result.setToolTipText(Messages.ComparisonViewer_ExpandTooltip);
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent event_p) {
        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
          /**
           * @see java.lang.Runnable#run()
           */
          public void run() {
            _viewerSynthesisMain.getInnerViewer().expandAll();
          }
        });
      }
    });
    return result;
  }
  
  /**
   * Create the "filter" item in the given context and return it
   * @param context_p a non-null ToolBar or Menu
   * @return a potentially null item
   */
  protected Item createItemFilter(Widget context_p) {
    assert context_p instanceof ToolBar || context_p instanceof Menu;
    final Item result = itemCreate(context_p, SWT.CHECK, null);
    String text = (context_p instanceof ToolBar)?
        Messages.ComparisonViewer_FilterToolTip:
          Messages.ComparisonViewer_FilterText;
    itemSetText(result, text);
    itemSetToolTipText(result, Messages.ComparisonViewer_EnhancedFilterToolTip);
    result.setImage(EMFDiffMergeUIPlugin.getDefault().getImage(
        EMFDiffMergeUIPlugin.ImageID.FILTER));
    itemSetSelection(result, false);
    if (_filterSelectionListener == null)
      _filterSelectionListener = new FilterSelectionListener();
    _filterSelectionListener.addItem(result);
    return result;
  }
  
  /**
   * Create the "ignore" item for the given side in the given context and return it
   * @param context_p a non-null object
   * @param onLeft_p whether the side is left
   * @return a potentially null item
   */
  protected Item createItemIgnore(ToolBar context_p, final boolean onLeft_p) {
    final ToolItem result = new ToolItem(context_p, SWT.PUSH);
    // Image
    result.setImage(EMFDiffMergeUIPlugin.getDefault().getImage(
        EMFDiffMergeUIPlugin.ImageID.CHECKED));
    // Tool tip
    result.setToolTipText(onLeft_p? Messages.ComparisonViewer_IgnoreLeftTooltip:
      Messages.ComparisonViewer_IgnoreRightTooltip);
    result.setEnabled(false);
    // Activation
    addPropertyChangeListener(new IPropertyChangeListener() {
      /**
       * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
       */
      public void propertyChange(PropertyChangeEvent event_p) {
        if (onLeft_p && PROPERTY_ACTIVATION_IGNORE_LEFT.equals(event_p.getProperty()) ||
            !onLeft_p && PROPERTY_ACTIVATION_IGNORE_RIGHT.equals(event_p.getProperty())) {
          Object newValue = event_p.getNewValue();
          if (newValue instanceof Boolean)
            result.setEnabled(((Boolean)newValue).booleanValue());
        }
      }
    });
    // Selection
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent event_p) {
        ignore(onLeft_p);
      }
    });
    return result;
  }
  
  /**
   * Create the "inconsistency" item in the given context and return it
   * @param context_p a non-null object
   * @return a potentially null item
   */
  protected Item createItemInconsistency(ToolBar context_p) {
    final ToolItem result = new ToolItem(context_p, SWT.PUSH);
    result.setImage(EMFDiffMergeUIPlugin.getDefault().getImage(
        EMFDiffMergeUIPlugin.ImageID.WARNING));
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent event_p) {
        final Shell shell = getShell();
        final EComparison comparison = getComparison();
        if (shell != null && comparison != null) {
          shell.getDisplay().syncExec(new Runnable() {
            /**
             * @see java.lang.Runnable#run()
             */
            public void run() {
              InconsistencyDialog dialog = new InconsistencyDialog(shell, comparison);
              dialog.open();
            }
          });
        }
      }
    });
    result.setDisabledImage(EMFDiffMergeUIPlugin.getDefault().getImage(ImageID.EMPTY));
    result.setEnabled(false);
    addPropertyChangeListener(new IPropertyChangeListener() {
      /**
       * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
       */
      public void propertyChange(PropertyChangeEvent event_p) {
        if (PROPERTY_CURRENT_INPUT.equals(event_p.getProperty())) {
          IComparison comparison = getComparison();
          boolean enabled = comparison != null && !comparison.isConsistent();
          result.setEnabled(enabled);
          result.setToolTipText(enabled? Messages.ComparisonViewer_InconsistencyTooltip: null);
        }
      }
    });
    return result;
  }
  
  /**
   * Create the locking item in the given context for the given side and return it
   * @param context_p a non-null object
   * @param onLeft_p whether the side is left or right
   * @return a potentially null item
   */
  protected Item createItemLock(ToolBar context_p, final boolean onLeft_p) {
    final ToolItem result = new ToolItem(context_p, SWT.CHECK);
    final Image openLockImage =
        EMFDiffMergeUIPlugin.getDefault().getImage(EMFDiffMergeUIPlugin.ImageID.LOCK_OPEN);
    final Image closedLockImage =
        EMFDiffMergeUIPlugin.getDefault().getImage(EMFDiffMergeUIPlugin.ImageID.LOCK_CLOSED);
    result.setImage(openLockImage);
    final String lockedTooltip = Messages.ComparisonViewer_LockTooltip_Locked;
    final String unlockedTooltip = Messages.ComparisonViewer_LockTooltip_Unlocked;
    result.setToolTipText(unlockedTooltip);
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent event_p) {
        boolean editable = !result.getSelection();
        getInput().setEditable(editable, onLeft_p);
        result.setImage(editable? openLockImage: closedLockImage);
        result.setToolTipText(editable? unlockedTooltip: lockedTooltip);
        refreshTools();
      }
    });
    addPropertyChangeListener(new IPropertyChangeListener() {
      /**
       * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
       */
      public void propertyChange(PropertyChangeEvent event_p) {
        if (PROPERTY_CURRENT_INPUT.equals(event_p.getProperty())) {
          EMFDiffNode input = getInput();
          if (input != null) {
            boolean editable = input.isEditable(onLeft_p);
            result.setSelection(!editable);
            result.setImage(editable? openLockImage:closedLockImage);
            result.setToolTipText(editable? unlockedTooltip: lockedTooltip);
            result.setEnabled(input.isEditionPossible(onLeft_p));
          }
        }
      }
    });
    return result;
  }
  
  /**
   * Create the "log events" item in the given context and return it
   * @param context_p a non-null object
   * @return result a potentially null item
   */
  protected Item createItemLogEvents(Menu context_p) {
    final MenuItem result = new MenuItem(context_p, SWT.CHECK);
    result.setText(Messages.ComparisonViewer_LogEventsMenuItem);
    String tooltip = Messages.ComparisonViewer_LogTooltipNoFile;
    Logger logger = getLogger();
    if (logger instanceof DiffMergeLogger) {
      IPath logFile = ((DiffMergeLogger)logger).getLogFile();
      tooltip = String.format(Messages.ComparisonViewer_LogTooltipFile, logFile.toOSString());
    }
    result.setToolTipText(tooltip);
    // Initialization
    addPropertyChangeListener(new IPropertyChangeListener() {
      /**
       * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
       */
      public void propertyChange(PropertyChangeEvent event_p) {
        if (PROPERTY_CURRENT_INPUT.equals(event_p.getProperty())) {
          EMFDiffNode input = getInput();
          if (input != null && !result.isDisposed())
            result.setSelection(input.isLogEvents());
        }
      }
    });
    // Selection
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent e_p) {
        boolean logEvents = result.getSelection();
        EMFDiffNode input = getInput();
        if (input != null) {
          input.setLogEvents(logEvents);
          if (logEvents)
            getLogger().log(new CompareLogEvent(getEditingDomain(), getComparison()));
        }
      }
    });
    return result;
  }
  
  /**
   * Create the "merge" item to the given side in the given context and return it
   * @param toolbar_p a non-null object
   * @param toLeft_p whether the side is left
   * @return a potentially null item
   */
  protected Item createItemMerge(ToolBar toolbar_p, final boolean toLeft_p) {
    final ToolItem result = new ToolItem(toolbar_p, SWT.PUSH);
    // Image
    EMFDiffMergeUIPlugin.ImageID imageID = toLeft_p?
        EMFDiffMergeUIPlugin.ImageID.CHECKOUT_ACTION:
          EMFDiffMergeUIPlugin.ImageID.CHECKIN_ACTION;
    result.setImage(EMFDiffMergeUIPlugin.getDefault().getImage(imageID));
    // Tool tip
    result.setToolTipText(toLeft_p? Messages.ComparisonViewer_MergeLeftTooltip:
      Messages.ComparisonViewer_MergeRightTooltip);
    result.setEnabled(false);
    // Activation
    addPropertyChangeListener(new IPropertyChangeListener() {
      /**
       * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
       */
      public void propertyChange(PropertyChangeEvent event_p) {
        if (toLeft_p && PROPERTY_ACTIVATION_MERGE_TO_LEFT.equals(event_p.getProperty()) ||
            !toLeft_p && PROPERTY_ACTIVATION_MERGE_TO_RIGHT.equals(event_p.getProperty())) {
          Object newValue = event_p.getNewValue();
          if (newValue instanceof Boolean)
            result.setEnabled(((Boolean)newValue).booleanValue());
        }
      }
    });
    // Selection
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent event_p) {
        merge(toLeft_p, true);
      }
    });
    return result;
  }
  
  /**
   * Create the "next" navigation item in the given context and return it
   * @param toolbar_p a non-null context
   * @return a potentially null item
   */
  protected Item createItemNavigationNext(ToolBar toolbar_p) {
    ToolItem result = new ToolItem(toolbar_p, SWT.PUSH);
    result.setImage(EMFDiffMergeUIPlugin.getDefault().getImage(
        EMFDiffMergeUIPlugin.ImageID.NEXT_DIFF_NAV));
    result.setToolTipText(Messages.ComparisonViewer_NextTooltip);
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent event_p) {
        navigate(true);
      }
    });
    return result;
  }
  
  /**
   * Create the "previous" navigation item in the given context and return it
   * @param context_p a non-null object
   * @return a potentially null tool item
   */
  protected Item createItemNavigationPrevious(ToolBar context_p) {
    ToolItem result = new ToolItem(context_p, SWT.PUSH);
    result.setImage(EMFDiffMergeUIPlugin.getDefault().getImage(
        EMFDiffMergeUIPlugin.ImageID.PREV_DIFF_NAV));
    result.setToolTipText(Messages.ComparisonViewer_PreviousTooltip);
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent event_p) {
        navigate(false);
      }
    });
    return result;
  }
  
  /**
   * Create the "restart" item in the given context and return it
   * @param context_p a non-null object
   * @return result a potentially null item
   */
  protected Item createItemRestart(Menu context_p) {
    final MenuItem result = new MenuItem(context_p, SWT.PUSH);
    result.setText(Messages.ComparisonViewer_ToolUpdate);
    result.setToolTipText(Messages.ComparisonViewer_ToolUpdate_Tooltip);
    result.setImage(EMFDiffMergeUIPlugin.getDefault().getImage(ImageID.UPDATE));
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent e_p) {
        restart();
      }
    });

    return result;
  }
  
  /**
   * Create the "show all values and properties" item in the given context and return it
   * @param context_p a non-null object
   * @return result a potentially null item
   */
  protected Item createItemShowAllFeatures(Menu context_p) {
    final MenuItem result = new MenuItem(context_p, SWT.RADIO);
    result.setText(Messages.ComparisonViewer_ShowAllFeatures);
    result.setToolTipText(Messages.ComparisonViewer_ShowAllFeaturesTooltip);
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent e_p) {
        _viewerFeatures.getInnerViewer().setDifferenceAgnostic(true);
        _viewerValuesLeft.getInnerViewer().setDifferenceAgnostic(true);
        _viewerValuesRight.getInnerViewer().setDifferenceAgnostic(true);
      }
    });
    return result;
  }
  
  /**
   * Create the "show all values" item in the given context and return it
   * @param context_p a non-null object
   * @return result a potentially null item
   */
  protected Item createItemShowAllValues(Menu context_p) {
    final MenuItem result = new MenuItem(context_p, SWT.RADIO);
    result.setText(Messages.ComparisonViewer_ShowAllValues);
    result.setToolTipText(Messages.ComparisonViewer_ShowAllValuesTooltip);
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent e_p) {
        _viewerFeatures.getInnerViewer().setDifferenceAgnostic(false);
        _viewerValuesLeft.getInnerViewer().setDifferenceAgnostic(true);
        _viewerValuesRight.getInnerViewer().setDifferenceAgnostic(true);
      }
    });
    return result;
  }
  
  /**
   * Create the "show difference numbers per match" item in the given context and return it
   * @param context_p a non-null object
   * @return result a potentially null item
   */
  protected Item createItemShowDifferenceNumbers(Menu context_p) {
    final MenuItem result = new MenuItem(context_p, SWT.CHECK);
    result.setText(Messages.ComparisonViewer_ShowDifferenceNumbersMenuItem);
    result.setToolTipText(Messages.ComparisonViewer_ShowDifferenceNumbersTooltip);
    // Initialization
    addPropertyChangeListener(new IPropertyChangeListener() {
      /**
       * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
       */
      public void propertyChange(PropertyChangeEvent event_p) {
        if (PROPERTY_CURRENT_INPUT.equals(event_p.getProperty())) {
          EMFDiffNode input = getInput();
          if (input != null && !result.isDisposed())
            result.setSelection(!input.isHideDifferenceNumbers());
        }
      }
    });
    // Selection
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent e_p) {
        boolean showDiffNumbers = result.getSelection();
        EMFDiffNode input = getInput();
        if (input != null) {
          input.setHideDifferenceNumbers(!showDiffNumbers);
          refresh();
        }
      }
    });
    return result;
  }
  
  /**
   * Create the "show values on differences" item in the given context and return it
   * @param context_p a non-null object
   * @return result a potentially null item
   */
  protected Item createItemShowDiffValues(Menu context_p) {
    final MenuItem result = new MenuItem(context_p, SWT.RADIO);
    result.setText(Messages.ComparisonViewer_ShowValueDiffs);
    result.setToolTipText(Messages.ComparisonViewer_ShowValueDiffsTooltip);
    result.setSelection(true);
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent e_p) {
        _viewerFeatures.getInnerViewer().setDifferenceAgnostic(false);
        _viewerValuesLeft.getInnerViewer().setDifferenceAgnostic(false);
        _viewerValuesRight.getInnerViewer().setDifferenceAgnostic(false);
      }
    });
    return result;
  }
  
  /**
   * Create the "show merge impact" item in the given context and return it
   * @param context_p a non-null object
   * @return result a potentially null item
   */
  protected Item createItemShowImpact(Menu context_p) {
    final MenuItem result = new MenuItem(context_p, SWT.CHECK);
    result.setText(Messages.ComparisonViewer_ImpactMenuItem);
    result.setToolTipText(Messages.ComparisonViewer_ImpactMenuItemTooltip);
    // Initialization
    addPropertyChangeListener(new IPropertyChangeListener() {
      /**
       * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
       */
      public void propertyChange(PropertyChangeEvent event_p) {
        if (PROPERTY_CURRENT_INPUT.equals(event_p.getProperty())) {
          EMFDiffNode input = getInput();
          if (input != null && !result.isDisposed())
            result.setSelection(input.isShowMergeImpact());
        }
      }
    });
    // Selection
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent e_p) {
        boolean showImpact = result.getSelection();
        EMFDiffNode input = getInput();
        if (input != null) {
          input.setShowMergeImpact(showImpact);
          input.setDefaultShowImpact(showImpact);
        }
      }
    });
    return result;
  }
  
  /**
   * Create the "show uncounted elements" item in the given context and return it
   * @param context_p a non-null object
   * @return result a potentially null item
   */
  protected Item createItemShowUncounted(Menu context_p) {
    final MenuItem result = new MenuItem(context_p, SWT.CHECK);
    result.setText(Messages.ComparisonViewer_ShowUncountedMenuItem);
    result.setToolTipText(Messages.ComparisonViewer_ShowUncountedMenuItemTooltip);
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent e_p) {
        if (result.getSelection())
          _viewerSynthesisMain.getInnerViewer().removeFilter(_filterUnchangedElements);
        else
          _viewerSynthesisMain.getInnerViewer().addFilter(_filterUnchangedElements);
      }
    });
    return result;
  }
  
  /**
   * Create the "support undo/redo" item in the given context and return it
   * @param context_p a non-null object
   * @return result a potentially null item
   */
  protected Item createItemSupportUndoRedo(Menu context_p) {
    final MenuItem result = new MenuItem(context_p, SWT.CHECK);
    result.setText(Messages.ComparisonViewer_SupportUndoRedoMenuItem);
    result.setToolTipText(Messages.ComparisonViewer_SupportUndoRedoMenuItemTooltip);
    // Initialization
    addPropertyChangeListener(new IPropertyChangeListener() {
      /**
       * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
       */
      public void propertyChange(PropertyChangeEvent event_p) {
        if (PROPERTY_CURRENT_INPUT.equals(event_p.getProperty())) {
          EMFDiffNode input = getInput();
          if (input != null && !result.isDisposed()) {
            result.setSelection(input.isUndoRedoSupported());
            result.setEnabled(input.getEditingDomain() != null);
          }
        }
      }
    });
    // Selection
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent e_p) {
        EMFDiffNode input = getInput();
        if (input != null)
          input.setUndoRedoSupported(result.getSelection());
      }
    });
    return result;
  }
  
  /**
   * Create the "sort" item in the given context and return it
   * @param context_p a non-null object
   * @return a potentially null item
   */
  protected Item createItemSort(Menu context_p) {
    final MenuItem result = new MenuItem(context_p, SWT.CHECK);
    result.setImage(EMFDiffMergeUIPlugin.getDefault().getImage(
        EMFDiffMergeUIPlugin.ImageID.SORT));
    result.setText(Messages.ComparisonViewer_SortTooltip);
    result.setToolTipText(Messages.ComparisonViewer_EnhancedSortTooltip);
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent event_p) {
        if (result.getSelection())
          _viewerSynthesisMain.getInnerViewer().setComparator(_sorterSynthesis);
        else
          _viewerSynthesisMain.getInnerViewer().setComparator(null);
      }
    });
    return result;
  }
  
  /**
   * Create the "sync" item in the given context and return it
   * @param context_p a non-null object
   * @return a potentially null item
   */
  protected Item createItemSync(Menu context_p) {
    final MenuItem result = new MenuItem(context_p, SWT.CHECK);
    result.setImage(EMFDiffMergeUIPlugin.getDefault().getImage(
        EMFDiffMergeUIPlugin.ImageID.SYNCED));
    result.setText(Messages.ComparisonViewer_LinkViewsTooltip);
    result.setToolTipText(Messages.ComparisonViewer_EnhancedLinkViewsTooltip);
    result.setSelection(_isLeftRightSynced);
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent event_p) {
        boolean synced = result.getSelection();
        _isLeftRightSynced = synced;
        if (_isLeftRightSynced) {
          BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
            /**
             * @see java.lang.Runnable#run()
             */
            public void run() {
              IStructuredSelection selection = _viewerSynthesisMain.getSelection();
              _viewerSynthesisLeft.setSelection(getSelectionAsSide(selection, true), true);
              _viewerSynthesisRight.setSelection(getSelectionAsSide(selection, false), true);
            }
          });
        }
      }
    });
    return result;
  }
  
  /**
   * Create the "use custom icons" item in the given context and return it
   * @param context_p a non-null object
   * @return result a potentially null item
   */
  protected Item createItemUseCustomIcons(Menu context_p) {
    final MenuItem result = new MenuItem(context_p, SWT.CHECK);
    result.setText(Messages.ComparisonViewer_IconsMenuItem);
    result.setToolTipText(Messages.ComparisonViewer_IconsMenuItemTooltip);
    // Initialization
    addPropertyChangeListener(new IPropertyChangeListener() {
      /**
       * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
       */
      public void propertyChange(PropertyChangeEvent event_p) {
        if (PROPERTY_CURRENT_INPUT.equals(event_p.getProperty())) {
          EMFDiffNode input = getInput();
          if (input != null && !result.isDisposed())
            result.setSelection(input.usesCustomIcons());
        }
      }
    });
    // Selection
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent e_p) {
        EMFDiffNode input = getInput();
        if (input != null) {
          input.setUseCustomIcons(result.getSelection());
          _viewerSynthesisMain.refresh();
          _viewerFeatures.refresh();
          _viewerValuesLeft.refresh();
          _viewerValuesRight.refresh();
        }
      }
    });
    return result;
  }
  
  /**
   * Create the "use custom labels" item in the given context and return it
   * @param context_p a non-null object
   * @return result a potentially null item
   */
  protected Item createItemUseCustomLabels(Menu context_p) {
    final MenuItem result = new MenuItem(context_p, SWT.CHECK);
    result.setText(Messages.ComparisonViewer_LabelsMenuItem);
    result.setToolTipText(Messages.ComparisonViewer_LabelsMenuItemTooltip);
    // Initialization
    addPropertyChangeListener(new IPropertyChangeListener() {
      /**
       * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
       */
      public void propertyChange(PropertyChangeEvent event_p) {
        if (PROPERTY_CURRENT_INPUT.equals(event_p.getProperty())) {
          EMFDiffNode input = getInput();
          if (input != null && !result.isDisposed())
            result.setSelection(input.usesCustomLabels());
        }
      }
    });
    // Selection
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent e_p) {
        EMFDiffNode input = getInput();
        if (input != null) {
          input.setUseCustomLabels(result.getSelection());
          _viewerSynthesisMain.refresh();
          _viewerFeatures.refresh();
          _viewerValuesLeft.refresh();
          _viewerValuesRight.refresh();
        }
      }
    });
    return result;
  }
  
  /**
   * Create the "Use technical representation" item in the given context and return it
   * @param context_p a non-null object
   * @return result a potentially null item
   */
  protected Item createItemUseTechnicalRepresentation(Menu context_p) {
      final MenuItem result = new MenuItem(context_p, SWT.CHECK);
      result.setText(Messages.ComparisonViewer_UseTechnicalRepresentation);
      result.setToolTipText(Messages.ComparisonViewer_UseTechnicalRepresentationTooltip);
      result.addSelectionListener(new SelectionAdapter() {
        /**
         * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        @Override
        public void widgetSelected(SelectionEvent e_p) {
          _viewerFeatures.getInnerViewer().setTechnical(result.getSelection());
        }
      });
      return result;
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.viewers.AbstractComparisonViewer#createNavigatable()
   */
  @Override
  protected INavigatable createNavigatable() {
    INavigatable result = new INavigatable() {
      /**
       * @see org.eclipse.compare.INavigatable#getInput()
       */
      public Object getInput() {
        return ComparisonViewer.this.getInput();
      }
      /**
       * @see org.eclipse.compare.INavigatable#hasChange(int)
       */
      public boolean hasChange(int changeFlag_p) {
        return true;
      }
      /**
       * @see org.eclipse.compare.INavigatable#openSelectedChange()
       */
      public boolean openSelectedChange() {
        return false;
      }
      /**
       * @see org.eclipse.compare.INavigatable#selectChange(int)
       */
      public boolean selectChange(int changeFlag_p) {
        boolean innerResult = false;
        switch (changeFlag_p) {
        case INavigatable.NEXT_CHANGE:
          innerResult = navigate(true); break;
        case INavigatable.PREVIOUS_CHANGE:
          innerResult = navigate(false); break;
        }
        return innerResult;
      }
    };
    return result;
  }
  
  /**
   * Create and return the lower row of the GUI
   * @param parent_p a non-null composite
   * @return a non-null widget
   */
  protected SashForm createRowLower(Composite parent_p) {
    SashForm result = new SashForm(parent_p, SWT.HORIZONTAL);
    // Features section
    _viewerFeatures = createViewerFeatures(result);
    // Values section
    _viewerValuesLeft = createViewerValues(result, true);
    _viewerValuesRight = createViewerValues(result, false);
    return result;
  }
  
  /**
   * Create and return the upper row of the GUI
   * @param parent_p a non-null composite
   * @return a non-null widget
   */
  protected SashForm createRowUpper(Composite parent_p) {
    SashForm result = new SashForm(parent_p, SWT.HORIZONTAL);
    _viewerSynthesisMain = createViewerSynthesis(result);
    _viewerSynthesisLeft = createViewerSynthesisSide(result, true);
    _viewerSynthesisRight = createViewerSynthesisSide(result, false);
    return result;
  }
  
  /**
   * Create context menus for the given viewer
   * @param viewer_p a non-null viewer
   * @param useLocalSelectionProvider_p whether the selection provider of the viewer must be used
   * @return a potentially null menu manager for the context menus
   */
  protected MenuManager createViewerContextMenus(HeaderViewer<?> viewer_p,
      boolean useLocalSelectionProvider_p) {
    MenuManager result = new MenuManager();
    result.setRemoveAllWhenShown(true);
    Control control = viewer_p.getInnerViewer().getControl();
    Menu contextMenu = result.createContextMenu(control);
    control.setMenu(contextMenu);
    ISelectionProvider selectionProvider = useLocalSelectionProvider_p?
        viewer_p.getInnerViewer(): getMultiViewerSelectionProvider();
    // Diff/merge-specific menu items
    populateContextMenu(result, viewer_p, selectionProvider);

    return result;
  }
  
  /**
   * Create, configure and return a features viewer
   * @param parent_p a non-null composite
   * @return a non-null viewer
   */
  protected EnhancedFeaturesViewer createViewerFeatures(Composite parent_p) {
    final EnhancedFeaturesViewer result = doCreateViewerFeatures(parent_p);
    // User selection: send to global viewer
    result.addSWTSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent event_p) {
        IStructuredSelection selection = result.getSelection();
        if (selection.size() == 1) {
          IMatch match = result.getInput() == null? null:
            result.getInput().getMatch();
          if (match instanceof EMatch) {
            EStructuralFeature feature = (EStructuralFeature)selection.getFirstElement();
            MatchAndFeature newInputDetails = new MatchAndFeatureImpl((EMatch)match, feature);
            setSelection(new ComparisonSelectionImpl(
                newInputDetails, getDrivingRole(), getInput()), true, result.getInnerViewer());
          }
        }
      }
    });
    // Global selection change: update local selection
    addSelectionChangedListener(new ISelectionChangedListener() {
      /**
       * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
       */
      public void selectionChanged(SelectionChangedEvent event_p) {
        ISelection rawSelection = event_p.getSelection();
        Object source = event_p.getSource();
        if (rawSelection instanceof ComparisonSelection && source != result.getInnerViewer()) {
          ComparisonSelection selection = (ComparisonSelection)rawSelection;
          if (selection.getSelectedMatches().size() <= 1) {
            // No more than one match
            EMatch match = selection.asMatch();
            if (match != null) {
              // One match: new input
              FeaturesInput newInput = new FeaturesInput(getInput(), match);
              boolean changeInput = !newInput.equals(result.getInput());
              if (changeInput)
                result.setInput(newInput);
              // New selection
              EStructuralFeature feature = selection.asFeature();
              if (feature != null) {
                IStructuredSelection newSelection = new StructuredSelection(feature);
                result.setSelection(newSelection, true);
              } else if (changeInput) {
                // New input and no feature selected: select first feature if any
                EStructuralFeature firstFeature = result.getInnerViewer().getFirstIn(newInput);
                if (firstFeature != null)
                  result.setSelection(new StructuredSelection(firstFeature));
              }
            } else {
              // No match: no input
              result.setInput(null);
            }
          } else {
            // More than one match: no input
            result.setInput(null);
          }
        }
      }
    });
    return result;
  }
  
  /**
   * Create, configure and return the main viewer of the synthesis row
   * @param parent_p a non-null composite
   * @return a non-null viewer
   */
  protected EnhancedComparisonTreeViewer createViewerSynthesis(Composite parent_p) {
    final EnhancedComparisonTreeViewer result = doCreateViewerSynthesis(parent_p);
    result.getInnerViewer().addFilter(_filterUnchangedElements);
    result.getInnerViewer().addFilter(_filterMoveOrigins);
    // Update header when filtering is activated
    addPropertyChangeListener(new IPropertyChangeListener() {
      /**
       * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
       */
      public void propertyChange(PropertyChangeEvent event_p) {
        if (PROPERTY_FILTERING.equals(event_p.getProperty())) {
          Boolean filtered = (Boolean)event_p.getNewValue();
          if (filtered != null) {
            StringBuilder builder = new StringBuilder();
            builder.append(result.getDefaultHeaderText());
            if (filtered.booleanValue())
              builder.append(Messages.ComparisonViewer_Filtered);
            result.getTextLabel().setText(builder.toString());
          }
        }
      }
    });
    // User selection: send to global viewer
    result.addSWTSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent event_p) {
        IStructuredSelection selection = result.getSelection();
        if (!selection.isEmpty())
          setSelection(new ComparisonSelectionImpl(
              selection.toList(), getDrivingRole(), getInput()), true, result.getInnerViewer());
      }
    });
    // Global selection change: update local selection
    addSelectionChangedListener(new ISelectionChangedListener() {
      /**
       * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
       */
      public void selectionChanged(SelectionChangedEvent event_p) {
        ISelection rawSelection = event_p.getSelection();
        Object source = event_p.getSource();
        if (rawSelection instanceof ComparisonSelection && source != result.getInnerViewer()) {
          ComparisonSelection selection = (ComparisonSelection)rawSelection;
          // New selection
          IStructuredSelection newSelection = StructuredSelection.EMPTY;
          int matchesSize = selection.getSelectedMatches().size();
          if (matchesSize > 1) {
            newSelection = new StructuredSelection(selection.getSelectedMatches());
          } else {
            TreePath path = selection.asMatchPath();
            if (path != null) {
              newSelection = new TreeSelection(path);
            } else {
              IMatch match = selection.asMatch();
              if (match != null)
                newSelection = new StructuredSelection(match);
            }
          }
          result.setSelection(newSelection, true);
        }
      }
    });
    // ... and enable context menus
    createViewerContextMenus(result, false);
    return result;
  }
  
  /**
   * Create, configure and return the viewer in the synthesis row for the given side
   * @param parent_p a non-null composite
   * @param isLeftSide_p whether the side is left or right
   * @return a non-null viewer
   */
  protected EnhancedComparisonSideViewer createViewerSynthesisSide(Composite parent_p,
      final boolean isLeftSide_p) {
    final EnhancedComparisonSideViewer result = doCreateViewerSynthesisSide(parent_p, isLeftSide_p);
    // User selection: send to global viewer
    result.addSWTSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent event_p) {
        if (_isLeftRightSynced) {
          IStructuredSelection selection = result.getSelection();
          Role sideRole = getInput().getRoleForSide(isLeftSide_p);
          IStructuredSelection synthesisSelection = getSelectionAsSynthesis(selection, isLeftSide_p);
          if (!synthesisSelection.isEmpty())
            setSelection(new ComparisonSelectionImpl(
                synthesisSelection.toList(), sideRole, getInput()), true, result.getInnerViewer());
        }
      }
    });
    // Global selection change: update local selection
    addSelectionChangedListener(new ISelectionChangedListener() {
      /**
       * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
       */
      public void selectionChanged(SelectionChangedEvent event_p) {
        ISelection rawSelection = event_p.getSelection();
        Object source = event_p.getSource();
        if (rawSelection instanceof ComparisonSelection && source != result.getInnerViewer() &&
            (source != _viewerSynthesisMain || _isLeftRightSynced)) {
          ComparisonSelection selection = (ComparisonSelection)rawSelection;
          // New selection
          IStructuredSelection newSelection = StructuredSelection.EMPTY;
          int matchesSize = selection.getSelectedMatches().size();
          if (matchesSize > 1) {
            newSelection = new StructuredSelection(selection.getSelectedMatches());
          } else {
            TreePath path = selection.asMatchPath();
            if (path != null) {
              newSelection = new TreeSelection(path);
            } else {
              IMatch match = selection.asMatch();
              if (match != null)
                newSelection = new StructuredSelection(match);
            }
          }
          result.setSelection(getSelectionAsSide(newSelection, isLeftSide_p), true);
        }
      }
    });
    // Register as selection provider ...
    result.getInnerViewer().getControl().addFocusListener(new FocusListener() {
      /**
       * @see org.eclipse.swt.events.FocusListener#focusGained(org.eclipse.swt.events.FocusEvent)
       */
      public void focusGained(FocusEvent e_p) {
        _multiViewerSelectionProvider.setSource(result.getInnerViewer());
      }
      /**
       * @see org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
       */
      public void focusLost(FocusEvent e_p) {
        _multiViewerSelectionProvider.setSource(ComparisonViewer.this);
      }
    });
    // ... and enable context menus
    createViewerContextMenus(result, true);
    return result;
  }
  
  /**
   * Create, configure and return the values viewer for the given side
   * @param parent_p a non-null composite
   * @param isLeftSide_p whether the side is left or right
   * @return a non-null viewer
   */
  protected EnhancedValuesViewer createViewerValues(Composite parent_p,
      final boolean isLeftSide_p) {
    final EnhancedValuesViewer result = doCreateViewerValues(parent_p, isLeftSide_p);
    // User selection: send to global viewer
    result.addSWTSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent event_p) {
        IStructuredSelection selection = result.getSelection();
        if (!selection.isEmpty()) {
          if (selection.getFirstElement() instanceof EObject) { // Skip attribute values
            setSelection(new ComparisonSelectionImpl(
                selection.toList(), getInput().getRoleForSide(isLeftSide_p), getInput()), true, result.getInnerViewer());
            // One element selected: show it in scope viewer
            if (selection.size() == 1) {
              EObject selectedElement = (EObject)selection.getFirstElement();
              IMatch match;
              if (selectedElement instanceof IMatch) {
                match = (IMatch)selectedElement;
              } else if (selectedElement instanceof IReferenceValuePresence) {
                IReferenceValuePresence rvp = (IReferenceValuePresence)selectedElement;
                boolean containment = rvp.isContainment();
                match = containment? rvp.getElementMatch(): rvp.getValueMatch();
              } else {
                match = null;
              }
              if (match != null)
                getModelScopeViewer(isLeftSide_p).setSelection(
                    new StructuredSelection(match.get(getInput().getRoleForSide(isLeftSide_p))));
            }
          }
        }
      }
    });
    // Global selection change: update local selection
    addSelectionChangedListener(new ISelectionChangedListener() {
      /**
       * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
       */
      public void selectionChanged(SelectionChangedEvent event_p) {
        ISelection rawSelection = event_p.getSelection();
        Object source = event_p.getSource();
        if (rawSelection instanceof ComparisonSelection && source != result.getInnerViewer()) {
          ComparisonSelection selection = (ComparisonSelection)rawSelection;
          EStructuralFeature feature = selection.asFeature();
          if (feature != null) {
            // New input
            MatchAndFeature mnf = new MatchAndFeatureImpl(selection.asMatch(), feature);
            ValuesInput newInput = new ValuesInput(getInput(), mnf);
            if (!newInput.equals(result.getInput()))
              result.setInput(newInput);
            // New selection
            List<EValuePresence> values = selection.getSelectedValuePresences();
            result.setSelection(new StructuredSelection(values), true);
          } else {
            // No feature in selection
            // Determine whether there is a change of match, triggering a new input and selection
            ValuesInput newInput = null;
            if (selection.getSelectedMatches().size() <= 1 && getInput() != null) {
              // No more than one match
              EMatch newMatch = selection.asMatch();
              if (newMatch != null) {
                // One match
                EMatch currentMatch = null;
                ValuesInput currentInput = result.getInput();
                if (currentInput != null && currentInput.getMatchAndFeature() != null)
                  currentMatch = currentInput.getMatchAndFeature().getMatch();
                if (newMatch != currentMatch) {
                  // New match is different from current match
                  HeaderViewer<?> rawFeaturesViewer = getFeaturesViewer();
                  if (rawFeaturesViewer instanceof EnhancedFeaturesViewer) {
                    EnhancedFeaturesViewer featuresViewer = (EnhancedFeaturesViewer)rawFeaturesViewer;
                    FeaturesInput featuresInput = new FeaturesInput(getInput(), newMatch);
                    EStructuralFeature firstFeature = featuresViewer.getInnerViewer().getFirstIn(featuresInput);
                    if (firstFeature != null) {
                      // First feature must be selected
                      newInput = new ValuesInput(getInput(), new MatchAndFeatureImpl(newMatch, firstFeature));
                    }
                  }
                } else {
                  // Same match and no feature
                  newInput = currentInput;
                }
              }
            }
            result.setInput(newInput);
          }
        }
      }
    });
    return result;
  }
  
  /**
   * Create and return a features viewer
   * @param parent_p a non-null composite
   * @return a non-null viewer
   */
  protected EnhancedFeaturesViewer doCreateViewerFeatures(Composite parent_p) {
    return new EnhancedFeaturesViewer(parent_p);
  }
  
  /**
   * Create and return the main viewer of the synthesis row
   * @param parent_p a non-null composite
   * @return a non-null viewer
   */
  protected EnhancedComparisonTreeViewer doCreateViewerSynthesis(Composite parent_p) {
    return new EnhancedComparisonTreeViewer(parent_p);
  }
  
  /**
   * Create and return a viewer in the synthesis row for the given side
   * @param parent_p a non-null composite
   * @param isLeftSide_p whether the side is left or right
   * @return a non-null viewer
   */
  protected EnhancedComparisonSideViewer doCreateViewerSynthesisSide(
      Composite parent_p, boolean isLeftSide_p) {
    return new EnhancedComparisonSideViewer(parent_p, isLeftSide_p);
  }
  
  /**
   * Create a values viewer for the given side
   * @param parent_p a non-null composite
   * @param isLeftSide_p whether the side is left or right
   * @return a non-null viewer
   */
  protected EnhancedValuesViewer doCreateViewerValues(Composite parent_p, boolean isLeftSide_p) {
    return new EnhancedValuesViewer(parent_p, isLeftSide_p);
  }
  
  /**
   * Return the default respective weights of the columns (sashes) of the GUI
   * @return an int array whose size is equal to the number of columns
   */
  protected int[] getDefaultColumnWeights() {
    return new int[] {3, 2, 2};
  }
  
  /**
   * Return the default respective weights of the rows (sashes) of the GUI
   * @return an int array whose size is equal to the number of rows
   */
  protected int[] getDefaultRowWeights() {
    return new int[] {5, 2};
  }
  
  /**
   * Return the differences to merge from a given list of selected matches and the given
   * criteria
   * @param selectedMatches_p a non-null list
   * @param coverChildren_p whether children of the matches must be covered
   * @param incrementalMode_p whether optional deletions must be skipped
   * @return a non-null, potentially empty, unmodifiable list
   */
  protected List<IDifference> getDifferencesToMerge(final List<EMatch> selectedMatches_p,
      final Role destination_p, final boolean coverChildren_p, final boolean incrementalMode_p) {
    final List<IDifference> result = new ArrayList<IDifference>();
    try {
      IProgressServiceProvider.INSTANCE
          .executeBusyCursor(new IRunnableWithProgress() {
        /**
         * @see org.eclipse.jface.operation.IRunnableWithProgress#run(IProgressMonitor)
         */
        public void run(final IProgressMonitor monitor_p)
        throws InvocationTargetException, InterruptedException {
          for (EMatch selectedMatch : selectedMatches_p) {
            if (coverChildren_p)
              addDifferencesToMergeRec(result, selectedMatch, destination_p, incrementalMode_p);
            else
              addDifferencesToMerge(result, selectedMatch, destination_p, incrementalMode_p);
          }
        }
      });
    } catch (Exception e) {
      // Proceed
    }
    return Collections.unmodifiableList(result);
  }
  
  /**
   * Return the driving role for this viewer
   * @return a role which is assumed non-null after setInput(Object) has been invoked
   */
  public Role getDrivingRole() {
    return getInput() == null? null: getInput().getDrivingRole();
  }
  
  /**
   * Return the inner viewer for the features of elements with differences
   * @return a viewer which is non-null if this viewer has been properly initialized
   */
  public HeaderViewer<?> getFeaturesViewer() {
    return _viewerFeatures;
  }
  
  /**
   * Return the set of inner viewers of this viewer
   * @return a non-null collection
   */
  protected Collection<Viewer> getInnerViewers() {
    return Arrays.<Viewer>asList(
        _viewerSynthesisMain.getInnerViewer(),
        _viewerSynthesisLeft.getInnerViewer(),
        _viewerSynthesisRight.getInnerViewer(),
        _viewerFeatures.getInnerViewer(),
        _viewerValuesLeft.getInnerViewer(),
        _viewerValuesRight.getInnerViewer());
  }
  
  /**
   * Return the logger for diff/merge events
   * @return a non-null logger
   */
  protected Logger getLogger() {
    return EMFDiffMergeUIPlugin.getDefault().getDiffMergeLogger();
  }
  
  /**
   * Return the inner viewer for the model from the given side
   * @param left_p whether the side is left or right
   * @return a viewer which is non-null if this viewer has been properly initialized
   */
  public EnhancedComparisonSideViewer getModelScopeViewer(boolean left_p) {
    EnhancedComparisonSideViewer result = left_p? _viewerSynthesisLeft:
      _viewerSynthesisRight;
    return result;
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.viewers.AbstractComparisonViewer#getMultiViewerSelectionProvider()
   */
  @Override
  public ISelectionProvider getMultiViewerSelectionProvider() {
    return _multiViewerSelectionProvider;
  }
  
  /**
   * Return the set of selected matches from the given selection in the
   * purpose of performing actions on differences
   * @param selection_p a non-null selection
   * @return a non-null, potentially empty list
   */
  protected List<EMatch> getSelectedMatchesForInteractions(
      final ComparisonSelection selection_p) {
    List<EMatch> selectedMatches = selection_p.getSelectedMatches();
    if (selectedMatches.isEmpty()) {
      List<EMatch> treePath = selection_p.getSelectedTreePath();
      if (!treePath.isEmpty())
        selectedMatches = Collections.singletonList(treePath.get(treePath.size()-1));
    }
    return selectedMatches;
  }
  
  /**
   * @see org.eclipse.jface.viewers.Viewer#getSelection()
   */
  @Override
  public ComparisonSelection getSelection() {
    return _lastUserSelection;
  }
  
  /**
   * Return a variant of the given match-based selection for the given side.
   * Matches are converted to their elements on the given side.
   * Elements other than matches are ignored.
   * @param selection_p a non-null selection
   * @param onLeft_p whether the desired side is left or right
   * @return a non-null, potentially empty selection
   */
  protected IStructuredSelection getSelectionAsSide(
      IStructuredSelection selection_p, boolean onLeft_p) {
    List<EObject> result = new FArrayList<EObject>();
    if (getInput() != null) {
      Role role = getInput().getRoleForSide(onLeft_p);
      for (Object selected : selection_p.toArray()) {
        if (selected instanceof IMatch) {
          EObject element = ((IMatch)selected).get(role);
          if (element != null)
            result.add(element);
        }
      }
    }
    return new StructuredSelection(result);
  }
  
  /**
   * Return a variant of the given element-based selection as a
   * match-based selection. Elements from the given side are converted to
   * their corresponding matches. Other elements are ignored.
   * @param selection_p a non-null selection
   * @param onLeft_p whether the original side is left or right
   * @return a non-null, potentially empty selection
   */
  protected IStructuredSelection getSelectionAsSynthesis(
      IStructuredSelection selection_p, boolean onLeft_p) {
    List<IMatch> result = new FArrayList<IMatch>();
    EMFDiffNode input = getInput();
    if (input != null) {
      IComparison comparison = input.getActualComparison();
      if (comparison != null) {
        Role role = input.getRoleForSide(onLeft_p);
        for (Object selected : selection_p.toArray()) {
          if (selected instanceof EObject) {
            IMatch match = comparison.getMapping().getMatchFor(
                (EObject)selected, role);
            if (match != null)
              result.add(match);
          }
        }
      }
    }
    return new StructuredSelection(result);
  }
  
  /**
   * Return the inner viewer for the comparison tree
   * @return a viewer which is non-null if this viewer has been properly initialized
   */
  public EnhancedComparisonTreeViewer getSynthesisViewer() {
    return _viewerSynthesisMain;
  }
  
  /**
   * Return the inner viewer for differences on values for the given side
   * @param left_p whether the side is left or right
   * @return a viewer which is non-null if this viewer has been properly initialized
   */
  public EnhancedValuesViewer getValuesViewer(boolean left_p) {
    EnhancedValuesViewer result = left_p? _viewerValuesLeft: _viewerValuesRight;
    return result;
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.viewers.AbstractComparisonViewer#handleDispose()
   */
  @Override
  protected void handleDispose() {
    super.handleDispose();
    _lastUserSelection = null;
    _viewerSynthesisLeft = null;
    _viewerSynthesisRight = null;
    _viewerValuesLeft = null;
    _viewerValuesRight = null;
    _viewerSynthesisMain = null;
    _viewerFeatures = null;
    _sorterSynthesis = null;
    _filterUnchangedElements = null;
    _filterMoveOrigins = null;
  }
  
  /**
   * Ignore the current selection
   * @param onLeft_p whether ignore occurs on the left
   */
  protected void ignore(boolean onLeft_p) {
    final ComparisonSelection selection = getSelection();
    if (selection == null) return; // Should not happen according to ignore tool activation
    EMFDiffNode input = getInput();
    List<EMatch> selectedMatches = getSelectedMatchesForInteractions(selection);
      // Make choices
    IgnoreChoiceData choices = new IgnoreChoiceData(
        input.isDefaultCoverChildren(), false);
    makeIgnoreChoices(choices, input, selectedMatches);
    if (!choices.isProceed()) return;
    // Ignore operation is set to proceed and choices have been made
    final Collection<IDifference> toIgnore = !selectedMatches.isEmpty()? getDifferencesToMerge(
        selectedMatches, input.getRoleForSide(onLeft_p), choices.isCoverChildren(), choices.isSideExclusive()):
          getInput().getCategoryManager().getPendingDifferencesFiltered(selection.asDifferencesToMerge());
    if (!toIgnore.isEmpty()) {
      executeOnModel(new Runnable() {
        /**
         * @see java.lang.Runnable#run()
         */
        public void run() {
          for (IDifference diff : toIgnore) {
            if (diff instanceof EElementRelativePresence) {
              EElementRelativePresence presence = (EElementRelativePresence)diff;
              getUIComparison().getDifferencesToIgnore().add(presence);
              // Also on symmetrical if any
              if (diff instanceof EValuePresence) {
                IValuePresence symmetrical = ((EValuePresence)diff).getSymmetrical();
                if (symmetrical instanceof EMergeableDifference)
                  getUIComparison().getDifferencesToIgnore().add(
                      (EMergeableDifference)symmetrical);
                // Also on symmetrical ownership if any
                if (diff instanceof IReferenceValuePresence) {
                  IReferenceValuePresence symmetricalOwnership =
                      ((IReferenceValuePresence)diff).getSymmetricalOwnership();
                  if (symmetricalOwnership instanceof EMergeableDifference)
                    getUIComparison().getDifferencesToIgnore().add(
                        (EMergeableDifference)symmetricalOwnership);
                }
              }
            }
          }
          getUIComparison().setLastActionSelection(selection);
        }
      }, onLeft_p);
      firePropertyChangeEvent(CompareEditorInput.DIRTY_STATE, new Boolean(true));
      input.updateDifferenceNumbers();
    }
  }
  
  /**
   * Initialize the non-graphical instance variables
   */
  protected void initialize() {
    _isLeftRightSynced = true;
    _lastUserSelection = null;
    _multiViewerSelectionProvider = new SelectionBridge.SingleSource();
    _multiViewerSelectionProvider.setSource(this);
    _sorterSynthesis = new ViewerComparator();
    _filterUnchangedElements = new ViewerFilter() {
      /**
       * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
       */
      @Override
      public boolean select(Viewer viewer_p, Object parentElement_p, Object element_p) {
        EMatch match = (EMatch)element_p;
        return getInput().getCategoryManager().getDifferenceNumber(match) > 0;
      }
    };
    _filterMoveOrigins = new ViewerFilter() {
      /**
       * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
       */
      @Override
      public boolean select(Viewer viewer_p, Object parentElement_p, Object element_p) {
        TreePath path;
        if (parentElement_p instanceof TreePath)
          path = ((TreePath)parentElement_p).createChildPath(element_p);
        else
          path = new TreePath(new Object[] {element_p});
        return !getInput().getCategoryManager().isMoveOrigin(path);
      }
    };
  }
  
  /**
   * Return whether user interactions must occur to determine how to execute
   * the ignore operation on the given list of matches in the context of the
   * given input and the given specification of choices
   * @param choices_p the non-null specification of the ignore choices
   * @param input_p a non-null input
   * @param selectedMatches a non-null, potentially empty list
   */
  protected boolean interactionsRequiredForIgnore(IgnoreChoiceData choices_p,
      EMFDiffNode input_p, List<EMatch> selectedMatches) {
    boolean childrenForMerge = false;
    boolean ownDifferences = false;
    // Determining whether selected matches have proper differences
    // and differences in children
    for (EMatch selectedMatch : selectedMatches) {
      ownDifferences = ownDifferences ||
          !input_p.getCategoryManager().getDifferenceKind(selectedMatch).isNeutral();
      if (childrenForMerge && ownDifferences)
        break;
      childrenForMerge = childrenForMerge ||
          input_p.getCategoryManager().hasChildrenForMergeFiltered(selectedMatch);
      if (childrenForMerge && ownDifferences)
        break;
    }
    if (!ownDifferences && childrenForMerge)
      // No own difference but differences in children: operation only
      // makes sense if children are covered
      choices_p.setCoverChildren(true);
    return ownDifferences && childrenForMerge;
  }
  
  /**
   * Return whether user interactions must occur to determine how to execute
   * the merge operation on the given list of matches in the context of the
   * given input and the given specification of choices
   * @param choices_p the non-null specification of the merge choices
   * @param input_p a non-null input
   * @param selectedMatches a non-null, potentially empty list
   */
  protected boolean interactionsRequiredForMerge(MergeChoiceData choices_p,
      EMFDiffNode input_p, List<EMatch> selectedMatches) {
    boolean result = !selectedMatches.isEmpty();
    if (result && selectedMatches.size() == 1) {
      EMatch selectedMatch = selectedMatches.get(0);
      if (!input_p.getCategoryManager().hasChildrenForMergeFiltered(selectedMatch)) {
        DifferenceKind kind = input_p.getCategoryManager().getDifferenceKind(selectedMatch);
        result = !(kind.isAddition() || kind.isDeletion());
      }
    }
    return result;
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.viewers.AbstractComparisonViewer#inputChanged(java.lang.Object, java.lang.Object)
   */
  @Override
  protected void inputChanged(Object input_p, Object oldInput_p) {
    super.inputChanged(input_p, oldInput_p);
    _viewerFeatures.setInput(null);
    _viewerValuesLeft.setInput(null);
    _viewerValuesRight.setInput(null);
    _viewerSynthesisMain.setInput(input_p);
    _viewerSynthesisLeft.setInput(input_p);
    _viewerSynthesisRight.setInput(input_p);
    if (getInput() != null && getInput().isLogEvents())
      getLogger().log(new CompareLogEvent(getEditingDomain(), getComparison()));
  }
  
  /**
   * Set the given specification of ignore choices according to the given
   * input and set of selected matches
   * @param choices_p the non-null specification of the ignore choices
   * @param input_p a non-null diff node input
   * @param selectedMatches_p the non-null, potentially empty set of matches that have
   *          been selected for merge
   */
  protected void makeIgnoreChoices(IgnoreChoiceData choices_p,
      EMFDiffNode input_p, List<EMatch> selectedMatches_p) {
    boolean requiresInteractions = interactionsRequiredForIgnore(choices_p, input_p, selectedMatches_p);
    if (requiresInteractions) {
      IgnoreChoicesDialog choicesDialog =
          new IgnoreChoicesDialog(getShell(), Messages.ComparisonViewer_IgnoreCommandName, choices_p);
      choicesDialog.open();
      if (choices_p.isProceed())
        getInput().setDefaultCoverChildren(choices_p.isCoverChildren());
    }
  }
  
  /**
   * Set the given specification of merge choices according to the given
   * input and set of selected matches
   * @param choices_p the non-null specification of the merge choices
   * @param input_p a non-null diff node input
   * @param selectedMatches_p the non-null, potentially empty set of matches that have
   *          been selected for merge
   * @param acceptIncrementalMode_p whether the incremental mode is acceptable in this context
   */
  protected void makeMergeChoices(MergeChoiceData choices_p, EMFDiffNode input_p,
      List<EMatch> selectedMatches_p, boolean acceptIncrementalMode_p) {
    boolean requiresInteractions = interactionsRequiredForMerge(
        choices_p, input_p, selectedMatches_p);
    if (requiresInteractions) {
      // Group of differences
      boolean mayAskAboutChildren = false;
      for (EMatch selectedMatch : selectedMatches_p) {
        if (input_p.getCategoryManager().getDifferenceKind(selectedMatch) == DifferenceKind.COUNTED) {
          choices_p.setCoverChildren(true);
          break;
        } else if (input_p.getCategoryManager().hasChildrenForMergeFiltered(selectedMatch)) {
          mayAskAboutChildren = true;
          break;
        }
      }
      // Choice dialog
      MergeChoicesDialog choicesDialog =
          new MergeChoicesDialog(getShell(), Messages.ComparisonViewer_MergeHeader,
              choices_p, mayAskAboutChildren, acceptIncrementalMode_p);
      choicesDialog.open();
      if (choices_p.isProceed()) {
        if (mayAskAboutChildren)
          input_p.setDefaultCoverChildren(choices_p.isCoverChildren());
        input_p.setDefaultIncrementalMode(choices_p.isIncrementalMode());
        input_p.setDefaultShowImpact(choices_p.isShowImpact());
      }
    }
  }
  
  /**
   * Merge the current selection to the given side
   * @param toLeft_p whether destination is left or right
   * @param acceptIncrementalMode_p whether the incremental mode is acceptable in this context
   */
  protected void merge(boolean toLeft_p, boolean acceptIncrementalMode_p) {
    merge(toLeft_p, acceptIncrementalMode_p, getSelection());
  }
  
  /**
   * Merge the given selection to the given side
   * @param toLeft_p whether destination is left or right
   * @param acceptIncrementalMode_p whether the incremental mode is acceptable in this context
   * @param selection_p the potentially null selection (e.g., set of matches) to merge
   */
  protected void merge(final boolean toLeft_p, boolean acceptIncrementalMode_p,
      final ComparisonSelection selection_p) {
    if (selection_p == null) return; // Should not happen according to merge tool activation
    final EMFDiffNode input = getInput();
    // Define the set of selected matches
    List<EMatch> selectedMatches = getSelectedMatchesForInteractions(selection_p);
    // Make choices
    MergeChoiceData choices = new MergeChoiceData(input.isDefaultCoverChildren(),
        input.isDefaultIncrementalMode() && acceptIncrementalMode_p,
        input.isDefaultShowImpact());
    makeMergeChoices(choices, input, selectedMatches, acceptIncrementalMode_p);
    if (!choices.isProceed()) return;
    // Merge is set to proceed and choices have been made
    final Role destination = input.getRoleForSide(toLeft_p);
    final Collection<IDifference> toMerge = !selectedMatches.isEmpty()? getDifferencesToMerge(
            selectedMatches, destination, choices.isCoverChildren(), choices.isIncrementalMode()):
          input.getCategoryManager().getPendingDifferencesFiltered(selection_p.asDifferencesToMerge());
    final Collection<IDifference> merged = new ArrayList<IDifference>();
    boolean done = false;
    if (!toMerge.isEmpty()) {
      // Merge is possible
      boolean proceed = true;
      if (choices.isShowImpact()) {
        // Show merge impact
        proceed = showMergeImpact(toMerge, toLeft_p, input);
      }
      if (proceed) {
        // Merge is confirmed
        executeOnModel(new IRunnableWithProgress() {
          /**
           * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
           */
          public void run(IProgressMonitor monitor_p) throws InvocationTargetException,
          InterruptedException {
            merged.addAll(getComparison().merge(toMerge, destination, true, monitor_p));
            getUIComparison().setLastActionSelection(selection_p);
          }
        }, toLeft_p);
        done = true;
      }
    } else {
      // Nothing to merge
      MessageDialog.openInformation(getShell(), Messages.ComparisonViewer_MergeHeader,
          Messages.ComparisonViewer_NoDiffsToMerge);
    }
    if (!merged.isEmpty() && done) {
      // React to merge
      input.setModified(true, toLeft_p);
      firePropertyChangeEvent(CompareEditorInput.DIRTY_STATE, new Boolean(true));
      input.updateDifferenceNumbers();
      if (input.isLogEvents())
        getLogger().log(
            new MergeLogEvent(getEditingDomain(), getComparison(), merged, toLeft_p));
    }
  }
  
  /**
   * Navigate to the next/previous difference according to the given flag
   * @param next_p whether navigation must be forward or back
   * @return whether the operation could not be completed due to the
   * last/first difference being reached
   */
  protected boolean navigate(boolean next_p) {
    ComparisonTreeViewer treeViewer = _viewerSynthesisMain.getInnerViewer();
    ITreeSelection selection = treeViewer.getSelection();
    TreePath current = (selection == null || selection.isEmpty())? TreePath.EMPTY:
      selection.getPaths()[0];
    TreePath newPath = next_p? treeViewer.getNextUserDifference(current):
      treeViewer.getPreviousUserDifference(current);
    if (newPath != null)
      setSelection(new ComparisonSelectionImpl(newPath, getDrivingRole(), getInput()), true);
    return newPath == null;
  }
  
  /**
   * Define the diff/merge-specific content of the contextual menu for
   * the given viewer and associated (possibly the same) selection provider
   * @param menuManager_p a non-null menu manager
   * @param viewer_p a non-null viewer
   * @param selectionProvider_p a non-null selection provider
   */
  protected void populateContextMenu(MenuManager menuManager_p,
      Viewer viewer_p, ISelectionProvider selectionProvider_p) {
    // Nothing by default
  }
  
  /**
   * @see org.eclipse.jface.viewers.Viewer#refresh()
   */
  @Override
  public void refresh() {
    _viewerSynthesisLeft.refresh();
    _viewerSynthesisRight.refresh();
    _viewerValuesLeft.refresh();
    _viewerValuesRight.refresh();
    _viewerFeatures.refresh();
    _viewerSynthesisMain.refresh();
    super.refresh();
  }
  
  /**
   * Refresh the tools of the viewer. This behavior is centralized instead of being
   * delegated to each tool via listeners in order to improve performance.
   */
  @Override
  protected void refreshTools() {
    ComparisonSelection selection = getSelection();
    EMFDiffNode input = getInput();
    // Merge
    boolean onLeft = false, onRight = false;
    boolean allowDeletion = false;
    boolean allowIgnoring = false;
    if (selection != null && input != null) {
      allowIgnoring = true;
      IValuePresence presence = selection.asValuePresence();
      if (presence != null && !presence.isMerged()) {
        // Value presence
        DifferenceKind kind = input.getCategoryManager().getDifferenceKind(presence);
        onLeft = canAddToTheRight(kind);
        onRight = canAddToTheLeft(kind);
        allowDeletion = input.getCategoryManager().isMany(presence) &&
            !input.getCategoryManager().isOwnership(presence);
      } else if (selection.asFeature() == null) {
        List<EMatch> matches = selection.asMatches();
        if (!matches.isEmpty()) {
          // Matches selected
          if (matches.size() > 1) {
            // Several matches selected
            allowDeletion = true;
            Iterator<EMatch> it = matches.iterator();
            while (it.hasNext() && (!onLeft || !onRight || allowDeletion)) {
              EMatch current = it.next();
              DifferenceKind kind = input.getCategoryManager().getDifferenceKind(current);
              if (kind.isAddition()) {
                onLeft = onLeft || kind.isLeft(true);
                onRight = onRight || kind.isRight(true);
              } else {
                onLeft = true;
                onRight = true;
                allowDeletion = false;
              }
            }
            allowDeletion = allowDeletion && (onLeft != onRight);
          } else {
            // Only one match selected
            IMatch match = matches.get(0);
            if (input.getCategoryManager().representAsModification(match) ||
                input.getCategoryManager().representAsMove(match) ||
                input.getCategoryManager().getDifferenceKind(match) == DifferenceKind.COUNTED) {
              // Modification or move or inner differences
              onLeft = true;
              onRight = true;
              allowDeletion = false;
            } else {
              // Partial match
              DifferenceKind kind = input.getCategoryManager().getDifferenceKind(match);
              onLeft = canAddToTheRight(kind);
              onRight = canAddToTheLeft(kind);
              allowDeletion = true;
            }
          }
        }
      }
    }
    if (input != null) {
      firePropertyChangeEvent(
          PROPERTY_ACTIVATION_MERGE_TO_RIGHT, new Boolean(input.isEditable(false) && onLeft));
      firePropertyChangeEvent(
          PROPERTY_ACTIVATION_DELETE_LEFT, new Boolean(input.isEditable(true) && onLeft && allowDeletion));
      firePropertyChangeEvent(
          PROPERTY_ACTIVATION_MERGE_TO_LEFT, new Boolean(input.isEditable(true) && onRight));
      firePropertyChangeEvent(
          PROPERTY_ACTIVATION_DELETE_RIGHT, new Boolean(input.isEditable(false) && onRight && allowDeletion));
    }
    firePropertyChangeEvent(
        PROPERTY_ACTIVATION_IGNORE_LEFT, new Boolean(onLeft && allowIgnoring));
    firePropertyChangeEvent(
        PROPERTY_ACTIVATION_IGNORE_RIGHT, new Boolean(onRight && allowIgnoring));
    super.refreshTools();
  }
  
  /**
   * Restart the comparison via a GUI
   */
  protected void restart() {
    // Left empty after refactoring, and making this class a Workbench-neutral
    // base implementation
  }

  /**
   * Set the "base" label provider for representing model elements
   * @param labelProvider_p a potentially null label provider, where null stands for default
   */
  public void setDelegateLabelProvider(ILabelProvider labelProvider_p) {
    for (Viewer viewer : getInnerViewers()) {
      if (viewer instanceof ContentViewer) {
        IBaseLabelProvider rawLP = ((ContentViewer)viewer).getLabelProvider();
        if (rawLP instanceof DelegatingLabelProvider) {
          DelegatingLabelProvider delegatingLP = (DelegatingLabelProvider)rawLP;
          delegatingLP.setDelegate(labelProvider_p);
        }
      }
    }
  }
  
  /**
   * @see org.eclipse.jface.viewers.Viewer#setSelection(org.eclipse.jface.viewers.ISelection, boolean)
   */
  @Override
  public void setSelection(ISelection selection_p, boolean reveal_p) {
    setSelection(selection_p, reveal_p, this);
  }
  
  /**
   * Set the selection of this viewer, indicating the source of this setting
   * @see Viewer#setSelection(ISelection, boolean)
   * @param selection_p a potentially null selection
   * @param reveal_p whether the selected elements must be revealed
   * @param source_p the potentially null source of the setting
   */
  protected void setSelection(ISelection selection_p, boolean reveal_p, Viewer source_p) {
    ComparisonSelection newSelection;
    if (selection_p instanceof ComparisonSelection &&
        (source_p == this || getInnerViewers().contains(source_p)))
      newSelection = (ComparisonSelection)selection_p; // Local selection
    else if (selection_p instanceof IStructuredSelection)
      newSelection = asComparisonSelection((IStructuredSelection)selection_p); // External selection
    else
      newSelection = new ComparisonSelectionImpl(null, null, getInput()); // Invalid selection
    _lastUserSelection = newSelection;
    fireSelectionChanged(new SelectionChangedEvent(source_p, getSelection()));
  }
  
  /**
   * Configure the columns (sashes) of the UI in terms of weights and synchronization
   * @param upperRow_p the sash form on the up side
   * @param lowerRow_p the sash form on the down side
   */
  protected void setupColumns(final SashForm upperRow_p, final SashForm lowerRow_p) {
    final int[] horizontalWeights = getDefaultColumnWeights();
    upperRow_p.setWeights(horizontalWeights);
    lowerRow_p.setWeights(horizontalWeights);
    // Synchronize lower row on upper row when the latter is subject to resize
    Control upperMiddleControl = upperRow_p.getChildren()[1];
    upperMiddleControl.addControlListener(new ControlListener() {
      /**
       * @see org.eclipse.swt.events.ControlListener#controlResized(org.eclipse.swt.events.ControlEvent)
       */
      public void controlResized(ControlEvent e_p) {
        int[] weights = upperRow_p.getWeights();
        lowerRow_p.setWeights(weights);
      }
      /**
       * @see org.eclipse.swt.events.ControlListener#controlMoved(org.eclipse.swt.events.ControlEvent)
       */
      public void controlMoved(ControlEvent e_p) {
        // Nothing
      }
    });
    // Synchronize upper row on lower row when the latter is subject to resize
    Control lowerMiddleControl = lowerRow_p.getChildren()[1];
    lowerMiddleControl.addControlListener(new ControlListener() {
      /**
       * @see org.eclipse.swt.events.ControlListener#controlResized(org.eclipse.swt.events.ControlEvent)
       */
      public void controlResized(ControlEvent e_p) {
        int[] weights = lowerRow_p.getWeights();
        upperRow_p.setWeights(weights);
      }
      /**
       * @see org.eclipse.swt.events.ControlListener#controlMoved(org.eclipse.swt.events.ControlEvent)
       */
      public void controlMoved(ControlEvent e_p) {
        // Nothing
      }
    });
  }
  
  /**
   * Create and return the menu related to the detailed representation of differences
   * in the given tool bar
   * @param toolbar_p a non-null tool bar
   * @return a potentially null menu
   */
  protected Menu setupMenuDetails(ToolBar toolbar_p) {
    new ToolItem(toolbar_p, SWT.SEPARATOR);
    Menu result = UIUtil.createMenuTool(_viewerFeatures.getToolbar());
    // Only differences
    createItemShowDiffValues(result);
    // All values
    createItemShowAllValues(result);
    // All values and features
    createItemShowAllFeatures(result);
    // Technical representation
    new MenuItem(result, SWT.SEPARATOR);
    createItemUseTechnicalRepresentation(result);
    return result;
  }
  
  /**
   * Create and return the synthesis menu in the given tool bar
   * @param toolbar_p a non-null tool bar
   * @return a potentially null menu
   */
  protected Menu setupMenuSynthesis(ToolBar toolbar_p) {
    Menu synthesisMenu = UIUtil.createMenuTool(toolbar_p);
    createItemRestart(synthesisMenu);
    new MenuItem(synthesisMenu, SWT.SEPARATOR);
    // Show all elements in synthesis
    createItemShowUncounted(synthesisMenu);
    createItemFilter(synthesisMenu);
    new MenuItem(synthesisMenu, SWT.SEPARATOR);
    // Common presentation features
    createItemSync(synthesisMenu);
    createItemSort(synthesisMenu);
    // UI options
    new MenuItem(synthesisMenu, SWT.SEPARATOR);
    setupMenuSynthesisMisc(synthesisMenu);
    return synthesisMenu;
  }
  
  /**
   * Fill the menu of the synthesis viewer with miscellaneous features
   * @param synthesisMenu_p a non-null menu
   */
  protected void setupMenuSynthesisMisc(Menu synthesisMenu_p) {
    createItemUseCustomIcons(synthesisMenu_p);
    createItemUseCustomLabels(synthesisMenu_p);
    createItemShowDifferenceNumbers(synthesisMenu_p);
    createItemShowImpact(synthesisMenu_p);
    new MenuItem(synthesisMenu_p, SWT.SEPARATOR);
    createItemSupportUndoRedo(synthesisMenu_p);
    createItemLogEvents(synthesisMenu_p);
  }
  
  /**
   * Set up the different tool bars
   */
  protected void setupToolBars() {
    // Tools: upper row
    setupToolsSynthesis(_viewerSynthesisMain.getToolbar());
    setupToolsSynthesisSide(_viewerSynthesisLeft.getToolbar(), true);
    setupToolsSynthesisSide(_viewerSynthesisRight.getToolbar(), false);
    // Tools: lower row
    setupToolsDetails(_viewerFeatures.getToolbar());
    setupToolsDetailsSide(_viewerValuesLeft.getToolbar(), true);
    setupToolsDetailsSide(_viewerValuesRight.getToolbar(), false);
    // Menus
    setupMenuSynthesis(_viewerSynthesisMain.getToolbar());
    setupMenuDetails(_viewerFeatures.getToolbar());
    // Tool refresh on selection change
    addSelectionChangedListener(new ISelectionChangedListener() {
      /**
       * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
       */
      public void selectionChanged(SelectionChangedEvent event_p) {
        refreshTools();
      }
    });
    refreshTools();
  }
  
  /**
   * Set up the navigation tools in the given tool bar
   */
  protected void setupToolsDetails(ToolBar toolbar_p) {
    // Nothing by default
  }
  
  /**
   * Set up the tools related to the "details" row in the given tool bar
   * for the given side
   * @param toolbar_p a non-null tool bar
   * @param onLeft_p whether the side is left or right
   */
  protected void setupToolsDetailsSide(ToolBar toolbar_p, boolean onLeft_p) {
    createItemMerge(toolbar_p, !onLeft_p);
    createItemIgnore(toolbar_p, onLeft_p);
    createItemDelete(toolbar_p, onLeft_p);
  }
  
  /**
   * Set up the "synthesis" tools in the given tool bar
   * @param toolbar_p a non-null tool bar
   */
  protected void setupToolsSynthesis(ToolBar toolbar_p) {
    new ToolItem(toolbar_p, SWT.SEPARATOR);
    createItemInconsistency(toolbar_p);
    // Next / Previous
    new ToolItem(toolbar_p, SWT.SEPARATOR);
    createItemNavigationNext(toolbar_p);
    createItemNavigationPrevious(toolbar_p);
    // Expand / Collapse
    new ToolItem(toolbar_p, SWT.SEPARATOR);
    createItemExpand(toolbar_p);
    createItemCollapse(toolbar_p);
    // Filters and sync
    new ToolItem(toolbar_p, SWT.SEPARATOR);
    createItemFilter(toolbar_p);
  }
  
  /**
   * Set up the tools related to the "synthesis" row in the given tool bar
   * for the given side
   * @param toolbar_p a non-null tool bar
   * @param onLeft_p whether the side is left or right
   */
  protected void setupToolsSynthesisSide(ToolBar toolbar_p, boolean onLeft_p) {
    createItemLock(toolbar_p, onLeft_p);
  }
  
  /**
   * Show a UI representing the merge impact and return whether merge is confirmed
   * @param toMerge_p the non-null collection of differences to merge
   * @param toLeft_p whether the destination is the left-hand side
   * @param input_p a non-null object
   * @return whether to proceed with merge
   */
  protected boolean showMergeImpact(final Collection<IDifference> toMerge_p,
      final boolean toLeft_p, final EMFDiffNode input_p) {
    boolean result = true;
    final ImpactInput mergeInput = new ImpactInput(toMerge_p, toLeft_p, input_p);
    try {
      IProgressServiceProvider.INSTANCE
          .executeBusyCursor(new IRunnableWithProgress() {
        /**
         * @see org.eclipse.jface.operation.IRunnableWithProgress#run(IProgressMonitor)
         */
        public void run(final IProgressMonitor monitor_p) throws InvocationTargetException, InterruptedException {
          mergeInput.compute(monitor_p);
        }
      });
      MergeImpactMessageDialog dialog = new MergeImpactMessageDialog(
          getShell(), mergeInput, getResourceManager(),
          _viewerSynthesisMain.getInnerViewer().getLabelProvider());
      result = dialog.openAndConfirm();
    } catch (Exception exception_p) {
      // Proceed
    }
    return result;
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.viewers.AbstractComparisonViewer#undoRedoPerformed(boolean)
   */
  @Override
  protected void undoRedoPerformed(final boolean undo_p) {
    super.undoRedoPerformed(undo_p);
    if (getInput() != null)
      getInput().updateDifferenceNumbers();
  }
  
  
  /**
   * A selection listener common to all "filter" items.
   */
  protected class FilterSelectionListener extends SelectionAdapter {
    /** The non-null, potentially empty set of tool/menu items that show/hide the
     * non-modal filter dialog */
    protected final Set<Item> _filterItems;
    /** The filter dialog lastly opened */
    protected CategoryDialog _lastDialog;
    /**
     * Constructor
     */
    public FilterSelectionListener() {
      _lastDialog = null;
      _filterItems = new HashSet<Item>();
    }
    /**
     * Register the given item as being a "filter" item
     * @param item_p a non-null tool/menu item
     */
    public void addItem(Item item_p) {
      _filterItems.add(item_p);
      itemAddSelectionListener(item_p, this);
      itemAddDisposeListener(item_p, new DisposeListener() {
        /**
         * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
         */
        public void widgetDisposed(DisposeEvent e_p) {
          if (_lastDialog != null && !_lastDialog.getShell().isDisposed())
            _lastDialog.close();
        }
      });
    }
    /**
     * Set the selection state of all related items
     * @param selected_p the new selection state
     */
    protected void setSelectionForAll(boolean selected_p) {
      for (Item item : _filterItems) {
        if (!item.isDisposed() && itemGetSelection(item) != selected_p)
          itemSetSelection(item, selected_p);
      }
    }
    /**
     * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
     */
    @Override
    public void widgetSelected(SelectionEvent event_p) {
      final Item item = (Item)event_p.getSource();
      boolean selected = itemGetSelection(item);
      if (selected) {
        // Just selected
        _lastDialog = new CategoryDialog(getShell(), getInput());
        _lastDialog.open();
        _lastDialog.getShell().addDisposeListener(new DisposeListener() {
          /**
           * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
           */
          public void widgetDisposed(DisposeEvent e_p) {
            // Toggle tool item when closing
            _lastDialog = null;
            setSelectionForAll(false);
          }
        });
      } else {
        // Not selected any longer
        if (_lastDialog != null)
          _lastDialog.close();
      }
      setSelectionForAll(selected);
    }
  }
  
}
