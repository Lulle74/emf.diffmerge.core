package org.eclipse.emf.diffmerge.ui.workbench.util;

import org.eclipse.emf.diffmerge.ui.util.UIUtil;
import org.eclipse.emf.diffmerge.ui.workbench.Activator;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.provider.IItemLabelProvider;
import org.eclipse.emf.edit.ui.provider.ExtendedImageRegistry;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.swt.graphics.Image;

/**
 * This class is the result of refactoring out 3.x workbench dependent things out
 * of {@link UIUtil} class. 
 *  
 * @author Erik Lundstrom (erik.lundstrom@solme.se)
 *
 */
public class UIUtilE3
{

	/**
	   * Return an image for the given element solely based on EMF mechanisms
	   * @param element_p a non-null object
	   * @return a potentially null image
	   */
	public static Image getEMFImage( Object element_p )
	{
		Image result = Activator.getAdapterFactoryLabelProvider().getImage( element_p );
		if ( result == null ) {
			// Try editing domain
			EditingDomain rawEditingDomain = AdapterFactoryEditingDomain.getEditingDomainFor( element_p );
			if ( rawEditingDomain == null )
				rawEditingDomain = TransactionUtil.getEditingDomain( element_p );
			if ( rawEditingDomain instanceof AdapterFactoryEditingDomain ) {
				AdapterFactoryEditingDomain editingDomain = (AdapterFactoryEditingDomain)rawEditingDomain;
				IItemLabelProvider provider = (IItemLabelProvider)editingDomain.getAdapterFactory().adapt( element_p, IItemLabelProvider.class );
				if ( provider != null ) {
					Object rawImage = provider.getImage( element_p );
					if ( rawImage != null )
						result = ExtendedImageRegistry.getInstance().getImage( rawImage );
				}
			}
		}
		return result;
	}

	public static String getEMFText( Object element_p )
	{
		String result = Activator.getAdapterFactoryLabelProvider().getText( element_p );
		if ( result == null ) {
			result = UIUtil.getEMFText( element_p );
		}
		return result;
	}
}
