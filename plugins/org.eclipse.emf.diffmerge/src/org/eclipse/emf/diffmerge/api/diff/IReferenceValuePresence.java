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
package org.eclipse.emf.diffmerge.api.diff;

import org.eclipse.emf.diffmerge.api.IDiffPolicy;
import org.eclipse.emf.diffmerge.api.IMatch;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;


/**
 * A difference which represents the unmatched presence of a reference value
 * (an element being referenced).
 * @author Olivier Constant
 */
public interface IReferenceValuePresence extends IValuePresence {
  
  /**
   * @see org.eclipse.emf.diffmerge.api.diff.IValuePresence#getFeature()
   */
  EReference getFeature();
  
  /**
   * Return the difference corresponding to the opposite side of the same link, if any
   * @return a potentially null reference value presence
   */
  IReferenceValuePresence getOpposite();
  
  /**
   * @see org.eclipse.emf.diffmerge.api.diff.IValuePresence#getSymmetrical()
   */
  IReferenceValuePresence getSymmetrical();
  
  /**
   * Return the difference, if any, which is the symmetrical ownership
   * of the value in the opposite role
   * @see IReferenceValuePresence#isSymmetricalOwnershipTo(IReferenceValuePresence)
   * Class invariant: getSymmetricalOwnership() == null ||
   *                      isSymmetricalOwnershipTo(getSymmetricalOwnership())
   * @return a potentially null reference value presence
   */
  IReferenceValuePresence getSymmetricalOwnership();
  
  /**
   * @see org.eclipse.emf.diffmerge.api.diff.IValuePresence#getValue()
   */
  EObject getValue();
  
  /**
   * Return the match that corresponds to the value, if any.
   * Class invariant:
   *  isOutOfScope() || getValueMatch().get(getPresenceRole()) == getValue()
   * @return a match that is non-null if and only if the value is in the presence scope
   */
  IMatch getValueMatch();
  
  /**
   * Return whether the reference of this value presence represents a containment,
   * independently of the fact that it may represent an order.
   */
  boolean isContainment();
  
  /**
   * Return whether the given reference value presence corresponds to
   * the same link as this difference on the opposite reference
   * @param peer_p a non-null reference value presence
   */
  boolean isOppositeOf(IReferenceValuePresence peer_p);
  
  /**
   * Return whether the value is outside the presence scope.
   * Class invariant:
   *    isOutOfScope() == (getValueMatch() == null)
   * Class invariant:
   *    !isOutOfScope() || getFeature() != null
   * @see IDiffPolicy#coverOutOfScopeValue(EObject, EReference)
   */
  boolean isOutOfScope();
  
  /**
   * Return whether this reference value presence represents an ownership,
   * i.e., it represents an arc in the containment tree.
   * Class invariant: isOwnership() == !isOrder() && isContainment()
   */
  boolean isOwnership();
  
  /**
   * Return whether the given reference value presence corresponds to
   * the symmetrical ownership of the same value.
   * Postcondition: !result || isOwnership()
   * @param peer_p a non-null reference value presence
   */
  boolean isSymmetricalOwnershipTo(IReferenceValuePresence peer_p);
  
}
