import com.sun.tools.javac.api.*;
import com.sun.tools.javac.file.*;

import org.javacs.SourceFileObject;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class TestJavac {

    private final String code1 ="public class Person {\n public String getName() { \n return \"rick\";\n } \n}";

    @Test
    public void testJavacTask() {
        JavacTool systemProvider = JavacTool.create();
        JavacFileManager fileManager = systemProvider.getStandardFileManager(err -> {
            System.out.println(err.toString());
        }, Locale.ENGLISH, Charset.defaultCharset());

        var file1 = new SourceFileObject(Path.of("Person.java"), code1, Instant.now());

        Iterable<String>                   opts             = Collections.emptyList();
        Iterable<String>                   classes          = Collections.emptyList();
        Iterable<? extends JavaFileObject> compilationUnits = new ArrayList<>(Arrays.asList(file1));

        JavacTaskImpl task = (JavacTaskImpl) systemProvider.getTask(
                null, fileManager,
                err -> {
                    System.out.println(err.toString()); //task.analyze()错误
                }, opts, classes, compilationUnits/*, currentContext*/);

        //task.addTaskListener(currentContext);
        assertNotNull(task);

        var trees1 = task.parse();
        //测试重复parse,结果第二次返回空
        //var trees2 = task.parse();
        var elements = task.analyze();

        assertNotNull(trees1);
    }

}
