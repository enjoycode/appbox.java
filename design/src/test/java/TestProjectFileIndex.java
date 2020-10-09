import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import java.util.List;
import java.util.Set;

public class TestProjectFileIndex implements ProjectFileIndex {
    @Override
    public Module getModuleForFile(VirtualFile virtualFile) {
        return null;
    }

    @Override
    public Module getModuleForFile(VirtualFile virtualFile, boolean b) {
        return null;
    }

    @Override
    public List<OrderEntry> getOrderEntriesForFile(VirtualFile virtualFile) {
        return null;
    }

    @Override
    public VirtualFile getClassRootForFile(VirtualFile virtualFile) {
        return null;
    }

    @Override
    public VirtualFile getSourceRootForFile(VirtualFile virtualFile) {
        return null;
    }

    @Override
    public VirtualFile getContentRootForFile(VirtualFile virtualFile) {
        return null;
    }

    @Override
    public VirtualFile getContentRootForFile(VirtualFile virtualFile, boolean b) {
        return null;
    }

    @Override
    public String getPackageNameByDirectory(VirtualFile virtualFile) {
        return null;
    }

    @Override
    public boolean isLibraryClassFile(VirtualFile virtualFile) {
        return false;
    }

    @Override
    public boolean isInSource(VirtualFile virtualFile) {
        return false;
    }

    @Override
    public boolean isInLibraryClasses(VirtualFile virtualFile) {
        return false;
    }

    @Override
    public boolean isInLibrary(VirtualFile virtualFile) {
        return false;
    }

    @Override
    public boolean isInLibrarySource(VirtualFile virtualFile) {
        return false;
    }

    @Override
    public boolean isIgnored(VirtualFile virtualFile) {
        return false;
    }

    @Override
    public boolean isExcluded(VirtualFile virtualFile) {
        return false;
    }

    @Override
    public boolean isUnderIgnored(VirtualFile virtualFile) {
        return false;
    }

    @Override
    public boolean iterateContent(ContentIterator contentIterator) {
        return false;
    }

    @Override
    public boolean iterateContent(ContentIterator contentIterator, VirtualFileFilter virtualFileFilter) {
        return false;
    }

    @Override
    public boolean iterateContentUnderDirectory(VirtualFile virtualFile, ContentIterator contentIterator) {
        return false;
    }

    @Override
    public boolean iterateContentUnderDirectory(VirtualFile virtualFile, ContentIterator contentIterator, VirtualFileFilter virtualFileFilter) {
        return false;
    }

    @Override
    public boolean isInContent(VirtualFile virtualFile) {
        return false;
    }

    @Override
    public boolean isContentSourceFile(VirtualFile virtualFile) {
        return false;
    }

    @Override
    public boolean isInSourceContent(VirtualFile virtualFile) {
        return false;
    }

    @Override
    public boolean isInTestSourceContent(VirtualFile virtualFile) {
        return false;
    }

    @Override
    public boolean isUnderSourceRootOfType(VirtualFile virtualFile, Set<? extends JpsModuleSourceRootType<?>> set) {
        return false;
    }
}
