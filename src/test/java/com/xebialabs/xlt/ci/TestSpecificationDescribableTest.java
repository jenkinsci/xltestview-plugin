package com.xebialabs.xlt.ci;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class TestSpecificationDescribableTest {

    @Test
    public void shouldMarkBuildAsUnstableByDefault() {
        TestSpecificationDescribable desc = new TestSpecificationDescribable("id", "*", "", null);

        assertTrue(desc.getMakeUnstable());
    }
}