scout.Form = function() {
  scout.Form.parent.call(this);
  this._$title;
  this._$parent;
  this.rootGroupBox;
  this.menus = [];
  this.staticMenus = [];
  this._addAdapterProperties(['rootGroupBox', 'menus']);
  this._locked;
};
scout.inherits(scout.Form, scout.ModelAdapter);

scout.Form.prototype._render = function($parent) {
  this._$parent = $parent;
  this.$container = $('<div>').
    appendTo($parent).
    attr('id', 'Form-' + this.id).
    addClass('form').
    data('model', this);

  this.htmlComp = new scout.HtmlComponent(this.$container, this.session);
  this.htmlComp.setLayout(new scout.FormLayout());
  this.htmlComp.pixelBasedSizing = false;

  this.rootGroupBox.render(this.$container);

  var closeable = false;
  var detachable = !this.modal;
  if (window.scout.sessions.length > 1 || this.session.parentJsonSession) {
    // Cannot detach if...
    // 1. there is more than one session inside the window (portlets), because
    //    we would not know which session to attach to.
    // 2. the window is already a child window (cannot detatch further).
    detachable = false;
  }

  var i, btn, systemButtons = this.rootGroupBox.systemButtons;
  for (i = 0; i < systemButtons.length; i++) {
    btn = systemButtons[i];
    if (btn.visible &&
       (btn.systemType == scout.Button.SYSTEM_TYPE.CANCEL ||
        btn.systemType == scout.Button.SYSTEM_TYPE.CLOSE)) {
      closeable = true;
    }
  }

  if (detachable) {
    this.staticMenus.push(new scout.DetachFormMenu(this, this.session));
  }

  // TODO AWE: append form title section (including ! ? and progress indicator)
  this.menubar = new scout.Menubar(this.$container);
  this.menubar.menuTypesForLeft1 = ['Form.System'];
  this.menubar.menuTypesForLeft2 = ['Form.Regular'];
  this.menubar.menuTypesForRight = ['Form.Tool'];
  this.menubar.staticMenus = this.staticMenus;
  this.menubar.updateItems(this.menus);

  if (closeable) {
    var $closeButton = $('<button>').text('X');
    this.menubar.$container.append($closeButton);
    $closeButton.on('click', function() {
      this.session.send('formClosing', this.id);
    }.bind(this));
  }

  if (this._locked) {
    this.disable();
  }
};

scout.Form.prototype.onResize = function() {
  // TODO AWE/CGU: dieses event müssten wir auch bekommen, wenn man den Divider zwischen
  // Tree und Working Area schiebt.
  $.log.trace('(Form#onResize) window was resized -> layout Form container');

  var htmlCont = scout.HtmlComponent.get(this.$container);
  var parentSize = scout.HtmlComponent.getSize(this.$container.parent());
  htmlCont.setSize(parentSize);
};

scout.Form.prototype.appendTo = function($parent) {
  this.$container.appendTo($parent);
};

/**
 * @override
 */
scout.Form.prototype.dispose = function() {
  scout.Form.parent.prototype.dispose.call(this);
};

scout.Form.prototype.onModelCreate = function() {};

scout.Form.prototype.onModelAction = function(event) {
  if (event.type === 'formClosed') {
    this.destroy();
  } else {
    $.log.warn('Model event not handled. Widget: Form. Event: ' + event.type + '.');
  }
};
