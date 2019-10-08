/*********************************************************************
 * Copyright (c) 2006-2019 Thales Global Services S.A.S.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thales Global Services S.A.S. - initial API and implementation
 **********************************************************************/
package org.eclipse.emf.diffmerge.sirius;

import java.util.Set;

import org.eclipse.emf.diffmerge.api.scopes.IFeaturedModelScope;
import org.eclipse.emf.diffmerge.api.scopes.IPersistentModelScope;
import org.eclipse.emf.diffmerge.gmf.GMFMergePolicy;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.ECrossReferenceAdapter;
import org.eclipse.sirius.diagram.DDiagramElement;
import org.eclipse.sirius.viewpoint.DRepresentation;
import org.eclipse.sirius.viewpoint.DRepresentationDescriptor;
import org.eclipse.sirius.viewpoint.DView;
import org.eclipse.sirius.viewpoint.ViewpointPackage;


/**
 * A merge policy for Sirius elements.
 */
public class SiriusMergePolicy extends GMFMergePolicy {
  
  /**
   * @see org.eclipse.emf.diffmerge.impl.policies.DefaultMergePolicy#copyFeature(org.eclipse.emf.ecore.EStructuralFeature, org.eclipse.emf.diffmerge.api.scopes.IFeaturedModelScope)
   */
  @Override
  public boolean copyFeature(EStructuralFeature feature_p, IFeaturedModelScope scope_p) {
    return // Replace DResentationDescriptor::repPath by ::representation
        feature_p == ViewpointPackage.eINSTANCE.getDRepresentationDescriptor_Representation() ||
        feature_p != ViewpointPackage.eINSTANCE.getDRepresentationDescriptor_RepPath() &&
        super.copyFeature(feature_p, scope_p);
  }
  
  /**
   * Extend the given addition group for the given descriptor within the given scope
   * @param group_p a non-null, modifiable collection
   * @param element_p a non-null element
   * @param scope_p a non-null scope
   */
  protected void extendDescriptorAdditionGroup(Set<EObject> group_p,
      DRepresentationDescriptor element_p, IFeaturedModelScope scope_p) {
    group_p.add(element_p.getRepresentation());
  }
  
  /**
   * Extend the given addition group for the given Sirius representation within
   * the given scope
   * @param group_p a non-null, modifiable collection
   * @param element_p a non-null element
   * @param scope_p a non-null scope
   */
  protected void extendDRepresentationAdditionGroup(Set<EObject> group_p,
      DRepresentation element_p, IFeaturedModelScope scope_p) {
    EObject container = scope_p.getContainer(element_p);
    if (container instanceof DView) {
      for (EObject descriptor : scope_p.get(container,
          ViewpointPackage.Literals.DVIEW__OWNED_REPRESENTATION_DESCRIPTORS)) {
        if (descriptor instanceof DRepresentationDescriptor) {
          if (element_p.equals(((DRepresentationDescriptor) descriptor).getRepresentation())) {
            group_p.add(descriptor);
          }
        }
      }
    } else if (container == null) {
      DRepresentationDescriptor descriptor = null;
      if (scope_p instanceof SiriusScope) {
        descriptor = ((SiriusScope)scope_p).getRepresentationDescriptor(element_p);
      } else if (scope_p instanceof IPersistentModelScope) {
        descriptor = SiriusScope.getRepresentationDescriptorByPhysicalExploration(
            element_p, (IPersistentModelScope)scope_p);
      }
      if (descriptor != null) {
        group_p.add(descriptor);
      }
    }
  }
  
  /**
   * Extend the given addition group for the given element within the given scope
   * based on Sirius peculiarities
   * @param group_p a non-null, modifiable collection
   * @param element_p a non-null element
   * @param scope_p a non-null scope
   */
  protected void extendSiriusAdditionGroup(Set<EObject> group_p, EObject element_p,
      IFeaturedModelScope scope_p) {
    // Semantic element -> DSemanticDecorators
    extendSemanticElementAdditionGroup(group_p, element_p, scope_p);
    // Sirius 4.1: Retrieve the diagram while merging the descriptor
    if (element_p instanceof DRepresentationDescriptor) {
      extendDescriptorAdditionGroup(group_p, (DRepresentationDescriptor)element_p, scope_p);
    }
    // Sirius 4.1: Retrieve the descriptor while merging the diagram
    if (element_p instanceof DRepresentation) {
      extendDRepresentationAdditionGroup(group_p, (DRepresentation)element_p, scope_p);
    }
    // Sirius/GMF consistency: GMF driven by Sirius
    if (element_p instanceof DDiagramElement) {
      extendGMFAdditionGroupSemanticTarget(group_p, element_p, scope_p);
    }
  }
  
  /**
   * Extend the given addition group for the given element within the given scope
   * in the case where the element is a Sirius semantic element
   * @param group_p a non-null, modifiable collection
   * @param element_p a non-null element
   * @param scope_p a non-null scope
   */
  protected void extendSemanticElementAdditionGroup(Set<EObject> group_p,
      EObject element_p, IFeaturedModelScope scope_p) {
    if (isGraphicalFromSemantic()) {
      ECrossReferenceAdapter crAdapter = ECrossReferenceAdapter.getCrossReferenceAdapter(element_p);
      if (crAdapter != null) {
        for (EStructuralFeature.Setting setting :
            crAdapter.getNonNavigableInverseReferences(element_p, false)) {
          if (setting.getEStructuralFeature() ==
              ViewpointPackage.eINSTANCE.getDSemanticDecorator_Target()) {
            group_p.add(setting.getEObject());
          }
        }
      }
    }
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.gmf.GMFMergePolicy#getAdditionGroup(org.eclipse.emf.ecore.EObject, org.eclipse.emf.diffmerge.api.scopes.IFeaturedModelScope)
   */
  @Override
  public Set<EObject> getAdditionGroup(EObject element_p, IFeaturedModelScope scope_p) {
    Set<EObject> result = super.getAdditionGroup(element_p, scope_p);
    extendSiriusAdditionGroup(result, element_p, scope_p);
    return result;
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.gmf.GMFMergePolicy#isSingleMandatory(org.eclipse.emf.ecore.EReference)
   */
  @Override
  protected boolean isSingleMandatory(EReference reference_p) {
    return super.isSingleMandatory(reference_p) ||
        reference_p == ViewpointPackage.eINSTANCE.getDSemanticDecorator_Target();
  }
  
}
