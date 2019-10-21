["^ ","~:resource-id",["~:shadow.build.classpath/resource","goog/result/simpleresult.js"],"~:js","goog.provide(\"goog.result.SimpleResult\");\ngoog.provide(\"goog.result.SimpleResult.StateError\");\ngoog.require(\"goog.Promise\");\ngoog.require(\"goog.Thenable\");\ngoog.require(\"goog.debug.Error\");\ngoog.require(\"goog.result.Result\");\n/**\n @constructor\n @implements {goog.result.Result}\n @deprecated Use {@link goog.Promise} instead - http://go/promisemigration\n */\ngoog.result.SimpleResult = function() {\n  /** @private @type {goog.result.Result.State} */ this.state_ = goog.result.Result.State.PENDING;\n  /** @private @type {!Array<!goog.result.SimpleResult.HandlerEntry_>} */ this.handlers_ = [];\n  /** @private @type {*} */ this.value_ = undefined;\n  /** @private @type {*} */ this.error_ = undefined;\n};\ngoog.Thenable.addImplementation(goog.result.SimpleResult);\n/** @private @typedef {{callback:function(goog.result.SimpleResult),scope:Object}} */ goog.result.SimpleResult.HandlerEntry_;\n/**\n @final\n @constructor\n @extends {goog.debug.Error}\n @deprecated Use {@link goog.Promise} instead - http://go/promisemigration\n */\ngoog.result.SimpleResult.StateError = function() {\n  goog.result.SimpleResult.StateError.base(this, \"constructor\", \"Multiple attempts to set the state of this Result\");\n};\ngoog.inherits(goog.result.SimpleResult.StateError, goog.debug.Error);\n/** @override */ goog.result.SimpleResult.prototype.getState = function() {\n  return this.state_;\n};\n/** @override */ goog.result.SimpleResult.prototype.getValue = function() {\n  return this.value_;\n};\n/** @override */ goog.result.SimpleResult.prototype.getError = function() {\n  return this.error_;\n};\n/**\n @param {function(this:T,!goog.result.SimpleResult)} handler\n @param {T=} opt_scope\n @template T\n @override\n */\ngoog.result.SimpleResult.prototype.wait = function(handler, opt_scope) {\n  if (this.isPending_()) {\n    this.handlers_.push({callback:handler, scope:opt_scope || null});\n  } else {\n    handler.call(opt_scope, this);\n  }\n};\n/**\n @param {*} value\n */\ngoog.result.SimpleResult.prototype.setValue = function(value) {\n  if (this.isPending_()) {\n    this.value_ = value;\n    this.state_ = goog.result.Result.State.SUCCESS;\n    this.callHandlers_();\n  } else {\n    if (!this.isCanceled()) {\n      throw new goog.result.SimpleResult.StateError;\n    }\n  }\n};\n/**\n @param {*=} opt_error\n */\ngoog.result.SimpleResult.prototype.setError = function(opt_error) {\n  if (this.isPending_()) {\n    this.error_ = opt_error;\n    this.state_ = goog.result.Result.State.ERROR;\n    this.callHandlers_();\n  } else {\n    if (!this.isCanceled()) {\n      throw new goog.result.SimpleResult.StateError;\n    }\n  }\n};\n/** @private */ goog.result.SimpleResult.prototype.callHandlers_ = function() {\n  var handlers = this.handlers_;\n  this.handlers_ = [];\n  for (var n = 0; n < handlers.length; n++) {\n    var handlerEntry = handlers[n];\n    handlerEntry.callback.call(handlerEntry.scope, this);\n  }\n};\n/**\n @private\n @return {boolean}\n */\ngoog.result.SimpleResult.prototype.isPending_ = function() {\n  return this.state_ == goog.result.Result.State.PENDING;\n};\n/**\n @return {boolean}\n @override\n */\ngoog.result.SimpleResult.prototype.cancel = function() {\n  if (this.isPending_()) {\n    this.setError(new goog.result.Result.CancelError);\n    return true;\n  }\n  return false;\n};\n/** @override */ goog.result.SimpleResult.prototype.isCanceled = function() {\n  return this.state_ == goog.result.Result.State.ERROR && this.error_ instanceof goog.result.Result.CancelError;\n};\n/** @override */ goog.result.SimpleResult.prototype.then = function(opt_onFulfilled, opt_onRejected, opt_context) {\n  var resolve, reject;\n  var promise = new goog.Promise(function(res, rej) {\n    resolve = res;\n    reject = rej;\n  });\n  this.wait(function(result) {\n    if (result.isCanceled()) {\n      promise.cancel();\n    } else {\n      if (result.getState() == goog.result.Result.State.SUCCESS) {\n        resolve(result.getValue());\n      } else {\n        if (result.getState() == goog.result.Result.State.ERROR) {\n          reject(result.getError());\n        }\n      }\n    }\n  });\n  return promise.then(opt_onFulfilled, opt_onRejected, opt_context);\n};\n/**\n @param {!goog.Promise<?>} promise\n @return {!goog.result.Result}\n */\ngoog.result.SimpleResult.fromPromise = function(promise) {\n  var result = new goog.result.SimpleResult;\n  promise.then(result.setValue, result.setError, result);\n  return result;\n};\n","~:source","// Copyright 2012 The Closure Library Authors. All Rights Reserved.\n//\n// Licensed under the Apache License, Version 2.0 (the \"License\");\n// you may not use this file except in compliance with the License.\n// You may obtain a copy of the License at\n//\n//      http://www.apache.org/licenses/LICENSE-2.0\n//\n// Unless required by applicable law or agreed to in writing, software\n// distributed under the License is distributed on an \"AS-IS\" BASIS,\n// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n// See the License for the specific language governing permissions and\n// limitations under the License.\n\n/**\n * @fileoverview A SimpleResult object that implements goog.result.Result.\n * See below for a more detailed description.\n */\n\ngoog.provide('goog.result.SimpleResult');\ngoog.provide('goog.result.SimpleResult.StateError');\n\ngoog.require('goog.Promise');\ngoog.require('goog.Thenable');\ngoog.require('goog.debug.Error');\ngoog.require('goog.result.Result');\n\n\n\n/**\n * A SimpleResult object is a basic implementation of the\n * goog.result.Result interface. This could be subclassed(e.g. XHRResult)\n * or instantiated and returned by another class as a form of result. The caller\n * receiving the result could then attach handlers to be called when the result\n * is resolved(success or error).\n *\n * @constructor\n * @implements {goog.result.Result}\n * @deprecated Use {@link goog.Promise} instead - http://go/promisemigration\n */\ngoog.result.SimpleResult = function() {\n  /**\n   * The current state of this Result.\n   * @type {goog.result.Result.State}\n   * @private\n   */\n  this.state_ = goog.result.Result.State.PENDING;\n\n  /**\n   * The list of handlers to call when this Result is resolved.\n   * @type {!Array<!goog.result.SimpleResult.HandlerEntry_>}\n   * @private\n   */\n  this.handlers_ = [];\n\n  // The value_ and error_ properties are initialized in the constructor to\n  // ensure that all SimpleResult instances share the same hidden class in\n  // modern JavaScript engines.\n\n  /**\n   * The 'value' of this Result.\n   * @type {*}\n   * @private\n   */\n  this.value_ = undefined;\n\n  /**\n   * The error slug for this Result.\n   * @type {*}\n   * @private\n   */\n  this.error_ = undefined;\n};\ngoog.Thenable.addImplementation(goog.result.SimpleResult);\n\n\n/**\n * A waiting handler entry.\n * @typedef {{\n *   callback: function(goog.result.SimpleResult),\n *   scope: Object\n * }}\n * @private\n */\ngoog.result.SimpleResult.HandlerEntry_;\n\n\n\n/**\n * Error thrown if there is an attempt to set the value or error for this result\n * more than once.\n *\n * @constructor\n * @extends {goog.debug.Error}\n * @final\n * @deprecated Use {@link goog.Promise} instead - http://go/promisemigration\n */\ngoog.result.SimpleResult.StateError = function() {\n  goog.result.SimpleResult.StateError.base(\n      this, 'constructor', 'Multiple attempts to set the state of this Result');\n};\ngoog.inherits(goog.result.SimpleResult.StateError, goog.debug.Error);\n\n\n/** @override */\ngoog.result.SimpleResult.prototype.getState = function() {\n  return this.state_;\n};\n\n\n/** @override */\ngoog.result.SimpleResult.prototype.getValue = function() {\n  return this.value_;\n};\n\n\n/** @override */\ngoog.result.SimpleResult.prototype.getError = function() {\n  return this.error_;\n};\n\n\n/**\n * Attaches handlers to be called when the value of this Result is available.\n *\n * @param {function(this:T, !goog.result.SimpleResult)} handler The function\n *     called when the value is available. The function is passed the Result\n *     object as the only argument.\n * @param {T=} opt_scope Optional scope for the handler.\n * @template T\n * @override\n */\ngoog.result.SimpleResult.prototype.wait = function(handler, opt_scope) {\n  if (this.isPending_()) {\n    this.handlers_.push({callback: handler, scope: opt_scope || null});\n  } else {\n    handler.call(opt_scope, this);\n  }\n};\n\n\n/**\n * Sets the value of this Result, changing the state.\n *\n * @param {*} value The value to set for this Result.\n */\ngoog.result.SimpleResult.prototype.setValue = function(value) {\n  if (this.isPending_()) {\n    this.value_ = value;\n    this.state_ = goog.result.Result.State.SUCCESS;\n    this.callHandlers_();\n  } else if (!this.isCanceled()) {\n    // setValue is a no-op if this Result has been canceled.\n    throw new goog.result.SimpleResult.StateError();\n  }\n};\n\n\n/**\n * Sets the Result to be an error Result.\n *\n * @param {*=} opt_error Optional error slug to set for this Result.\n */\ngoog.result.SimpleResult.prototype.setError = function(opt_error) {\n  if (this.isPending_()) {\n    this.error_ = opt_error;\n    this.state_ = goog.result.Result.State.ERROR;\n    this.callHandlers_();\n  } else if (!this.isCanceled()) {\n    // setError is a no-op if this Result has been canceled.\n    throw new goog.result.SimpleResult.StateError();\n  }\n};\n\n\n/**\n * Calls the handlers registered for this Result.\n *\n * @private\n */\ngoog.result.SimpleResult.prototype.callHandlers_ = function() {\n  var handlers = this.handlers_;\n  this.handlers_ = [];\n  for (var n = 0; n < handlers.length; n++) {\n    var handlerEntry = handlers[n];\n    handlerEntry.callback.call(handlerEntry.scope, this);\n  }\n};\n\n\n/**\n * @return {boolean} Whether the Result is pending.\n * @private\n */\ngoog.result.SimpleResult.prototype.isPending_ = function() {\n  return this.state_ == goog.result.Result.State.PENDING;\n};\n\n\n/**\n * Cancels the Result.\n *\n * @return {boolean} Whether the result was canceled. It will not be canceled if\n *    the result was already canceled or has already resolved.\n * @override\n */\ngoog.result.SimpleResult.prototype.cancel = function() {\n  // cancel is a no-op if the result has been resolved.\n  if (this.isPending_()) {\n    this.setError(new goog.result.Result.CancelError());\n    return true;\n  }\n  return false;\n};\n\n\n/** @override */\ngoog.result.SimpleResult.prototype.isCanceled = function() {\n  return this.state_ == goog.result.Result.State.ERROR &&\n      this.error_ instanceof goog.result.Result.CancelError;\n};\n\n\n/** @override */\ngoog.result.SimpleResult.prototype.then = function(\n    opt_onFulfilled, opt_onRejected, opt_context) {\n  var resolve, reject;\n  // Copy the resolvers to outer scope, so that they are available\n  // when the callback to wait() fires (which may be synchronous).\n  var promise = new goog.Promise(function(res, rej) {\n    resolve = res;\n    reject = rej;\n  });\n  this.wait(function(result) {\n    if (result.isCanceled()) {\n      promise.cancel();\n    } else if (result.getState() == goog.result.Result.State.SUCCESS) {\n      resolve(result.getValue());\n    } else if (result.getState() == goog.result.Result.State.ERROR) {\n      reject(result.getError());\n    }\n  });\n  return promise.then(opt_onFulfilled, opt_onRejected, opt_context);\n};\n\n\n/**\n * Creates a SimpleResult that fires when the given promise resolves.\n * Use only during migration to Promises.\n * @param {!goog.Promise<?>} promise\n * @return {!goog.result.Result}\n */\ngoog.result.SimpleResult.fromPromise = function(promise) {\n  var result = new goog.result.SimpleResult();\n  promise.then(result.setValue, result.setError, result);\n  return result;\n};\n","~:compiled-at",1570534492634,"~:source-map-json","{\n\"version\":3,\n\"file\":\"goog.result.simpleresult.js\",\n\"lineCount\":139,\n\"mappings\":\"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\",\n\"sources\":[\"goog/result/simpleresult.js\"],\n\"sourcesContent\":[\"// Copyright 2012 The Closure Library Authors. All Rights Reserved.\\n//\\n// Licensed under the Apache License, Version 2.0 (the \\\"License\\\");\\n// you may not use this file except in compliance with the License.\\n// You may obtain a copy of the License at\\n//\\n//      http://www.apache.org/licenses/LICENSE-2.0\\n//\\n// Unless required by applicable law or agreed to in writing, software\\n// distributed under the License is distributed on an \\\"AS-IS\\\" BASIS,\\n// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\\n// See the License for the specific language governing permissions and\\n// limitations under the License.\\n\\n/**\\n * @fileoverview A SimpleResult object that implements goog.result.Result.\\n * See below for a more detailed description.\\n */\\n\\ngoog.provide('goog.result.SimpleResult');\\ngoog.provide('goog.result.SimpleResult.StateError');\\n\\ngoog.require('goog.Promise');\\ngoog.require('goog.Thenable');\\ngoog.require('goog.debug.Error');\\ngoog.require('goog.result.Result');\\n\\n\\n\\n/**\\n * A SimpleResult object is a basic implementation of the\\n * goog.result.Result interface. This could be subclassed(e.g. XHRResult)\\n * or instantiated and returned by another class as a form of result. The caller\\n * receiving the result could then attach handlers to be called when the result\\n * is resolved(success or error).\\n *\\n * @constructor\\n * @implements {goog.result.Result}\\n * @deprecated Use {@link goog.Promise} instead - http://go/promisemigration\\n */\\ngoog.result.SimpleResult = function() {\\n  /**\\n   * The current state of this Result.\\n   * @type {goog.result.Result.State}\\n   * @private\\n   */\\n  this.state_ = goog.result.Result.State.PENDING;\\n\\n  /**\\n   * The list of handlers to call when this Result is resolved.\\n   * @type {!Array<!goog.result.SimpleResult.HandlerEntry_>}\\n   * @private\\n   */\\n  this.handlers_ = [];\\n\\n  // The value_ and error_ properties are initialized in the constructor to\\n  // ensure that all SimpleResult instances share the same hidden class in\\n  // modern JavaScript engines.\\n\\n  /**\\n   * The 'value' of this Result.\\n   * @type {*}\\n   * @private\\n   */\\n  this.value_ = undefined;\\n\\n  /**\\n   * The error slug for this Result.\\n   * @type {*}\\n   * @private\\n   */\\n  this.error_ = undefined;\\n};\\ngoog.Thenable.addImplementation(goog.result.SimpleResult);\\n\\n\\n/**\\n * A waiting handler entry.\\n * @typedef {{\\n *   callback: function(goog.result.SimpleResult),\\n *   scope: Object\\n * }}\\n * @private\\n */\\ngoog.result.SimpleResult.HandlerEntry_;\\n\\n\\n\\n/**\\n * Error thrown if there is an attempt to set the value or error for this result\\n * more than once.\\n *\\n * @constructor\\n * @extends {goog.debug.Error}\\n * @final\\n * @deprecated Use {@link goog.Promise} instead - http://go/promisemigration\\n */\\ngoog.result.SimpleResult.StateError = function() {\\n  goog.result.SimpleResult.StateError.base(\\n      this, 'constructor', 'Multiple attempts to set the state of this Result');\\n};\\ngoog.inherits(goog.result.SimpleResult.StateError, goog.debug.Error);\\n\\n\\n/** @override */\\ngoog.result.SimpleResult.prototype.getState = function() {\\n  return this.state_;\\n};\\n\\n\\n/** @override */\\ngoog.result.SimpleResult.prototype.getValue = function() {\\n  return this.value_;\\n};\\n\\n\\n/** @override */\\ngoog.result.SimpleResult.prototype.getError = function() {\\n  return this.error_;\\n};\\n\\n\\n/**\\n * Attaches handlers to be called when the value of this Result is available.\\n *\\n * @param {function(this:T, !goog.result.SimpleResult)} handler The function\\n *     called when the value is available. The function is passed the Result\\n *     object as the only argument.\\n * @param {T=} opt_scope Optional scope for the handler.\\n * @template T\\n * @override\\n */\\ngoog.result.SimpleResult.prototype.wait = function(handler, opt_scope) {\\n  if (this.isPending_()) {\\n    this.handlers_.push({callback: handler, scope: opt_scope || null});\\n  } else {\\n    handler.call(opt_scope, this);\\n  }\\n};\\n\\n\\n/**\\n * Sets the value of this Result, changing the state.\\n *\\n * @param {*} value The value to set for this Result.\\n */\\ngoog.result.SimpleResult.prototype.setValue = function(value) {\\n  if (this.isPending_()) {\\n    this.value_ = value;\\n    this.state_ = goog.result.Result.State.SUCCESS;\\n    this.callHandlers_();\\n  } else if (!this.isCanceled()) {\\n    // setValue is a no-op if this Result has been canceled.\\n    throw new goog.result.SimpleResult.StateError();\\n  }\\n};\\n\\n\\n/**\\n * Sets the Result to be an error Result.\\n *\\n * @param {*=} opt_error Optional error slug to set for this Result.\\n */\\ngoog.result.SimpleResult.prototype.setError = function(opt_error) {\\n  if (this.isPending_()) {\\n    this.error_ = opt_error;\\n    this.state_ = goog.result.Result.State.ERROR;\\n    this.callHandlers_();\\n  } else if (!this.isCanceled()) {\\n    // setError is a no-op if this Result has been canceled.\\n    throw new goog.result.SimpleResult.StateError();\\n  }\\n};\\n\\n\\n/**\\n * Calls the handlers registered for this Result.\\n *\\n * @private\\n */\\ngoog.result.SimpleResult.prototype.callHandlers_ = function() {\\n  var handlers = this.handlers_;\\n  this.handlers_ = [];\\n  for (var n = 0; n < handlers.length; n++) {\\n    var handlerEntry = handlers[n];\\n    handlerEntry.callback.call(handlerEntry.scope, this);\\n  }\\n};\\n\\n\\n/**\\n * @return {boolean} Whether the Result is pending.\\n * @private\\n */\\ngoog.result.SimpleResult.prototype.isPending_ = function() {\\n  return this.state_ == goog.result.Result.State.PENDING;\\n};\\n\\n\\n/**\\n * Cancels the Result.\\n *\\n * @return {boolean} Whether the result was canceled. It will not be canceled if\\n *    the result was already canceled or has already resolved.\\n * @override\\n */\\ngoog.result.SimpleResult.prototype.cancel = function() {\\n  // cancel is a no-op if the result has been resolved.\\n  if (this.isPending_()) {\\n    this.setError(new goog.result.Result.CancelError());\\n    return true;\\n  }\\n  return false;\\n};\\n\\n\\n/** @override */\\ngoog.result.SimpleResult.prototype.isCanceled = function() {\\n  return this.state_ == goog.result.Result.State.ERROR &&\\n      this.error_ instanceof goog.result.Result.CancelError;\\n};\\n\\n\\n/** @override */\\ngoog.result.SimpleResult.prototype.then = function(\\n    opt_onFulfilled, opt_onRejected, opt_context) {\\n  var resolve, reject;\\n  // Copy the resolvers to outer scope, so that they are available\\n  // when the callback to wait() fires (which may be synchronous).\\n  var promise = new goog.Promise(function(res, rej) {\\n    resolve = res;\\n    reject = rej;\\n  });\\n  this.wait(function(result) {\\n    if (result.isCanceled()) {\\n      promise.cancel();\\n    } else if (result.getState() == goog.result.Result.State.SUCCESS) {\\n      resolve(result.getValue());\\n    } else if (result.getState() == goog.result.Result.State.ERROR) {\\n      reject(result.getError());\\n    }\\n  });\\n  return promise.then(opt_onFulfilled, opt_onRejected, opt_context);\\n};\\n\\n\\n/**\\n * Creates a SimpleResult that fires when the given promise resolves.\\n * Use only during migration to Promises.\\n * @param {!goog.Promise<?>} promise\\n * @return {!goog.result.Result}\\n */\\ngoog.result.SimpleResult.fromPromise = function(promise) {\\n  var result = new goog.result.SimpleResult();\\n  promise.then(result.setValue, result.setError, result);\\n  return result;\\n};\\n\"],\n\"names\":[\"goog\",\"provide\",\"require\",\"result\",\"SimpleResult\",\"goog.result.SimpleResult\",\"state_\",\"Result\",\"State\",\"PENDING\",\"handlers_\",\"value_\",\"undefined\",\"error_\",\"Thenable\",\"addImplementation\",\"HandlerEntry_\",\"StateError\",\"goog.result.SimpleResult.StateError\",\"base\",\"inherits\",\"debug\",\"Error\",\"prototype\",\"getState\",\"goog.result.SimpleResult.prototype.getState\",\"getValue\",\"goog.result.SimpleResult.prototype.getValue\",\"getError\",\"goog.result.SimpleResult.prototype.getError\",\"wait\",\"goog.result.SimpleResult.prototype.wait\",\"handler\",\"opt_scope\",\"isPending_\",\"push\",\"callback\",\"scope\",\"call\",\"setValue\",\"goog.result.SimpleResult.prototype.setValue\",\"value\",\"SUCCESS\",\"callHandlers_\",\"isCanceled\",\"setError\",\"goog.result.SimpleResult.prototype.setError\",\"opt_error\",\"ERROR\",\"goog.result.SimpleResult.prototype.callHandlers_\",\"handlers\",\"n\",\"length\",\"handlerEntry\",\"goog.result.SimpleResult.prototype.isPending_\",\"cancel\",\"goog.result.SimpleResult.prototype.cancel\",\"CancelError\",\"goog.result.SimpleResult.prototype.isCanceled\",\"then\",\"goog.result.SimpleResult.prototype.then\",\"opt_onFulfilled\",\"opt_onRejected\",\"opt_context\",\"resolve\",\"reject\",\"promise\",\"Promise\",\"res\",\"rej\",\"fromPromise\",\"goog.result.SimpleResult.fromPromise\"]\n}\n"]