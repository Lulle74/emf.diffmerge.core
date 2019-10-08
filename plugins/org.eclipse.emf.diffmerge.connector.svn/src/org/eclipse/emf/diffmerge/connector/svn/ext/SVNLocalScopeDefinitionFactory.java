/*******************************************************************************
 * Copyright (c) 2015-2019 Intel Corporation and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Stephane Bouchet (Intel Corporation) - initial API and implementation
 *    Olivier Constant (Thales Global Services) - tight integration
 *******************************************************************************/
package org.eclipse.emf.diffmerge.connector.svn.ext;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.diffmerge.ui.specification.ext.URIScopeDefinitionFactory;
import org.eclipse.team.svn.ui.compare.ResourceCompareInput.ResourceElement;


/**
 * A scope definition factory for local SVN files.
 */
public class SVNLocalScopeDefinitionFactory extends URIScopeDefinitionFactory {
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.specification.ext.URIScopeDefinitionFactory#convertToURI(java.lang.Object)
   */
  @Override
  protected URI convertToURI(Object entrypoint_p) {
    URI result;
    if (entrypoint_p instanceof ResourceElement)
      result = getResourceElementURI((ResourceElement)entrypoint_p);
    else
      result = super.convertToURI(entrypoint_p);
    return result;
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.specification.AbstractScopeDefinitionFactory#getOverridenClasses()
   */
	@Override
	public Collection<? extends Class<?>> getOverridenClasses() {
		return Collections.<Class<?>> singleton(URIScopeDefinitionFactory.class);
	}
	
  /**
   * Return a URI for the given ResourceElement
   * @param resourceElement_p a non-null object
   * @return a non-null URI
   */
  protected URI getResourceElementURI(ResourceElement resourceElement_p) {
    return URI.createPlatformResourceURI(
        resourceElement_p.getLocalResource().getResource().getFullPath().toOSString(), true);
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.specification.ext.URIScopeDefinitionFactory#isApplicableTo(java.lang.Object)
   */
	@Override
	public boolean isApplicableTo(Object entrypoint_p) {
		return entrypoint_p instanceof ResourceElement &&
		    ((ResourceElement)entrypoint_p).isEditable();
	}
	
}
