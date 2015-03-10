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
package org.eclipse.scout.rt.client.ui.form.fields.smartfield;

import java.util.Collection;
import java.util.List;

import org.eclipse.scout.commons.CompareUtility;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.TriState;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.extension.ui.form.fields.smartfield.IProposalFieldExtension;
import org.eclipse.scout.rt.client.ui.form.fields.ParsingFailedStatus;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.rt.shared.services.lookup.ILookupCall;
import org.eclipse.scout.rt.shared.services.lookup.ILookupRow;
import org.eclipse.scout.rt.shared.services.lookup.LookupRow;
import org.eclipse.scout.service.SERVICES;

/**
 * This field is similar to the smart field but also allows custom text. A proposal field is always of the value type
 * {@link String}. The proposals are delivered as lookup rows of any type.
 */
public abstract class AbstractProposalField<LOOKUP_KEY> extends AbstractContentAssistField<String, LOOKUP_KEY> implements IProposalField<LOOKUP_KEY> {
  private P_UIFacade m_uiFacade;

  public AbstractProposalField() {
    this(true);
  }

  public AbstractProposalField(boolean callInitializer) {
    super(callInitializer);
  }

  @Override
  protected void initConfig() {
    super.initConfig();
    m_uiFacade = new P_UIFacade();

  }

  @Override
  public IContentAssistFieldUIFacade getUIFacade() {
    return m_uiFacade;
  }

  @Override
  public void applyLazyStyles() {
  }

  @Override
  public LOOKUP_KEY getValueAsLookupKey() {
    return null;
  }

  @Override
  public void acceptProposal(ILookupRow<LOOKUP_KEY> row) {
    setCurrentLookupRow(row);
    setValue(row.getText());
  }

  @Override
  protected void installLookupRowContext(ILookupRow<LOOKUP_KEY> row) {
    setCurrentLookupRow(row);
    super.installLookupRowContext(row);
  }

  @Override
  protected String parseValueInternal(String text) throws ProcessingException {
    if (text != null && text.length() == 0) {
      text = null;
    }
    IProposalChooser<?, LOOKUP_KEY> proposalChooser = getProposalChooser();
    ILookupRow<LOOKUP_KEY> acceptedProposalRow = null;
    if (StringUtility.equalsIgnoreNewLines(proposalChooser.getSearchText(), text)) {
      acceptedProposalRow = proposalChooser.getAcceptedProposal();
    }
//    try {
    String oldText = getDisplayText();
    boolean parsingError = getErrorStatus() != null && getErrorStatus().containsStatus(ParsingFailedStatus.class);
    if (acceptedProposalRow == null && (!parsingError) && getCurrentLookupRow() != null && StringUtility.equalsIgnoreNewLines(StringUtility.emptyIfNull(text), StringUtility.emptyIfNull(oldText))) {
      // no change
      return getValue();
    }
    else {
      // changed
      if (acceptedProposalRow != null) {
        setCurrentLookupRow(acceptedProposalRow);
        return acceptedProposalRow.getText();
      }
      else if (text == null) {
        setCurrentLookupRow(EMPTY_LOOKUP_ROW);
        return null;
      }
      else {
        setCurrentLookupRow(null);
        doSearch(text, false, true);
        proposalChooser = getProposalChooser();
        acceptedProposalRow = proposalChooser.getAcceptedProposal();
        if (acceptedProposalRow != null) {
          setCurrentLookupRow(acceptedProposalRow);
          return acceptedProposalRow.getText();
        }
        else {
          // no match possible and proposal is inactive; reject change
          setCurrentLookupRow(null);
        }
        return text;
      }
    }
//    }
//    finally {
//      unregisterProposalFormInternal(proposalChooser);
//    }

  }

  @Override
  protected String formatValueInternal(String validKey) {
    if (!isCurrentLookupRowValid(validKey)) {
      setCurrentLookupRow(null);
    }

    if (getCurrentLookupRow() != null) {
      installLookupRowContext(getCurrentLookupRow());
      String text = getCurrentLookupRow().getText();
      if (!isMultilineText() && text != null) {
        text = text.replaceAll("[\\n\\r]+", " ");
      }
      return text;
    }
    return validKey;
  }

  @Override
  protected void handleProposalChooserClosed() throws ProcessingException {
//    if (getProposalChooser() == proposalChooser) {
    ILookupRow<LOOKUP_KEY> row = getProposalChooser().getAcceptedProposal();
    if (row != null) {
      acceptProposal(row);
    }
//    }
  }

  @Override
  protected IProposalChooser<?, LOOKUP_KEY> createProposalChooser() throws ProcessingException {
    return createProposalChooser(true);
  }

  @Override
  protected void filterKeyLookup(ILookupCall<LOOKUP_KEY> call, List<ILookupRow<LOOKUP_KEY>> result) throws ProcessingException {
    super.filterKeyLookup(call, result);
    /*
     * ticket 79027
     */
    if (result.size() == 0) {
      String key = "" + call.getKey();
      result.add(new LookupRow<LOOKUP_KEY>(call.getKey(), key));
    }
  }

  @Override
  protected void handleFetchResult(IContentAssistFieldDataFetchResult<LOOKUP_KEY> result) {
    IProposalChooser<?, LOOKUP_KEY> proposalChooser = getProposalChooser();
    if (result == null) {
      unregisterProposalChooserInternal();
    }
    else {
      Collection<? extends ILookupRow<LOOKUP_KEY>> rows = result.getLookupRows();
      if (rows == null || rows.isEmpty()) {
        unregisterProposalChooserInternal();
      }
      else {
        proposalChooser.dataFetchedDelegate(result, getBrowseMaxRowCount());
      }
    }
  }

  private class P_UIFacade implements IContentAssistFieldUIFacade {

    @Override
    public boolean setTextFromUI(String text) {
      String currentValidText = getValue();
      IProposalChooser<?, LOOKUP_KEY> proposalChooser = getProposalChooser();
      // accept proposal form if either input text matches search text or
      // existing display text is valid
      try {
        if (proposalChooser.getAcceptedProposal() != null) {
          // a proposal was selected
          return acceptProposalFromUI();
        }
        if ((StringUtility.equalsIgnoreNewLines(text, proposalChooser.getSearchText()) || StringUtility.equalsIgnoreNewLines(StringUtility.emptyIfNull(text), StringUtility.emptyIfNull(currentValidText)))) {
          /*
           * empty text means null
           */
          if (text == null || text.length() == 0) {
            boolean b = parseValue(text);
            return b;
          }
          else {
            // no proposal was selected...
            if (!StringUtility.equalsIgnoreNewLines(StringUtility.emptyIfNull(text), StringUtility.emptyIfNull(currentValidText))) {
              return parseValue(text);
            }
            else {
              // ... and current display is unchanged from model value -> nop
              unregisterProposalChooserInternal();
              return true;
            }
          }

        }
        else {
          /*
           * ticket 88359
           * check if changed at all
           */
          if (CompareUtility.equals(text, currentValidText)) {
            return true;
          }
          else {
            return parseValue(text);
          }
        }
      }
      catch (ProcessingException e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(e);
        return true;
      }
    }

    @Override
    public void openProposalFromUI(String newText, boolean selectCurrentValue) {
      if (newText == null) {
        newText = BROWSE_ALL_TEXT;
      }
      IProposalChooser<?, LOOKUP_KEY> proposalChooser = getProposalChooser();
      if (proposalChooser == null) {
        setActiveFilter(TriState.TRUE);
        doSearch(newText, selectCurrentValue, false);
      }
      else {
        if (!StringUtility.equalsIgnoreNewLines(getLookupRowFetcher().getLastSearchText(), newText)) {
          doSearch(newText, false, false);
        }
      }
    }

    @Override
    public boolean acceptProposalFromUI() {
      try {
        IProposalChooser<?, LOOKUP_KEY> proposalChooser = getProposalChooser();
        if (proposalChooser.getAcceptedProposal() != null) {
          proposalChooser.doOk();
          return true;
        }
        else {
          // allow with null text traverse
          if (StringUtility.isNullOrEmpty(getDisplayText())) {
            return true;
          }
          else {
            // select first
            proposalChooser.forceProposalSelection();
            return false;
          }
        }
      }
      catch (ProcessingException e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(e);
      }
      return false;
    }

    @Override
    public void closeProposalFromUI() {
      unregisterProposalChooserInternal();
    }
  }

  protected static class LocalProposalFieldExtension<LOOKUP_KEY, OWNER extends AbstractProposalField<LOOKUP_KEY>> extends LocalContentAssistFieldExtension<String, LOOKUP_KEY, OWNER> implements IProposalFieldExtension<LOOKUP_KEY, OWNER> {

    public LocalProposalFieldExtension(OWNER owner) {
      super(owner);
    }
  }

  @Override
  protected IProposalFieldExtension<LOOKUP_KEY, ? extends AbstractProposalField<LOOKUP_KEY>> createLocalExtension() {
    return new LocalProposalFieldExtension<LOOKUP_KEY, AbstractProposalField<LOOKUP_KEY>>(this);
  }

}
