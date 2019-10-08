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
package org.eclipse.emf.diffmerge.ui.diffuidata;

import org.eclipse.emf.diffmerge.diffdata.EComparison;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.provider.IDisposable;

/**
 * <!-- begin-user-doc -->
 * A wrapper for EComparison which contains GUI-related data.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.emf.diffmerge.ui.diffuidata.UIComparison#getActualComparison <em>Actual Comparison</em>}</li>
 *   <li>{@link org.eclipse.emf.diffmerge.ui.diffuidata.UIComparison#getLastActionSelection <em>Last Action Selection</em>}</li>
 * </ul>
 *
 * @see org.eclipse.emf.diffmerge.ui.diffuidata.DiffuidataPackage#getUIComparison()
 * @model superTypes="org.eclipse.emf.diffmerge.ui.diffuidata.IDisposable"
 * @generated
 */
public interface UIComparison extends EObject, IDisposable {
  /**
   * Returns the value of the '<em><b>Actual Comparison</b></em>' reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Actual Comparison</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Actual Comparison</em>' reference.
   * @see #setActualComparison(EComparison)
   * @see org.eclipse.emf.diffmerge.ui.diffuidata.DiffuidataPackage#getUIComparison_ActualComparison()
   * @model required="true"
   * @generated
   */
  EComparison getActualComparison();

  /**
   * Sets the value of the '{@link org.eclipse.emf.diffmerge.ui.diffuidata.UIComparison#getActualComparison <em>Actual Comparison</em>}' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Actual Comparison</em>' reference.
   * @see #getActualComparison()
   * @generated
   */
  void setActualComparison(EComparison value);

  /**
   * Returns the value of the '<em><b>Last Action Selection</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * The user selection when the user last made an action
   * <!-- end-user-doc -->
   * @return the value of the '<em>Last Action Selection</em>' containment reference.
   * @see #setLastActionSelection(ComparisonSelection)
   * @see org.eclipse.emf.diffmerge.ui.diffuidata.DiffuidataPackage#getUIComparison_LastActionSelection()
   * @model containment="true"
   * @generated
   */
  ComparisonSelection getLastActionSelection();

  /**
   * Sets the value of the '{@link org.eclipse.emf.diffmerge.ui.diffuidata.UIComparison#getLastActionSelection <em>Last Action Selection</em>}' containment reference.
   * <!-- begin-user-doc -->
   * Set selection that was effective when the user last made an action
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Last Action Selection</em>' containment reference.
   * @see #getLastActionSelection()
   * @generated
   */
  void setLastActionSelection(ComparisonSelection value);

  /**
   * <!-- begin-user-doc -->
   * Clear the receiver
   * <!-- end-user-doc -->
   * @model
   * @generated
   */
  void clear();

} // UIComparison
