/**
 * Copyright (c) 2012 Eclipse contributors and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.emf.diffmerge.ui.util;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * This class was copied from bundle "org.eclipse.emf.common.ui" (an unusable bundle in pure E4)
 * 
 * A decorating label provider that also provides access to the undecorated text and image.
 * @since 2.9
 */
public interface IUndecoratingLabelProvider extends ILabelProvider
{
  String getUndecoratedText(Object element);
  Image getUndecoratedImage(Object element);
}
