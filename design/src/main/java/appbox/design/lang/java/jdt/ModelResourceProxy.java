package appbox.design.lang.java.jdt;

import org.eclipse.core.internal.resources.ICoreConstants;
import org.eclipse.core.internal.resources.ResourceInfo;
import org.eclipse.core.internal.watson.IPathRequestor;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;

public class ModelResourceProxy implements IResourceProxy, ICoreConstants {

    protected final ModelWorkspace workspace;
    protected IPathRequestor requestor;
    protected ResourceInfo   info;

    //cached info
    protected IPath     fullPath;
    protected IResource resource;

    public ModelResourceProxy(ModelWorkspace workspace) {
        this.workspace = workspace;
    }

    /**
     * @see org.eclipse.core.resources.IResourceProxy#getModificationStamp()
     */
    @Override
    public long getModificationStamp() {
        return info.getModificationStamp();
    }

    @Override
    public String getName() {
        return requestor.requestName();
    }

    @Override
    public Object getSessionProperty(QualifiedName key) {
        return info.getSessionProperty(key);
    }

    @Override
    public int getType() {
        return info.getType();
    }

    /**
     * @see org.eclipse.core.resources.IResourceProxy#isAccessible()
     */
    @Override
    public boolean isAccessible() {
        int flags = info.getFlags();
        if (info.getType() == IResource.PROJECT)
            return flags != NULL_FLAG && ResourceInfo.isSet(flags, M_OPEN);
        return flags != NULL_FLAG;
    }

    /**
     * @see org.eclipse.core.resources.IResourceProxy#isDerived()
     */
    @Override
    public boolean isDerived() {
        int flags = info.getFlags();
        return flags != NULL_FLAG && ResourceInfo.isSet(flags, M_DERIVED);
    }

    /**
     * @see org.eclipse.core.resources.IResourceProxy#isLinked()
     */
    @Override
    public boolean isLinked() {
        int flags = info.getFlags();
        return flags != NULL_FLAG && ResourceInfo.isSet(flags, M_LINK);
    }

    /**
     * @see org.eclipse.core.resources.IResourceProxy#isPhantom()
     */
    @Override
    public boolean isPhantom() {
        int flags = info.getFlags();
        return flags != NULL_FLAG && ResourceInfo.isSet(flags, M_PHANTOM);
    }

    /**
     * @see org.eclipse.core.resources.IResourceProxy#isTeamPrivateMember()
     */
    @Override
    public boolean isTeamPrivateMember() {
        int flags = info.getFlags();
        return flags != NULL_FLAG && ResourceInfo.isSet(flags, M_TEAM_PRIVATE_MEMBER);
    }

    /**
     * @see org.eclipse.core.resources.IResourceProxy#isHidden()
     */
    @Override
    public boolean isHidden() {
        int flags = info.getFlags();
        return flags != NULL_FLAG && ResourceInfo.isSet(flags, M_HIDDEN);
    }

    /**
     * @see org.eclipse.core.resources.IResourceProxy#requestFullPath()
     */
    @Override
    public IPath requestFullPath() {
        if (fullPath == null)
            fullPath = requestor.requestPath();
        return fullPath;
    }

    /**
     * @see org.eclipse.core.resources.IResourceProxy#requestResource()
     */
    @Override
    public IResource requestResource() {
        if (resource == null)
            resource = workspace.newResource(requestFullPath(), info.getType());
        return resource;
    }

    protected void reset() {
        fullPath = null;
        resource = null;
    }

}
