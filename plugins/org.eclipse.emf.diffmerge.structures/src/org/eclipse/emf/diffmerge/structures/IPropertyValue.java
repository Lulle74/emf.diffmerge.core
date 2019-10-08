/*********************************************************************
 * Copyright (c) 2016-2019 Thales Global Services S.A.S.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thales Global Services S.A.S. - initial API and implementation
 **********************************************************************/
package org.eclipse.emf.diffmerge.structures;


/**
 * The value of a property which may be null, an actual value, or unknown.
 * @see IProperty
 * 
 * @param <V> the type of values of the property
 * @author Olivier Constant
 */
public interface IPropertyValue<V> {
  
  /**
   * Return whether the actual value cannot be the given one, i.e.,
   * the value is known and it is not equal to the given one.
   * Postcondition: result == isKnown() && !is(value_p)
   * @param value_p a potentially null object
   */
  boolean cannotBe(V value_p);
  
  /**
   * Return the actual value if known, or null if unknown.
   * Note that the case where the value is known and null is also possible.
   * @return a potentially null value object
   */
  V getValue();
  
  /**
   * Return whether the actual value is known and it is equal to
   * the given one (or both are null)
   * @param value_p a potentially null value object
   */
  boolean is(V value_p);
  
  /**
   * Return whether the actual value of the property is known
   */
  boolean isKnown();
  
  /**
   * Return whether the actual value of the property is unknown.
   * Class invariant: isKnown() == !isUnknown()
   */
  boolean isUnknown();
  
  /**
   * Return whether the actual value may be the given one, i.e.,
   * the value is unknown or is equal to the given one.
   * Postcondition: result == isUnknown() || is(value_p)
   * @param value_p a potentially null object
   */
  boolean mayBe(V value_p);
  
}
