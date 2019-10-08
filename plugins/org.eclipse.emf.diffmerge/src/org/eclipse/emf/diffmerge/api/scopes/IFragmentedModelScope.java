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
package org.eclipse.emf.diffmerge.api.scopes;

import java.util.Collection;
import java.util.List;

import org.eclipse.emf.ecore.resource.Resource;


/**
 * A model scope which is aware of its underlying multi-resource persistence
 * structure. Resources are assumed to be organized in an acyclic "inclusion"
 * graph derived from the containment tree, with additional "referencing" arcs
 * that manifest the existence of at least one cross-reference between elements
 * of two resources. The "holding" resource defined in the super-interface must
 * be one of the resources.
 * @author Olivier Constant
 */
public interface IFragmentedModelScope extends IPersistentModelScope {
  
  /**
   * Return the list of the resources containing elements which belong to
   * the direct contents of elements of the given resource.
   * Postcondition: getResources().containsAll(result)
   * Postcondition: getRootResources().retainsAll(result).isEmpty()
   * @param resource_p a non-null resource belonging to getResources()
   * @return a non-null, potentially empty list
   */
  Collection<Resource> getIncludedResources(Resource resource_p);
  
  /**
   * Return the list of the resources which are not included in the
   * given resource and which contain elements that are cross-referenced
   * by elements of the given resource.
   * Postcondition: getResources().containsAll(result)
   * Postcondition: getIncludedResources(resource_p).retainsAll(result).isEmpty()
   * @param resource_p a non-null resource belonging to getResources()
   * @return a non-null, potentially empty list
   */
  Collection<Resource> getReferencedResources(Resource resource_p);
  
  /**
   * Return the list of the resources which are covered by this scope.
   * @return a non-null, potentially empty list
   */
  List<Resource> getResources();
  
  /**
   * Return the list of the root resources which are covered by this scope.
   * Class invariant: getResources().containsAll(getRootResources())
   * @return a non-null, potentially empty list
   */
  List<Resource> getRootResources();
  
  /**
   * Return whether the scope has been fully explored. While true, the result of the methods
   * related to resources remains identical.
   * Class invariant: !isFullyExplored() || isLoaded()
   */
  boolean isFullyExplored();
  
  
  /**
   * An IFragmentedModelScope which can be modified.
   * If a loading stream is defined via {@link IPersistentModelScope.Editable#setStream(java.io.InputStream)},
   * then only {@link IPersistentModelScope#getHoldingResource()} is taken into account as a root resource.
   */
  interface Editable extends IFragmentedModelScope, IPersistentModelScope.Editable {
    // Nothing more
  }
  
}
