/**
 * Copyright 2007-2015, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.license.maven.plugin;

import java.util.Comparator;

public class ProjectDescriptionComparator implements Comparator<ProjectDescription> {

    @Override
    public int compare(ProjectDescription o1, ProjectDescription o2) {
        String o1String = o1.getProjectName() + o1.getLicenseName() + o1.getLicenseUrl() + o1.getHomePage()
                + o1.getVersion();
        String o2String = o2.getProjectName() + o2.getLicenseName() + o2.getLicenseUrl() + o2.getHomePage()
                + o2.getVersion();
        return o2String.compareTo(o1String);
    }

}
