/**
 * <copyright>
 * 
 * Copyright (c) 2010-2017 Thales Global Services S.A.S.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Thales Global Services S.A.S. - initial API and implementation
 * 
 * </copyright>
 */
package org.eclipse.emf.diffmerge.ui.workbench.specification.ext;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.diffmerge.ui.specification.IComparisonMethod;
import org.eclipse.emf.diffmerge.ui.specification.IComparisonMethodFactory;
import org.eclipse.emf.diffmerge.ui.specification.IModelScopeDefinition;
import org.eclipse.emf.diffmerge.ui.specification.ext.DefaultComparisonMethod;
import org.eclipse.emf.diffmerge.ui.specification.ext.DefaultComparisonMethodFactory;
import org.eclipse.emf.diffmerge.ui.viewers.AbstractComparisonViewer;
import org.eclipse.emf.diffmerge.ui.viewers.ComparisonViewer;
import org.eclipse.emf.diffmerge.ui.viewers.IDifferenceCategoryProvider;
import org.eclipse.emf.diffmerge.ui.workbench.Activator;
import org.eclipse.emf.diffmerge.ui.workbench.viewers.ComparisonViewerE3;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.provider.resource.ResourceItemProviderAdapterFactory;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;

/**
 * A default implementation of IComparisonMethodFactory.
 * @author Olivier Constant
 */
public class DefaultComparisonMethodFactoryE3 extends DefaultComparisonMethodFactory
{

	/**
	 * @see org.eclipse.emf.diffmerge.ui.specification.IComparisonMethodFactory#createComparisonMethod(org.eclipse.emf.diffmerge.ui.specification.IModelScopeDefinition, org.eclipse.emf.diffmerge.ui.specification.IModelScopeDefinition, org.eclipse.emf.diffmerge.ui.specification.IModelScopeDefinition)
	 */
	@Override
	public IComparisonMethod createComparisonMethod(
			IModelScopeDefinition leftScopeSpec_p, IModelScopeDefinition rightScopeSpec_p,
			IModelScopeDefinition ancestorScopeSpec_p )
	{
		return new DefaultComparisonMethodE3( leftScopeSpec_p, rightScopeSpec_p, ancestorScopeSpec_p, this );
	}

	class DefaultComparisonMethodE3 extends DefaultComparisonMethod implements IComparisonMethodE3
	{

		public DefaultComparisonMethodE3( IModelScopeDefinition leftScopeDef_p, IModelScopeDefinition rightScopeDef_p,
				IModelScopeDefinition ancestorScopeDef_p, IComparisonMethodFactory factory_p )
		{
			super( leftScopeDef_p, rightScopeDef_p, ancestorScopeDef_p, factory_p );
		}

		@Override
		public AbstractComparisonViewer createComparisonViewer( Composite parent_p,
				IActionBars actionBars_p )
		{
			AbstractComparisonViewer result = doCreateComparisonViewer( parent_p, actionBars_p );
			IDifferenceCategoryProvider provider = getCustomCategoryProvider();
			if ( provider != null )
				result.setCategoryProvider( provider );
			if ( result instanceof ComparisonViewer ) {
				ILabelProvider customLP = getCustomLabelProvider();
				if ( customLP != null )
					((ComparisonViewer)result).setDelegateLabelProvider( customLP );
			}
			return result;
		}

		protected AbstractComparisonViewer doCreateComparisonViewer( Composite parent_p,
				IActionBars actionBars_p )
		{
			return new ComparisonViewerE3( parent_p, actionBars_p );
		}

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

	}
}
