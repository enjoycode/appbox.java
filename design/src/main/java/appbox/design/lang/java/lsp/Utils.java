package appbox.design.lang.java.lsp;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.lsp4j.Location;

public final class Utils {

    public static Location toLocation(IJavaElement element) throws JavaModelException {
        return toLocation(element, false);
    }

    public static Location toLocation(IJavaElement element, boolean fullRange) throws JavaModelException {
        ICompilationUnit unit = (ICompilationUnit) element.getAncestor(IJavaElement.COMPILATION_UNIT);
        IClassFile       cf   = (IClassFile) element.getAncestor(IJavaElement.CLASS_FILE);
        if (unit != null || cf != null) {
            if (element instanceof ISourceReference) {
                ISourceRange nameRange = fullRange ? getSourceRange(element) : JDTUtils.getNameRange(element);
                if (SourceRange.isAvailable(nameRange)) {
                    if (cf == null) {
                        return toLocation(unit, nameRange.getOffset(), nameRange.getLength());
                    }

                    return JDTUtils.toLocation(cf, nameRange.getOffset(), nameRange.getLength());
                }
            }
            //TODO: element is other, eg: org.eclipse.jdt.internal.core.ClassFile
        }
        return null;
    }

    private static Location toLocation(ICompilationUnit unit, int offset, int length) throws JavaModelException {
        //TODO: fix Location.uri
        final var uri = String.format("file:/%s", unit.getResource().getFullPath());
        return new Location(uri, JDTUtils.toRange(unit, offset, length));
    }

    private static ISourceRange getSourceRange(IJavaElement element) throws JavaModelException {
        ISourceRange sourceRange = null;
        if (element instanceof IMember) {
            IMember member = (IMember) element;
            sourceRange = member.getSourceRange();
        } else if (!(element instanceof ITypeParameter) && !(element instanceof ILocalVariable)) {
            if (element instanceof ISourceReference) {
                sourceRange = ((ISourceReference) element).getSourceRange();
            }
        } else {
            sourceRange = ((ISourceReference) element).getSourceRange();
        }

        if (!SourceRange.isAvailable(sourceRange) && element.getParent() != null) {
            sourceRange = getSourceRange(element.getParent());
        }

        return sourceRange;
    }

}
