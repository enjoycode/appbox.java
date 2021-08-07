package appbox.design.lang.java.jdt;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.spi.IRegistryProvider;

import java.io.InputStream;
import java.util.ResourceBundle;

public final class HackRegistryProvider implements IRegistryProvider {
    private final IExtensionRegistry _registry = new HackExtensionRegistry();

    @Override
    public IExtensionRegistry getRegistry() {
        return _registry;
    }

    public static class HackExtensionRegistry implements IExtensionRegistry {
        @Override
        public void addRegistryChangeListener(IRegistryChangeListener iRegistryChangeListener, String s) {

        }

        @Override
        public void addRegistryChangeListener(IRegistryChangeListener iRegistryChangeListener) {

        }

        @Override
        public IConfigurationElement[] getConfigurationElementsFor(String s) {
            return new IConfigurationElement[0];
        }

        @Override
        public IConfigurationElement[] getConfigurationElementsFor(String s, String s1) {
            return new IConfigurationElement[0];
        }

        @Override
        public IConfigurationElement[] getConfigurationElementsFor(String s, String s1, String s2) {
            return new IConfigurationElement[0];
        }

        @Override
        public IExtension getExtension(String s) {
            return null;
        }

        @Override
        public IExtension getExtension(String s, String s1) {
            return null;
        }

        @Override
        public IExtension getExtension(String s, String s1, String s2) {
            return null;
        }

        @Override
        public IExtensionPoint getExtensionPoint(String s) {
            return null;
        }

        @Override
        public IExtensionPoint getExtensionPoint(String s, String s1) {
            //if (s.equals("org.eclipse.core.resources") && s1.equals("markers")) {
            //    return new TestExtensionPoint();
            //} else if (s.equals("org.eclipse.core.filesystem") && s1.equals("filesystems")) {
            //    return new TestExtensionPoint();
            //} else if (s.equals("org.eclipse.core.resources") && s1.equals("variableResolvers")) {
            //    return new TestExtensionPoint();
            //}
            return null;
        }

        @Override
        public IExtensionPoint[] getExtensionPoints() {
            return new IExtensionPoint[0];
        }

        @Override
        public IExtensionPoint[] getExtensionPoints(String s) {
            return new IExtensionPoint[0];
        }

        @Override
        public IExtensionPoint[] getExtensionPoints(IContributor iContributor) {
            return new IExtensionPoint[0];
        }

        @Override
        public IExtension[] getExtensions(String s) {
            return new IExtension[0];
        }

        @Override
        public IExtension[] getExtensions(IContributor iContributor) {
            return new IExtension[0];
        }

        @Override
        public String[] getNamespaces() {
            return new String[0];
        }

        @Override
        public void removeRegistryChangeListener(IRegistryChangeListener iRegistryChangeListener) {

        }

        @Override
        public boolean addContribution(InputStream inputStream, IContributor iContributor, boolean b, String s, ResourceBundle resourceBundle, Object o) throws IllegalArgumentException {
            return false;
        }

        @Override
        public boolean removeExtension(IExtension iExtension, Object o) throws IllegalArgumentException {
            return false;
        }

        @Override
        public boolean removeExtensionPoint(IExtensionPoint iExtensionPoint, Object o) throws IllegalArgumentException {
            return false;
        }

        @Override
        public void stop(Object o) throws IllegalArgumentException {

        }

        @Override
        public void addListener(IRegistryEventListener iRegistryEventListener) {

        }

        @Override
        public void addListener(IRegistryEventListener iRegistryEventListener, String s) {

        }

        @Override
        public void removeListener(IRegistryEventListener iRegistryEventListener) {

        }

        @Override
        public boolean isMultiLanguage() {
            return false;
        }
    }
}
