package de.tum.in.www1.artemis.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import de.tum.in.www1.artemis.AbstractSpringIntegrationBambooBitbucketJiraTest;
import de.tum.in.www1.artemis.service.feature.Feature;
import de.tum.in.www1.artemis.service.feature.FeatureToggleService;

public class FeatureToggleServiceTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    @Autowired
    FeatureToggleService featureToggleService;

    @AfterEach
    public void checkReset() {
        // Verify that the test has resetted the state
        // Must be extended if additional features are added
        assertTrue(featureToggleService.isFeatureEnabled(Feature.PROGRAMMING_EXERCISES));
    }

    @Test
    public void testSetFeaturesEnabled() {
        Map<Feature, Boolean> featureStates = new HashMap<>();
        featureStates.put(Feature.PROGRAMMING_EXERCISES, true);
        featureToggleService.updateFeatureToggles(featureStates);
        assertTrue(featureToggleService.isFeatureEnabled(Feature.PROGRAMMING_EXERCISES));
    }

    @Test
    public void testSetFeaturesDisabled() {
        assertTrue(featureToggleService.isFeatureEnabled(Feature.PROGRAMMING_EXERCISES));

        Map<Feature, Boolean> featureStates = new HashMap<>();
        featureStates.put(Feature.PROGRAMMING_EXERCISES, false);
        featureToggleService.updateFeatureToggles(featureStates);
        assertFalse(featureToggleService.isFeatureEnabled(Feature.PROGRAMMING_EXERCISES));

        // Reset
        featureToggleService.enableFeature(Feature.PROGRAMMING_EXERCISES);
    }

    @Test
    public void testEnableDisableFeature() {
        assertTrue(featureToggleService.isFeatureEnabled(Feature.PROGRAMMING_EXERCISES));

        featureToggleService.disableFeature(Feature.PROGRAMMING_EXERCISES);
        assertFalse(featureToggleService.isFeatureEnabled(Feature.PROGRAMMING_EXERCISES));

        featureToggleService.enableFeature(Feature.PROGRAMMING_EXERCISES);
        assertTrue(featureToggleService.isFeatureEnabled(Feature.PROGRAMMING_EXERCISES));
    }

    @Test
    public void testShouldNotEnableTwice() {
        // Must be updated if additional features are added
        assertEquals(1, featureToggleService.enabledFeatures().size());
        featureToggleService.enableFeature(Feature.PROGRAMMING_EXERCISES);

        // Feature should not be added multiple times
        assertEquals(1, featureToggleService.enabledFeatures().size());
    }

    @Test
    public void testShouldNotDisableTwice() {
        featureToggleService.disableFeature(Feature.PROGRAMMING_EXERCISES);

        // Must be updated if additional features are added
        assertEquals(1, featureToggleService.disabledFeatures().size());
        featureToggleService.disableFeature(Feature.PROGRAMMING_EXERCISES);

        // Feature should not be added multiple times
        assertEquals(1, featureToggleService.disabledFeatures().size());

        // Reset
        featureToggleService.enableFeature(Feature.PROGRAMMING_EXERCISES);
    }
}
