/*
 * Copyright (c) 2010-2017 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 */
package org.eclipse.scout.rt.client.ui.form.fields.browserfield;

import org.eclipse.scout.rt.platform.resource.BinaryResource;

public interface IBrowserFieldUIFacade {

  void firePostExternalWindowStateFromUI(boolean windowState);

  void firePostMessageFromUI(String data, String origin);

  void firePostMessageFromUI(Object data, String origin);

  BinaryResource requestBinaryResourceFromUI(String filename);

  void setLocationFromUI(String location);
}
