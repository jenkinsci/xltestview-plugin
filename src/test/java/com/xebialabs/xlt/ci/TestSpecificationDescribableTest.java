package com.xebialabs.xlt.ci;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class TestSpecificationDescribableTest {

    @Test
    public void shouldMarkBuildAsUnstableByDefault() {
        TestSpecificationDescribable desc = new TestSpecificationDescribable("id", "*", "", null);

        assertThat(desc.getMakeUnstable(), is(true));
    }
}