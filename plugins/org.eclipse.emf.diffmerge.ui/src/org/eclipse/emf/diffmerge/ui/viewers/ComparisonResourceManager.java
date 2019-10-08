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
package org.eclipse.emf.diffmerge.ui.viewers;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.diffmerge.ui.EMFDiffMergeUIPlugin;
import org.eclipse.emf.diffmerge.ui.EMFDiffMergeUIPlugin.ImageID;
import org.eclipse.emf.edit.provider.IDisposable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;


/**
 * A storage for SWT resources.
 * @author Olivier Constant
 */
public class ComparisonResourceManager implements IDisposable {
  
  /**
   * A trivial structure for identifying images with a predefined overlay
   */
  public static class ImageOverlay {
    /** The non-null original image */
    private Image _image;
    
    /** The non-null overlay */
    private ImageID _overlay;
    
    /**
     * Constructor
     * @param image_p a non-null image
     * @param overlay_p a non-null image ID
     */
    public ImageOverlay(Image image_p, ImageID overlay_p) {
      _image = image_p;
      _overlay = overlay_p;
    }
    
    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      return _image.hashCode() + _overlay.hashCode();
    }
    
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object object_p) {
      boolean result = false;
      if (object_p instanceof ImageOverlay) {
        ImageOverlay peer = (ImageOverlay)object_p;
        result = _image.equals(peer._image) && _overlay.equals(peer._overlay);
      }
      return result;
    }
  }
  
  
  /** The map from normal images to their disabled variant */
  private final Map<Image, Image> _normalToDisabled;
  
  /** The map from normal images to their variant with overlays */
  private final Map<ImageOverlay, Image> _normalToOverlay;
  
  
  /**
   * Constructor
   */
  public ComparisonResourceManager() {
    _normalToDisabled = new HashMap<Image, Image>();
    _normalToOverlay = new HashMap<ImageOverlay, Image>();
  }
  
  /**
   * Dispose the receiver
   */
  public void dispose() {
    for (Image disabled : _normalToDisabled.values())
      disabled.dispose();
    _normalToDisabled.clear();
    for (Image overlay : _normalToOverlay.values())
      overlay.dispose();
    _normalToOverlay.clear();
  }
  
  /**
   * Return an image which is the disabled variant of the given one
   * @param image_p a non-null image
   * @return a non-null image
   */
  public Image getDisabledVersion(Image image_p) {
    Image result = _normalToDisabled.get(image_p);
    if (result == null) {
      result = new Image(image_p.getDevice(), image_p, SWT.IMAGE_DISABLE);
      _normalToDisabled.put(image_p, result);
    }
    return result;
  }
  
  /**
   * Return an image which is based on the given one with the given overlay
   * @param image_p a non-null image
   * @param overlay_p a potentially null image ID
   * @return a non-null image
   */
  public Image getOverlayVersion(Image image_p, ImageID overlay_p) {
    Image result;
    if (overlay_p == null) {
      result = image_p;
    } else {
      ImageOverlay io = new ImageOverlay(image_p, overlay_p);
      result = _normalToOverlay.get(io);
      if (result == null) {
        ImageDescriptor overlayDescriptor =
            EMFDiffMergeUIPlugin.getDefault().getImageDescriptor(overlay_p);
        DecorationOverlayIcon icon = new DecorationOverlayIcon(
            image_p, overlayDescriptor, IDecoration.BOTTOM_RIGHT);
        result = icon.createImage(image_p.getDevice());
        _normalToOverlay.put(io, result);
      }
    }
    return result;
  }
  
  /**
   * Return an image which represents the given overlay
   * @param overlay_p a potentially null image ID
   * @return a non-null image
   */
  public Image getStandaloneOverlay(ImageID overlay_p) {
    Image empty = EMFDiffMergeUIPlugin.getDefault().getImage(ImageID.EMPTY);
    return getOverlayVersion(empty, overlay_p);
  }
  
}
