/*******************************************************************************
 * Copyright (c) 2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.shared.extension.dto.fixture;

import java.io.Serializable;

import javax.annotation.Generated;

import org.eclipse.scout.commons.annotations.Extends;

/**
 * <b>NOTE:</b><br>
 * This class is auto generated by the Scout SDK. No manual modifications recommended.
 * 
 * @generated
 */
@Extends(OrigPageWithTableData.OrigPageWithTableRowData.class)
@Generated(value = "org.eclipse.scout.sdk.workspace.dto.pagedata.PageDataDtoUpdateOperation", comments = "This class is auto generated by the Scout SDK. No manual modifications recommended.")
public class MultiColumnExtensionData implements Serializable {

  private static final long serialVersionUID = 1L;
  public static final String thirdLong = "thirdLong";
  public static final String fourthDouble = "fourthDouble";
  private Long m_thirdLong;
  private Double m_fourthDouble;

  public MultiColumnExtensionData() {
  }

  public Long getThirdLong() {
    return m_thirdLong;
  }

  public void setThirdLong(Long thirdLong) {
    m_thirdLong = thirdLong;
  }

  public Double getFourthDouble() {
    return m_fourthDouble;
  }

  public void setFourthDouble(Double fourthDouble) {
    m_fourthDouble = fourthDouble;
  }
}
