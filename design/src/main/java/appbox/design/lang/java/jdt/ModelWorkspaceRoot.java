package appbox.design.lang.java.jdt;

import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.internal.resources.WorkspaceRoot;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ModelWorkspaceRoot extends WorkspaceRoot {

    private final Map<String, IProject> projectTable = Collections.synchronizedMap(new HashMap<>(8));

    ModelWorkspaceRoot(IPath path, Workspace container) {
        super(path, container);
    }

    @Override
    public IProject getProject(String name) {
        var result = projectTable.get(name);
        if (result == null) {
            IPath projectPath = (new Path(null, name)).makeAbsolute();
            result = new ModelProject(projectPath, (Workspace) this.getWorkspace());
            projectTable.put(name, result);
        }
        return result;
    }

}
