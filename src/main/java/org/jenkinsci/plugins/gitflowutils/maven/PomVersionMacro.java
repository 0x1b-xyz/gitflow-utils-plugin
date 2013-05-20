package org.jenkinsci.plugins.gitflowutils.maven;

import hudson.Extension;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Looks at the build artifact record to get the version.
 *
 * @author jason@stiefel.io
 */
@Extension
public class PomVersionMacro extends AbstractPomMacro {

    public static final String MACRO_POM_VERSION = "POM_VERSION";

    @Override
    protected String getMacroName() {
        return MACRO_POM_VERSION;
    }

    @Override
    protected String getMacroValue(MavenModule mavenModule) {
        return mavenModule.getVersion();
    }

}
