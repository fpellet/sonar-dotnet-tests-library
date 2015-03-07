/*
 * SonarQube .NET Tests Library
 * Copyright (C) 2014 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.dotnet.tests;

import com.google.common.base.Preconditions;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.test.TestCase;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VisualStudioTestResultsFileParser implements UnitTestResultsParser {

  private static final Logger LOG = LoggerFactory.getLogger(VisualStudioTestResultsFileParser.class);

  @Override
  public void parse(File file, UnitTestResults unitTestResults) {
    LOG.info("Parsing the Visual Studio Test Results file " + file.getAbsolutePath());
    new Parser(file, unitTestResults).parse();
  }

  private static class Parser {

    private final File file;
    private XmlParserHelper xmlParserHelper;
    private final UnitTestResults unitTestResults;

    private boolean foundCounters;

    public Parser(File file, UnitTestResults unitTestResults) {
      this.file = file;
      this.unitTestResults = unitTestResults;
    }

    public void parse() {
      try {
        xmlParserHelper = new XmlParserHelper(file);
        checkRootTag();
        dispatchTags();
        Preconditions.checkArgument(foundCounters, "The mandatory <Counters> tag is missing in " + file.getAbsolutePath());
      } finally {
        if (xmlParserHelper != null) {
          xmlParserHelper.close();
        }
      }
    }

    private void checkRootTag() {
      xmlParserHelper.checkRootTag("TestRun");
    }

    private void dispatchTags() {
      String tagName;
      while ((tagName = xmlParserHelper.nextTag()) != null) {
        if ("Counters".equals(tagName)) {
          handleCountersTag();
        }

        if ("UnitTestResult".equals(tagName)) {
          addTestResult();
        }

        if ("UnitTest".equals(tagName)) {
          addTestData();
        }
      }
    }

    private void handleCountersTag() {
      foundCounters = true;

      int passed = xmlParserHelper.getRequiredIntAttribute("passed");
      int failed = xmlParserHelper.getRequiredIntAttribute("failed");
      int errors = xmlParserHelper.getRequiredIntAttribute("error");
      int timeout = xmlParserHelper.getRequiredIntAttribute("timeout");
      int aborted = xmlParserHelper.getRequiredIntAttribute("aborted");

      int skipped = xmlParserHelper.getRequiredIntAttribute("inconclusive");

      int tests = passed + failed + errors + timeout + aborted;
      int failures = timeout + failed + aborted;

      unitTestResults.add(tests, passed, skipped, failures, errors);
    }

    private void addTestResult() {
      String id = xmlParserHelper.getRequiredAttribute("testId");
      String name = xmlParserHelper.getRequiredAttribute("testName");
      String durationFormatted = xmlParserHelper.getRequiredAttribute("duration");
      String resultFormatted = xmlParserHelper.getRequiredAttribute("outcome"); // Passed, Failed

      TestCase.Status result = resultFormatted.compareToIgnoreCase("Passed") == 0 ? TestCase.Status.OK : TestCase.Status.ERROR;
      long milliseconds = parseDurationInMilliseconds(durationFormatted);

      unitTestResults.addTestResult(id, name, milliseconds, result);
    }

    private static Pattern pattern = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2}).(\\d{3})");

    public static long parseDurationInMilliseconds(String period) {
      period = period.substring(0, 2 + 1 + 2 + 1 + 2 + 1 + 3);
      Matcher matcher = pattern.matcher(period);
      if (matcher.matches()) {
        return Long.parseLong(matcher.group(1)) * 3600000L
                + Long.parseLong(matcher.group(2)) * 60000
                + Long.parseLong(matcher.group(3)) * 1000
                + Long.parseLong(matcher.group(4));
      } else {
        throw new IllegalArgumentException("Invalid format " + period);
      }
    }

    private void addTestData() {
      String id = xmlParserHelper.getRequiredAttribute("id");

      goToTag("TestMethod");

      String dllLocation = xmlParserHelper.getRequiredAttribute("codeBase");
      String className = xmlParserHelper.getRequiredAttribute("className");

      String projectName = extractProjectName(dllLocation);
      unitTestResults.setClassNameOfTestResult(id, className, projectName);
    }

    private void goToTag(String tagName) {
      String readTagName;
      while ((readTagName = xmlParserHelper.nextTag()) != null) {
        if (tagName.equals(readTagName)) {
          break;
        }
      }
    }

    private static String extractProjectName(String dllLocation) {
      File dllFile = new File(dllLocation);
      return FilenameUtils.getBaseName(dllFile.getName());
    }
  }
}
