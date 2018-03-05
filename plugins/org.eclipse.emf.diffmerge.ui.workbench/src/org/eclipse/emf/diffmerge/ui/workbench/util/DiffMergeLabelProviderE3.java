package org.eclipse.emf.diffmerge.ui.workbench.util;

import org.eclipse.emf.diffmerge.api.scopes.IModelScope;
import org.eclipse.emf.diffmerge.ui.EMFDiffMergeUIPlugin;
import org.eclipse.emf.diffmerge.ui.EMFDiffMergeUIPlugin.ImageID;
import org.eclipse.emf.diffmerge.ui.util.DiffMergeLabelProvider;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.graphics.Image;

/**
 * This impl is the result of refactoring out 3.x workbench dependent things out
 * of {@link DiffMergeLabelProvider} class. 
 *  
 * @author Erik Lundstrom (erik.lundstrom@solme.se)
 *
 */
public class DiffMergeLabelProviderE3 extends DiffMergeLabelProvider
{

	public DiffMergeLabelProviderE3()
	{}

	@Override
	public Image getImage( Object element_p )
	{
		Object element = element_p;
		if ( element instanceof IModelScope )
			element = ((IModelScope)element).getOriginator();
		Image result = UIUtilE3.getEMFImage( element );
		if ( result == null )
			result = EMFDiffMergeUIPlugin.getDefault().getImage( ImageID.EMPTY );
		return result;
	}

	@Override
	public String getText( Object element_p )
	{
		Object element = element_p;
		String result = null;
		if ( element instanceof IModelScope )
			element = ((IModelScope)element).getOriginator();
		if ( element instanceof EObject )
			result = UIUtilE3.getEMFText( element );
		if ( result == null ) {
			result = super.getText( element_p );
		}

		return result;
	}
}
