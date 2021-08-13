package appbox.design.lang.java.jdt;

import org.eclipse.core.resources.IWorkspaceDescription;

public final class HackWorkspaceDescription implements IWorkspaceDescription {
    @Override
    public String[] getBuildOrder() {
        return new String[0];
    }

    @Override
    public long getFileStateLongevity() {
        return 604800000L;
    }

    @Override
    public int getMaxBuildIterations() {
        return 10;
    }

    @Override
    public int getMaxFileStates() {
        return 50;
    }

    @Override
    public long getMaxFileStateSize() {
        return 1048576L;
    }

    @Override
    public boolean isKeepDerivedState() {
        return false;
    }

    @Override
    public boolean isApplyFileStatePolicy() {
        return true;
    }

    @Override
    public long getSnapshotInterval() {
        return 300000L;
    }

    @Override
    public boolean isAutoBuilding() {
        return false;
    }

    @Override
    public void setAutoBuilding(boolean b) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void setBuildOrder(String[] strings) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void setFileStateLongevity(long l) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void setMaxBuildIterations(int i) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void setMaxFileStates(int i) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void setMaxFileStateSize(long l) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void setKeepDerivedState(boolean b) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void setApplyFileStatePolicy(boolean b) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void setSnapshotInterval(long l) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void setMaxConcurrentBuilds(int i) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public int getMaxConcurrentBuilds() {
        return 1;
    }
}
