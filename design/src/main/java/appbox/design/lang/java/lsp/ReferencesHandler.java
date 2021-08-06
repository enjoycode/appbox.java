package appbox.design.lang.java.lsp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.handlers.JsonRpcHelpers;
import org.eclipse.jdt.ls.core.internal.preferences.PreferenceManager;
import org.eclipse.lsp4j.Location;

public final class ReferencesHandler {

    public static List<Location> findReferences(ITypeRoot typeRoot, int line, int column,
                                                PreferenceManager preferenceManager, IProgressMonitor monitor) {
        final List<Location> locations = new ArrayList<>();
        if (typeRoot == null) {
            return locations;
        }
        try {
            IJavaElement elementToSearch = JDTUtils.findElementAtSelection(typeRoot, line, column, preferenceManager, monitor);
            if (elementToSearch == null) {
                int offset = JsonRpcHelpers.toOffset(typeRoot.getBuffer(), line, column);
                elementToSearch = typeRoot.getElementAt(offset);
            }
            if (elementToSearch == null) {
                return locations;
            }

            search(elementToSearch, locations, monitor);

            if (preferenceManager.getPreferences().isIncludeAccessors() && elementToSearch instanceof IField) { // IField
                IField  field  = (IField) elementToSearch;
                IMethod getter = GetterSetterUtil.getGetter(field);
                if (getter != null) {
                    search(getter, locations, monitor);
                }
                IMethod setter = GetterSetterUtil.getSetter(field);
                if (setter != null) {
                    search(setter, locations, monitor);
                }
                String builderName = getBuilderName(field);
                IType  builder     = field.getJavaProject().findType(builderName);
                if (builder != null) {
                    String fieldSignature = field.getTypeSignature();
                    for (IMethod method : builder.getMethods()) {
                        String[] parameters = method.getParameterTypes();
                        if (parameters.length == 1 && field.getElementName().equals(method.getElementName()) && fieldSignature.equals(parameters[0])) {
                            search(method, locations, monitor);
                        }
                    }
                }
            }
        } catch (CoreException e) {
            JavaLanguageServerPlugin.logException("Find references failure ", e);
        }
        return locations;
    }

    private static IJavaSearchScope createSearchScope() throws JavaModelException {
        IJavaProject[] projects = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
        int            scope    = IJavaSearchScope.SOURCES;
        //if (preferenceManager.isClientSupportsClassFileContent()) {
        //    scope |= IJavaSearchScope.APPLICATION_LIBRARIES;
        //}
        return SearchEngine.createJavaSearchScope(projects, scope);
    }

    private static String getBuilderName(IField field) {
        IType       declaringType = field.getDeclaringType();
        IAnnotation annotation    = declaringType.getAnnotation("Builder");
        if (annotation == null || !annotation.exists()) {
            annotation = declaringType.getAnnotation("lombok.Builder");
        }
        if (annotation != null && annotation.exists()) {
            try {
                for (IMemberValuePair pair : annotation.getMemberValuePairs()) {
                    if (pair.getValueKind() == IMemberValuePair.K_STRING) {
                        String memberName = pair.getMemberName();
                        Object value      = pair.getValue();
                        if ("builderClassName".equals(memberName) && value instanceof String && !((String) value).isEmpty()) {
                            return declaringType.getFullyQualifiedName() + "." + (String) value;
                        }
                    }
                }
            } catch (JavaModelException e) {
                JavaLanguageServerPlugin.logException(e.getMessage(), e);
            }
        }
        return declaringType.getFullyQualifiedName() + "." + declaringType.getElementName() + "Builder";
    }

    public static void search(IJavaElement elementToSearch,
                              final List<Location> locations,
                              IProgressMonitor monitor) throws CoreException {
        boolean includeClassFiles = false; //preferenceManager.isClientSupportsClassFileContent();

        SearchEngine  engine  = new SearchEngine();
        SearchPattern pattern = SearchPattern.createPattern(elementToSearch, IJavaSearchConstants.REFERENCES);

        engine.search(pattern,
                new SearchParticipant[]{SearchEngine.getDefaultSearchParticipant()},
                createSearchScope(),
                new SearchRequestor() {
                    @Override
                    public void acceptSearchMatch(SearchMatch match) throws CoreException {
                        Object o = match.getElement();
                        if (o instanceof IJavaElement) {
                            IJavaElement     element         = (IJavaElement) o;
                            ICompilationUnit compilationUnit = (ICompilationUnit) element.getAncestor(IJavaElement.COMPILATION_UNIT);
                            Location         location        = null;
                            if (compilationUnit != null) {
                                location = Utils.toLocation(compilationUnit, match.getOffset(), match.getLength());
                            } else if (includeClassFiles) {
                                IClassFile cf = (IClassFile) element.getAncestor(IJavaElement.CLASS_FILE);
                                if (cf != null && cf.getSourceRange() != null) {
                                    location = JDTUtils.toLocation(cf, match.getOffset(), match.getLength());
                                }
                            }
                            if (location != null) {
                                locations.add(location);
                            }
                        }
                    }
                },
                monitor);
    }

}
