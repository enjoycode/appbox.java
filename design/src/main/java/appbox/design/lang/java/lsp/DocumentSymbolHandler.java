package appbox.design.lang.java.lsp;

import appbox.logging.Log;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.hover.JavaElementLabels;
import org.eclipse.lsp4j.*;
import org.eclipse.xtext.xbase.lib.Exceptions;

import static org.eclipse.jdt.core.IJavaElement.COMPILATION_UNIT;
import static org.eclipse.jdt.core.IJavaElement.FIELD;
import static org.eclipse.jdt.core.IJavaElement.METHOD;
import static org.eclipse.jdt.core.IJavaElement.PACKAGE_DECLARATION;
import static org.eclipse.jdt.core.IJavaElement.TYPE;
import static org.eclipse.jdt.ls.core.internal.hover.JavaElementLabels.ALL_DEFAULT;
import static org.eclipse.jdt.ls.core.internal.hover.JavaElementLabels.M_APP_RETURNTYPE;
import static org.eclipse.jdt.ls.core.internal.hover.JavaElementLabels.ROOT_VARIABLE;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DocumentSymbolHandler {
    private static Range DEFAULT_RANGE = new Range(new Position(0, 0), new Position(0, 0));

    public static List<DocumentSymbol> getHierarchicalOutline(ITypeRoot unit, IProgressMonitor monitor) {
        try {
            return Stream.of(filter(unit.getChildren()))
                    .map(child -> toDocumentSymbol(child, monitor))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (OperationCanceledException e) {
            Log.warn("User abort while collecting the document symbols.");
        } catch (JavaModelException e) {
            Log.warn("Get outline for " + unit.getElementName() + " error: " + e);
        }
        return emptyList();
    }

    private static DocumentSymbol toDocumentSymbol(IJavaElement unit, IProgressMonitor monitor) {
        int type = unit.getElementType();
        if (type != TYPE && type != FIELD && type != METHOD && type != PACKAGE_DECLARATION && type != COMPILATION_UNIT) {
            return null;
        }
        //if (monitor.isCanceled()) {
        //    throw new OperationCanceledException("User abort");
        //}
        DocumentSymbol symbol = new DocumentSymbol();
        try {
            String name = getName(unit);
            symbol.setName(name);
            symbol.setRange(getRange(unit));
            symbol.setSelectionRange(getSelectionRange(unit));
            symbol.setKind(mapKind(unit));
            symbol.setDeprecated(isDeprecated(unit));
            symbol.setDetail(getDetail(unit, name));
            if (unit instanceof IParent) {
                //@formatter:off
                IJavaElement[] children = filter(((IParent) unit).getChildren());
                symbol.setChildren(Stream.of(children)
                        .map(child -> toDocumentSymbol(child, monitor))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
                //@formatter:off
            }
        } catch (JavaModelException e) {
            Exceptions.sneakyThrow(e);
        }
        return symbol;
    }

    private static String getName(IJavaElement element) {
        String name = JavaElementLabels.getElementLabel(element, ALL_DEFAULT);
        return name == null ? element.getElementName() : name;
    }

    private static Range getRange(IJavaElement element) throws JavaModelException {
        Location location = Utils.toLocation(element, true); //JDTUtils.toLocation(element, FULL_RANGE);
        return location == null ? DEFAULT_RANGE : location.getRange();
    }

    private static Range getSelectionRange(IJavaElement element) throws JavaModelException {
        Location location = Utils.toLocation(element, false); //JDTUtils.toLocation(element);
        return location == null ? DEFAULT_RANGE : location.getRange();
    }

    private static boolean isDeprecated(IJavaElement element) throws JavaModelException {
        if (element instanceof ITypeRoot) {
            return Flags.isDeprecated(((ITypeRoot) element).findPrimaryType().getFlags());
        }
        return false;
    }

    private static String getDetail(IJavaElement element, String name) {
        String nameWithDetails = JavaElementLabels.getElementLabel(element, ALL_DEFAULT | M_APP_RETURNTYPE | ROOT_VARIABLE);
        if (nameWithDetails != null && nameWithDetails.startsWith(name)) {
            return nameWithDetails.substring(name.length());
        }
        return "";
    }

    private static IJavaElement[] filter(IJavaElement[] elements) {
        return Stream.of(elements)
                .filter(e -> (!isInitializer(e) && !isSyntheticElement(e)))
                .toArray(IJavaElement[]::new);
    }

    private static boolean isInitializer(IJavaElement element) {
        if (element.getElementType() == IJavaElement.METHOD) {
            String name = element.getElementName();
            if ((name != null && name.indexOf('<') >= 0)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSyntheticElement(IJavaElement element) {
        if (!(element instanceof IMember)) {
            return false;
        }
        IMember member = (IMember) element;
        if (!(member.isBinary())) {
            return false;
        }
        try {
            return Flags.isSynthetic(member.getFlags());
        } catch (JavaModelException e) {
            return false;
        }
    }

    public static SymbolKind mapKind(IJavaElement element) {
        switch (element.getElementType()) {
            case IJavaElement.TYPE:
                try {
                    IType type = (IType) element;
                    if (type.isInterface()) {
                        return SymbolKind.Interface;
                    } else if (type.isEnum()) {
                        return SymbolKind.Enum;
                    }
                } catch (JavaModelException ignore) {
                }
                return SymbolKind.Class;
            case IJavaElement.ANNOTATION:
                return SymbolKind.Property; // TODO: find a better mapping
            case IJavaElement.CLASS_FILE:
            case IJavaElement.COMPILATION_UNIT:
                return SymbolKind.File;
            case IJavaElement.FIELD:
                IField field = (IField) element;
                try {
                    if (field.isEnumConstant()) {
                        return SymbolKind.EnumMember;
                    }
                    int flags = field.getFlags();
                    if (Flags.isStatic(flags) && Flags.isFinal(flags)) {
                        return SymbolKind.Constant;
                    }
                } catch (JavaModelException ignore) {
                }
                return SymbolKind.Field;
            case IJavaElement.IMPORT_CONTAINER:
            case IJavaElement.IMPORT_DECLARATION:
                //should we return SymbolKind.Namespace?
            case IJavaElement.JAVA_MODULE:
                return SymbolKind.Module;
            case IJavaElement.INITIALIZER:
                return SymbolKind.Constructor;
            case IJavaElement.LOCAL_VARIABLE:
                return SymbolKind.Variable;
            case IJavaElement.TYPE_PARAMETER:
                return SymbolKind.TypeParameter;
            case IJavaElement.METHOD:
                try {
                    // TODO handle `IInitializer`. What should be the `SymbolKind`?
                    if (element instanceof IMethod) {
                        if (((IMethod) element).isConstructor()) {
                            return SymbolKind.Constructor;
                        }
                    }
                    return SymbolKind.Method;
                } catch (JavaModelException e) {
                    return SymbolKind.Method;
                }
            case IJavaElement.PACKAGE_DECLARATION:
                return SymbolKind.Package;
        }
        return SymbolKind.String;
    }

}
