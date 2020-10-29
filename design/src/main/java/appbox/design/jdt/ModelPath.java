package appbox.design.jdt;

import org.eclipse.core.runtime.Path;

public class ModelPath extends Path { //TODO:考虑移除直接使用Path

    public ModelPath(String fullPath) {
        super(fullPath);
    }

    public ModelPath(String device, String path) {
        super(device, path);
    }

}
