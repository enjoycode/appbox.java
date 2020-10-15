package appbox.design.idea;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.openapi.project.Project;

public final class IdeaFeatureUsageTracker extends FeatureUsageTracker {
    @Override
    public void triggerFeatureUsed(String featureId) {
    }

    @Override
    public void triggerFeatureShown(String featureId) {
    }

    @Override
    public boolean isToBeShown(String featureId, Project project) {
        return false;
    }

    @Override
    public boolean isToBeAdvertisedInLookup(String featureId, Project project) {
        return false;
    }
}
