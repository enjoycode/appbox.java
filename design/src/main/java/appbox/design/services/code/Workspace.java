package appbox.design.services.code;

import org.javacs.InferConfig;
import org.javacs.JavaCompilerService;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

public final class Workspace {
    private JavaCompilerService cacheCompiler;
    private boolean             modifiedBuild = true;

    private final TypeSystem typeSystem;

    public Workspace(TypeSystem owner) {
        typeSystem = owner;
    }

    public JavaCompilerService compiler() {
        if (needsCompiler()) {
            cacheCompiler = createCompiler();
            modifiedBuild = false;
        }
        return cacheCompiler;
    }

    private boolean needsCompiler() {
        if (modifiedBuild) {
            return true;
        }
        //if (!settings.equals(cacheSettings)) {
        //    LOG.info("Settings\n\t" + settings + "\nis different than\n\t" + cacheSettings);
        //    return true;
        //}
        return false;
    }

    private JavaCompilerService createCompiler() {
        //Objects.requireNonNull(workspaceRoot, "Can't create compiler because workspaceRoot has not been initialized");
        //
        //javaStartProgress(new JavaStartProgressParams("Configure javac"));
        //javaReportProgress(new JavaReportProgressParams("Finding source roots"));

        var externalDependencies = externalDependencies();
        var classPath            = classPath();
        var addExports           = addExports();
        // If classpath is specified by the user, don't infer anything
        if (!classPath.isEmpty()) {
            //javaEndProgress();
            return new JavaCompilerService(classPath, Collections.emptySet(), addExports);
        }
        // Otherwise, combine inference with user-specified external dependencies
        else {
            var infer = new InferConfig(Path.of(""), externalDependencies);

            //javaReportProgress(new JavaReportProgressParams("Inferring class path"));
            classPath = infer.classPath();
            //
            //javaReportProgress(new JavaReportProgressParams("Inferring doc path"));
            var docPath = infer.buildDocPath();
            //
            //javaEndProgress();
            return new JavaCompilerService(classPath, docPath, addExports);
        }
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
