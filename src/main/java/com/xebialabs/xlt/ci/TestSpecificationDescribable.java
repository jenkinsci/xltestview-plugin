package com.xebialabs.xlt.ci;

import java.util.Map;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Objects;
import com.google.inject.Inject;

import com.xebialabs.xlt.ci.server.XLTestServer;
import com.xebialabs.xlt.ci.server.XLTestServerFactory;
import com.xebialabs.xlt.ci.server.domain.TestSpecification;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;

import static java.lang.String.format;

// TODO: select default include pattern
public class TestSpecificationDescribable extends AbstractDescribableImpl<TestSpecificationDescribable> {
    private static final Logger LOG = LoggerFactory.getLogger(TestSpecificationDescribable.class);

    private final String testSpecificationName;
    private final String includes;
    private final String excludes;
    private final Boolean makeUnstable;

    // Attention: This constructor is *NOT* used when loading the config.xml, so previously stored TestSpecificationDescribable's have
    // their values injected via some other way. :'(
    @DataBoundConstructor
    public TestSpecificationDescribable(String testSpecificationName, String includes, String excludes, Boolean makeUnstable) {
        LOG.debug("TestSpecificationDescribable testSpecName={} includes={} excludes={} makeUnstable={}", testSpecificationName, includes, excludes, makeUnstable);
        this.includes = includes;
        this.excludes = excludes;
        this.testSpecificationName = testSpecificationName;
        this.makeUnstable = makeUnstable;
    }

    // this getter must correspond with the name of the field in the config.jelly else the selected value is not filled in
    public String getTestSpecificationName() {
        return testSpecificationName;
    }

    public String getExcludes() {
        return excludes;
    }

    public String getIncludes() {
        return includes;
    }

    // Previous jenkins plugin would affect build stability by default. So if value is not known, keep doing that.
    public boolean getMakeUnstable() {
        return makeUnstable == null || makeUnstable;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("testSpecificationName", testSpecificationName)
                .add("includes", includes)
                .add("excludes", excludes)
                .add("makeUnstable", makeUnstable)
                .toString();
    }

    @Extension
    public static class TestSpecificationDescriptor extends Descriptor<TestSpecificationDescribable> {
        /**
         * Stunningly simple solution, if you dig deep enough.
         * This gets automagically set with the descriptor of the XLTestView relevant to this config bit.
         */
        @Override
        public String getDisplayName() {
            return "TestSpecification";
        }

        public static ListBoxModel getSpecificationOptions(Map<String, TestSpecification> ts) {
            ListBoxModel items = new ListBoxModel();
            for (Map.Entry<String, TestSpecification> t : ts.entrySet()) {
                TestSpecification testSpecification = t.getValue();
                if (!isSetOfTestSpecifications(testSpecification)) {
                    items.add(
                            format("%s > %s (%s, %s) - %s",
                                    testSpecification.getProject().getTitle(),
                                    testSpecification.getTitle(),
                                    testSpecification.getTestToolName(),
                                    testSpecification.getTestToolDefaultSearchPattern(),
                                    testSpecification.getQualificationDescription()
                            ),
                            t.getKey()
                    );
                }
            }
            JellyUtil.sortListBoxModel(items);
            return items;
        }

        static private boolean isSetOfTestSpecifications(TestSpecification testSpecification) {
            // TODO: this should be a check on the type system
            return "xlt.TestSpecificationSet".equals(testSpecification.getType());
        }
    }
}
