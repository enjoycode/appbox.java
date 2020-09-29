package appbox.design.services.code;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

public final class Workspace {

    private final TypeSystem typeSystem;

    public Workspace(TypeSystem owner) {
        typeSystem = owner;
    }

    private Set<String> externalDependencies() {
        return Set.of();
        //if (!settings.has("externalDependencies")) return Set.of();
        //var array = settings.getAsJsonArray("externalDependencies");
        //var strings = new HashSet<String>();
        //for (var each : array) {
        //    strings.add(each.getAsString());
        //}
        //return strings;
    }

    private Set<Path> classPath() {
        return Set.of();
        //if (!settings.has("classPath")) return Set.of();
        //var array = settings.getAsJsonArray("classPath");
        //var paths = new HashSet<Path>();
        //for (var each : array) {
        //    paths.add(Paths.get(each.getAsString()).toAbsolutePath());
        //}
        //return paths;
    }

    private Set<String> addExports() {
        return Set.of();
        //if (!settings.has("addExports")) return Set.of();
        //var array = settings.getAsJsonArray("addExports");
        //var strings = new HashSet<String>();
        //for (var each : array) {
        //    strings.add(each.getAsString());
        //}
        //return strings;
    }

}
