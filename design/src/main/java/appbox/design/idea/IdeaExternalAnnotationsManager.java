package appbox.design.idea;

import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;

import java.util.List;

public final class IdeaExternalAnnotationsManager extends ExternalAnnotationsManager {
    @Override
    public boolean hasAnnotationRootsForFile(VirtualFile virtualFile) {
        return false;
    }

    @Override
    public boolean isExternalAnnotation(PsiAnnotation psiAnnotation) {
        return false;
    }

    @Override
    public PsiAnnotation findExternalAnnotation(PsiModifierListOwner psiModifierListOwner, String s) {
        return null;
    }

    @Override
    public List<PsiAnnotation> findExternalAnnotations(PsiModifierListOwner psiModifierListOwner, String s) {
        return null;
    }

    @Override
    public boolean isExternalAnnotationWritable(PsiModifierListOwner psiModifierListOwner, String s) {
        return false;
    }

    @Override
    public PsiAnnotation[] findExternalAnnotations(PsiModifierListOwner psiModifierListOwner) {
        return new PsiAnnotation[0];
    }

    @Override
    public List<PsiAnnotation> findDefaultConstructorExternalAnnotations(PsiClass psiClass) {
        return null;
    }

    @Override
    public List<PsiAnnotation> findDefaultConstructorExternalAnnotations(PsiClass psiClass, String s) {
        return null;
    }

    @Override
    public void annotateExternally(PsiModifierListOwner psiModifierListOwner, String s, PsiFile psiFile, PsiNameValuePair[] psiNameValuePairs) throws CanceledConfigurationException {

    }

    @Override
    public boolean deannotate(PsiModifierListOwner psiModifierListOwner, String s) {
        return false;
    }

    @Override
    public boolean editExternalAnnotation(PsiModifierListOwner psiModifierListOwner, String s, PsiNameValuePair[] psiNameValuePairs) {
        return false;
    }

    @Override
    public AnnotationPlace chooseAnnotationsPlaceNoUi(PsiElement psiElement) {
        return null;
    }

    @Override
    public AnnotationPlace chooseAnnotationsPlace(PsiElement psiElement) {
        return null;
    }

    @Override
    public List<PsiFile> findExternalAnnotationsFiles(PsiModifierListOwner psiModifierListOwner) {
        return null;
    }
}
