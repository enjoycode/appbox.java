package appbox.design.idea;

import com.intellij.codeInsight.NullableNotNullManagerImpl;
import com.intellij.openapi.project.Project;

import java.util.Collections;
import java.util.Set;

public final class IdeaNullableNotNullManager extends NullableNotNullManagerImpl {

    public IdeaNullableNotNullManager(Project project) {
        super(project);
    }

    @Override
    protected Set<String> getAllNullabilityAnnotationsWithNickNames() {
        return Collections.emptySet(); //Rick
    }
}
