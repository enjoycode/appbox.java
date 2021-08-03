package appbox.design.lang.java.lsp;

import appbox.logging.Log;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SwitchExpression;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;
import org.eclipse.jdt.internal.corext.dom.TokenScanner;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.handlers.JsonRpcHelpers;
import org.eclipse.jdt.ls.core.internal.managers.ProjectsManager;
import org.eclipse.jdt.ls.core.internal.preferences.PreferenceManager;
import org.eclipse.lsp4j.Location;

import java.util.Collections;
import java.util.List;

public final class NavigateToDefinitionHandler {

    public static List<? extends Location> definition(ITypeRoot unit, int line, int column,
                                                      PreferenceManager preferenceManager, IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            return Collections.emptyList();
        }
        Location location = null;
        if (unit != null && !monitor.isCanceled()) {
            location = computeDefinitionNavigation(unit, line, column, preferenceManager, monitor);
        }
        return location == null || monitor.isCanceled() ? Collections.emptyList() : List.of(location);
    }

    private static Location computeDefinitionNavigation(ITypeRoot unit, int line, int column,
                                                        PreferenceManager preferenceManager, IProgressMonitor monitor) {
        try {
            IJavaElement element = JDTUtils.findElementAtSelection(unit, line, column, preferenceManager, monitor);
            if (monitor.isCanceled()) {
                return null;
            }
            if (element == null) {
                return computeBreakContinue(unit, line, column);
            }
            return computeDefinitionNavigation(element, unit.getJavaProject());
        } catch (CoreException e) {
            Log.warn("Computing definition for " + unit.getElementName() + " error: " + e);
        }

        return null;
    }

    private static Location computeBreakContinue(ITypeRoot typeRoot, int line, int column) throws CoreException {
        int offset = JsonRpcHelpers.toOffset(typeRoot.getBuffer(), line, column);
        if (offset >= 0) {
            CompilationUnit unit = SharedASTProviderCore.getAST(typeRoot, SharedASTProviderCore.WAIT_YES, null);
            if (unit == null) {
                return null;
            }
            ASTNode    selectedNode = NodeFinder.perform(unit, offset, 0);
            ASTNode    node         = null;
            SimpleName label        = null;
            if (selectedNode instanceof BreakStatement) {
                node  = selectedNode;
                label = ((BreakStatement) node).getLabel();
            } else if (selectedNode instanceof ContinueStatement) {
                node  = selectedNode;
                label = ((ContinueStatement) node).getLabel();
            } else if (selectedNode instanceof SimpleName && selectedNode.getParent() instanceof BreakStatement) {
                node  = selectedNode.getParent();
                label = ((BreakStatement) node).getLabel();
            } else if (selectedNode instanceof SimpleName && selectedNode.getParent() instanceof ContinueStatement) {
                node  = selectedNode.getParent();
                label = ((ContinueStatement) node).getLabel();
            }
            if (node != null) {
                ASTNode parent = node.getParent();
                ASTNode target = null;
                while (parent != null) {
                    if (parent instanceof MethodDeclaration || parent instanceof Initializer) {
                        break;
                    }
                    if (label == null) {
                        if (parent instanceof ForStatement || parent instanceof EnhancedForStatement || parent instanceof WhileStatement || parent instanceof DoStatement) {
                            target = parent;
                            break;
                        }
                        if (node instanceof BreakStatement) {
                            if (parent instanceof SwitchStatement || parent instanceof SwitchExpression) {
                                target = parent;
                                break;
                            }
                        }
                        if (node instanceof LabeledStatement) {
                            target = parent;
                            break;
                        }
                    } else if (LabeledStatement.class.isInstance(parent)) {
                        LabeledStatement ls = (LabeledStatement) parent;
                        if (ls.getLabel().getIdentifier().equals(label.getIdentifier())) {
                            target = ls;
                            break;
                        }
                    }
                    parent = parent.getParent();
                }
                if (target != null) {
                    int start = target.getStartPosition();
                    int end   = new TokenScanner(unit.getTypeRoot()).getNextEndOffset(node.getStartPosition(), true) - start;
                    if (start >= 0 && end >= 0) {
                        return JDTUtils.toLocation((ICompilationUnit) typeRoot, start, end);
                    }
                }
            }
        }
        return null;
    }

    public static Location computeDefinitionNavigation(IJavaElement element, IJavaProject javaProject) throws JavaModelException {
        if (element == null) {
            return null;
        }

        ICompilationUnit compilationUnit = (ICompilationUnit) element.getAncestor(IJavaElement.COMPILATION_UNIT);
        IClassFile       cf              = (IClassFile) element.getAncestor(IJavaElement.CLASS_FILE);
        if (compilationUnit != null || (cf != null && cf.getSourceRange() != null)) {
            return fixLocation(element, Utils.toLocation(element), javaProject);
        }

        if (element instanceof IMember && ((IMember) element).getClassFile() != null) {
            return fixLocation(element, Utils.toLocation(((IMember) element).getClassFile()), javaProject);
        }

        return null;
    }

    private static Location fixLocation(IJavaElement element, Location location, IJavaProject javaProject) {
        if (location == null) {
            return null;
        }
        if (!javaProject.equals(element.getJavaProject()) && element.getJavaProject()
                .getProject().getName().equals(ProjectsManager.DEFAULT_PROJECT_NAME)) {
            // see issue at: https://github.com/eclipse/eclipse.jdt.ls/issues/842 and https://bugs.eclipse.org/bugs/show_bug.cgi?id=541573
            // for jdk classes, jdt will reuse the java model by altering project to share the model between projects
            // so that sometimes the project for `element` is default project and the project is different from the project for `unit`
            // this fix is to replace the project name with non-default ones since default project should be transparent to users.
            if (location.getUri().contains(ProjectsManager.DEFAULT_PROJECT_NAME)) {
                String patched = StringUtils.replaceOnce(location.getUri(),
                        ProjectsManager.DEFAULT_PROJECT_NAME, javaProject.getProject().getName());
                try {
                    IClassFile cf = (IClassFile) JavaCore.create(JDTUtils.toURI(patched).getQuery());
                    if (cf != null && cf.exists()) {
                        location.setUri(patched);
                    }
                } catch (Exception ex) {

                }
            }
        }
        return location;
    }


}
