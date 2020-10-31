package appbox.design.jdt;

import appbox.design.utils.ReflectUtil;
import org.eclipse.jdt.internal.launching.LibraryInfo;
import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.launching.AbstractVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;

import java.io.File;
import java.util.HashSet;

public final class DefaultVMType extends StandardVMType {

    private final LibraryInfo defaultLibInfo =
            new LibraryInfo(System.getProperty("java.version"), new String[0], new String[0], new String[0]);

    @Override
    protected synchronized LibraryInfo getLibraryInfo(File javaHome, File javaExecutable) {
        return defaultLibInfo; //TODO:
    }

    public static void init() throws Exception {
        var vmType = new DefaultVMType();
        ReflectUtil.setField(AbstractVMInstallType.class,
                "fId", vmType, "org.eclipse.jdt.launching.StandardVMType");
        var javaHome = System.getProperty("java.home");
        var vm       = vmType.createVMInstall("1603760885369");
        vm.setName("OpenJDK11");
        vm.setInstallLocation(new File(javaHome));

        var fgVMTypes = new HashSet<Object>();
        fgVMTypes.add(vmType);
        ReflectUtil.setField(JavaRuntime.class, "fgVMTypes", null, fgVMTypes);

        JavaRuntime.setDefaultVMInstall(vm, null, false);
    }
}
