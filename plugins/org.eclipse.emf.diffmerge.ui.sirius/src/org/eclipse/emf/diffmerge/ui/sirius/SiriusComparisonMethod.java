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
package org.eclipse.emf.diffmerge.ui.sirius;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.diffmerge.api.IDiffPolicy;
import org.eclipse.emf.diffmerge.api.IMatchPolicy;
import org.eclipse.emf.diffmerge.api.IMergePolicy;
import org.eclipse.emf.diffmerge.api.Role;
import org.eclipse.emf.diffmerge.sirius.SiriusDiffPolicy;
import org.eclipse.emf.diffmerge.sirius.SiriusMatchPolicy;
import org.eclipse.emf.diffmerge.sirius.SiriusMergePolicy;
import org.eclipse.emf.diffmerge.ui.EMFDiffMergeUIPlugin;
import org.eclipse.emf.diffmerge.ui.gmf.GMFComparisonMethod;
import org.eclipse.emf.diffmerge.ui.specification.IComparisonMethodFactory;
import org.eclipse.emf.diffmerge.ui.specification.IModelScopeDefinition;
import org.eclipse.emf.diffmerge.ui.specification.ext.URIScopeDefinition;
import org.eclipse.emf.diffmerge.ui.viewers.IDifferenceCategoryProvider;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.sirius.business.api.session.Session;
import org.eclipse.sirius.business.api.session.SessionManager;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;


/**
 * A Sirius comparison method.
 */
public class SiriusComparisonMethod extends GMFComparisonMethod {
  
  /** A non-null map from roles to the corresponding Sirius sessions
   * that may contain the model scope of the role */
  protected final Map<Role, Session> _roleToSession;
  
  
  /**
   * Constructor
   * @param leftScopeDef_p a non-null scope definition
   * @param rightScopeDef_p a non-null scope definition
   * @param ancestorScopeDef_p an optional scope definition
   * @param factory_p the optional factory this comparison method originates from
   */
  public SiriusComparisonMethod(IModelScopeDefinition leftScopeDef_p,
      IModelScopeDefinition rightScopeDef_p, IModelScopeDefinition ancestorScopeDef_p,
      IComparisonMethodFactory factory_p) {
    super(leftScopeDef_p, rightScopeDef_p, ancestorScopeDef_p, factory_p);
    _roleToSession = new HashMap<Role, Session>(3);
  }
  
  /**
   * Return the Sirius session for the given role if possible
   * (this method is only called once per role)
   * @param role_p a non-null role
   * @return a potentially null resource set
   */
  protected Session checkSession(Role role_p) {
    Session result = null;
    if (role_p != Role.ANCESTOR || isThreeWay()) {
      // Use session resource set if available
      IModelScopeDefinition scopeDefinition = getModelScopeDefinition(role_p);
      if (scopeDefinition instanceof URIScopeDefinition) {
        URIScopeDefinition uriScopeDefinition = (URIScopeDefinition)scopeDefinition;
        URI uri = uriScopeDefinition.getEntrypoint();
        result = SessionManager.INSTANCE.getExistingSession(uri);
      }
    }
    return result;
  }
  
  /**
   * Look for Sirius sessions for the scope definitions of the different roles,
   * see if they can be reused, and return the central editing domain in which
   * the whole comparison can take place if any.
   * This method is not supposed to be called more than once in a comparison method.
   * @return a potentially null editing domain
   */
  protected EditingDomain checkSessions() {
    for (Role role : Role.values()) {
      _roleToSession.put(role, checkSession(role));
    }
    Set<Session> sessions = new HashSet<Session>(_roleToSession.values());
    sessions.remove(null);
    int nbResourceSets = sessions.size();
    EditingDomain result;
    switch (nbResourceSets) {
      case 0:
        // No session found: use new editing domain
        result = createEditingDomain();
        _isDedicatedEditingDomain = true;
        break;
      case 1:
        // One session found: use its editing domain
        result = sessions.iterator().next().getTransactionalEditingDomain();
        break;
      case 2:
        // Two sessions found
        if (isThreeWay() && _roleToSession.get(Role.ANCESTOR) != null) {
          // One of them is for the ancestor: use the other
          Session sideSession = _roleToSession.get(Role.TARGET);
          if (sideSession == null) {
            sideSession = _roleToSession.get(Role.REFERENCE);
          }
          result = sideSession.getTransactionalEditingDomain(); // sideSession cannot be null
        } else {
          // The two sessions are for the left and right sides:
          // no central editing domain
          result = null;
        }
        break;
      default:
        // Three sessions found: no central editing domain
        result = null;
    }
    return result;
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.gmf.GMFComparisonMethod#createDiffPolicy()
   */
  @Override
  protected IDiffPolicy createDiffPolicy() {
    return new SiriusDiffPolicy();
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.gmf.GMFComparisonMethod#createMatchPolicy()
   */
  @Override
  protected IMatchPolicy createMatchPolicy() {
    return new SiriusMatchPolicy();
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.gmf.GMFComparisonMethod#createMergePolicy()
   */
  @Override
  protected IMergePolicy createMergePolicy() {
    return new SiriusMergePolicy();
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.specification.ext.DefaultComparisonMethod#doGetEditingDomain()
   */
  @Override
  protected EditingDomain doGetEditingDomain() {
    EditingDomain result = checkSessions();
    if (result == null && isVerbose()) {
      showNoEditingDomainWarning();
    }
    return result;
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.specification.ext.AbstractComparisonMethod#getCustomCategoryProvider()
   */
  @Override
  protected IDifferenceCategoryProvider getCustomCategoryProvider() {
    return new SiriusDifferenceCategoryProvider();
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.gmf.GMFComparisonMethod#getCustomLabelProvider()
   */
  @Override
  protected ILabelProvider getCustomLabelProvider() {
    return SiriusDiffMergeLabelProvider.getInstance();
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.specification.ext.DefaultComparisonMethod#getResourceSet(org.eclipse.emf.diffmerge.api.Role)
   */
  @Override
  public ResourceSet getResourceSet(Role role_p) {
    ResourceSet result = null;
    EditingDomain centralDomain = getEditingDomain(); // Ensure initialization
    if (centralDomain == null) {
      // Only if no central editing domain
      Session roleSession = _roleToSession.get(role_p);
      if (roleSession != null) {
        EditingDomain sessionDomain = roleSession.getTransactionalEditingDomain();
        if (sessionDomain != null) {
          result = sessionDomain.getResourceSet();
        }
      }
    }
    return result;
  }
  
  /**
   * Warn the user that no central editing domain will be used
   */
  protected void showNoEditingDomainWarning() {
    Display.getDefault().asyncExec(new Runnable() {
      /**
       * @see java.lang.Runnable#run()
       */
      public void run() {
        Shell shell = getShell();
        if (shell != null) {
          String message = Messages.SiriusComparisonMethod_UndoRedoWarning;
          MessageDialog.openWarning(shell, EMFDiffMergeUIPlugin.LABEL, message);
        }
      }
    });
  }
  
}
