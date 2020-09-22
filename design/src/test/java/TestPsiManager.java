import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.PsiTreeChangeEventImpl;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.psi.util.PsiModificationTracker;

public class TestPsiManager extends PsiManagerEx {
    private final TestProject _project;

    public TestPsiManager(TestProject project) {
        _project = project;
    }

    //region ====PsiManager====
    @Override
    public Project getProject() {
        return _project;
    }

    @Override
    public PsiFile findFile(VirtualFile virtualFile) {
        return null;
    }

    @Override
    public FileViewProvider findViewProvider(VirtualFile virtualFile) {
        //JavaLanguage.INSTANCE.
        return null;
    }

    @Override
    public PsiDirectory findDirectory(VirtualFile virtualFile) {
        return null;
    }

    @Override
    public boolean areElementsEquivalent(PsiElement psiElement, PsiElement psiElement1) {
        return false;
    }

    @Override
    public void reloadFromDisk(PsiFile psiFile) {

    }

    @Override
    public void addPsiTreeChangeListener(PsiTreeChangeListener psiTreeChangeListener) {

    }

    @Override
    public void addPsiTreeChangeListener(PsiTreeChangeListener psiTreeChangeListener, Disposable disposable) {

    }

    @Override
    public void removePsiTreeChangeListener(PsiTreeChangeListener psiTreeChangeListener) {

    }

    @Override
    public PsiModificationTracker getModificationTracker() {
        return null;
    }

    @Override
    public void startBatchFilesProcessingMode() {

    }

    @Override
    public void finishBatchFilesProcessingMode() {

    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Override
    public void dropResolveCaches() {

    }

    @Override
    public void dropPsiCaches() {

    }

    @Override
    public boolean isInProject(PsiElement psiElement) {
        return false;
    }

    @Override
    public boolean isBatchFilesProcessingMode() {
        return false;
    }

    @Override
    public void setAssertOnFileLoadingFilter(VirtualFileFilter virtualFileFilter, Disposable disposable) {

    }

    @Override
    public boolean isAssertOnFileLoading(VirtualFile virtualFile) {
        return false;
    }

    @Override
    public void registerRunnableToRunOnChange(Runnable runnable) {

    }

    @Override
    public void registerRunnableToRunOnAnyChange(Runnable runnable) {

    }

    @Override
    public void registerRunnableToRunAfterAnyChange(Runnable runnable) {

    }

    @Override
    public FileManager getFileManager() {
        return null;
    }

    @Override
    public void beforeChildAddition(PsiTreeChangeEventImpl psiTreeChangeEvent) {

    }

    @Override
    public void beforeChildRemoval(PsiTreeChangeEventImpl psiTreeChangeEvent) {

    }

    @Override
    public void beforeChildReplacement(PsiTreeChangeEventImpl psiTreeChangeEvent) {

    }

    @Override
    public void beforeChange(boolean b) {

    }

    @Override
    public void afterChange(boolean b) {

    }
    //endregion

}
