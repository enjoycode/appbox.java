package appbox.design.idea;

import com.intellij.lang.Language;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.ClassFileViewProvider;
import com.intellij.psi.ClassFileViewProviderFactory;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiManager;

public final class IdeaClassFileViewProviderFactory extends ClassFileViewProviderFactory {

    @Override
    public FileViewProvider createFileViewProvider(VirtualFile file, Language language,
                                                   PsiManager manager, boolean eventSystemEnabled) {
        //return super.createFileViewProvider(file, language, manager, eventSystemEnabled);
        return new ClassFileViewProvider(manager, file, false /*eventSystemEnabled*/);
    }

}
