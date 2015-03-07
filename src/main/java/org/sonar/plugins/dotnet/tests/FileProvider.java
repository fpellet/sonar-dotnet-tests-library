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

import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;

import java.io.File;

/**
 * Layer of abstraction to improve the testability of the sensor.
 */
public class FileProvider {

  private final Project project;
  private final SensorContext context;

  public FileProvider(Project project, SensorContext context) {
    this.project = project;
    this.context = context;
  }

  public org.sonar.api.resources.File fromPath(String path) {
    // Workaround SonarQube < 4.2, the context should not be required
    File file = new File(new File(path).getAbsolutePath());
    org.sonar.api.resources.File ioFile = org.sonar.api.resources.File.fromIOFile(file, project);
    return context.getResource(ioFile);
  }
}
