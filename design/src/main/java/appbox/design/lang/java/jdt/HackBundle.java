package appbox.design.lang.java.jdt;

import org.osgi.framework.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class HackBundle implements Bundle {
    @Override
    public int getState() {
        return 0;
    }

    @Override
    public void start(int i) throws BundleException {

    }

    @Override
    public void start() throws BundleException {

    }

    @Override
    public void stop(int i) throws BundleException {

    }

    @Override
    public void stop() throws BundleException {

    }

    @Override
    public void update(InputStream inputStream) throws BundleException {

    }

    @Override
    public void update() throws BundleException {

    }

    @Override
    public void uninstall() throws BundleException {

    }

    @Override
    public Dictionary<String, String> getHeaders() {
        return null;
    }

    @Override
    public long getBundleId() {
        return 0;
    }

    @Override
    public String getLocation() {
        return null;
    }

    @Override
    public ServiceReference<?>[] getRegisteredServices() {
        return new ServiceReference[0];
    }

    @Override
    public ServiceReference<?>[] getServicesInUse() {
        return new ServiceReference[0];
    }

    @Override
    public boolean hasPermission(Object o) {
        return false;
    }

    @Override
    public URL getResource(String s) {
        return null;
    }

    @Override
    public Dictionary<String, String> getHeaders(String s) {
        return null;
    }

    @Override
    public String getSymbolicName() {
        return "org.eclipse.core.resources";
    }

    @Override
    public Class<?> loadClass(String s) throws ClassNotFoundException {
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String s) throws IOException {
        return null;
    }

    @Override
    public Enumeration<String> getEntryPaths(String s) {
        return null;
    }

    @Override
    public URL getEntry(String s) {
        return null;
    }

    @Override
    public long getLastModified() {
        return 0;
    }

    @Override
    public Enumeration<URL> findEntries(String s, String s1, boolean b) {
        return null;
    }

    @Override
    public BundleContext getBundleContext() {
        return null;
    }

    @Override
    public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int i) {
        return null;
    }

    @Override
    public Version getVersion() {
        return null;
    }

    @Override
    public <A> A adapt(Class<A> aClass) {
        return null;
    }

    @Override
    public File getDataFile(String s) {
        return null;
    }

    @Override
    public int compareTo(Bundle o) {
        return 0;
    }
}
