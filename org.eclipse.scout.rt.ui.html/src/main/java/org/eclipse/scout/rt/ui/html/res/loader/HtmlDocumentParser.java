/*
 * Copyright (c) 2010-2021 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 */
package org.eclipse.scout.rt.ui.html.res.loader;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.config.CONFIG;
import org.eclipse.scout.rt.platform.config.PlatformConfigProperties.ApplicationVersionProperty;
import org.eclipse.scout.rt.platform.html.HtmlHelper;
import org.eclipse.scout.rt.platform.text.TEXTS;
import org.eclipse.scout.rt.platform.util.IOUtility;
import org.eclipse.scout.rt.platform.util.StringUtility;
import org.eclipse.scout.rt.server.commons.servlet.cache.GlobalHttpResourceCache;
import org.eclipse.scout.rt.server.commons.servlet.cache.IHttpResourceCache;
import org.eclipse.scout.rt.shared.ui.webresource.ScriptResourceIndexes;
import org.eclipse.scout.rt.shared.ui.webresource.WebResourceDescriptor;
import org.eclipse.scout.rt.shared.ui.webresource.WebResources;
import org.eclipse.scout.rt.ui.html.UiThemeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple tag-parser used to replace scout-tags in HTML documents.
 * <p>
 * Note: This is not a &#64;Bean, because the constructor requires an argument. To customize this class, override the
 * factory method in {@link HtmlFileLoader}.
 */
public class HtmlDocumentParser {
  private static final Logger LOG = LoggerFactory.getLogger(HtmlDocumentParser.class);

  protected static final String ENTRY_POINT_VALUE_REGEX = "[^\"~]*";
  protected static final Pattern PATTERN_INCLUDE_TAG = Pattern.compile("<scout:include\\s+template=\"([^\"]*)\"\\s*/?>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  protected static final Pattern PATTERN_MESSAGE_TAG = Pattern.compile("<scout:message(.*?)\\s*/?>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  protected static final Pattern PATTERN_STYLESHEET_TAG = Pattern.compile("<scout:stylesheet\\s+src=\"([^\"]*)\"\\s*/?>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  protected static final Pattern PATTERN_SCRIPT_TAG = Pattern.compile("<scout:script\\s+src=\"([^\"]*)\"\\s*/?>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  protected static final Pattern PATTERN_SCRIPTS_TAG = Pattern.compile("<scout:scripts\\s+entrypoint=\"(" + ENTRY_POINT_VALUE_REGEX + ")\"\\s*/?>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  protected static final Pattern PATTERN_STYLESHEETS_TAG = Pattern.compile("<scout:stylesheets\\s+entrypoint=\"(" + ENTRY_POINT_VALUE_REGEX + ")\"\\s*/?>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  protected static final Pattern PATTERN_BASE_TAG = Pattern.compile("<scout:base\\s*/?>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  protected static final Pattern PATTERN_VERSION_TAG = Pattern.compile("<scout:version\\s*/?>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  protected static final Pattern PATTERN_UNKNOWN_TAG = Pattern.compile("<scout:(\"[^\"]*\"|[^>]*?)*>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
  protected static final Pattern PATTERN_KEY_VALUE = Pattern.compile("([^\\s]+)=\"([^\"]*)\"");
  public static final String SCRIPT_TAG_PREFIX = "<script src=\"";
  public static final String SCRIPT_TAG_SUFFIX = "\"></script>";
  public static final String STYLESHEET_TAG_PREFIX = "<link rel=\"stylesheet\" type=\"text/css\" href=\"";
  public static final String STYLESHEET_TAG_SUFFIX = "\">";

  protected final HtmlDocumentParserParameters m_params;
  protected final IHttpResourceCache m_cache;
  protected String m_workingContent;

  public HtmlDocumentParser(HtmlDocumentParserParameters params) {
    m_params = params;
    m_cache = BEANS.get(GlobalHttpResourceCache.class);
  }

  public byte[] parseDocument(byte[] document) throws IOException {
    // the order of calls is important: first we must resolve all includes
    m_workingContent = new String(document, StandardCharsets.UTF_8);
    replaceAllTags();
    return m_workingContent.getBytes(StandardCharsets.UTF_8);
  }

  protected void replaceAllTags() throws IOException {
    replaceIncludeTags();
    replaceBaseTags();
    replaceVersionTags();
    replaceMessageTags();
    replaceStylesheetsTags();
    replaceStylesheetTags();
    replaceScriptsTags();
    replaceScriptTags();
    stripUnknownTags();
  }

  @SuppressWarnings("squid:S1149")
  protected void replaceScriptTags(Pattern pattern, String tagPrefix, String tagSuffix) {
    Matcher m = pattern.matcher(m_workingContent);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      String srcPath = m.group(1);
      //noinspection StringBufferReplaceableByString
      StringBuilder scriptTag = new StringBuilder(tagPrefix);
      scriptTag.append(createExternalPath(srcPath));
      scriptTag.append(tagSuffix);
      m.appendReplacement(sb, scriptTag.toString());
    }
    m.appendTail(sb);
    m_workingContent = sb.toString();
  }

  /**
   * Creates the external path of the given resource, including theme, fingerprint and '.min' extensions.
   */
  protected String createExternalPath(String internalPath) {
    String theme = UiThemeHelper.get().isDefaultTheme(m_params.getTheme()) ? null : m_params.getTheme();
    return new WebResourceLoader(m_params.isMinify(), false, theme)
        .resolveResource(internalPath)
        .map(WebResourceDescriptor::getResolvedPath)
        .orElse(internalPath);
  }

  protected void replaceStylesheetTags() {
    // <scout:stylesheet src="scout-all-macro.css" />
    replaceScriptTags(PATTERN_STYLESHEET_TAG, STYLESHEET_TAG_PREFIX, STYLESHEET_TAG_SUFFIX);
  }

  @SuppressWarnings("bsiRulesDefinition:htmlInString")
  protected void replaceScriptTags() {
    // <scout:script src="scout-all-macro.css" />
    replaceScriptTags(PATTERN_SCRIPT_TAG, SCRIPT_TAG_PREFIX, SCRIPT_TAG_SUFFIX);
  }

  protected void replaceScriptsTags() {
    // <scout:scripts entryPoint="entry-point-name"/>
    replaceEntryPointTags(PATTERN_SCRIPTS_TAG, ".js", SCRIPT_TAG_PREFIX, SCRIPT_TAG_SUFFIX);
  }

  protected void replaceStylesheetsTags() {
    // <scout:stylesheets entrypoint="entry-point-name"/>
    replaceEntryPointTags(PATTERN_STYLESHEETS_TAG, ".css", STYLESHEET_TAG_PREFIX, STYLESHEET_TAG_SUFFIX);
  }

  protected void replaceEntryPointTags(Pattern pattern, String fileSuffixFilter, String tagPrefix, String tagSuffix) {
    Matcher m = pattern.matcher(m_workingContent);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      String entryPoint = m.group(1);
      m.appendReplacement(sb, buildScriptTagsForEntryPoint(entryPoint, fileSuffixFilter, tagPrefix, tagSuffix));
    }
    m.appendTail(sb);
    m_workingContent = sb.toString();
  }

  protected String buildScriptTagsForEntryPoint(String entryPoint, String fileSuffixFilter, String tagPrefix, String tagSuffix) {
    return getAssetsForEntryPoint(entryPoint)
        .filter(script -> script.toLowerCase().endsWith(fileSuffixFilter))
        .map(path -> tagPrefix + createExternalPath(path) + tagSuffix)
        .collect(joining("\n"));
  }

  protected Stream<String> getAssetsForEntryPoint(String entryPoint) {
    return ScriptResourceIndexes.getAssetsForEntryPoint(entryPoint, m_params.isMinify()).stream();
  }

  protected void replaceBaseTags() {
    // <scout:base />
    String basePath = m_params.getBasePath();
    if (StringUtility.isNullOrEmpty(basePath)) {
      basePath = "/";
    }
    else if (basePath.lastIndexOf('/') != basePath.length() - 1) {
      // add / at end of string (unless it already has a slash at the end)
      basePath += "/";
    }
    String baseTag = "<base href=\"" + basePath + "\">";
    m_workingContent = PATTERN_BASE_TAG.matcher(m_workingContent).replaceAll(baseTag);
  }

  protected void replaceVersionTags() {
    // <scout:version />
    String version = CONFIG.getPropertyValue(ApplicationVersionProperty.class);
    String versionTag = "<scout-version data-value=\"" + version + "\"></scout-version>";
    m_workingContent = PATTERN_VERSION_TAG.matcher(m_workingContent).replaceAll(versionTag);
  }

  @SuppressWarnings("squid:S1149")
  protected void replaceIncludeTags() throws IOException {
    // <scout:include template="no-script.html" />
    Matcher m = PATTERN_INCLUDE_TAG.matcher(m_workingContent);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      String includeName = m.group(1);
      URL includeUrl = resolveInclude(includeName);
      if (includeUrl == null) {
        throw new IOException("Could not resolve include '" + includeName + "'");
      }
      else {
        byte[] includeContent = IOUtility.readFromUrl(includeUrl);
        String replacement = new String(includeContent, StandardCharsets.UTF_8);
        // Ensure exactly 1 newline before and after the replacement (to improve readability in resulting document)
        replacement = "\n" + replacement.trim() + "\n";
        m.appendReplacement(sb, replacement);
        LOG.trace("Resolved include '{}'", includeName);
      }
    }
    m.appendTail(sb);
    m_workingContent = sb.toString();
  }

  protected URL resolveInclude(String includeName) {
    return WebResources
        .resolveWebResource(includeName, m_params.isMinify())
        .map(WebResourceDescriptor::getUrl)
        .orElse(null);
  }

  @SuppressWarnings("squid:S1149")
  protected void replaceMessageTags() {
    // <scout:message key="ui.JavaScriptDisabledTitle" />
    Matcher m = PATTERN_MESSAGE_TAG.matcher(m_workingContent);
    StringBuffer sb = new StringBuffer();
    HtmlHelper htmlHelper = BEANS.get(HtmlHelper.class);
    while (m.find()) {
      Matcher m2 = PATTERN_KEY_VALUE.matcher(m.group(1));
      String style = "";
      List<String> keys = new ArrayList<>();
      while (m2.find()) {
        String key = m2.group(1);
        String value = m2.group(2);
        if (StringUtility.equalsIgnoreCase(key, "style")) {
          style = StringUtility.lowercase(value);
        }
        else if (StringUtility.equalsIgnoreCase(key, "key")) {
          keys.add(value);
        }
      }
      // Generate output
      String text = "";
      if (!keys.isEmpty()) {
        switch (style) {
          case "javascript":
            // JavaScript style replacement
            StringBuilder js = new StringBuilder("{");
            for (String key : keys) {
              js.append("'").append(key).append("': ");
              js.append(toJavaScriptString(TEXTS.get(key)));
              js.append(", ");
            }
            int length = js.length();
            js.delete(length - 2, length);
            js.append("}");
            text = js.toString();
            break;
          case "plain":
            // Plain normal replacement (only supports one key, because we don't know how to separate multiple keys)
            text = TEXTS.get(keys.get(0));
            break;
          case "tag":
            StringBuilder tags = new StringBuilder();
            for (String key : keys) {
              tags.append("<scout-text data-key=\"").append(htmlHelper.escape(key)).append("\" ");
              tags.append("data-value=\"").append(htmlHelper.escape(TEXTS.get(key))).append("\"></scout-text>");
            }
            text = tags.toString();
            break;
          case "html":
          default:
            text = htmlHelper.escape(TEXTS.get(keys.get(0)));
            break;
        }
      }
      m.appendReplacement(sb, Matcher.quoteReplacement(text));
    }
    m.appendTail(sb);
    m_workingContent = sb.toString();
  }

  protected String toJavaScriptString(String text) {
    // escape single quotes
    text = text.replaceAll("'", "\\\\'");
    // escape new-lines
    text = text.replaceAll("(\r\n|\n)", "\\\\n");
    return "'" + text + "'";
  }

  @SuppressWarnings("squid:S1149")
  protected void stripUnknownTags() {
    Matcher m = PATTERN_UNKNOWN_TAG.matcher(m_workingContent);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      LOG.warn("Removing unknown or improperly formatted scout tag from '{}': {}", m_params.getHtmlPath(), m.group());
      m.appendReplacement(sb, "");
    }
    m.appendTail(sb);
    m_workingContent = sb.toString();
  }
}
