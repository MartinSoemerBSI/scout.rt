/*
 * Copyright (c) 2010, 2023 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.scout.rt.jackson.dataobject.id;

import static org.eclipse.scout.rt.platform.util.Assertions.assertInstance;

import java.io.IOException;

import org.eclipse.scout.rt.dataobject.id.IId;
import org.eclipse.scout.rt.dataobject.id.IdCodec;
import org.eclipse.scout.rt.platform.util.LazyValue;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Custom deserializer for {@link IId} instances - like {@link TypedIdDeserializer} it uses {@link IdCodec} for
 * serialization. It may be used as a replacement for {@link UnqualifiedIIdDeserializer}.
 */
public class QualifiedIIdDeserializer extends StdDeserializer<IId> {
  private static final long serialVersionUID = 1L;

  protected final LazyValue<IdCodec> m_idCodec = new LazyValue<>(IdCodec.class);
  protected final Class<? extends IId> m_idClass;

  public QualifiedIIdDeserializer(Class<? extends IId> idClass) {
    super(idClass);
    m_idClass = idClass;
  }

  @Override
  public IId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    // check required to prevent returning an instance that isn't compatible with requested ID class
    return assertInstance(m_idCodec.get().fromQualified(p.getText()), m_idClass);
  }
}
