package org.eclipse.emf.diffmerge.ui.util;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

public abstract class IProgressServiceProvider {

  /** The instance of this class (singleton pattern) */
  public static IProgressServiceProvider INSTANCE = new ProgressServiceProviderImpl();

  /**
   * Executes the given runnable. Uses the IProgressService, if available.
   *
   * @param runnable
   *          The {@link IRunnableWithProgress} to execute.
   * @throws InvocationTargetException
   * @throws InterruptedException
   */
  public abstract void executeRunnable(IRunnableWithProgress runnable)
      throws InvocationTargetException, InterruptedException;

  public abstract void executeBusyCursor(IRunnableWithProgress runnable)
      throws InvocationTargetException, InterruptedException;

  /**
   * Executes the given runnable. Uses the IProgressService, if available.
   *
   * @param runnable
   *          The {@link IRunnableWithProgress} to execute.
   * @param fork
   *          indicates whether to run within a separate thread.
   * @param cancelable
   *          indicates whether the operation shall be cancelable
   * @throws InvocationTargetException
   * @throws InterruptedException
   */
  public abstract void executeRunnable(IRunnableWithProgress runnable,
      boolean fork,
      boolean cancelable)
      throws InvocationTargetException, InterruptedException;
}

class ProgressServiceProviderImpl extends IProgressServiceProvider {

  public void executeRunnable(IRunnableWithProgress runnable)
      throws InvocationTargetException, InterruptedException {
    if (runnable != null) {
      // TODO Might want to improve this
      runnable.run(new NullProgressMonitor());
    }
  }

  public void executeBusyCursor(IRunnableWithProgress runnable)
      throws InvocationTargetException, InterruptedException {
    if (runnable != null) {
      // TODO Might want to improve this
      runnable.run(new NullProgressMonitor());
    }

  }

  public void executeRunnable(IRunnableWithProgress runnable, boolean fork,
      boolean cancelable)
      throws InvocationTargetException, InterruptedException {
    if (runnable != null) {
      // TODO Might want to improve this
      runnable.run(new NullProgressMonitor());
    }
  }
}