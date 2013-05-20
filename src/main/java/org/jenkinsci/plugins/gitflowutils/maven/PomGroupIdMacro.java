package org.jenkinsci.plugins.gitflowutils.maven;

import hudson.maven.MavenModule;

/**
 * Looks at the build artifact record to get the groupId.
 *
 * @author jason@stiefel.io
 */
public class PomGroupIdMacro extends AbstractPomMacro {

    public static final String MACRO_POM_GROUPID = "POM_GROUPID";

    @Override
    protected String getMacroName() {
        return MACRO_POM_GROUPID;
    }

    @Override
    protected String getMacroValue(MavenModule mavenModule) {
        return mavenModule.getModuleName().groupId;
    }

}
