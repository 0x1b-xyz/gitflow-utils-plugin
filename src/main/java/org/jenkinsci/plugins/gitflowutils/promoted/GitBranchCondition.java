package org.jenkinsci.plugins.gitflowutils.promoted;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.plugins.git.GitSCM;
import hudson.plugins.promoted_builds.*;
import hudson.util.LogTaskListener;
import org.kohsuke.stapler.DataBoundConstructor;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link PromotionCondition} that is met when the {@code GIT_BRANCH} variable on a build matches a specified
 * condition. Matching is performed using an {@link AntPathMatcher}. Uses a {@link RunListener} to listen for
 * builds that match the patterns.
 *
 * @author jason@stiefel.io
 */
public class GitBranchCondition extends PromotionCondition {

    private static final Logger LOGGER = Logger.getLogger(GitBranchCondition.class.getName());
    private static final AntPathMatcher matcher = new AntPathMatcher();

    private final String patterns;
    private final boolean evenIfUnstable;

    @DataBoundConstructor
    public GitBranchCondition(String patterns, boolean evenIfUnstable) {
        this.patterns = patterns;
        this.evenIfUnstable = evenIfUnstable;
    }

    @Override
    public PromotionBadge isMet(PromotionProcess promotionProcess, AbstractBuild<?, ?> build) {

        Result result = build.getResult();
        if (result != Result.SUCCESS || (result == Result.UNSTABLE && !evenIfUnstable))
            return null;

        String gitBranch = getBranch(build);
        if (gitBranch == null)
            return null;

        String match = getMatch(gitBranch);
        if (match == null)
            return null;

        return new Badge(gitBranch, match);

    }

    /**
     * Extracts the branch from the {@code GIT_BRANCH} build variable, returning {@code null} when not possible.
     */
    public String getBranch(AbstractBuild<?,?> build) {

        EnvVars envVars = null;
        try {
            envVars = build.getEnvironment(new LogTaskListener(LOGGER, Level.INFO));
        } catch (Exception e) {
            LOGGER.severe("Failed getting environment from build: " + build);
            return null;
        }

        String gitBranch = envVars.get(GitSCM.GIT_BRANCH);
        if (gitBranch == null)
            LOGGER.warning("No GIT_BRANCH env var member on build: " + build);

        return gitBranch;

    }

    /**
     * Indicates whether the specified {@param gitBranch} matches one of the patterns we expect, returning the
     * pattern or {@code null} when none match.
     */
    public String getMatch(String gitBranch) {

        for (String pattern : patterns.split(",")) {
            if (matcher.match(pattern.trim(), gitBranch))
                return pattern;
        }

        return null;
    }

    /**
     * Returns the comma delimited list of patterns to be matched
     */
    public String getPatterns() {
        return patterns;
    }

    /**
     * Indicates the condition is met even when the build is not stable
     */
    public boolean isEvenIfUnstable() {
        return evenIfUnstable;
    }

    /**
     * Records the user that executed the promotion.
     */
    public static final class Badge extends PromotionBadge {

        private final String branch;
        private final String matched;

        public Badge(String branch, String matched) {
            this.branch = branch;
            this.matched = matched;
        }

        public String getBranch() {
            return branch;
        }

        public String getMatched() {
            return matched;
        }

    }

    @Extension
    public static final class DescriptorImpl extends PromotionConditionDescriptor {

        @Override
        public boolean isApplicable(AbstractProject<?, ?> abstractProject) {
            return abstractProject.getScm() instanceof GitSCM;
        }

        @Override
        public String getDisplayName() {
            return "Branch Matches Pattern";
        }

    }

    @Extension
    public static final class RunListenerImpl extends RunListener<AbstractBuild<?,?>> {

        public RunListenerImpl() {
            super((Class)AbstractBuild.class);
        }

        @Override
        public void onCompleted(AbstractBuild<?, ?> build, TaskListener listener) {

            JobPropertyImpl jp = build.getProject().getProperty(JobPropertyImpl.class);
            if (jp == null)
                return;

            for (PromotionProcess p : jp.getItems()) {
                for (PromotionCondition cond : p.conditions) {
                    if (cond instanceof GitBranchCondition) {
                        try {
                            p.considerPromotion2(build);
                            break;
                        } catch (IOException e) {
                            e.printStackTrace(listener.error("Failed to promote a build"));
                        }
                    }
                }
            }

        }
    }

}
