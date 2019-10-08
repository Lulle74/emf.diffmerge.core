package org.eclipse.emf.diffmerge.ui.workbench.util;

import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.emf.diffmerge.ui.util.MiscUtil.ExtendedUnloader;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.util.TransactionUtil;
import org.eclipse.emf.workspace.ResourceUndoContext;
import org.eclipse.ui.PlatformUI;

public class ExtendedUnloaderE3 extends ExtendedUnloader
{
	/** The default instance: singleton with default behavior */
    private static ExtendedUnloader __default = null;
    /**
     * Return the singleton with default behavior
     * @return a non-null object
     */
    public static ExtendedUnloader getDefault() {
      if (__default == null) {
        __default = new ExtendedUnloaderE3();
      }
      return __default;
    }
    
	public ExtendedUnloaderE3()
	{
		// Stateless
	}
	
	/**
     * Disconnect the given resource from the given editing domain according to
     * transactional/workspace concerns.
     * Transactional concerns: handled.
     * @param domain_p a potentially null editing domain
     * @param resource_p a potentially null resource
     * @return whether the operation succeeded
     */
    @Override
	public boolean disconnectResource(EditingDomain domain_p, Resource resource_p) {
      boolean result = true;
      if (resource_p != null && domain_p instanceof TransactionalEditingDomain) {
        TransactionalEditingDomain tDomain = (TransactionalEditingDomain)domain_p;
        IOperationHistory opHistory =
            PlatformUI.getWorkbench().getOperationSupport().getOperationHistory();
        TransactionUtil.disconnectFromEditingDomain(resource_p);
        // Cleaning up Eclipse operation history
        try {
          ResourceUndoContext context = new ResourceUndoContext(tDomain, resource_p);
          opHistory.dispose(context, true, true, true);
        } catch (Exception e) {
          // Workbench being disposed: proceed
          result = false;
        }
      }
      return result;
    }

}
