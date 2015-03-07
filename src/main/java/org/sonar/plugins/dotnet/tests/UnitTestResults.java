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

import org.sonar.api.test.TestCase;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class UnitTestResults {
  private int tests;
  private int passed;
  private int skipped;
  private int failures;
  private int errors;
  private long totalExecutionTimeInMilliseconds;
  private Map<String, UnitTestResult> resultsById = new HashMap<String, UnitTestResult>();

  public void add(int tests, int passed, int skipped, int failures, int errors) {
    this.tests += tests;
    this.passed += passed;
    this.skipped += skipped;
    this.failures += failures;
    this.errors += errors;
  }

  public void addTestResult(String id, String name, long milliseconds, TestCase.Status result) {
    resultsById.put(id, new UnitTestResult(id, name, milliseconds, result));

    this.totalExecutionTimeInMilliseconds += milliseconds;
  }

  public void setClassNameOfTestResult(String id, String className, String projectName) {
    if(resultsById.containsKey(id)){
      resultsById.get(id).setClassName(className, projectName);
    }
  }

  public double tests() {
    return tests;
  }

  public double passedPercentage() {
    return passed * 100.0 / tests();
  }

  public double skipped() {
    return skipped;
  }

  public double failures() {
    return failures;
  }

  public double errors() {
    return errors;
  }

  public long totalExecutionTimeInMilliseconds() {
    return totalExecutionTimeInMilliseconds;
  }

  public Collection<UnitTestResult> results() {
    return resultsById.values();
  }
}
