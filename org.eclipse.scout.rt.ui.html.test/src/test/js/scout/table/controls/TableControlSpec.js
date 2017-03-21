/*******************************************************************************
 * Copyright (c) 2014-2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
describe("TableControl", function() {
  var session;
  var tableHelper;

  beforeEach(function() {
    setFixtures(sandbox());
    session = sandboxSession();
    tableHelper = new scout.TableSpecHelper(session);

    jasmine.Ajax.install();
    jasmine.clock().install();
  });

  afterEach(function() {
    session = null;
    jasmine.Ajax.uninstall();
    jasmine.clock().uninstall();
  });

  function createModel() {
    var model = createSimpleModel('TableControl', session);
    $.extend({
      "enabled": true,
      "visible": true
    });

    return model;
  }

  function createAction(model) {
    var action = new scout.TableControl();
    action.init(model);
    return action;
  }

  function createTableControlAdapter(model) {
    var action = new scout.TableControlAdapter();
    action.init(model);
    return action;
  }

  function createTable() {
    var tableModel = tableHelper.createModelFixture(2);
    return tableHelper.createTable(tableModel);
  }

  describe("selected", function() {
    var table;

    beforeEach(function() {
      // Open and closing of the container is animated -> disable animation in order to be able to test it
      $.fx.off = true;
      table = createTable();
    });

    afterEach(function() {
      $.fx.off = false;
    });

    it("opens and closes the control container", function() {
      var action = createAction(createModel());
      table._setTableControls([action]);
      table.render(session.$entryPoint);
      var $controlContainer = table.footer.$controlContainer;

      expect($controlContainer).toBeHidden();

      action.setSelected(true);
      expect($controlContainer).toBeVisible();

      action.setSelected(false);
      expect($controlContainer).toBeHidden();
    });

    it("removes the content of the previous selected control without closing the container", function() {
      var action = createAction(createModel());
      var action2 = createAction(createModel());
      table._setTableControls([action, action2]);

      action.selected = true;
      table.render(session.$entryPoint);
      var $controlContainer = table.footer.$controlContainer;

      expect($controlContainer).toBeVisible();
      expect(action.contentRendered).toBe(true);
      expect(action2.contentRendered).toBe(false);

      action2.setSelected(true);
      expect($controlContainer).toBeVisible();
      expect(action2.contentRendered).toBe(true);
      expect(action2.selected).toBe(true);
      expect(action2.contentRendered).toBe(true);
      expect(action2.selected).toBe(true);

      action.setSelected(false);
      expect($controlContainer).toBeVisible();
      expect(action.contentRendered).toBe(false);
      expect(action.selected).toBe(false);
    });

    it("sends selected events (for current and previous selection)", function() {
      var model = createModel();
      var adapter = createTableControlAdapter(model);
      var action = adapter.createWidget(model, session.desktop);
      var model2 = createModel();
      var adapter2 = createTableControlAdapter(model);
      var action2 = adapter2.createWidget(model, session.desktop);
      table._setTableControls([action, action2]);

      action.selected = true;
      table.render(session.$entryPoint);

      action2.setSelected(true);
      sendQueuedAjaxCalls();
      var events = [
        new scout.RemoteEvent(action.id, 'property', {
          selected: false
        }),
        new scout.RemoteEvent(action2.id, 'property', {
          selected: true
        })
      ];
      expect(mostRecentJsonRequest()).toContainEvents(events);
    });
  });
});
