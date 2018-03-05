package org.eclipse.emf.diffmerge.ui.workbench.specification.ext;

import org.eclipse.emf.diffmerge.ui.specification.IComparisonMethod;
import org.eclipse.emf.diffmerge.ui.viewers.AbstractComparisonViewer;
import org.eclipse.emf.diffmerge.ui.viewers.ComparisonViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;

/**
 * This sub interface is the result of refactoring out 3.x workbench dependent things out
 * of {@link IComparisonMethod} interface. 
 *  
 * @author Erik Lundstrom (erik.lundstrom@solme.se)
 *
 */
public interface IComparisonMethodE3 extends IComparisonMethod
{
	/**
	   * Create and return the viewer for the comparison.
	   * Although a default viewer is available, it can be customized or replaced by a different
	   * one in this operation.
	   * @see ComparisonViewer
	   * @param parent_p a non-null composite
	   * @param actionBars_p an optional IActionBars, typically for contributing global actions
	   *          such as undo/redo
	   */
	AbstractComparisonViewer createComparisonViewer( Composite parent_p, IActionBars actionBars_p );
}
