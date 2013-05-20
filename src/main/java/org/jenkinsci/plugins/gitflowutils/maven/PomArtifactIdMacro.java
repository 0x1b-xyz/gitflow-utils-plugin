package org.jenkinsci.plugins.gitflowutils.maven;

import hudson.maven.MavenModule;

/**
 * Looks at the build artifact record to get the artifactId.
 *
 * @author jason@stiefel.io
 */
public class PomArtifactIdMacro extends AbstractPomMacro {

    public static final String MACRO_POM_ARTIFACTID = "POM_ARTIFACTID";

    @Override
    protected String getMacroName() {
        return MACRO_POM_ARTIFACTID;
    }

    @Override
    protected String getMacroValue(MavenModule mavenModule) {
        return mavenModule.getModuleName().artifactId;
    }

}
