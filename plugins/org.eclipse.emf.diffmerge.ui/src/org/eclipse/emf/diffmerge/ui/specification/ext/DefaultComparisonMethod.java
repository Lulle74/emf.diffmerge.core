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
package org.eclipse.emf.diffmerge.ui.specification.ext;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.diffmerge.api.IDiffPolicy;
import org.eclipse.emf.diffmerge.api.IMatchPolicy;
import org.eclipse.emf.diffmerge.api.IMergePolicy;
import org.eclipse.emf.diffmerge.api.Role;
import org.eclipse.emf.diffmerge.ui.specification.IComparisonMethodFactory;
import org.eclipse.emf.diffmerge.ui.specification.IModelScopeDefinition;


/**
 * A default implementation of IComparisonMethod aimed at being sub-classed
 * and used in the setup GUI.
 * @author Olivier Constant
 */
public class DefaultComparisonMethod extends AbstractComparisonMethod {
  
  /** The map from roles to the corresponding scope definitions */
  private final Map<Role, IModelScopeDefinition> _roleToScopeDefinition;
  
  /** The potentially null role to use as a reference in a two-way comparison */
  private Role _twoWayReferenceRole;
  
  /** The (potentially null) match policy */
  protected IMatchPolicy _matchPolicy;
  
  /** The (potentially null) diff policy */
  protected IDiffPolicy _diffPolicy;
  
  /** The (potentially null) merge policy */
  protected IMergePolicy _mergePolicy;
  
  
  /**
   * Constructor
   * @param leftScopeDef_p a non-null scope definition
   * @param rightScopeDef_p a non-null scope definition
   * @param ancestorScopeDef_p an optional scope definition
   * @param factory_p the optional factory this comparison method originates from
   */
  public DefaultComparisonMethod(IModelScopeDefinition leftScopeDef_p,
      IModelScopeDefinition rightScopeDef_p, IModelScopeDefinition ancestorScopeDef_p,
      IComparisonMethodFactory factory_p) {
    super();
    Role leftRole = getLeftRole();
    _factory = factory_p;
    _roleToScopeDefinition = new HashMap<Role, IModelScopeDefinition>();
    _roleToScopeDefinition.put(leftRole, leftScopeDef_p);
    _roleToScopeDefinition.put(leftRole.opposite(), rightScopeDef_p);
    _roleToScopeDefinition.put(Role.ANCESTOR, ancestorScopeDef_p);
    _twoWayReferenceRole = null;
    _matchPolicy = createMatchPolicy();
    _diffPolicy = createDiffPolicy();
    _mergePolicy = createMergePolicy();
  }
  
  /**
   * Create and return the diff policy
   * @return a potentially null diff policy
   */
  protected IDiffPolicy createDiffPolicy() {
    // Override for custom policy
    return null;
  }
  
  /**
   * Create and return the match policy
   * @return a potentially null match policy
   */
  protected IMatchPolicy createMatchPolicy() {
    // Override for custom policy
    return null;
  }
  
  /**
   * Create and return the merge policy
   * @return a potentially null merge policy
   */
  protected IMergePolicy createMergePolicy() {
    // Override for custom policy
    return null;
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.specification.IComparisonMethod#getModelScopeDefinition(org.eclipse.emf.diffmerge.api.Role)
   */
  public IModelScopeDefinition getModelScopeDefinition(Role role_p) {
    return _roleToScopeDefinition.get(role_p);
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.specification.IComparisonMethod#getDiffPolicy()
   */
  public IDiffPolicy getDiffPolicy() {
    return _diffPolicy;
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.specification.IComparisonMethod#getMatchPolicy()
   */
  public IMatchPolicy getMatchPolicy() {
    return _matchPolicy;
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.specification.IComparisonMethod#getMergePolicy()
   */
  public IMergePolicy getMergePolicy() {
    return _mergePolicy;
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.specification.IComparisonMethod#getTwoWayReferenceRole()
   */
  public Role getTwoWayReferenceRole() {
    return _twoWayReferenceRole;
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.specification.IComparisonMethod#setTwoWayReferenceRole(org.eclipse.emf.diffmerge.api.Role)
   */
  public void setTwoWayReferenceRole(Role role_p) {
    if (!isThreeWay())
      _twoWayReferenceRole = role_p;
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.specification.IComparisonMethod#swapScopeDefinitions(org.eclipse.emf.diffmerge.api.Role, org.eclipse.emf.diffmerge.api.Role)
   */
  public boolean swapScopeDefinitions(Role role1_p, Role role2_p) {
    boolean result = false;
    IModelScopeDefinition scope1 = getModelScopeDefinition(role1_p);
    IModelScopeDefinition scope2 = getModelScopeDefinition(role2_p);
    if (scope1 != null && scope2 != null) {
      _roleToScopeDefinition.put(role1_p, scope2);
      _roleToScopeDefinition.put(role2_p, scope1);
      result = true;
    }
    return result;
  }
  
}
