import com.intellij.lang.Language;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.lang.java.JavaParserDefinition;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.FileElement;

import java.util.List;
import java.util.Set;

public class TestFileViewProvider extends AbstractFileViewProvider {

    private static final JavaParserDefinition parserDefinition = new JavaParserDefinition();

    public TestFileViewProvider(PsiManager psiManager, VirtualFile file) {
        super(psiManager, file, false);
    }

    @Override
    public boolean isPhysical() {
        return true;
    }

    //@Override
    //public Document getDocument() {
    //    return super.getDocument();
    //}

    //region ====AbstractFileViewProvider====
    @Override
    protected PsiFile getPsiInner(Language language) {
        return parserDefinition.createFile(this);
    }

    @Override
    public PsiFile getCachedPsi(Language language) {
        return null;
    }

    @Override
    public List<PsiFile> getCachedPsiFiles() {
        return null;
    }

    @Override
    public List<FileElement> getKnownTreeRoots() {
        return null;
    }

    @Override
    public Language getBaseLanguage() {
        return null;
    }

    @Override
    public Set<Language> getLanguages() {
        return null;
    }

    @Override
    public List<PsiFile> getAllFiles() {
        return null;
    }

    @Override
    public PsiElement findElementAt(int i) {
        return null;
    }

    @Override
    public PsiReference findReferenceAt(int i) {
        return null;
    }

    @Override
    public PsiElement findElementAt(int i, Class<? extends Language> aClass) {
        return null;
    }

    @Override
    public FileViewProvider createCopy(VirtualFile virtualFile) {
        return null;
    }
    //endregion

}
