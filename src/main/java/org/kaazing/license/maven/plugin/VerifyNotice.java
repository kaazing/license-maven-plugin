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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.License;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;

/**
 * Verify Notice
 * 
 * @goal verify-notice
 * @phase validate
 * 
 * @requiresDependencyResolution compile
 */
public class VerifyNotice extends AbstractLicenseMojo {

    /** @component */
    private MavenProjectBuilder mavenProjectBuilder;

    /** @parameter default-value="${localRepository}" */
    private org.apache.maven.artifact.repository.ArtifactRepository localRepository;

    @SuppressWarnings("rawtypes")
    /** @parameter default-value="${project.remoteArtifactRepositories}" */
    private java.util.List remoteArtifactRepositories;

    /**
     * @parameter default-value="${project.build.directory}/NOTICE.txt"
     */
    private String noticeOutput;

    /**
     * @parameter default-value=true
     */
    private boolean strict;

    /**
     * @parameter default-value=true
     */
    private boolean matchWithExisting;

    /**
     * @parameter default-value="${basedir}/NOTICE.txt"
     */
    private String notice;

    /**
     * @parameter
     */
    private List<ProjectDescription> modifiedCode;

    /**
     * @parameter
     */
    private List<ProjectDescription> projectHints;

    /**
     * @parameter default-value="UTF-8"
     */
    private String encoding;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        // Recursively get Maven Projects for Dependencies
        Set<MavenProject> dependenciesMavenProject = new TreeSet<MavenProject>(new MavenProjectComparator());
        loadAllDepenencyProject(dependenciesMavenProject, getProject());
        List<MavenProject> dependenciesMavenProjectList = new ArrayList<MavenProject>(dependenciesMavenProject);
        StringBuilder sb = new StringBuilder();
        for (MavenProject dependencyProj : dependenciesMavenProjectList) {
            sb.append("This product depends on ");
            sb.append(dependencyProj.getName());
            sb.append(" ");
            String version = dependencyProj.getVersion();
            String[] versions = version.split("\\.");
            if (versions.length == 1) {
                sb.append(versions[0]);
            } else if (versions.length >= 2) {
                sb.append(versions[0] + "." + versions[1]);
            } else {
                sb.append(version);
            }
            sb.append("\n\n");
            List<License> licenses = getLicenses(dependencyProj);
            if (licenses.size() > 0) {
                for (License license : licenses) {
                    sb.append("\tLicense:\t");
                    sb.append(license.getUrl());
                    sb.append(" (");
                    sb.append(license.getName());
                    sb.append(")");
                    sb.append("\n");
                }
            } else {
                // attempt license resolution
                if (!resolveLicense(sb, dependencyProj)) {
                    if (!strict) {
                        sb.append("\tLicense is not included in maven artifact, look at homepage for license\t");
                        sb.append("\n");
                    } else {
                        throw new MojoFailureException("Artifact " + dependencyProj.getArtifactId()
                                + " does not have a license in pom, include it in plugin configuration");
                    }
                }
            }
            sb.append("\tHomepage:\t");
            String projUrl = dependencyProj.getUrl();
            if (projUrl == null) {
                if (!resolveProjUrl(sb, dependencyProj)) {
                    if (!strict) {
                        sb.append("Home page is not included in maven artifact, and thus couldn't be referenced here");
                    } else {
                        throw new MojoFailureException("Artifact " + dependencyProj.getArtifactId()
                                + " does not have a homepage in pom, include it in plugin configuration");
                    }
                }
            } else {
                sb.append(projUrl);
            }
            sb.append("\n\n");
        }
        if (modifiedCode != null && !modifiedCode.isEmpty()) {
            Collections.sort(modifiedCode, new ProjectDescriptionComparator());
            for (ProjectDescription modifiedCodeInstance : modifiedCode) {
                sb.append("This product contains a modified version of ");
                sb.append(modifiedCodeInstance.getProjectName());
                sb.append(" ");
                sb.append(modifiedCodeInstance.getVersion());
                sb.append("\n\n");
                sb.append("\tLicense:\t");
                sb.append(modifiedCodeInstance.getLicenseName());
                sb.append(" (");
                sb.append(modifiedCodeInstance.getLicenseUrl());
                sb.append(")");
                sb.append("\n");
                sb.append("\tHomepage:\t");
                sb.append(modifiedCodeInstance.getHomePage());
                sb.append("\n\n");
            }
        }

        if (dependenciesMavenProjectList.isEmpty() && (modifiedCode == null || modifiedCode.isEmpty())) {

        } else {
            new File(new File(noticeOutput).getParent()).mkdirs();
            try (PrintWriter out = new PrintWriter(noticeOutput)) {
                out.write(sb.toString());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                throw new MojoFailureException("Failed to save notice to output file ", e);
            }
        }
        if (matchWithExisting) {
            try {
                boolean cmp = compareFilesLineByLine(notice, noticeOutput);
                if (!cmp) {
                    throw new MojoFailureException(notice + " does not equal generated " + noticeOutput);
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new MojoFailureException("Failed to compare notice files", e);
            }
        }

    }

    private boolean compareFilesLineByLine(String noticeString, String noticeOutputString) throws IOException {
        File notice = new File(noticeString);
        File noticeOutput = new File(noticeOutputString);
        boolean result = true;
        if (!notice.exists() && !noticeOutput.exists()) {
            return true;
        }
        if (notice.exists() && !noticeOutput.exists()) {
            return false;
        }

        List<String> noticeOutputLines = Files.readAllLines(Paths.get(noticeOutput.getAbsolutePath()),
                Charset.forName(encoding));

        if (!(noticeOutputLines.size() == 0) && notice.exists()) {
            List<String> noticeLines = Files.readAllLines(Paths.get(notice.getAbsolutePath()),
                    Charset.forName(encoding));
            if (noticeLines.size() == noticeOutputLines.size()) {
                for (int i = 0; i < noticeLines.size(); i++) {
                    if (!noticeLines.get(i).equals(noticeOutputLines.get(i))) {
                        result = false;
                        break;
                    }
                }
            } else {
                result = false;
            }
        }
        if (!notice.exists() && noticeOutput.exists()) {
            result = false;
        }
        return result;
    }

    private boolean resolveProjUrl(StringBuilder sb, MavenProject dependencyProj) {
        boolean result = false;
        ProjectDescription description = getProjectDescription(dependencyProj);
        if (description != null) {
            sb.append(description.getHomePage());
            result = true;
        }
        return result;
    }

    private boolean resolveLicense(StringBuilder sb, MavenProject dependencyProj) {
        boolean result = false;
        ProjectDescription description = getProjectDescription(dependencyProj);
        if (description != null) {
            sb.append("\tLicense:\t");
            sb.append(description.getLicenseUrl());
            sb.append(" (");
            sb.append(description.getLicenseName());
            sb.append(")");
            sb.append("\n");
            result = true;
        }
        return result;
    }

    private ProjectDescription getProjectDescription(MavenProject dependencyProj) {
        ProjectDescription result = null;
        if (!(projectHints == null || projectHints.isEmpty())) {
            for (ProjectDescription description : projectHints) {
                if (description.getProjectName().equalsIgnoreCase(dependencyProj.getName())) {
                    result = description;
                    break;
                }
            }
        }
        return result;
    }

    private void loadAllDepenencyProject(Set<MavenProject> dependenciesMavenProject, MavenProject project) {
        Log log = getLog();

        if (!dependenciesMavenProject.contains(project)) {
            Set<Artifact> artifacts = getDependencyArtifacts(project);
            // artifacts.addAll(getTestDependencies(project));
            for (Artifact artifact : artifacts) {
                // if (artifact != null) {
                try {
                    MavenProject depProject = mavenProjectBuilder.buildFromRepository(artifact,
                            remoteArtifactRepositories, localRepository, true);
                    loadAllDepenencyProject(dependenciesMavenProject, depProject);
                    dependenciesMavenProject.add(depProject);
                } catch (ProjectBuildingException e) {
                    log.warn("Could not find a pom for the artifact: " + artifact.getGroupId() + ":"
                            + artifact.getArtifactId());
                    continue;
                }
                // }
            }
        }
        return;
    }

}
