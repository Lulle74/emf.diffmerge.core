package org.eclipse.emf.diffmerge.ui.workbench;

import org.eclipse.emf.diffmerge.EMFDiffMergePlugin;
import org.eclipse.emf.diffmerge.ui.EMFDiffMergeUIPlugin;
import org.eclipse.emf.diffmerge.ui.util.DiffMergeLabelProvider;
import org.eclipse.emf.diffmerge.ui.util.IProgressServiceProvider;
import org.eclipse.emf.diffmerge.ui.workbench.setup.ComparisonSetupManagerE3;
import org.eclipse.emf.diffmerge.ui.workbench.util.DiffMergeLabelProviderE3;
import org.eclipse.emf.diffmerge.ui.workbench.util.ProgressServiceProviderE3Impl;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator
{

	private static BundleContext context;

	static BundleContext getContext()
	{
		return context;
	}

	/** A label provider based on the EMF Edit registry (initially null) */
	private static AdapterFactoryLabelProvider _composedAdapterFactoryLabelProvider;

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start( BundleContext bundleContext ) throws Exception
	{
		Activator.context = bundleContext;
		//Running 3.x Workbench: swap the default label provider impl:
		DiffMergeLabelProvider.setInstance( new DiffMergeLabelProviderE3() );

		_composedAdapterFactoryLabelProvider = null;

		//Running 3.x Workbench: Use the impl with full-fledged editor input support
		EMFDiffMergeUIPlugin.getDefault().setComparisonSetupManager( new ComparisonSetupManagerE3() );

		//Running 3.x Workbench: Use impl with IProgressMonitor support
		IProgressServiceProvider.INSTANCE = new ProgressServiceProviderE3Impl();
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop( BundleContext bundleContext ) throws Exception
	{
		Activator.context = null;
		if ( _composedAdapterFactoryLabelProvider != null )
			_composedAdapterFactoryLabelProvider.dispose();
	}

	/**
	 * Return a label provider that is based on the EMF Edit registry
	 * @return a non-null object
	 */
	public static AdapterFactoryLabelProvider getAdapterFactoryLabelProvider()
	{
		if ( _composedAdapterFactoryLabelProvider == null )
			_composedAdapterFactoryLabelProvider = new AdapterFactoryLabelProvider( EMFDiffMergePlugin.getDefault().getAdapterFactory() );
		return _composedAdapterFactoryLabelProvider;
	}
}
