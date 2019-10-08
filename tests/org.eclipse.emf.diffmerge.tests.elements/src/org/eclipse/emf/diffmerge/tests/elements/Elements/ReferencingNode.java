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
package org.eclipse.emf.diffmerge.tests.elements.Elements;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Referencing Node</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.eclipse.emf.diffmerge.tests.elements.Elements.ReferencingNode#getReferenced <em>Referenced</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.eclipse.emf.diffmerge.tests.elements.Elements.ElementsPackage#getReferencingNode()
 * @model
 * @generated
 */
public interface ReferencingNode extends Node {
	/**
   * Returns the value of the '<em><b>Referenced</b></em>' reference.
   * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Referenced</em>' reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
   * @return the value of the '<em>Referenced</em>' reference.
   * @see #setReferenced(Node)
   * @see org.eclipse.emf.diffmerge.tests.elements.Elements.ElementsPackage#getReferencingNode_Referenced()
   * @model required="true"
   * @generated
   */
	Node getReferenced();

	/**
   * Sets the value of the '{@link org.eclipse.emf.diffmerge.tests.elements.Elements.ReferencingNode#getReferenced <em>Referenced</em>}' reference.
   * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
   * @param value the new value of the '<em>Referenced</em>' reference.
   * @see #getReferenced()
   * @generated
   */
	void setReferenced(Node value);

} // ReferencingNode
