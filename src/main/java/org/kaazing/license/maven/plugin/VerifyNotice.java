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

import static java.lang.String.format;

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
     * Where the generated notice file will be written
     * @parameter default-value="${project.build.directory}/NOTICE.txt"
     */
    private String noticeOutput;

    /**
     * Whether to fail or not if it can not find the license information (either in the plugin or in m2/global
     * repository) for a dependency
     * @parameter default-value=true
     */
    private boolean strict;

    /**
     * Whether to compare the generated notice file with the already existing notice file and fail when they do not
     * match
     * @parameter default-value=true
     */
    private boolean matchWithExisting;

    /**
     * The already existing notice file
     * @parameter default-value="${basedir}/NOTICE.txt"
     */
    private String notice;

    /**
     * List all projects that have the generated notice file should include as having contained modified code from
     * @parameter
     */
    private List<ProjectDescription> modifiedCode;

    /**
     * Projects that do not have fully specified license information in maven central (Older maven projects) can be
     * filled in here to provide informational hints to the plugin
     * @parameter
     */
    private List<ProjectDescription> projectHints;

    /**
     * @parameter default-value="UTF-8"
     */
    private String encoding;

    private String formatLicense(String url, String name) {
        return String.format("\tLicense:\t%s (%s)\n", url, name);
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        Set<MavenProject> dependenciesMavenProject = new TreeSet<MavenProject>(new MavenProjectComparator());
        loadAllDepenencyProject(dependenciesMavenProject, getProject());
        List<MavenProject> dependenciesMavenProjectList = new ArrayList<MavenProject>(dependenciesMavenProject);
        StringBuilder sb = new StringBuilder();
        for (MavenProject dependencyProj : dependenciesMavenProjectList) {

            String version = dependencyProj.getVersion();
            String[] versions = version.split("\\.");
            // attempt to just get major minor version of dependency
            if (versions.length == 1) {
                version = versions[0];
            } else if (versions.length >= 2) {
                version = versions[0] + "." + versions[1];
            }
            sb.append(String.format("This product depends on %s %s\n\n", dependencyProj.getName(), version));

            // add license to notice
            List<License> licenses = getLicenses(dependencyProj);
            if (licenses.size() > 0) {
                // if have license add them
                for (License license : licenses) {
                    sb.append(formatLicense(license.getUrl(), license.getName()));
                }
            } else {
                // else attempt adding license from hints
                ProjectDescription description = getProjectDescriptionFromHints(dependencyProj);
                if (description != null) {
                    sb.append(formatLicense(description.getLicenseUrl(), description.getLicenseName()));
                } else if (!strict) {
                    sb.append("\tLicense is not included in maven artifact, look at homepage for license\t\n");
                } else {
                    throw new MojoFailureException("Artifact " + dependencyProj.getArtifactId() + " with name \""
                            + dependencyProj.getName() + "\""
                            + " does not have a license in pom, include it in plugin configuration");
                }
            }

            // add homepage to notice
            String homePage = dependencyProj.getUrl();
            if (homePage != null) {
                sb.append(String.format("\tHomepage:\t%s\n", homePage));
            } else {
                ProjectDescription description = getProjectDescriptionFromHints(dependencyProj);
                if (description != null) {
                    sb.append(String.format("\tHomepage:\t%s\n", description.getHomePage()));
                } else if (!strict) {
                    sb.append("Home page is not included in maven artifact, and thus couldn't be referenced here\n");
                } else {
                    throw new MojoFailureException("Artifact " + dependencyProj.getArtifactId()
                            + " does not have a homepage in pom, include it in plugin configuration");
                }
            }

            // add new line for formatting
            sb.append("\n");
        }
        if (modifiedCode != null && !modifiedCode.isEmpty()) {
            Collections.sort(modifiedCode, new ProjectDescriptionComparator());
            for (ProjectDescription modifiedCodeInstance : modifiedCode) {
                sb.append(format("This product contains a modified version of %s %s\n\n",
                        modifiedCodeInstance.getProjectName(), modifiedCodeInstance.getVersion()));
                sb.append(format("\tLicense:\t%s (%s)\n", modifiedCodeInstance.getLicenseName(),
                        modifiedCodeInstance.getLicenseUrl()));
                sb.append(format("\tHomepage:\t%s\n\n", modifiedCodeInstance.getHomePage()));
            }
        }

        // If there are dependencies or modified code, write it to the output file
        if (!(dependenciesMavenProjectList.isEmpty() && (modifiedCode == null || modifiedCode.isEmpty()))) {
            new File(new File(noticeOutput).getParent()).mkdirs();
            try (PrintWriter out = new PrintWriter(noticeOutput)) {
                out.write(sb.toString());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                throw new MojoFailureException("Failed to save notice to output file ", e);
            }
        }

        // If matching with existing, attempt match
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

    /**
     * Compares two files line by line
     * @param noticeString
     * @param noticeOutputString
     * @return
     * @throws IOException
     */
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

    private ProjectDescription getProjectDescriptionFromHints(MavenProject dependencyProj) {
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

    /**
     * Recursively load all dependencies of this project (not in the test scope) and adds them to the mavenDependencies
     * @param mavenDependencies
     * @param project
     */
    private void loadAllDepenencyProject(Set<MavenProject> mavenDependencies, MavenProject project) {
        Log log = getLog();

        if (!mavenDependencies.contains(project)) {
            Set<Artifact> artifacts = getDependencyArtifacts(project);
            // artifacts.addAll(getTestDependencies(project));
            for (Artifact artifact : artifacts) {
                // if (artifact != null) {
                try {
                    MavenProject depProject = mavenProjectBuilder.buildFromRepository(artifact,
                            remoteArtifactRepositories, localRepository, true);
                    loadAllDepenencyProject(mavenDependencies, depProject);
                    mavenDependencies.add(depProject);
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
