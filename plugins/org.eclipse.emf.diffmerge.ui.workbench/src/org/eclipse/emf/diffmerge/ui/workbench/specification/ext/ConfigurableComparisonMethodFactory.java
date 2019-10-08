
/*********************************************************************
 * Copyright (c) 2013-2019 Thales Global Services S.A.S.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thales Global Services S.A.S. - initial API and implementation
 **********************************************************************/
package org.eclipse.emf.diffmerge.ui.workbench.specification.ext;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.diffmerge.ui.Messages;
import org.eclipse.emf.diffmerge.ui.specification.IComparisonMethod;
import org.eclipse.emf.diffmerge.ui.specification.IModelScopeDefinition;
import org.eclipse.emf.diffmerge.ui.specification.ext.ConfigurableComparisonMethod;
import org.eclipse.emf.diffmerge.ui.specification.ext.DefaultComparisonMethodFactory;
import org.eclipse.emf.diffmerge.ui.workbench.Activator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.provider.resource.ResourceItemProviderAdapterFactory;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * A contribution to the Diff/Merge UI extension point.
 * @author Olivier Constant
 */
public class ConfigurableComparisonMethodFactory extends DefaultComparisonMethodFactory {

	/**
	 * @see org.eclipse.emf.diffmerge.ui.specification.ext.DefaultComparisonMethodFactory#createComparisonMethod(org.eclipse.emf.diffmerge.ui.specification.IModelScopeDefinition, org.eclipse.emf.diffmerge.ui.specification.IModelScopeDefinition, org.eclipse.emf.diffmerge.ui.specification.IModelScopeDefinition)
	 */
	@Override
	public IComparisonMethod createComparisonMethod(
			IModelScopeDefinition leftScopeSpec_p, IModelScopeDefinition rightScopeSpec_p,
			IModelScopeDefinition ancestorScopeSpec_p )
	{
		return new ConfigurableComparisonMethod( leftScopeSpec_p, rightScopeSpec_p, ancestorScopeSpec_p, this ) {
			@Override
			public void dispose()
			{
				super.dispose();
				// Also clean shared adapter factory: icons associated to resources
				AdapterFactory af = Activator.getAdapterFactoryLabelProvider().getAdapterFactory();
				if ( af instanceof ComposedAdapterFactory ) {
					ComposedAdapterFactory composed = (ComposedAdapterFactory)af;
					AdapterFactory afForType = composed.getFactoryForType( Resource.class.getPackage() );
					if ( afForType instanceof ResourceItemProviderAdapterFactory ) {
						ResourceItemProviderAdapterFactory ripaf = (ResourceItemProviderAdapterFactory)afForType;
						ripaf.dispose();
					}
				}
			}

			/**
			 * Probably unnecessary override...it is just to keep the code "intact" after
			 * separation of UI/UI Workbench.
			 */
			@Override
			public Shell getShell()
			{
				Shell result;
				try {
					result = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				}
				catch ( Exception e ) {
					result = null;
				}
				return result;
			}
		};
	}

	/**
	 * @see org.eclipse.emf.diffmerge.ui.specification.IComparisonMethodFactory#getLabel()
	 */
	@Override
  public String getLabel() {
		return Messages.ConfigurableComparisonMethodFactory_Label;
	}

	/**
	 * @see org.eclipse.emf.diffmerge.ui.specification.IComparisonMethodFactory#getOverridenClasses()
	 */
	@Override
  public Collection<Class<?>> getOverridenClasses() {
		return Collections.<Class<?>> emptySet();
	}

}
