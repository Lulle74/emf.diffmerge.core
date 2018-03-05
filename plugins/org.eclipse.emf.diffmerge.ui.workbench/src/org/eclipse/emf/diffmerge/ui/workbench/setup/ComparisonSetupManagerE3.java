package org.eclipse.emf.diffmerge.ui.workbench.setup;

import org.eclipse.emf.diffmerge.ui.EMFDiffMergeUIPlugin;
import org.eclipse.emf.diffmerge.ui.setup.ComparisonSetup;
import org.eclipse.emf.diffmerge.ui.setup.ComparisonSetupManager;
import org.eclipse.emf.diffmerge.ui.specification.IComparisonMethod;
import org.eclipse.swt.widgets.Shell;

/**
 * This impl is the result of refactoring out 3.x workbench dependent things out
 * of {@link ComparisonSetupManager} class. 
 *  
 * @author Erik Lundstrom (erik.lundstrom@solme.se)
 *
 */
public class ComparisonSetupManagerE3 extends ComparisonSetupManager
{

	public ComparisonSetupManagerE3()
	{
		super();
	}

	/**
	   * Return a default compare editor input for the given entry points
	   * @param entrypoint1_p a non-null object
	   * @param entrypoint2_p a non-null object
	   * @param entrypoint3_p an optional object
	   * @return a potentially null object (null means failure)
	   */
	public EMFDiffMergeEditorInput createDefaultEditorInput( Object entrypoint1_p,
			Object entrypoint2_p, Object entrypoint3_p )
	{
		EMFDiffMergeEditorInput result = null;
		try {
			ComparisonSetup setup = createComparisonSetup( entrypoint1_p, entrypoint2_p, entrypoint3_p );
			if ( setup != null && setup.getComparisonMethod() != null )
				result = new EMFDiffMergeEditorInput( setup.getComparisonMethod() );
		}
		catch ( IllegalArgumentException e ) {
			handleSetupError( null, e.getLocalizedMessage() );
		}
		return result;
	}

	/**
	   * Create and return a compare editor input as a result of user interactions
	   * for the given entry points, if possible
	   * @param shell_p a non-null shell
	   * @param entrypoint1_p a non-null object
	   * @param entrypoint2_p a non-null object
	   * @param entrypoint3_p an optional object
	   */
	public EMFDiffMergeEditorInput createEditorInputWithUI( Shell shell_p, Object entrypoint1_p,
			Object entrypoint2_p, Object entrypoint3_p )
	{
		EMFDiffMergeEditorInput result = null;
		try {
			ComparisonSetupManager manager = EMFDiffMergeUIPlugin.getDefault().getSetupManager();
			ComparisonSetup setup = manager.createComparisonSetup( entrypoint1_p, entrypoint2_p, entrypoint3_p );
			result = createEditorInputWithUI( shell_p, setup );
		}
		catch ( IllegalArgumentException e ) {
			handleSetupError( shell_p, e.getLocalizedMessage() );
		}
		return result;
	}

	/**
	   * Create and return a compare editor input as a result of user interactions
	   * for the given comparison setup, if possible
	   * @param shell_p a non-null shell
	   * @param setup_p a comparison setup or null if none could be computed
	   */
	public EMFDiffMergeEditorInput createEditorInputWithUI( Shell shell_p,
			ComparisonSetup setup_p )
	{
		EMFDiffMergeEditorInput result = null;
		if ( setup_p != null ) {
			IComparisonMethod method = openSetupWizard( shell_p, setup_p );
			if ( method != null )
				result = new EMFDiffMergeEditorInput( method );
		}
		else {
			handleSetupError( shell_p, null );
		}
		return result;
	}

	/**
	   * Update the given editor input through a configuration GUI
	   * @param shell_p a non-null shell
	   * @param input_p a non-null, non-disposed editor input
	   * @return whether the operation succeeded
	   */
	public boolean updateEditorInputWithUI( Shell shell_p,
			EMFDiffMergeEditorInput input_p )
	{
		assert input_p.getComparisonMethod() != null;
		boolean result = false;
		ComparisonSetup setup = new ComparisonSetup( input_p.getComparisonMethod() );
		setup.setCanSwapScopeDefinitions( false );
		IComparisonMethod method = openSetupWizard( shell_p, setup );
		if ( method != null ) {
			input_p.setComparisonMethod( method );
			result = true;
		}
		return result;
	}
}
