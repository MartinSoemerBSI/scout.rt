/*
 * Copyright (c) 2010-2019 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 */
import {graphics, NullWidget, scrollbars} from '../../src/index';

describe('scrollbars', () => {
  let session;

  beforeEach(() => {
    setFixtures(sandbox());
    session = sandboxSession();
  });

  function createScrollable() {
    return $('<div>')
      .css('height', '50px')
      .css('width', '200px')
      .css('position', 'absolute')
      .appendTo($('#sandbox'));
  }

  function createContent($parent) {
    return $('<div>')
      .text('element')
      .css('height', '100px')
      .appendTo($parent);
  }

  describe('onScroll', () => {

    it('attaches handler to scrolling parents which execute when scrolling', () => {
      let exec = false;
      let handler = () => {
        exec = true;
      };
      let $container = createScrollable();
      let $content = scrollbars.install($container, {
        parent: new NullWidget(),
        session: session
      });
      let $element = createContent($content);

      scrollbars.onScroll($element, handler);
      $container.scroll();
      expect(exec).toBe(true);
    });

  });

  describe('offScroll', () => {

    it('detaches handler from scrolling parents', () => {
      let exec = false;
      let handler = () => {
        exec = true;
      };
      let $container = createScrollable();
      let $content = scrollbars.install($container, {
        parent: new NullWidget(),
        session: session
      });
      let $element = createContent($content);

      scrollbars.onScroll($element, handler);
      $container.scroll();
      expect(exec).toBe(true);

      exec = false;
      scrollbars.offScroll(handler);
      $container.scroll();
      expect(exec).toBe(false);
    });

  });

  describe('isLocationInView', () => {
    let $scrollable, scrollableBounds, $element;

    beforeEach(() => {
      $scrollable = createScrollable();
      scrollableBounds = graphics.offsetBounds($scrollable);
      $element = $('<div>')
        .css('height', '10px')
        .css('width', '10px')
        .css('position', 'absolute')
        .appendTo($('#sandbox'));
    });

    it('returns true if the given location is inside the given $scrollable', () => {
      $element
        .cssLeft(scrollableBounds.x)
        .cssTop(scrollableBounds.y);
      let bounds = graphics.offsetBounds($element);
      expect(scrollbars.isLocationInView(bounds, $scrollable)).toBe(true);
    });

    it('returns false if x of the given location is outside of the given $scrollable (smaller)', () => {
      $element
        .cssLeft(scrollableBounds.x - 1)
        .cssTop(scrollableBounds.y);
      let bounds = graphics.offsetBounds($element);
      expect(scrollbars.isLocationInView(bounds, $scrollable)).toBe(false);
    });

    it('returns false if y of the given location is outside of the given $scrollable (smaller)', () => {
      $element
        .cssLeft(scrollableBounds.x)
        .cssTop(scrollableBounds.y - 1);
      let bounds = graphics.offsetBounds($element);
      expect(scrollbars.isLocationInView(bounds, $scrollable)).toBe(false);
    });

    it('returns false if x of the given location is outside of the given $scrollable (greater)', () => {
      $element
        .cssLeft(scrollableBounds.x + scrollableBounds.width + 1)
        .cssTop(scrollableBounds.y);
      let bounds = graphics.offsetBounds($element);
      expect(scrollbars.isLocationInView(bounds, $scrollable)).toBe(false);
    });

    it('returns false if y of the given location is outside of the given $scrollable (greater)', () => {
      $element
        .cssLeft(scrollableBounds.x)
        .cssTop(scrollableBounds.y + scrollableBounds.height + 1);
      let bounds = graphics.offsetBounds($element);
      expect(scrollbars.isLocationInView(bounds, $scrollable)).toBe(false);
    });

  });

  describe('render', () => {

    it('ensures parent has position absolute or relative', () => {
      // Create scrollable without explicit position
      let $scrollable = $('<div>')
        .css('height', '50px')
        .css('width', '200px')
        .appendTo($('#sandbox'));
      expect($scrollable.css('position')).toBe('static');

      // Install scrollbars --> position should have been set automatically by Scrollbar._render()
      scrollbars.install($scrollable, {
        parent: new NullWidget(),
        session: session
      });
      expect($scrollable.css('position')).toBe('relative');

      // Clear
      scrollbars.uninstall($scrollable, session);
      $scrollable.remove();

      // ---------------------------

      // Create a new scrollable without explicit position
      $scrollable = $('<div>')
        .css('height', '50px')
        .css('width', '200px')
        .appendTo($('#sandbox'));
      expect($scrollable.css('position')).toBe('static');

      // Detach the scrollable
      $scrollable.detach();
      expect($scrollable.css('position')).toBe('');

      // Install scrollbars into the detached scrollable --> position should not be set yet
      scrollbars.install($scrollable, {
        parent: new NullWidget(),
        session: session
      });
      expect($scrollable.css('position')).toBe('');

      // Simulate "attach" lifecycle of widget
      $scrollable.appendTo($('#sandbox'));
      $scrollable.data('scrollbars').forEach(scrollbar => {
        scrollbar.attached = false;
        scrollbar.attach();
      });
      // Position should now have been set automatically by Scrollbar._renderOnAttach()
      expect($scrollable.css('position')).toBe('relative');
    });

  });

});
