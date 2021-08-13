package appbox.design.lang.java.jdt;

import org.osgi.framework.*;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Dictionary;

public class HackBundleContext implements BundleContext {
    private final HackBundle _bundle = new HackBundle();


    @Override
    public String getProperty(String s) {
        if ("osgi.os".equals(s)) {
            return "linux";
        }
        return null;
    }

    @Override
    public Bundle getBundle() {
        return _bundle;
    }

    @Override
    public Bundle installBundle(String s, InputStream inputStream) throws BundleException {
        return null;
    }

    @Override
    public Bundle installBundle(String s) throws BundleException {
        return null;
    }

    @Override
    public Bundle getBundle(long l) {
        return null;
    }

    @Override
    public Bundle[] getBundles() {
        return new Bundle[0];
    }

    @Override
    public void addServiceListener(ServiceListener serviceListener, String s) throws InvalidSyntaxException {

    }

    @Override
    public void addServiceListener(ServiceListener serviceListener) {

    }

    @Override
    public void removeServiceListener(ServiceListener serviceListener) {

    }

    @Override
    public void addBundleListener(BundleListener bundleListener) {

    }

    @Override
    public void removeBundleListener(BundleListener bundleListener) {

    }

    @Override
    public void addFrameworkListener(FrameworkListener frameworkListener) {

    }

    @Override
    public void removeFrameworkListener(FrameworkListener frameworkListener) {

    }

    @Override
    public ServiceRegistration<?> registerService(String[] strings, Object o, Dictionary<String, ?> dictionary) {
        return null;
    }

    @Override
    public ServiceRegistration<?> registerService(String s, Object o, Dictionary<String, ?> dictionary) {
        return null;
    }

    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> aClass, S s, Dictionary<String, ?> dictionary) {
        return null;
    }

    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> aClass, ServiceFactory<S> serviceFactory, Dictionary<String, ?> dictionary) {
        return null;
    }

    @Override
    public ServiceReference<?>[] getServiceReferences(String s, String s1) throws InvalidSyntaxException {
        return new ServiceReference[0];
    }

    @Override
    public ServiceReference<?>[] getAllServiceReferences(String s, String s1) throws InvalidSyntaxException {
        return new ServiceReference[0];
    }

    @Override
    public ServiceReference<?> getServiceReference(String s) {
        return null;
    }

    @Override
    public <S> ServiceReference<S> getServiceReference(Class<S> aClass) {
        return null;
    }

    @Override
    public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> aClass, String s) throws InvalidSyntaxException {
        return null;
    }

    @Override
    public <S> S getService(ServiceReference<S> serviceReference) {
        return null;
    }

    @Override
    public boolean ungetService(ServiceReference<?> serviceReference) {
        return false;
    }

    @Override
    public <S> ServiceObjects<S> getServiceObjects(ServiceReference<S> serviceReference) {
        return null;
    }

    @Override
    public File getDataFile(String s) {
        return null;
    }

    @Override
    public Filter createFilter(String s) throws InvalidSyntaxException {
        return null;
    }

    @Override
    public Bundle getBundle(String s) {
        return null;
    }
}
