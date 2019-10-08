/*********************************************************************
 * Copyright (c) 2010-2019 Thales Global Services S.A.S.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thales Global Services S.A.S. - initial API and implementation
 **********************************************************************/
package org.eclipse.emf.diffmerge.ui.setup;

import static org.eclipse.emf.diffmerge.ui.setup.ComparisonSetup.PROPERTY_COMPARISON_METHOD;

import org.eclipse.emf.diffmerge.api.Role;
import org.eclipse.emf.diffmerge.ui.EMFDiffMergeUIPlugin;
import org.eclipse.emf.diffmerge.ui.EMFDiffMergeUIPlugin.ImageID;
import org.eclipse.emf.diffmerge.ui.Messages;
import org.eclipse.emf.diffmerge.ui.setup.ComparisonSetup.Side;
import org.eclipse.emf.diffmerge.ui.specification.IComparisonMethod;
import org.eclipse.emf.diffmerge.ui.specification.IComparisonMethodFactory;
import org.eclipse.emf.diffmerge.ui.specification.IModelScopeDefinition;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;


/**
 * The wizard page for setting up a comparison.
 * @author Olivier Constant
 */
public class ComparisonSetupWizardPage extends WizardPage {
  
  /** The non-null data object for the wizard */
  final protected ComparisonSetup _setup;
  
  
  /**
   * Constructor
   * @param name_p the non-null name of the page
   * @param setup_p a non-null data object for the wizard
   */
  public ComparisonSetupWizardPage(String name_p, ComparisonSetup setup_p) {
    super(name_p);
    _setup = setup_p;
    if (_setup.getSelectedFactory() == null)
      _setup.setSelectedFactoryToLast();
  }
  
  /**
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent_p) {
    setTitle(Messages.ComparisonSetupWizardPage_Title);
    setDescription(Messages.ComparisonSetupWizardPage_Description);
    // Main composite
    Composite composite = new Composite(parent_p, SWT.NONE);
    setControl(composite);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    composite.setLayout(new GridLayout(1, false));
    // Sections
    createRolesSection(composite);
    createComparisonMethodSection(composite);
    // Init
    _setup.swapScopeDefinitions(Role.TARGET, Role.TARGET);
    Point size = composite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
    ((GridData)parent_p.getLayoutData()).heightHint = size.y + 5;
  }
  
  /**
   * Create the section for selecting the comparison method
   * @param parent_p a non-null composite
   */
  protected void createComparisonMethodSection(Composite parent_p) {
    // Group
    Group group = new Group(parent_p, SWT.NONE);
    group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    group.setLayout(new GridLayout(1, false));
    group.setText(Messages.ComparisonSetupWizardPage_GroupMethod);
    // Widgets and viewers
    ComboViewer methodViewer = createComparisonMethodViewer(group);
    createConfigureMethodButton(group);
    // Init
    methodViewer.setInput(_setup);
    Object toSelect = _setup.getSelectedFactory();
    if (toSelect == null)
      toSelect = methodViewer.getElementAt(0);
    IStructuredSelection viewerSelection;
    if (toSelect != null)
      viewerSelection = new StructuredSelection(toSelect);
    else
      viewerSelection = new StructuredSelection();
    methodViewer.setSelection(viewerSelection);
  }
  
  /**
   * Create and return a button for configuring the comparison method
   * @param parent_p a non-null composite
   * @return a non-null button
   */
  protected Button createConfigureMethodButton(Composite parent_p) {
    final Button result = new Button(parent_p, SWT.PUSH);
    result.setText(Messages.ComparisonSetupWizardPage_ConfigureButton);
    result.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
    result.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent e_p) {
        IComparisonMethod method = _setup.getComparisonMethod();
        if (method != null)
          method.configure();
      }
    });
    _setup.addPropertyChangeListener(new IPropertyChangeListener() {
      /**
       * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
       */
      public void propertyChange(PropertyChangeEvent event_p) {
        if (PROPERTY_COMPARISON_METHOD.equals(event_p.getProperty())) {
          IComparisonMethod method = _setup.getComparisonMethod();
          result.setEnabled(method != null && method.isConfigurable());
        }
      }
    });
    return result;
  }
  
  /**
   * Create and return a viewer for selecting the comparison method
   * @param parent_p a non-null composite
   * @return a non-null viewer
   */
  protected ComboViewer createComparisonMethodViewer(Composite parent_p) {
    final ComboViewer result = new ComboViewer(parent_p);
    result.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    // Behavior
    result.setContentProvider(new IStructuredContentProvider() {
      /**
       * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
       */
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        // Nothing to do
      }
      /**
       * @see org.eclipse.jface.viewers.IContentProvider#dispose()
       */
      public void dispose() {
        // Nothing to do
      }
      /**
       * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
       */
      public Object[] getElements(Object inputElement_p) {
        Object[] localResult = new Object[0];
        if (inputElement_p instanceof ComparisonSetup) {
          ComparisonSetup selection =
            (ComparisonSetup)inputElement_p;
          localResult = selection.getApplicableComparisonMethodFactories().toArray();
        }
        return localResult;
      }
    });
    result.setLabelProvider(new LabelProvider() {
      /**
       * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
       */
      @Override
      public String getText(Object element_p) {
        String localResult;
        if (element_p instanceof IComparisonMethodFactory) {
          IComparisonMethodFactory factory = (IComparisonMethodFactory)element_p;
          localResult = factory.getLabel();
        } else {
          localResult = super.getText(element_p);
        }
        return localResult;
      }
    });
    result.setComparator(new ViewerComparator());
    result.addSelectionChangedListener(new ISelectionChangedListener() {
      /**
       * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
       */
      public void selectionChanged(SelectionChangedEvent event_p) {
        ISelection selection = event_p.getSelection();
        if (selection instanceof IStructuredSelection) {
          Object selected = ((IStructuredSelection)selection).getFirstElement();
          if (selected instanceof IComparisonMethodFactory)
            _setup.setSelectedFactory((IComparisonMethodFactory)selected);
        }
      }
    });
    return result;
  }
  
  /**
   * Create the section for selecting the roles
   * @param parent_p a non-null composite
   */
  protected void createRolesSection(Composite parent_p) {
    // Group
    Group group = new Group(parent_p, SWT.NONE);
    group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    group.setLayout(new GridLayout(2, false));
    group.setText(Messages.ComparisonSetupWizardPage_GroupRoles);
    // Subsections
    createRolesSubsection(group);
    createSwapRolesSubsection(group);
    createTargetSideSubsection(group);
  }
  
  /**
   * Create the subsection for the given side
   * @param parent_p a non-null composite
   * @param side_p the non-null side
   */
  protected void createRoleSubsection(Composite parent_p, final Side side_p) {
    final boolean isAncestor = side_p == Side.ANCESTOR;
    // Label
    String labelText = isAncestor? Messages.ComparisonSetupWizardPage_RoleAncestor:
      (side_p == Side.LEFT)? Messages.ComparisonSetupWizardPage_RoleLeft:
        Messages.ComparisonSetupWizardPage_RoleRight;
    new Label(parent_p, SWT.NONE).setText(labelText);
    // Row composite
    Composite composite = new Composite(parent_p, SWT.BORDER);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    GridLayout layout = new GridLayout(2, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    composite.setLayout(layout);
    // Text
    final Text roleText = new Text(composite, SWT.READ_ONLY | SWT.NONE);
    roleText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    _setup.addPropertyChangeListener(new IPropertyChangeListener() {
      /**
       * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
       */
      public void propertyChange(PropertyChangeEvent event_p) {
        if (ComparisonSetup.PROPERTY_ROLES.equals(event_p.getProperty())) {
          Role role = _setup.getRoleForSide(side_p);
          IModelScopeDefinition scope = _setup.getScopeDefinition(role);
          roleText.setText(scope.getLabel());
        }
      }
    });
    // "Modifiable" button
    final Button editableButton = new Button(composite, SWT.CHECK);
    editableButton.setText(Messages.ComparisonSetupWizardPage_ModifiableScope);
    editableButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    Role role = _setup.getRoleForSide(side_p);
    final IModelScopeDefinition scopeDef = _setup.getScopeDefinition(role);
    Role targetRole = _setup.getRoleForSide(_setup.getTargetSide());
    boolean isTargetOpposite = targetRole != null && targetRole.opposite() == role;
    editableButton.setEnabled(
        !isAncestor && !isTargetOpposite && scopeDef.isEditableSettable());
    editableButton.setSelection(scopeDef.isEditable());
    editableButton.addSelectionListener(new SelectionAdapter() {
      /**
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent e_p) {
        Role currentRole = _setup.getRoleForSide(side_p);
        IModelScopeDefinition currentScopeDef = _setup.getScopeDefinition(currentRole);
        currentScopeDef.setEditable(!currentScopeDef.isEditable());
      }
    });
    _setup.addPropertyChangeListener(new IPropertyChangeListener() {
      /**
       * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
       */
      public void propertyChange(PropertyChangeEvent event_p) {
        if (ComparisonSetup.PROPERTY_ROLES.equals(event_p.getProperty())) {
          Role currentRole = _setup.getRoleForSide(side_p);
          IModelScopeDefinition currentScopeDef =
              _setup.getScopeDefinition(currentRole);
          Role currentTargetRole = _setup.getRoleForSide(_setup.getTargetSide());
          boolean currentIsTargetOpposite = currentTargetRole != null &&
              currentTargetRole.opposite() == currentRole;
          editableButton.setEnabled(!isAncestor &&
              !currentIsTargetOpposite && currentScopeDef.isEditableSettable());
          editableButton.setSelection(currentScopeDef.isEditable());
        }
      }
    });
  }

  /**
   * Create the subsection for choosing roles
   * @param parent_p a non-null composite
   */
  protected void createRolesSubsection(Composite parent_p) {
    // Composite
    Composite subsection = new Composite(parent_p, SWT.NONE);
    subsection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    GridLayout rolesLayout = new GridLayout(2, false);
    rolesLayout.marginHeight = 0;
    rolesLayout.marginWidth = 0;
    subsection.setLayout(rolesLayout);
    // Role rows
    createRoleSubsection(subsection, Side.LEFT);
    createRoleSubsection(subsection, Side.RIGHT);
    if (_setup.isThreeWay())
      createRoleSubsection(subsection, Side.ANCESTOR);
  }
  
  /**
   * Create the subsection for swapping roles
   * @param parent_p a non-null composite
   */
  protected void createSwapRolesSubsection(Composite parent_p) {
    // Composite
    Composite buttonSubsection = new Composite(parent_p, SWT.NONE);
    buttonSubsection.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
    RowLayout buttonLayout = new RowLayout(SWT.VERTICAL);
    buttonLayout.justify = true;
    buttonSubsection.setLayout(buttonLayout);
    if (_setup.canSwapScopeDefinitions()) {
      // Left/right swap button
      final Button leftRightSwap = new Button(buttonSubsection, SWT.PUSH);
      leftRightSwap.setImage(EMFDiffMergeUIPlugin.getDefault().getImage(ImageID.SWAP));
      leftRightSwap.setToolTipText(Messages.ComparisonSetupWizardPage_SwapLeftRight);
      leftRightSwap.addSelectionListener(new SelectionAdapter() {
        /**
         * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        @Override
        public void widgetSelected(SelectionEvent event_p) {
          _setup.swapScopeDefinitions(Role.TARGET, Role.REFERENCE);
        }
      });
      if (_setup.isThreeWay()) {
        // Right/ancestor swap button
        final Button rightAnSwap = new Button(buttonSubsection, SWT.PUSH);
        rightAnSwap.setImage(EMFDiffMergeUIPlugin.getDefault().getImage(ImageID.SWAP));
        rightAnSwap.setToolTipText(Messages.ComparisonSetupWizardPage_SwapRightAncestor);
        rightAnSwap.addSelectionListener(new SelectionAdapter() {
          /**
           * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
           */
          @Override
          public void widgetSelected(SelectionEvent event_p) {
            _setup.swapScopeDefinitions(
                _setup.getLeftRole().opposite(), Role.ANCESTOR);
          }
        });
      }
    }
  }
  
  /**
   * Create the subsection for setting the target side in a two-way comparison
   * @param parent_p a non-null composite
   */
  protected void createTargetSideSubsection(Composite parent_p) {
    if (_setup.getScopeDefinition(Role.TARGET).isEditableSettable() &&
        _setup.getScopeDefinition(Role.REFERENCE).isEditableSettable()) {
      // Composite
      Composite composite = new Composite(parent_p, SWT.NONE);
      GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, true);
      composite.setLayoutData(gridData);
      GridLayout layout = new GridLayout(4, false);
      layout.marginHeight = 0;
      layout.marginWidth = 0;
      layout.marginTop = 2;
      composite.setLayout(layout);
      // Label
      Label label = new Label(composite, SWT.NONE);
      label.setText(Messages.ComparisonSetupWizardPage_ReferenceRole);
      label.setToolTipText(Messages.ComparisonSetupWizardPage_ReferenceRoleTooltip);
      // Left to Right
      final Button rightButton = new Button(composite, SWT.RADIO);
      rightButton.setText(Messages.ComparisonSetupWizardPage_ReferenceRight);
      rightButton.setSelection(_setup.getTargetSide() == Side.RIGHT);
      rightButton.addSelectionListener(new SelectionAdapter() {
        /**
         * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        @Override
        public void widgetSelected(SelectionEvent e_p) {
          if (rightButton.getSelection()) {
            _setup.setTargetSide(Side.RIGHT);
          }
        }
      });
      rightButton.setEnabled(rightButton.getSelection() || _setup.canChangeTargetSide());
      // Right to Left
      final Button leftButton = new Button(composite, SWT.RADIO);
      leftButton.setText(Messages.ComparisonSetupWizardPage_ReferenceLeft);
      leftButton.setSelection(_setup.getTargetSide() == Side.LEFT);
      leftButton.addSelectionListener(new SelectionAdapter() {
        /**
         * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        @Override
        public void widgetSelected(SelectionEvent e_p) {
          if (leftButton.getSelection()) {
            _setup.setTargetSide(Side.LEFT);
          }
        }
      });
      leftButton.setEnabled(leftButton.getSelection() || _setup.canChangeTargetSide());
      // Any
      final Button noneButton = new Button(composite, SWT.RADIO);
      noneButton.setText(Messages.ComparisonSetupWizardPage_ReferenceNone);
      noneButton.setSelection(_setup.getTargetSide() == null);
      noneButton.addSelectionListener(new SelectionAdapter() {
        /**
         * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        @Override
        public void widgetSelected(SelectionEvent e_p) {
          if (noneButton.getSelection()) {
            _setup.setTargetSide(null);
          }
        }
      });
      noneButton.setEnabled(noneButton.getSelection() || _setup.canChangeTargetSide());
    }
  }
  
}
