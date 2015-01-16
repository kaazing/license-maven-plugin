# Kaazing maven.license.plugin

[![Build Status][build-status-image]][build-status]

[build-status-image]: https://travis-ci.org/kaazing/license-maven-plugin.svg?branch=develop
[build-status]: https://travis-ci.org/kaazing/license-maven-plugin

Plugin to generate Notice on a project 
```
    /**
     * Where to generate notice
     * @parameter default-value="target/NOTICE.txt"
     */
    private String noticeOutput;

    /**
     * Fail if any artifact is missing a required parameter 
     * @parameter default-value=true
     */
    private boolean strict;

    /**
     * Fail if generated does not match existing 
     * @parameter default-value=true
     */
    private boolean matchWithExisting;

    /**
     * Existing notice 
     * @parameter default-value="NOTICE.txt"
     */
    private String notice;
```

Hints and modified code notices can be added as shown below
```
            <plugin>
                <groupId>org.kaazing</groupId>
                <artifactId>license-maven-plugin</artifactId>
                <version>1.0.0.0-SNAPSHOT</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>verify-notice</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <!-- Modified code hints go here -->
                    <modifiedCode>
                        <projectDescription>
                            <projectName>projectName</projectName>
                            <licenseName>licenseName</licenseName>
                            <licenseUrl>licenseUrl</licenseUrl>
                            <homePage>homePage</homePage>
                            <version>version</version>
                        </projectDescription>
                        <projectDescription>
                            <projectName>projectName2</projectName>
                            <licenseName>licenseName2</licenseName>
                            <licenseUrl>licenseUrl2</licenseUrl>
                            <homePage>homePage2</homePage>
                            <version>version2</version>
                        </projectDescription>
                    </modifiedCode>
                    <!-- Project hints go here -->
                    <projectHints>
                        <projectDescription>
                            <projectName>JDOM</projectName>
                            <licenseName>JDOMlicenseName</licenseName>
                            <licenseUrl>licenseUrl</licenseUrl>
                            <homePage>JDOMhomePage</homePage>
                            <version>JDOMhomePage</version>
                        </projectDescription>
                    </projectHints>
                </configuration>
            </plugin>
```

