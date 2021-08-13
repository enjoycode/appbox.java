package appbox.design.lang.java.jdt;

import org.eclipse.core.internal.preferences.PreferencesService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.*;
import org.osgi.service.prefs.Preferences;

import java.io.InputStream;
import java.io.OutputStream;

public final class HackPreferencesService implements IPreferencesService {

    @Override
    public String get(String key, String defaultValue, Preferences[] nodes) {
        if (nodes == null) {
            return defaultValue;
        }

        for (Preferences node : nodes) {
            if (node != null) {
                String result = node.get(key, null);
                if (result != null) {
                    return result;
                }
            }
        }
        return defaultValue;
    }

    @Override
    public boolean getBoolean(String s, String s1, boolean b, IScopeContext[] iScopeContexts) {
        return false;
    }

    @Override
    public byte[] getByteArray(String s, String s1, byte[] bytes, IScopeContext[] iScopeContexts) {
        return new byte[0];
    }

    @Override
    public double getDouble(String s, String s1, double v, IScopeContext[] iScopeContexts) {
        return 0;
    }

    @Override
    public float getFloat(String s, String s1, float v, IScopeContext[] iScopeContexts) {
        return 0;
    }

    @Override
    public int getInt(String s, String s1, int i, IScopeContext[] iScopeContexts) {
        return 0;
    }

    @Override
    public long getLong(String s, String s1, long l, IScopeContext[] iScopeContexts) {
        return 0;
    }

    @Override
    public String getString(String s, String s1, String s2, IScopeContext[] iScopeContexts) {
        return null;
    }

    @Override
    public IEclipsePreferences getRootNode() {
        return null;
    }

    @Override
    public IStatus exportPreferences(IEclipsePreferences iEclipsePreferences, OutputStream outputStream, String[] strings) throws CoreException {
        return null;
    }

    @Override
    public IStatus importPreferences(InputStream inputStream) throws CoreException {
        return null;
    }

    @Override
    public IStatus applyPreferences(IExportedPreferences iExportedPreferences) throws CoreException {
        return null;
    }

    @Override
    public IExportedPreferences readPreferences(InputStream inputStream) throws CoreException {
        return null;
    }

    @Override
    public String[] getDefaultLookupOrder(String s, String s1) {
        return new String[0];
    }

    @Override
    public String[] getLookupOrder(String s, String s1) {
        return new String[0];
    }

    @Override
    public void setDefaultLookupOrder(String s, String s1, String[] strings) {

    }

    @Override
    public void exportPreferences(IEclipsePreferences iEclipsePreferences, IPreferenceFilter[] iPreferenceFilters, OutputStream outputStream) throws CoreException {

    }

    @Override
    public IPreferenceFilter[] matches(IEclipsePreferences iEclipsePreferences, IPreferenceFilter[] iPreferenceFilters) throws CoreException {
        return new IPreferenceFilter[0];
    }

    @Override
    public void applyPreferences(IEclipsePreferences iEclipsePreferences, IPreferenceFilter[] iPreferenceFilters) throws CoreException {

    }
}
