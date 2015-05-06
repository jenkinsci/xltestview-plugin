package com.xebialabs.xltest.ci;

import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.xebialabs.xltest.ci.server.XLTestServer;
import com.xebialabs.xltest.ci.server.XLTestServerFactory;
import com.xebialabs.xltest.ci.server.domain.TestSpecification;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Map;

import static java.lang.String.format;

// TODO: default include pattern selecteren
public class TestSpecificationDescribable extends AbstractDescribableImpl<TestSpecificationDescribable> {

    private final String testSpecificationId;
    private final String includes;
    private final String excludes;

    @DataBoundConstructor
    public TestSpecificationDescribable(String testSpecificationId, String includes, String excludes) {
        System.out.printf("constructor %s %s %s\n", testSpecificationId, includes, excludes);
        this.includes = includes;
        this.excludes = excludes;
        this.testSpecificationId = testSpecificationId;
    }

    // this getter must correspond with the name of the field in the config.jelly else the selected value is not filled in
    public String getTestSpecificationId() {
        return testSpecificationId;
    }

    public String getExcludes() {
        return excludes;
    }

    public String getIncludes() {
        return includes;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("testSpecificationId", testSpecificationId)
                .add("includes", includes)
                .add("excludes", excludes)
                .toString();
    }

    @Extension
    public static class TestSpecificationDescriptor extends Descriptor<TestSpecificationDescribable> {
        /**
         * Stunningly simple solution, if you dig deep enough.
         * This gets automagically set with the descriptor of the XLTestNotifier relevant to this config bit.
         */
        @Inject
        private XLTestNotifier.XLTestDescriptor xlTestDescriptor;

        @Override
        public String getDisplayName() {
            return "TestSpecification";
        }

        public ListBoxModel doFillTestSpecificationIdItems() {
            ListBoxModel items = new ListBoxModel();

            // no use if no url/creds
            if (xlTestDescriptor.getServerUrl().isEmpty() || xlTestDescriptor.getCredentialsId().isEmpty()) {
                return items;
            }

            XLTestServer xlTest = XLTestServerFactory.newInstance(
                    xlTestDescriptor.getServerUrl(),
                    xlTestDescriptor.getProxyUrl(),
                    XLTestNotifier.lookupSystemCredentials(xlTestDescriptor.getCredentialsId()));

            // TODO: sort and filter, no supersets
            Map<String, TestSpecification> ts = xlTest.getTestSpecifications();
            for (Map.Entry<String, TestSpecification> t : ts.entrySet()) {
                items.add(
                        format("%s > %s", t.getValue().getProject().getTitle(), t.getValue().getTitle()),
                        t.getKey()
                );
            }
            return items;
        }
    }
}
