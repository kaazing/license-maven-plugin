/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.license.maven.plugin;

import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.License;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

public abstract class AbstractLicenseMojo extends AbstractMojo {

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    // Collection<? extends Artifact> getTestDependencies(MavenProject project) {
    // @SuppressWarnings("unchecked")
    // List<Artifact> testDependencies = project.get;
    // return testDependencies;
    // }

    Set<Artifact> getDependencyArtifacts(MavenProject project) {
        @SuppressWarnings("unchecked") Set<Artifact> artifacts = project.getArtifacts();
        return artifacts;
    }

    List<License> getLicenses(MavenProject project) {
        @SuppressWarnings("unchecked") List<License> licenses = project.getLicenses();
        return licenses;
    }

    public MavenProject getProject() {
        return project;
    }

}
