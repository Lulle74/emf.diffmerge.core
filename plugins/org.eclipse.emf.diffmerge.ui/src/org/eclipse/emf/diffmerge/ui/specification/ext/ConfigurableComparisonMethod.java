/*********************************************************************
 * Copyright (c) 2013-2019 Thales Global Services S.A.S.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thales Global Services S.A.S. - initial API and implementation
 **********************************************************************/
package org.eclipse.emf.diffmerge.ui.specification.ext;


import static org.eclipse.emf.diffmerge.impl.policies.ConfigurableMatchPolicy.CRITERION_SEMANTICS_DEFAULTCONTENTS;
import static org.eclipse.emf.diffmerge.impl.policies.ConfigurableMatchPolicy.CRITERION_STRUCTURE_ROOTS;
import static org.eclipse.emf.diffmerge.impl.policies.ConfigurableMatchPolicy.MatchCriterionKind.EXTRINSIC_ID;
import static org.eclipse.emf.diffmerge.impl.policies.ConfigurableMatchPolicy.MatchCriterionKind.INTRINSIC_ID;
import static org.eclipse.emf.diffmerge.impl.policies.ConfigurableMatchPolicy.MatchCriterionKind.SEMANTICS;
import static org.eclipse.emf.diffmerge.impl.policies.ConfigurableMatchPolicy.MatchCriterionKind.STRUCTURE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.emf.diffmerge.api.IDiffPolicy;
import org.eclipse.emf.diffmerge.api.IMatchPolicy;
import org.eclipse.emf.diffmerge.api.IMergePolicy;
import org.eclipse.emf.diffmerge.api.config.IComparisonConfigurator;
import org.eclipse.emf.diffmerge.impl.policies.ComparisonConfigurator;
import org.eclipse.emf.diffmerge.impl.policies.ConfigurableDiffPolicy;
import org.eclipse.emf.diffmerge.impl.policies.ConfigurableMatchPolicy;
import org.eclipse.emf.diffmerge.impl.policies.ConfigurableMatchPolicy.FineGrainedMatchCriterion;
import org.eclipse.emf.diffmerge.impl.policies.ConfigurableMergePolicy;
import org.eclipse.emf.diffmerge.impl.policies.DefaultMatchPolicy;
import org.eclipse.emf.diffmerge.ui.Messages;
import org.eclipse.emf.diffmerge.ui.specification.IComparisonMethodFactory;
import org.eclipse.emf.diffmerge.ui.specification.IModelScopeDefinition;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;


/**
 * A configurable multi-criteria comparison method.
 * @author Olivier Constant
 */
public class ConfigurableComparisonMethod extends DefaultComparisonMethod
implements IComparisonConfigurator.Provider {
  
  /** The "transfer data between independent models" configurator */
  public static final IComparisonConfigurator CONFIGURATOR_DATA_TRANSFER =
      new ComparisonConfigurator(
        Messages.ConfigurableComparisonMethod_Usage_Transfer,
        Messages.ConfigurableComparisonMethod_Usage_Transfer_Tooltip,
        Arrays.asList(INTRINSIC_ID, EXTRINSIC_ID, STRUCTURE, SEMANTICS),
        Arrays.asList(
            CRITERION_STRUCTURE_ROOTS,
            CRITERION_SEMANTICS_DEFAULTCONTENTS));
  
  /** The "compare versions of the same model" configurator */
  public static final IComparisonConfigurator CONFIGURATOR_VERSIONS =
    new ComparisonConfigurator(
        Messages.ConfigurableComparisonMethod_Usage_Versions,
        Messages.ConfigurableComparisonMethod_Usage_Versions_Tooltip,
        Arrays.asList(INTRINSIC_ID, EXTRINSIC_ID),
        Collections.<FineGrainedMatchCriterion>emptySet());
  
  /** The initially null lastly used comparison configuration */
  protected static ComparisonConfiguration __lastComparisonConfiguration = null;
  
  /** The initially null type of the lastly used comparison method */
  protected static Class<? extends ConfigurableComparisonMethod> __lastComparisonMethodType = null;
  
  
  /** The non-null, potentially empty list of predefined configurators */
  protected final List<IComparisonConfigurator> _configurators;
  
  
  /**
   * Constructor
   * @param leftScopeDef_p a non-null scope definition
   * @param rightScopeDef_p a non-null scope definition
   * @param ancestorScopeDef_p an optional scope definition
   * @param factory_p the optional factory this comparison method originates from
   */
  public ConfigurableComparisonMethod(IModelScopeDefinition leftScopeDef_p,
      IModelScopeDefinition rightScopeDef_p, IModelScopeDefinition ancestorScopeDef_p,
      IComparisonMethodFactory factory_p) {
    super(leftScopeDef_p, rightScopeDef_p, ancestorScopeDef_p, factory_p);
    _configurators = new ArrayList<IComparisonConfigurator>(createConfigurators());
    initialize();
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.specification.ext.DefaultComparisonMethod#configure()
   */
  @Override
  public void configure() {
    Shell shell = getShell();
    if (shell != null) {
      ComparisonConfiguration data =
          new ComparisonConfiguration(this);
      int confirmed = new ConfigureComparisonDialog(shell, data).open();
      if (Window.OK == confirmed)
        configurationConfirmed(data);
    }
  }
  
  /**
   * Handle the confirmation of the given configuration
   * @param data_p a non-null object
   */
  protected void configurationConfirmed(ComparisonConfiguration data_p) {
    __lastComparisonConfiguration = data_p;
    __lastComparisonMethodType = getClass();
    update(data_p);
  }
  
  /**
   * Create and return the ordered set of configurators for this comparison method
   * @return a non-null, potentially empty ordered set
   */
  protected List<IComparisonConfigurator> createConfigurators() {
    List<IComparisonConfigurator> result = new LinkedList<IComparisonConfigurator>();
    result.add(CONFIGURATOR_VERSIONS);
    result.add(CONFIGURATOR_DATA_TRANSFER);
    return result;
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.specification.ext.DefaultComparisonMethod#createDiffPolicy()
   */
  @Override
  protected IDiffPolicy createDiffPolicy() {
    return new ConfigurableDiffPolicy();
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.specification.ext.DefaultComparisonMethod#createMatchPolicy()
   */
  @Override
  protected IMatchPolicy createMatchPolicy() {
    return new ConfigurableMatchPolicy();
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.specification.ext.DefaultComparisonMethod#createMergePolicy()
   */
  @Override
  protected IMergePolicy createMergePolicy() {
    return new ConfigurableMergePolicy();
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.api.config.IComparisonConfigurator.Provider#getConfigurators()
   */
  public List<IComparisonConfigurator> getConfigurators() {
    return Collections.unmodifiableList(_configurators);
  }
  
  /**
   * Return the configurator to be applied by default if any
   * @return a potentially null configurator
   */
  public IComparisonConfigurator getDefaultConfigurator() {
    return CONFIGURATOR_VERSIONS;
  }
  
  /**
   * Return a shell if available
   * @return a potentially null shell (always null if current thread is not the UI thread)
   */
  public Shell getShell() {
    return Display.getCurrent() != null ? Display.getCurrent().getActiveShell()
        : null;
  }
  
  /**
   * Initialize this comparison method
   */
  protected void initialize() {
    if (__lastComparisonMethodType != null &&
        __lastComparisonMethodType.isAssignableFrom(getClass())) {
      update(__lastComparisonConfiguration);
    } else {
      IComparisonConfigurator defaultConfigurator = getDefaultConfigurator();
      if (defaultConfigurator != null) {
        defaultConfigurator.apply(this);
      }
    }
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.specification.ext.DefaultComparisonMethod#isConfigurable()
   */
  @Override
  public boolean isConfigurable() {
    return true;
  }
  
  /**
   * Update this comparison method according to the given configuration
   * @param data_p a potentially null configuration
   */
  protected void update(ComparisonConfiguration data_p) {
    if (data_p == null) return;
    // Match policy
    IMatchPolicy originalMatchPolicy = getMatchPolicy();
    ConfigurableMatchPolicy configuredMatchPolicy = data_p.getMatchPolicy();
    if (originalMatchPolicy instanceof ConfigurableMatchPolicy &&
        configuredMatchPolicy != null) {
      ((ConfigurableMatchPolicy)originalMatchPolicy).update(configuredMatchPolicy);
    } else if (originalMatchPolicy instanceof DefaultMatchPolicy) {
      ((DefaultMatchPolicy)originalMatchPolicy).setKeepMatchIDs(data_p.isKeepMatchIDs());
    }
    // Diff policy
    IDiffPolicy originalDiffPolicy = getDiffPolicy();
    ConfigurableDiffPolicy configuredDiffPolicy = data_p.getDiffPolicy();
    if (originalDiffPolicy instanceof ConfigurableDiffPolicy &&
        configuredDiffPolicy != null)
      ((ConfigurableDiffPolicy)originalDiffPolicy).update(configuredDiffPolicy);
    // Merge policy
    IMergePolicy originalMergePolicy = getMergePolicy();
    ConfigurableMergePolicy configuredMergePolicy = data_p.getMergePolicy();
    if (originalMergePolicy instanceof ConfigurableMergePolicy &&
        configuredMergePolicy != null)
      ((ConfigurableMergePolicy)originalMergePolicy).update(configuredMergePolicy);
  }
  
}
