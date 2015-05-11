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
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;

// TODO: default include pattern selecteren
public class TestSpecificationDescribable extends AbstractDescribableImpl<TestSpecificationDescribable> {

    private final String testSpecificationId;
    private final String includes;
    private final String excludes;
    private final String uuid;

    @DataBoundConstructor
    public TestSpecificationDescribable(String testSpecificationId, String includes, String excludes) {
        System.out.printf("constructor %s %s %s\n", testSpecificationId, includes, excludes);
        this.includes = includes;
        this.excludes = excludes;
        this.testSpecificationId = testSpecificationId;
        this.uuid = UUID.randomUUID().toString();
    }

    // this getter must correspond with the name of the field in the config.jelly else the selected value is not filled in
    public String getTestSpecificationId() {
        return testSpecificationId;
    }

    public String getExcludes() {
        return excludes;
    }

    public String getIncludes() {
        if (StringUtils.isBlank(includes)) {
            return "the default value";
        }
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

        @JavaScriptMethod
        public String fooText() {
            return "foo text baby! yeah! - " + UUID.randomUUID().toString();
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

            Map<String, TestSpecification> ts = xlTest.getTestSpecifications();
            for (Map.Entry<String, TestSpecification> t : ts.entrySet()) {
                TestSpecification testSpecification = t.getValue();
                if (!isSetOfTestSpecifications(testSpecification)) {
                    items.add(
                            format("%s > %s (%s, %s) - %s",
                                testSpecification.getProject().getTitle(),
                                testSpecification.getTitle(),
                                testSpecification.getTestTool().getName(),
                                testSpecification.getTestTool().getDefaultSearchPattern(),
                                testSpecification.getQualification().getDescription()
                            ),
                            t.getKey()
                    );
                }
            }
            JellyUtil.sortListBoxModel(items);
            return items;
        }

        private boolean isSetOfTestSpecifications(TestSpecification testSpecification) {
            // TODO: this should be a check on the type systems
            return "xltest.TestSpecificationSet".equals(testSpecification.getType());
        }
    }
}
