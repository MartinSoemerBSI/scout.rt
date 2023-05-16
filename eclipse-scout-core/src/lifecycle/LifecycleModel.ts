/*
 * Copyright (c) 2010, 2023 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
import {Lifecycle, ObjectModel, Widget} from '../index';

export interface LifecycleModel extends ObjectModel<Lifecycle<any>> {
  widget?: Widget;

  emptyMandatoryElementsTextKey?: string;
  emptyMandatoryElementsText?: string;

  invalidElementsErrorTextKey?: string;
  invalidElementsErrorText?: string;

  invalidElementsWarningTextKey?: string;
  invalidElementsWarningText?: string;

  saveChangesQuestionTextKey?: string;
  /** Java: cancelVerificationText */
  askIfNeedSaveText?: string;

  askIfNeedSave?: boolean;
}
