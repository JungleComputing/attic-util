package ibis.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * This class exports a method for searching the classpath for jar-files
 * with a specified name in the Manifest.
 */
public class ClassLister {

    private static ClassLister classLister;

    private JarFile[] jarFiles;

    protected ClassLister() {
        readJarFiles();
    }

    /**
     * Singleton method to construct a ClassLister.
     * 
     * @return A ClassLister instance
     */
    public static synchronized ClassLister getClassLister() {
        if (classLister == null)
            classLister = new ClassLister();
        return classLister;
    }

    /**
     * This method reads all jar files from the classpath, and stores them
     * in a list that can be searched for specific names later on.
     */
    protected void readJarFiles() {
        ArrayList jarList = new ArrayList();
        String classPath = System.getProperty("java.class.path");
        if (classPath != null) {
            StringTokenizer st = new StringTokenizer(classPath,
                    File.pathSeparator);
            while (st.hasMoreTokens()) {
                String jar = st.nextToken();
                File f = new File(jar);
                try {
                    JarFile jarFile = new JarFile(f, true);
                    Manifest manifest = jarFile.getManifest();
                    if (manifest != null) {
                        manifest.getMainAttributes();
                        jarList.add(jarFile);
                    }
                } catch(IOException e) {
                    // ignore. Could be a directory.
                }
            }
        }
        jarFiles = (JarFile[]) jarList.toArray(new JarFile[0]);
    }

    /**
     * Returns a list of classes for the specified attribute name.
     * The specified manifest attribute name is assumed to be
     * mapped to the name of a class. All jar files in the classpath
     * are scanned for the specified manifest attribute name, and
     * the attribute values are loaded.
     * @param attribName the manifest attribute name.
     * @return the list of classes.
     */
    public List getClassList(String attribName) {
        ArrayList list = new ArrayList();

        for (int i = 0; i < jarFiles.length; i++) {
            Manifest mf = null;
            try {
                mf = jarFiles[i].getManifest();
            } catch(IOException e) {
                throw new Error("Could not get Manifest from "
                        + jarFiles[i].getName(), e);
            }
            if (mf != null) {
                Attributes ab = mf.getMainAttributes();
                String className = ab.getValue(attribName);
                try {
                    Class cl = Class.forName(className);
                    list.add(cl);
                } catch(Exception e) {
                    throw new Error("Could not load class " + className
                            + ". Something wrong with jar "
                            + jarFiles[i].getName() + "?", e);
                }
            }
        }
        return list;
    }

    /**
     * Returns a list of classes for the specified attribute name.
     * The specified manifest attribute name is assumed to be
     * mapped to the name of a class. All jar files in the classpath
     * are scanned for the specified manifest attribute name, and
     * the attribute values are loaded.
     * The classes thus obtained should be extensions of the specified
     * class, or, if it is an interface, implementations of it.
     * @param attribName the manifest attribute name.
     * @param clazz the class of which the returned classes are implementations
     *    or extensions.       
     * @return the list of classes.
     */
    public List getClassList(String attribName, Class clazz) {
        List list = getClassList(attribName);

        for (Iterator iter = list.iterator(); iter.hasNext();) {
            Class cl = (Class) iter.next();
            if (! clazz.isAssignableFrom(cl)) {
                throw new Error("Class " + cl.getName()
                        + " cannot be assigned to class " + clazz.getName());
            }
        }
        return list;
    }
}
