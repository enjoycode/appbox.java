import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.SuppressIntentionAction;
import com.intellij.codeInspection.SuppressManager;
import com.intellij.codeInspection.SuppressQuickFix;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiElement;

public class TestSuppressManager extends SuppressManager {
    @Override
    public SuppressIntentionAction[] createSuppressActions(HighlightDisplayKey highlightDisplayKey) {
        return new SuppressIntentionAction[0];
    }

    @Override
    public boolean isSuppressedFor(PsiElement psiElement, String s) {
        return false;
    }

    @Override
    public SuppressQuickFix[] getSuppressActions(PsiElement psiElement, String s) {
        return new SuppressQuickFix[0];
    }

    @Override
    public String getSuppressedInspectionIdsIn(PsiElement psiElement) {
        return null;
    }

    @Override
    public PsiElement getElementToolSuppressedIn(PsiElement psiElement, String s) {
        return null;
    }

    @Override
    public boolean canHave15Suppressions(PsiElement psiElement) {
        return false;
    }

    @Override
    public boolean alreadyHas14Suppressions(PsiDocCommentOwner psiDocCommentOwner) {
        return false;
    }
}
