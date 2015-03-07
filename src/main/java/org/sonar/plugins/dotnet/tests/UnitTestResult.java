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

import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.TestCase;

public class UnitTestResult {
  private String id;
  private String name;
  private long milliseconds;
  private TestCase.Status result;
  private String className;
  private String projectName;

  public UnitTestResult(String id, String name, long milliseconds, TestCase.Status result) {
    this.id = id;
    this.name = name;
    this.milliseconds = milliseconds;
    this.result = result;
  }

  public String getId() {
    return id;
  }

  public UnitTestResult setClassName(String className, String projectName) {
    this.className = className;
    this.projectName = projectName;

    return this;
  }

  public void storeMeasure(ResourcePerspectives perspectives, FileProvider fileProvider) {
    org.sonar.api.resources.File resource = GetFile(fileProvider);
    if (resource == null) {
      return;
    }

    MutableTestPlan testPlan = perspectives.as(MutableTestPlan.class, resource);
    if (testPlan == null) {
      return;
    }

    testPlan.addTestCase(name)
            .setDurationInMs(milliseconds)
            .setStatus(result)
            .setType(TestCase.TYPE_UNIT);
  }

  private org.sonar.api.resources.File GetFile(FileProvider fileProvider) {
    if (projectName == null || className == null) {
      return null;
    }

    String path = className.substring(projectName.length() + 1);
    path = path.replace('.', '\\');

    String fullPath = (projectName + "\\" + path + ".cs").replace('\\', '/');
    return fileProvider.fromPath(fullPath);
  }

  @Override
  public String toString() {
    return "id:" + id + ", name:" + name + ", time:" + milliseconds + ", result:" + result + ", className:" + className + ", projectName:" + projectName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (getClass() != o.getClass()) return false;

    UnitTestResult that = (UnitTestResult) o;

    if (milliseconds != that.milliseconds) return false;
    if (className != null ? !className.equals(that.className) : that.className != null) return false;
    if (projectName != null ? !projectName.equals(that.projectName) : that.projectName != null) return false;
    if (id != null ? !id.equals(that.id) : that.id != null) return false;
    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    return result == that.result;
  }

  @Override
  public int hashCode() {
    int result1 = id != null ? id.hashCode() : 0;
    result1 = 31 * result1 + (name != null ? name.hashCode() : 0);
    result1 = 31 * result1 + (int) (milliseconds ^ (milliseconds >>> 32));
    result1 = 31 * result1 + (result != null ? result.hashCode() : 0);
    result1 = 31 * result1 + (className != null ? className.hashCode() : 0);
    result1 = 31 * result1 + (projectName != null ? projectName.hashCode() : 0);
    return result1;
  }
}
