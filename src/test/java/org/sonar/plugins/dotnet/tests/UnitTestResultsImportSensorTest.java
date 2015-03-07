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

import com.google.common.collect.ImmutableList;
import edu.emory.mathcs.backport.java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.TestCase;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedList;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class UnitTestResultsImportSensorTest {
  private ResourcePerspectives perspectives;

  @Before
  public void before() {
    perspectives = mock(ResourcePerspectives.class);
  }

  @Test
  public void should_execute_on_project() {
    Project project = mock(Project.class);

    UnitTestResultsAggregator unitTestResultsAggregator = mock(UnitTestResultsAggregator.class);

    when(unitTestResultsAggregator.hasUnitTestResultsProperty()).thenReturn(true);
    assertThat(new UnitTestResultsImportSensor(unitTestResultsAggregator, perspectives).shouldExecuteOnProject(project)).isTrue();

    when(unitTestResultsAggregator.hasUnitTestResultsProperty()).thenReturn(false);
    assertThat(new UnitTestResultsImportSensor(unitTestResultsAggregator, perspectives).shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void analyze() {
    UnitTestResults results = mock(UnitTestResults.class);
    when(results.tests()).thenReturn(42.0);
    when(results.passedPercentage()).thenReturn(84.0);
    when(results.skipped()).thenReturn(1.0);
    when(results.failures()).thenReturn(2.0);
    when(results.errors()).thenReturn(3.0);
    when(results.totalExecutionTimeInMilliseconds()).thenReturn(6l);

    UnitTestResultsAggregator unitTestResultsAggregator = mock(UnitTestResultsAggregator.class);
    SensorContext context = mock(SensorContext.class);

    when(unitTestResultsAggregator.aggregate(Mockito.any(WildcardPatternFileProvider.class), Mockito.any(UnitTestResults.class))).thenReturn(results);

    new UnitTestResultsImportSensor(unitTestResultsAggregator, perspectives).analyze(context, results);

    verify(unitTestResultsAggregator).aggregate(Mockito.any(WildcardPatternFileProvider.class), Mockito.eq(results));

    verify(context).saveMeasure(CoreMetrics.TESTS, 42.0);
    verify(context).saveMeasure(CoreMetrics.TEST_SUCCESS_DENSITY, 84.0);
    verify(context).saveMeasure(CoreMetrics.SKIPPED_TESTS, 1.0);
    verify(context).saveMeasure(CoreMetrics.TEST_FAILURES, 2.0);
    verify(context).saveMeasure(CoreMetrics.TEST_ERRORS, 3.0);
    verify(context).saveMeasure(CoreMetrics.TEST_EXECUTION_TIME, 6.0);
  }

  @Test
  public void should_not_save_metrics_with_empty_results() {
    SensorContext context = mock(SensorContext.class);

    UnitTestResultsAggregator unitTestResultsAggregator = mock(UnitTestResultsAggregator.class);
    UnitTestResults results = mock(UnitTestResults.class);
    when(results.tests()).thenReturn(0.0);
    when(results.skipped()).thenReturn(1.0);
    when(results.failures()).thenReturn(2.0);
    when(results.errors()).thenReturn(3.0);
    when(unitTestResultsAggregator.aggregate(Mockito.any(WildcardPatternFileProvider.class), Mockito.any(UnitTestResults.class))).thenReturn(results);

    new UnitTestResultsImportSensor(unitTestResultsAggregator, perspectives).analyze(context, results);

    verify(unitTestResultsAggregator).aggregate(Mockito.any(WildcardPatternFileProvider.class), Mockito.eq(results));
    verify(context).saveMeasure(CoreMetrics.TESTS, 0.0);
    verify(context).saveMeasure(CoreMetrics.SKIPPED_TESTS, 1.0);
    verify(context).saveMeasure(CoreMetrics.TEST_FAILURES, 2.0);
    verify(context).saveMeasure(CoreMetrics.TEST_ERRORS, 3.0);
    verify(context, Mockito.never()).saveMeasure(Mockito.eq(CoreMetrics.TEST_SUCCESS_DENSITY), Mockito.anyDouble());
  }

  @Test
  public void should_analyze_on_reactor_project() {
    Project project = mock(Project.class);
    when(project.isRoot()).thenReturn(true);
    when(project.getModules()).thenReturn(ImmutableList.of(mock(Project.class)));

    SensorContext context = mock(SensorContext.class);

    UnitTestResultsAggregator unitTestResultsAggregator = mock(UnitTestResultsAggregator.class);
    UnitTestResults results = mock(UnitTestResults.class);
    when(results.tests()).thenReturn(1.0);
    when(unitTestResultsAggregator.aggregate(Mockito.any(WildcardPatternFileProvider.class), Mockito.any(UnitTestResults.class))).thenReturn(results);

    new UnitTestResultsImportSensor(unitTestResultsAggregator, perspectives).analyse(project, context);

    verify(context, Mockito.atLeastOnce()).saveMeasure(Mockito.any(Metric.class), Mockito.anyDouble());
  }

  @Test
  public void should_not_analyze_on_multi_module_modules() {
    Project project = mock(Project.class);
    when(project.isRoot()).thenReturn(false);

    SensorContext context = mock(SensorContext.class);

    UnitTestResultsAggregator unitTestResultsAggregator = mock(UnitTestResultsAggregator.class);
    UnitTestResults results = mock(UnitTestResults.class);
    when(results.tests()).thenReturn(1.0);
    when(unitTestResultsAggregator.aggregate(Mockito.any(WildcardPatternFileProvider.class), Mockito.any(UnitTestResults.class))).thenReturn(results);

    new UnitTestResultsImportSensor(unitTestResultsAggregator, perspectives).analyse(project, context);

    verify(context, Mockito.never()).saveMeasure(Mockito.any(Metric.class), Mockito.anyDouble());
  }

  @Test
  public void should_analyze_on_non_multi_module_project() {
    Project project = mock(Project.class);
    when(project.isRoot()).thenReturn(true);
    when(project.getModules()).thenReturn(Collections.emptyList());

    SensorContext context = mock(SensorContext.class);

    UnitTestResultsAggregator unitTestResultsAggregator = mock(UnitTestResultsAggregator.class);
    UnitTestResults results = mock(UnitTestResults.class);
    when(results.tests()).thenReturn(1.0);
    when(unitTestResultsAggregator.aggregate(Mockito.any(WildcardPatternFileProvider.class), Mockito.any(UnitTestResults.class))).thenReturn(results);

    new UnitTestResultsImportSensor(unitTestResultsAggregator, perspectives).analyse(project, context);

    verify(context, Mockito.atLeastOnce()).saveMeasure(Mockito.any(Metric.class), Mockito.anyDouble());
  }

  @Test
  public void should_register_tests() throws URISyntaxException {
    UnitTestResult test = new UnitTestResult("idA", "NameA", 2, TestCase.Status.OK).setClassName("ProjectA.ClassA", "ProjectA");
    org.sonar.api.resources.File file = mock(org.sonar.api.resources.File.class);

    MutableTestCase testCase = mock(MutableTestCase.class);
    when(testCase.setDurationInMs(eq(2l))).thenReturn(testCase);
    when(testCase.setStatus(eq(TestCase.Status.OK))).thenReturn(testCase);
    when(testCase.setType(eq(TestCase.TYPE_UNIT))).thenReturn(testCase);
    MutableTestPlan testPlan = mock(MutableTestPlan.class);
    when(testPlan.addTestCase(eq("NameA"))).thenReturn(testCase);

    when(perspectives.as(eq(MutableTestPlan.class), eq(file))).thenReturn(testPlan);

    UnitTestResultsAggregator unitTestResultsAggregator = mock(UnitTestResultsAggregator.class);
    UnitTestResults results = mock(UnitTestResults.class);
    Collection<UnitTestResult> testResults = new LinkedList<UnitTestResult>();
    testResults.add(test);
    when(results.results()).thenReturn(testResults);
    when(unitTestResultsAggregator.aggregate(Mockito.any(WildcardPatternFileProvider.class), Mockito.any(UnitTestResults.class))).thenReturn(results);

    FileProvider fileProvider = mock(FileProvider.class);
    when(fileProvider.fromPath(eq("ProjectA/ClassA.cs"))).thenReturn(file);

    new UnitTestResultsImportSensor(unitTestResultsAggregator, perspectives).analyzeProject(fileProvider, results);

    verify(testPlan).addTestCase("NameA");
  }

  @Test
  public void should_ignore_tests_without_className() throws URISyntaxException {
    UnitTestResult test = new UnitTestResult("idA", "NameA", 2, TestCase.Status.OK);

    MutableTestPlan testPlan = mock(MutableTestPlan.class);
    when(perspectives.as(eq(MutableTestPlan.class), any(File.class))).thenReturn(testPlan);

    UnitTestResultsAggregator unitTestResultsAggregator = mock(UnitTestResultsAggregator.class);
    UnitTestResults results = mock(UnitTestResults.class);
    Collection<UnitTestResult> testResults = new LinkedList<UnitTestResult>();
    testResults.add(test);
    when(results.results()).thenReturn(testResults);
    when(unitTestResultsAggregator.aggregate(Mockito.any(WildcardPatternFileProvider.class), Mockito.any(UnitTestResults.class))).thenReturn(results);

    FileProvider fileProvider = mock(FileProvider.class);

    new UnitTestResultsImportSensor(unitTestResultsAggregator, perspectives).analyzeProject(fileProvider, results);

    verifyZeroInteractions(testPlan);
  }

  @Test
  public void should_ignore_tests_with_bad_file() throws URISyntaxException {
    UnitTestResult test = new UnitTestResult("idA", "NameA", 2, TestCase.Status.OK).setClassName("ProjectA.ClassA", "ProjectA");

    MutableTestPlan testPlan = mock(MutableTestPlan.class);
    when(perspectives.as(eq(MutableTestPlan.class), any(File.class))).thenReturn(testPlan);

    UnitTestResultsAggregator unitTestResultsAggregator = mock(UnitTestResultsAggregator.class);
    UnitTestResults results = mock(UnitTestResults.class);
    Collection<UnitTestResult> testResults = new LinkedList<UnitTestResult>();
    testResults.add(test);
    when(results.results()).thenReturn(testResults);
    when(unitTestResultsAggregator.aggregate(Mockito.any(WildcardPatternFileProvider.class), Mockito.any(UnitTestResults.class))).thenReturn(results);

    FileProvider fileProvider = mock(FileProvider.class);
    when(fileProvider.fromPath(any(String.class))).thenReturn(null);

    new UnitTestResultsImportSensor(unitTestResultsAggregator, perspectives).analyzeProject(fileProvider, results);

    verifyZeroInteractions(testPlan);
  }
}
