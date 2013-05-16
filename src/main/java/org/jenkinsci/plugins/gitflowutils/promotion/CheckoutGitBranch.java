package org.jenkinsci.plugins.gitflowutils.promotion;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.plugins.git.GitSCM;
import hudson.plugins.promoted_builds.Promotion;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.apache.commons.lang.RandomStringUtils;
import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Checks the promoted {@link hudson.plugins.git.GitSCM#GIT_BRANCH} into a randomized directory.
 *
 * @author jason@stiefel.io
 */
public class CheckoutGitBranch extends Builder {

    private static final Logger LOGGER = Logger.getLogger(CheckoutGitBranch.class.getName());

    @DataBoundConstructor
    public CheckoutGitBranch() {}

    @Override
    public boolean perform(AbstractBuild<?, ?> _build, Launcher launcher, final BuildListener listener)
            throws InterruptedException, IOException {

        Promotion build = (Promotion)_build;

        final EnvVars environment = build.getEnvironment(listener);
        final String gitBranch = environment.get("PROMOTED_" + GitSCM.GIT_BRANCH);
        if (gitBranch == null)
            throw new IllegalStateException("PROMOTED_" + GitSCM.GIT_BRANCH + " environment variable is not present");

        if (!(build.getTarget().getProject().getScm() instanceof GitSCM)) {
            listener.error("Promoted build does not use the GitSCM!");
            return false;
        }

        FilePath workspace = build.getWorkspace();

        listener.getLogger().println("Clearing workspace at " + workspace.getRemote());
        workspace.deleteContents();

        GitSCM scm = (GitSCM)build.getTarget().getProject().getScm();
        final String gitExe = scm.getGitExe(build.getBuiltOn(), listener);

        listener.getLogger().println("Checking out " + gitBranch + " into " + workspace.getRemote());

        workspace.act(new FilePath.FileCallable<Void>() {
            public Void invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {

                GitClient git = Git.with(listener, environment).in(workspace).using(gitExe).getClient();
                git.checkoutBranch(gitBranch, "HEAD");

                return null;
            }
        });

        listener.getLogger().println("Complete");

        return true;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return jobType == PromotionProcess.class;
        }
        @Override
        public String getDisplayName() {
            return "Checkout Promoted Git Branch";
        }
    }

}
