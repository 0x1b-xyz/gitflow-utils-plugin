package org.jenkinsci.plugins.gitflowutils.maven;

import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.promoted_builds.Promotion;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author jason@stiefel.io
 */
public abstract class AbstractPomMacro extends DataBoundTokenMacro {

    private Logger LOG = Logger.getLogger(getClass().getName());

    public static final String UNKNOWN = "unknown";

    /**
     * Sub-class returns the name of the macro we can handle
     */
    protected abstract String getMacroName();

    /**
     * Sub-class should use the provided {@link MavenModule} to return the value or {@code null} if not possible
     */
    protected abstract String getMacroValue(MavenModule mavenModule);

    /**
     * Returns {@link #getMacroName()} == {@param s}.
     */
    @Override
    public boolean acceptsMacroName(String s) {
        return getMacroName().equals(s);
    }

    @Override
    public String evaluate(AbstractBuild<?, ?> abstractBuild, TaskListener taskListener, String s)
            throws MacroEvaluationException, IOException, InterruptedException {

        MavenModuleSetBuild build = getModuleSetBuild(abstractBuild);
        if (build == null)
            return UNKNOWN;

        try {
            String value = getMacroValue(getModule(getModuleSetBuild(abstractBuild)));
            if (value != null)
                return value;

        } catch (Throwable t) {
            taskListener.error("Could not determine value for " + getMacroName() + ": " + t);
        }

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Attempts to find the {@link hudson.maven.MavenModuleSetBuild} from the specified build that may be a {@link hudson.plugins.promoted_builds.Promotion}.
     */
    protected MavenModuleSetBuild getModuleSetBuild(AbstractBuild abstractBuild) {

        if (abstractBuild instanceof Promotion)
            return getModuleSetBuild(((Promotion)abstractBuild).getTarget());

        if (!(abstractBuild instanceof MavenModuleSetBuild)) {
            LOG.warning("Could not determine " + PomVersionMacro.MACRO_POM_VERSION + " for non-Maven build: " + abstractBuild.getClass());
            return null;
        }

        return (MavenModuleSetBuild)abstractBuild;

    }

    /**
     * Attempts to find the root {@link hudson.maven.MavenModule}.
     */
    protected MavenModule getModule(MavenModuleSetBuild build) {
        return build.getModuleBuilds().entrySet().iterator().next().getKey();
    }

}
