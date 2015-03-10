/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.ui.html.json.form.fields.tablefield;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.desktop.outline.IOutline;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPage;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPageWithTable;
import org.eclipse.scout.rt.client.ui.form.fields.tablefield.ITableField;
import org.eclipse.scout.rt.ui.html.json.desktop.JsonOutline;
import org.eclipse.scout.rt.ui.html.json.desktop.fixtures.Outline;
import org.eclipse.scout.rt.ui.html.json.desktop.fixtures.TablePage;
import org.eclipse.scout.rt.ui.html.json.fixtures.JsonSessionMock;
import org.eclipse.scout.rt.ui.html.json.form.fields.tablefield.fixtures.TableField;
import org.eclipse.scout.rt.ui.html.json.table.fixtures.Table;
import org.eclipse.scout.rt.testing.client.runner.ClientTestRunner;
import org.eclipse.scout.rt.client.testenvironment.TestEnvironmentClientSession;
import org.eclipse.scout.rt.testing.platform.runner.RunWithSubject;
import org.eclipse.scout.rt.testing.client.runner.RunWithClientSession;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ClientTestRunner.class)
@RunWithSubject("default")
@RunWithClientSession(TestEnvironmentClientSession.class)
public class JsonTableFieldTest {
  private JsonSessionMock m_jsonSession;

  @Before
  public void before() {
    m_jsonSession = new JsonSessionMock();
  }

  @Test
  public void testTableDisposal() {
    ITable table = new Table();
    ITableField<ITable> tableField = new TableField<ITable>(table);
    JsonTableField<ITableField<?>> jsonTableField = m_jsonSession.newJsonAdapter(tableField, null, null);

    assertNotNull(jsonTableField.getAdapter(table));
    jsonTableField.dispose();
    m_jsonSession.flush();
    assertNull(jsonTableField.getAdapter(table));
  }

  @Test
  public void testTableDisposalOnPropertyChange() {
    ITable table = new Table();
    ITable table2 = new Table();
    ITableField<ITable> tableField = new TableField<ITable>(table);
    JsonTableField<ITableField<?>> jsonTableField = m_jsonSession.newJsonAdapter(tableField, null, null);

    //Switch table -> old one needs to be disposed
    assertNotNull(jsonTableField.getAdapter(table));
    tableField.setTable(table2, false);
    m_jsonSession.flush();
    assertNull(jsonTableField.getAdapter(table));
    assertNotNull(jsonTableField.getAdapter(table2));
    assertTrue(jsonTableField.getAdapter(table2).isAttached());

    jsonTableField.dispose();
    m_jsonSession.flush();
    assertNull(jsonTableField.getAdapter(table2));
  }

  @Test
  public void testPreventTableDisposal() throws ProcessingException {
    // Create tablePage
    IPageWithTable<?> tablePage = createTablePageAndSelectNode();
    ITable tablePageTable = tablePage.getTable();
    JsonOutline<IOutline> jsonOutline = m_jsonSession.newJsonAdapter(tablePage.getOutline(), null, null);
    Assert.assertNotNull(jsonOutline.getAdapter(tablePageTable));

    // Create table field which uses the table from the table page
    ITableField<ITable> tableField = new TableField<ITable>();
    JsonTableField<ITableField<?>> jsonTableField = m_jsonSession.newJsonAdapter(tableField, null, null);
    tableField.setTable(tablePageTable, true);

    // Dispose table field -> table must not be disposed because table page still needs it
    jsonTableField.dispose();
    m_jsonSession.flush();
    assertNotNull(jsonOutline.getAdapter(tablePageTable));
    assertTrue(jsonOutline.getAdapter(tablePageTable).isAttached());
  }

  private IPageWithTable<?> createTablePageAndSelectNode() {
    TablePage tablePage = new TablePage(1, new TablePage.NodePageWithFormFactory());
    List<IPage<?>> pages = new ArrayList<IPage<?>>();
    pages.add(tablePage);
    IOutline outline = new Outline(pages);
    outline.selectNode(tablePage);

    return tablePage;
  }

  @Test
  public void testPreventTableDisposal2() throws ProcessingException {
    // Create tablePage
    IPageWithTable<?> tablePage = createTablePageAndSelectNode();
    ITable tablePageTable = tablePage.getTable();
    JsonOutline<IOutline> jsonOutline = m_jsonSession.newJsonAdapter(tablePage.getOutline(), null, null);
    Assert.assertNotNull(jsonOutline.getAdapter(tablePageTable));

    // Create table field which uses the table from the table page
    ITableField<ITable> tableField = new TableField<ITable>();
    JsonTableField<ITableField<?>> jsonTableField = m_jsonSession.newJsonAdapter(tableField, null, null);
    tableField.setTable(tablePageTable, true);

    // Switch table -> table must not be disposed because table page still needs it
    ITable table2 = new Table();
    tableField.setTable(table2, true);
    m_jsonSession.flush();
    assertNotNull(jsonTableField.getAdapter(table2));
    assertTrue(jsonTableField.getAdapter(table2).isAttached());
    assertNotNull(jsonOutline.getAdapter(tablePageTable));
    assertTrue(jsonOutline.getAdapter(tablePageTable).isAttached());
  }

}
