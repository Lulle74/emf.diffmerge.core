/**
 * <copyright>
 * 
 * Copyright (c) 2010-2017 Thales Global Services S.A.S and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Thales Global Services S.A.S. - initial API and implementation
 *    Stephane Bouchet (Intel Corporation) - Bug #442492 : hide number of differences in the UI
 * 
 * </copyright>
 */
package org.eclipse.emf.diffmerge.ui.workbench.viewers;

import org.eclipse.emf.diffmerge.diffdata.EComparison;
import org.eclipse.emf.diffmerge.ui.viewers.EMFDiffNode;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.ui.IEditorInput;

/**
 * An ICompareInput that wraps a model comparison.
 * @author Olivier Constant
 */
public class EMFDiffNodeE3 extends EMFDiffNode
{
	/** The optional associated editor input */
	private IEditorInput _editorInput = null;

	/**
	 * Constructor
	 * @param comparison_p a non-null comparison
	 */
	public EMFDiffNodeE3( EComparison comparison_p )
	{
		this( comparison_p, null );
	}

	/**
	 * Constructor
	 * @param comparison_p a non-null comparison
	 * @param domain_p the optional editing domain for undo/redo
	 */
	public EMFDiffNodeE3( EComparison comparison_p, EditingDomain domain_p )
	{
		this( comparison_p, domain_p, true, true );
	}

	/**
	 * Constructor
	 * @param comparison_p a non-null comparison
	 * @param domain_p the optional editing domain for undo/redo
	 * @param isLeftEditionPossible_p whether edition on the left is possible at all
	 * @param isRightEditionPossible_p whether edition on the right is possible at all
	 */
	public EMFDiffNodeE3( EComparison comparison_p, EditingDomain domain_p,
			boolean isLeftEditionPossible_p, boolean isRightEditionPossible_p )
	{
		super( comparison_p, domain_p, isLeftEditionPossible_p, isRightEditionPossible_p );
	}

	/**
	 * Return the editor input associated to this node, if any
	 * @return a potentially null editor input
	 */
	public IEditorInput getEditorInput()
	{
		return _editorInput;
	}

	/**
	   * Set the editor input associated to this node
	   * @param editorInput_p a potentially null editor input
	   */
	public void setEditorInput( IEditorInput editorInput_p )
	{
		_editorInput = editorInput_p;
	}

	/**
	 * @see org.eclipse.emf.edit.provider.IDisposable#dispose()
	 */
	public void dispose()
	{
		super.dispose();
		_editorInput = null;
	}

}