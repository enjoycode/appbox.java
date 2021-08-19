package appbox.design.lang.java.lsp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import appbox.design.IDeveloperSession;
import appbox.design.lang.java.jdt.ModelProject;
import appbox.logging.Log;
import appbox.runtime.RuntimeContext;
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
import org.eclipse.jdt.internal.core.search.matching.TypeReferencePattern;
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
        //TODO:暂在这里直接忽略无关项目,即只搜索设计时服务项目
        final var hub         = ((IDeveloperSession) RuntimeContext.current().currentSession()).getDesignHub();
        final var allProjects = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
        final var projects = Arrays.stream(allProjects).filter(p -> {
            var modelProject = (ModelProject) p.getProject();
            return modelProject.getDesignHub() == hub &&
                    modelProject.getProjectType() == ModelProject.ModelProjectType.DesigntimeService;
        }).toArray(IJavaProject[]::new);

        //IJavaProject[] projects = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
        int scope = IJavaSearchScope.SOURCES;
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
        //boolean includeClassFiles = false; //preferenceManager.isClientSupportsClassFileContent();

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
                                //TODO:考虑在这里获取范围文本并设置给Location,另根据match类型分析语义(如读或写值等)
                                //TODO:以下临时解决全名称的问题,eg:sys.entities.Person
                                int offset = match.getOffset();
                                int length = match.getLength();
                                if (pattern instanceof TypeReferencePattern) {
                                    var tp = (TypeReferencePattern)pattern;
                                    var simpleNameLength = tp.getIndexKey().length;
                                    if (length != simpleNameLength) {
                                        offset += length - simpleNameLength;
                                        length = simpleNameLength;
                                    }
                                }
                                location = Utils.toLocation(compilationUnit, offset, length);
                            }
                            //else if (includeClassFiles) {
                            //    IClassFile cf = (IClassFile) element.getAncestor(IJavaElement.CLASS_FILE);
                            //    if (cf != null && cf.getSourceRange() != null) {
                            //        location = JDTUtils.toLocation(cf, match.getOffset(), match.getLength());
                            //    }
                            //}
                            if (location != null) {
                                locations.add(location);
                            }
                        } else {
                            Log.debug("None IJavaElement: " + o.getClass());
                        }
                    }
                },
                monitor);
    }

}
