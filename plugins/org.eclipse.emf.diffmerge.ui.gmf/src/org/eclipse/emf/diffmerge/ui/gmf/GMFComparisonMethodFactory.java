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
package org.eclipse.emf.diffmerge.ui.gmf;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.emf.diffmerge.ui.specification.IComparisonMethod;
import org.eclipse.emf.diffmerge.ui.specification.IModelScopeDefinition;
import org.eclipse.emf.diffmerge.ui.workbench.specification.ext.ConfigurableComparisonMethodFactory;


/**
 * A ComparisonConfigurationFactory with GMF-specific features.
 * @author Olivier Constant
 */
public class GMFComparisonMethodFactory extends ConfigurableComparisonMethodFactory {
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.e3.specification.ext.ConfigurableComparisonMethodFactory#createComparisonMethod(org.eclipse.emf.diffmerge.ui.specification.IModelScopeDefinition, org.eclipse.emf.diffmerge.ui.specification.IModelScopeDefinition, org.eclipse.emf.diffmerge.ui.specification.IModelScopeDefinition)
   */
  @Override
  public IComparisonMethod createComparisonMethod(
      IModelScopeDefinition leftScopeSpec_p,
      IModelScopeDefinition rightScopeSpec_p,
      IModelScopeDefinition ancestorScopeSpec_p) {
    return new GMFComparisonMethod(
        leftScopeSpec_p, rightScopeSpec_p, ancestorScopeSpec_p, this);
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.e3.specification.ext.ConfigurableComparisonMethodFactory#getLabel()
   */
  @Override
  public String getLabel() {
    return Messages.GmfComparisonFactory_Label;
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.e3.specification.ext.ConfigurableComparisonMethodFactory#getOverridenClasses()
   */
  @Override
  public Collection<Class<?>> getOverridenClasses() {
    return Collections.<Class<?>>singleton(ConfigurableComparisonMethodFactory.class);
  }
  
}
