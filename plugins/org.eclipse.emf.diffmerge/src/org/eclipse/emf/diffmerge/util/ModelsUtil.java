/*********************************************************************
 * Copyright (c) 2010-2019 Thales Global Services S.A.S.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thales Global Services S.A.S. - initial API and implementation
 **********************************************************************/
package org.eclipse.emf.diffmerge.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.diffmerge.EMFDiffMergePlugin;
import org.eclipse.emf.diffmerge.structures.common.FOrderedSet;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.ECrossReferenceAdapter;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.edit.provider.IDisposable;


/**
 * A utility class related to the structure of models.
 * @author Olivier Constant
 */
public final class ModelsUtil {
  
  /**
   * An interface defining filters for model elements
   */
  public static interface IElementFilter {
    /**
     * Return whether the given element is accepted by this filter
     * @param element_p a non-null element
     */
    boolean accepts(EObject element_p);
  }
  
  
  /**
   * A stateless object that is in charge of disposing of resources taking various concerns
   * into account.
   */
  public static class Unloader {
    /** The default instance: singleton with default behavior */
    private static Unloader __default = null;
    /**
     * Return the singleton with default behavior
     * @return a non-null object
     */
    public static Unloader getDefault() {
      if (__default == null) {
        __default = new Unloader();
      }
      return __default;
    }
    /**
     * Constructor
     */
    protected Unloader() {
      // Stateless
    }
    /**
     * Remove certain adapters in the given resources in the perspective of unloading them.
     * Transactional concerns: not handled.
     * @param resources_p a non-null set
     * @return whether the operation succeeded for all resources
     */
    public boolean unloadAdapters(Iterable<? extends Resource> resources_p) {
      boolean result = true;
      for (Resource resource : resources_p) {
        boolean success = unloadAdapters(resource);
        result = result && success;
      }
      return result;
    }
    /**
     * Remove certain adapters in the given resource in the perspective of unloading it.
     * Transactional concerns: not handled.
     * @param resource_p a potentially null resource
     * @return whether the operation succeeded
     */
    public boolean unloadAdapters(Resource resource_p) {
      if (resource_p != null) {
        for (Adapter adapter : new ArrayList<Adapter>(resource_p.eAdapters())) {
          if (adapter instanceof ECrossReferenceAdapter) {
            resource_p.eAdapters().remove(adapter);
          }
        }
      }
      return true;
    }
    /**
     * Unload the given resources and remove them from their resource set if any.
     * Transactional concerns: not handled.
     * @param resources_p a non-null set
     * @return whether the operation succeeded for all resources
     */
    public boolean unloadResources(Iterable<? extends Resource> resources_p) {
      boolean result = true;
      for (Resource resource : resources_p) {
        boolean success = unloadResource(resource);
        result = result && success;
      }
      return result;
    }
    /**
     * Unload the given resource and remove it from its resource set if any.
     * Transactional concerns: not handled.
     * @param resource_p a potentially null resource
     * @return whether the operation succeeded
     */
    public boolean unloadResource(Resource resource_p) {
      return unloadResource(resource_p, false);
    }
    /**
     * Unload the given resource and remove it from its resource set if any.
     * Transactional concerns: not handled.
     * @param resource_p a potentially null resource
     * @param checkRoots_p whether roots should be checked for disposal if applicable
     * @return whether the operation succeeded
     */
    public boolean unloadResource(Resource resource_p, boolean checkRoots_p) {
      boolean result = true;
      if (resource_p != null) {
        try {
          ResourceSet rs = resource_p.getResourceSet();
          if (resource_p.isLoaded()) {// Actually loaded, not just assumed as such
            if (checkRoots_p) {
              // Disposing roots
              for (EObject root : resource_p.getContents()) {
                if (root instanceof IDisposable) {
                  ((IDisposable)root).dispose();
                }
              }
            }
            // Actually unloading
            resource_p.unload();
          }
          if (rs != null) {
            rs.getResources().remove(resource_p);
          }
        } catch (Exception e) {
          EMFDiffMergePlugin.getDefault().getLog().log(new Status(
              IStatus.ERROR, EMFDiffMergePlugin.getDefault().getPluginId(), e.getMessage()));
          result = false;
        }
      }
      return result;
    }
  }
  
  
  /**
   * Constructor
   */
  private ModelsUtil() {
    // Forbids instantiation
  }
  
  /**
   * From a set of elements, return all the elements of their containment trees in
   * depth-first order
   * Postcondition: elements_p is not modified.
   * @param elements_p a non-null collection
   * @param filter_p an optional filter
   * @return a non-null, modifiable list
   */
  private static List<EObject> getAllContentsDF(Collection<? extends EObject> elements_p,
      IElementFilter filter_p) {
    List<EObject> result = new FOrderedSet<EObject>();
    for (EObject element : elements_p) {
      result.addAll(getAllContentsDF(element, filter_p));
    }
    return result;
  }
  
  /**
   * Return all the elements of the containment tree of the given element in depth-first
   * order
   * @param element_p a non-null element
   * @param filter_p an optional filter
   * @return a non-null, modifiable list
   */
  private static List<EObject> getAllContentsDF(EObject element_p,
      IElementFilter filter_p) {
    List<EObject> result = new FOrderedSet<EObject>();
    if (filter_p == null || filter_p.accepts(element_p))
      result.add(element_p);
    TreeIterator<EObject> it = element_p.eAllContents();
    while (it.hasNext()) {
      EObject current = it.next();
      if (filter_p == null || filter_p.accepts(current))
        result.add(current);
    }
    return result;
  }
  
  /**
   * From a set of elements, build a list of all the elements of their containment trees in
   * breadth-first order
   * Postcondition: elements_p is not modified.
   * We use a LinkedList for queuing behavior.
   * @param elements_p a non-null, modifiable, potentially empty queue of the roots
   * @param result_p the non-null modifiable result being built
   * @param filter_p an optional filter
   */
  private static void getAllContentsBF(LinkedList<EObject> elements_p,
      List<EObject> result_p, IElementFilter filter_p) {
    if (!elements_p.isEmpty()) {
      EObject current = elements_p.poll();
      if (filter_p == null || filter_p.accepts(current)) {
        result_p.add(current);
        elements_p.addAll(current.eContents());
      }
      getAllContentsBF(elements_p, result_p, filter_p);
    }
  }
  
  /**
   * Return all the elements in the containment tree of the given element
   * @param element_p a non-null element
   * @param depthFirst_p whether the elements must be returned in breadth-first order or in
   *         depth-first order
   * @param filter_p an optional filter
   * @return a non-null, modifiable list
   */
  public static List<EObject> getAllContents(EObject element_p, boolean depthFirst_p,
      IElementFilter filter_p) {
    return getAllContents(Collections.singletonList(element_p), depthFirst_p,
        filter_p);
  }
  
  /**
   * From a set of elements, return all the elements in their containment trees
   * Postcondition: elements_p is not modified.
   * @param elements_p a non-null collection
   * @param depthFirst_p whether the elements must be returned in breadth-first order or in
   *         depth-first order
   * @param filter_p an optional filter
   * @return a non-null, modifiable list
   */
  public static List<EObject> getAllContents(Collection<? extends EObject> elements_p,
      boolean depthFirst_p, IElementFilter filter_p) {
    List<EObject> result;
    if (depthFirst_p) {
      result = getAllContentsDF(elements_p, filter_p);
    } else {
      result = new FOrderedSet<EObject>();
      getAllContentsBF(new LinkedList<EObject>(elements_p), result, filter_p);
    }
    return result;
  }
  
  /**
   * Return the list of ancestors including self, from higher to deeper.
   * The result is not immutable but modifying it has no impact whatsoever.
   * @param element_p a potentially null element
   * @return a non-null, modifiable ordered set
   */
  public static List<EObject> getAncestors(EObject element_p) {
    if (element_p == null) return new FOrderedSet<EObject>();
    List<EObject> containerList = getAncestors(element_p.eContainer());
    containerList.add(element_p);
    return containerList;
  }
  
  /**
   * Return the lowest common ancestor in the containment hierarchy, if any,
   * of the given set of elements
   * @param acceptSelf_p whether the result can be any of the given elements
   * @return a potentially null element
   */
  public static EObject getCommonAncestor(
      Collection<? extends EObject> elements_p, boolean acceptSelf_p) {
    if (elements_p == null || elements_p.isEmpty()) return null;
    Iterator<? extends EObject> it = elements_p.iterator();
    List<EObject> commonHierarchy = getAncestors(it.next());
    while (it.hasNext()) {
      List<EObject> currentHierarchy = getAncestors(it.next());
      // Compute intersection of ancestors
      commonHierarchy.retainAll(currentHierarchy);
    }
    // Exclude the given elements
    if (!acceptSelf_p) commonHierarchy.removeAll(elements_p);
    // Take lowest ancestor in common hierarchy
    if (commonHierarchy.isEmpty()) return null;
    return commonHierarchy.get(commonHierarchy.size()-1);
  }
  
  /**
   * Return the lowest common ancestor, in the containment hierarchy,
   * of the two given elements (inclusive)
   * @param first_p a non-null element
   * @param second_p a non-null element
   * @return a potentially null element
   */
  public static EObject getCommonAncestor(EObject first_p, EObject second_p) {
    if (null == first_p || null == second_p) return null;
    return getCommonAncestor(Arrays.asList(new EObject[] {first_p, second_p}), true);
  }
  
  /**
   * Given a set of elements, find their lowest common meta-class
   * @param elements_p a non-null collection of model elements
   * @return a meta-class which is not null if elements_p is not empty
   */
  public static EClass getCommonType(Collection<? extends EObject> elements_p) {
    EClass result = null;
    if (!elements_p.isEmpty()) {
      List<EClass> common = new ArrayList<EClass>(
          getSuperTypes(elements_p.iterator().next().eClass()));
      for(EObject elt : elements_p) {
        common.retainAll(getSuperTypes(elt.eClass()));
      }
      if (!common.isEmpty()) {
        result = common.get(common.size()-1);
      }
    }
    return result;
  }
  
  /**
   * Return the depth in the containment tree of the given element
   * @param element_p a potentially null element
   * @return 0 if null, a strictly positive integer otherwise
   */
  public static int getDepth(EObject element_p) {
    if (element_p == null)
      return 0;
    return 1 + getDepth(element_p.eContainer());
  }
  
  /**
   * Return the overall depth in the containment tree of the given collection of elements,
   * where overall means maximum if max_p is true, or minimum otherwise
   * @param elements_p a non-null, potentially empty collection of elements
   */
  public static int getDepth(Iterable<? extends EObject> elements_p, boolean max_p) {
    int result = max_p? 0: Integer.MAX_VALUE;
    for (EObject element : elements_p) {
      int depth = getDepth(element);
      result = max_p? Math.max(result, depth): Math.min(result, depth);
    }
    return result;
  }
  
  /**
   * From a set of elements, return all the leaves in their containment trees
   * @param elements_p a non-null collection
   * @return a non-null list
   */
  public static List<EObject> getLeaves(Collection<? extends EObject> elements_p) {
    List<EObject> result = new FOrderedSet<EObject>();
    for (EObject element : elements_p) {
      result.addAll(getLeaves(element));
    }
    return result;
  }
  
  /**
   * Return all the leaves in the containment tree of the given element
   * @param element_p a non-null element
   * @return a non-null list
   */
  public static List<EObject> getLeaves(EObject element_p) {
    List<EObject> result;
    if (element_p.eContents().isEmpty()) {
      result = Collections.singletonList(element_p);
    } else {
      result = getLeaves(element_p.eContents());
    }
    return result;
  }
  
  /**
   * From a set of elements, filter out those which are transitively contained
   * in others
   * @param elements_p a non-null collection
   * @return a non-null list
   */
  public static <T extends EObject> List<T> getRoots(
      Collection<? extends T> elements_p) {
    List<T> result = new FOrderedSet<T>();
    Collection<T> elements = new FOrderedSet<T>(elements_p, null);
    for (T element : elements) {
      if (!result.contains(element) && isRootAmong(element, elements))
        result.add(element);
    }
    return result;
  }
  
  /**
   * Return the super types of the given meta-class including the class itself,
   * ordered from higher to lower in the hierarchy
   * @param class_p a non-null meta-class
   * @return a non-null, non-empty, unmodifiable list
   */
  private static List<EClass> getSuperTypes(EClass class_p) {
    List<EClass> allButSelf = class_p.getEAllSuperTypes();
    List<EClass> result = new ArrayList<EClass>(allButSelf.size() + 1);
    result.addAll(allButSelf);
    result.add(class_p);
    return Collections.unmodifiableList(result);
  }
  
  /**
   * Return whether the given element is not transitively contained by any
   * of the given elements, unless it is one of the given elements
   * @param element_p a non-null element
   * @param elements_p a non-null collection
   */
  private static boolean isRootAmong(EObject element_p,
      Collection<? extends EObject> elements_p) {
    Collection<EObject> filtered = new FOrderedSet<EObject>(elements_p, null);
    filtered.remove(element_p);
    return !EcoreUtil.isAncestor(filtered, element_p);
  }
  
}
