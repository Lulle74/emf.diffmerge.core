package org.eclipse.emf.diffmerge.ui.workbench.util;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.emf.diffmerge.ui.util.IProgressServiceProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

public class ProgressServiceProviderE3Impl extends IProgressServiceProvider
{
	@Override
	public void executeRunnable( IRunnableWithProgress runnable ) throws InvocationTargetException, InterruptedException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void executeBusyCursor( IRunnableWithProgress runnable )
			throws InvocationTargetException, InterruptedException
	{
		IProgressService progress = PlatformUI.getWorkbench().getProgressService();
		progress.busyCursorWhile( runnable );
	}

	@Override
	public void executeRunnable( IRunnableWithProgress runnable, boolean fork, boolean cancelable )
			throws InvocationTargetException, InterruptedException
	{
		// TODO Auto-generated method stub

	}

}
