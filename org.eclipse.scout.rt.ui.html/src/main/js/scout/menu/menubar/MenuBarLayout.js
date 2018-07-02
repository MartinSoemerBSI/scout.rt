/*******************************************************************************
 * Copyright (c) 2014-2017 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
scout.MenuBarLayout = function(menuBar) {
  scout.MenuBarLayout.parent.call(this);
  this._menuBar = menuBar;

  this._overflowMenuItems = [];
  this._visibleMenuItems = [];
  this._ellipsis = null;
  this.collapsed = false;
};
scout.inherits(scout.MenuBarLayout, scout.AbstractLayout);

scout.MenuBarLayout.prototype.layout = function($container) {
  var menuItems = this._menuBar.orderedMenuItems.left.concat(this._menuBar.orderedMenuItems.right),
    htmlContainer = scout.HtmlComponent.get($container),
    ellipsis;

  ellipsis = scout.arrays.find(menuItems, function(menuItem) {
    return menuItem.ellipsis;
  });

  this.preferredLayoutSize($container, {
    widthHint: htmlContainer.availableSize().width
  });

  // first set visible to ensure the correct menu gets the tabindex. Therefore the ellipsis visibility is split.
  if (ellipsis && this._overflowMenuItems.length > 0) {
    ellipsis.setHidden(false);
  }
  this._visibleMenuItems.forEach(function(menuItem) {
    menuItem._setOverflown(false);
    menuItem.setParent(this._menuBar);
  }, this);

  this._overflowMenuItems.forEach(function(menuItem) {
    menuItem._setOverflown(true);
  });
  if (ellipsis && this._overflowMenuItems.length === 0) {
    ellipsis.setHidden(true);
  }
  // remove all separators
  this._overflowMenuItems = this._overflowMenuItems.filter(function(menuItem) {
    return !menuItem.separator;
  });

  // set childActions to empty array to prevent the menuItems from calling remove.
  if (ellipsis) {
    ellipsis._closePopup();
    /* workaround to ensure current child action will not be removed when setting the new ones.
     * This workaround and also the setParent on all visible menu items (see above) can be removed
     * with the context menu clean up planned in the UI team.
     */
    ellipsis.childActions = [];
    ellipsis.setChildActions(this._overflowMenuItems);
  }

  // trigger menu items layout
  this._visibleMenuItems.forEach(function(menuItem) {
    menuItem.validateLayout();
  });

  this._visibleMenuItems.forEach(function(menuItem) {
    // Make sure open popups are at the correct position after layouting
    if (menuItem.popup) {
      menuItem.popup.position();
    }
  });
};

scout.MenuBarLayout.prototype.preferredLayoutSize = function($container, options) {
  this._overflowMenuItems = [];
  if (!this._menuBar.isVisible()) {
    return new scout.Dimension(0, 0);
  }
  var visibleMenuItems = this._menuBar.orderedMenuItems.all.filter(function(menuItem) {
      return menuItem.visible;
    }, this),
    overflowMenuItems = visibleMenuItems.filter(function(menuItem) {
      var overflown = menuItem.overflown;
      menuItem._setOverflown(false);
      return overflown;
    }),
    overflowableIndexes = [],
    htmlComp = scout.HtmlComponent.get($container),
    prefSize = new scout.Dimension(0, 0),
    prefWidth = Number.MAX_VALUE;

  // consider avoid falsy 0 in tabboxes a 0 withHint will be used to calculate the minimum width
  if (options.widthHint === 0 || options.widthHint) {
    prefWidth = options.widthHint - htmlComp.insets().horizontal();
  }
  // shortcut for minimum size.
  if (prefWidth <= 0) {
    return this._minSize(visibleMenuItems);
  }

  // fill overflowable indexes
  visibleMenuItems.forEach(function(menuItem, index) {
    if (menuItem.stackable) {
      overflowableIndexes.push(index);
    }
  });

  var overflowIndex = -1;
  this._setFirstLastMenuMarker(visibleMenuItems);
  prefSize = this._prefSize(visibleMenuItems);
  while (prefSize.width > prefWidth && overflowableIndexes.length > 0) {
    overflowIndex = overflowableIndexes.splice(-1)[0];
    this._overflowMenuItems.splice(0, 0, visibleMenuItems[overflowIndex]);
    visibleMenuItems.splice(overflowIndex, 1);
    this._setFirstLastMenuMarker(visibleMenuItems);
    prefSize = this._prefSize(visibleMenuItems);
  }

  //reset overflown
  overflowMenuItems.forEach(function(menuItem) {
    menuItem._setOverflown(true);
  });

  this._visibleMenuItems = visibleMenuItems;
  return prefSize.add(htmlComp.insets());
};

scout.MenuBarLayout.prototype._minSize = function(visibleMenuItems) {
  var prefSize,
    minVisibleMenuItems = visibleMenuItems.filter(function(menuItem) {
      return menuItem.ellisis || !menuItem.stackable;
    }, this);
  this._setFirstLastMenuMarker(minVisibleMenuItems, true);
  prefSize = this._prefSize(minVisibleMenuItems, true);
  return prefSize;
};

scout.MenuBarLayout.prototype._prefSize = function(menuItems, considerEllipsis) {
  var prefSize = new scout.Dimension(0, 0),
    itemSize = new scout.Dimension(0, 0);
  considerEllipsis = scout.nvl(considerEllipsis, this._overflowMenuItems.length > 0);
  menuItems.forEach(function(menuItem) {
    itemSize = new scout.Dimension(0, 0);
    if (menuItem.ellipsis) {
      if (considerEllipsis) {
        itemSize = this._menuItemSize(menuItem);
      }
    } else {
      itemSize = this._menuItemSize(menuItem);
    }
    prefSize.height = Math.max(prefSize.height, itemSize.height);
    prefSize.width += itemSize.width;
  }, this);
  return prefSize;
};

scout.MenuBarLayout.prototype._menuItemSize = function(menuItem) {
  var prefSize,
    classList = menuItem.$container.attr('class');

  menuItem.$container.removeClass('overflown');
  menuItem.$container.removeClass('hidden');

  prefSize = menuItem.htmlComp.prefSize({
    useCssSize: true,
    exact: true
  }).add(scout.graphics.margins(menuItem.$container));

  menuItem.$container.attrOrRemove('class', classList);
  return prefSize;
};

scout.MenuBarLayout.prototype._setFirstLastMenuMarker = function(visibleMenuItems, considerEllipsis) {
  var menuItems = visibleMenuItems;
  considerEllipsis = scout.nvl(considerEllipsis, this._overflowMenuItems.length > 0);
  if (!considerEllipsis) {
    // remove ellipsis
    menuItems = menuItems.filter(function(menuItem) {
      return !menuItem.ellipsis;
    });
  }
  menuItems.forEach(function(menuItem, index, arr) {
    menuItem.$container.removeClass('first last');
    if (index === 0) {
      menuItem.$container.addClass('first');
    }
    // consider ellipsis
    if (index === (arr.length - 1)) {
      menuItem.$container.addClass('last');
    }
  });
};

/* --- STATIC HELPERS ------------------------------------------------------------- */

/**
 * @memberOf scout.MenuBarLayout
 */
scout.MenuBarLayout.size = function(htmlMenuBar, containerSize) {
  var menuBarSize = htmlMenuBar.prefSize();
  menuBarSize.width = containerSize.width;
  menuBarSize = menuBarSize.subtract(htmlMenuBar.margins());
  return menuBarSize;
};
