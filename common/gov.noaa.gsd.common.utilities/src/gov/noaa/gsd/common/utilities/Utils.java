package gov.noaa.gsd.common.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;

/**
 * Description: General class of static utilities.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 04, 2013            daniel.s.schaffer Initial creation
 * Nov 25, 2013    2336    Chris.Golden      Moved to gov.noaa.gsd.common.utilities,
 *                                           and removed unused methods and variables.
 * Apr 09, 2014    2925    Chris.Golden      Fixed typo in method name.
 * Jul 25, 2016   19537    Chris.Golden      Cleaned up, and added methods for testing
 *                                           equality and generating hash codes for
 *                                           potentially null objects.
 * </pre>
 * 
 * @author daniel.s.schaffer
 * @version 1.0
 */
public class Utils {

    // Public Static Methods

    /**
     * Compare the specified objects to see if they are equivalent, or are both
     * <code>null</code>.
     * 
     * @param object1
     *            First object to be compared; may be <code>null</code>.
     * @param object2
     *            Second object to be compared; may be <code>null</code>.
     * @return <code>true>/code> if the two objects are equivalent or are both
     *         <code>null</code>, <code>false</code> otherwise.
     */
    public static boolean equal(Object object1, Object object2) {
        return (object1 == null ? object2 == null : object1.equals(object2));
    }

    /**
     * Get the hash code of the specified object.
     * 
     * @param object
     *            Object for which the hash code is to be generated, or
     *            <code>null</code>.
     * @return Hash code, or 0 if the object is <code>null</code>.
     */
    public static long getHashCode(Object object) {
        return (object == null ? 0L : object.hashCode());
    }

    /**
     * Read the specified text file from the local/network file system and
     * return it as a {@link String}, with new lines not retained.
     * 
     * @param path
     *            Path to the file.
     * @return String contents of the file.
     */
    public static String textFileAsString(String path) {
        return textFileAsString(path, false);
    }

    /**
     * Read the specified text file from the local/network file system and
     * return it as a {@link String}.
     * 
     * @param path
     *            Path to the file.
     * @param retainNewLines
     *            Flag indicating whether or not new line characters should be
     *            retained.
     * @return String contents of the file.
     */
    public static String textFileAsString(String path, boolean retainNewlines) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String s = readBufferedTextReader(reader, retainNewlines);
            return s;
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            throw new RuntimeException("Unexpected exception: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Read the specified text file from the local/network file system and
     * return it as a {@link String}, with new lines not retained.
     * 
     * @param file
     *            File to be read.
     * @return String contents of the file.
     */
    public static String textFileAsString(File file) {
        return textFileAsString(file.getPath());
    }

    /**
     * Read text from the specified buffered reader and return it as a
     * {@link String}.
     * 
     * @param reader
     *            Buffered reader.
     * @return String contents of the buffered reader.
     * @throws IOException
     *             If the text could not be read.
     */
    public static String readBufferedTextReader(BufferedReader reader,
            boolean retainNewlines) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            String string = null;
            while ((string = reader.readLine()) != null) {
                stringBuilder.append(string);
                if (retainNewlines) {
                    stringBuilder.append("\n");
                }
            }
            if (retainNewlines && stringBuilder.length() > 0) {
                stringBuilder.setLength(stringBuilder.length() - 1);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return (stringBuilder.toString());
    }

    /**
     * Convert the specified throwable's stack trace into a {@link String}.
     * 
     * @param e
     *            Throwable.
     * @return String representation of the throwable's stack trace.
     */
    public static String getStackTraceAsString(Throwable e) {
        StringBuilder stringBuilder = new StringBuilder();
        for (StackTraceElement stackTraceElement : e.getStackTrace()) {
            stringBuilder.append(stackTraceElement.toString() + "\n");
        }
        return stringBuilder.toString();
    }

    /**
     * Determine whether or not the specified value is an instance of at least
     * one of the specified classes or interfaces.
     * 
     * @param value
     *            Value to have its class checked.
     * @param classes
     *            Set of classes, at least one of which which the value must
     *            implement.
     * @return <code>true</code> if the value is an instance of at least one of
     *         the classes, <code>false</code> otherwise.
     */
    public static boolean isValueInstanceOfAtLeastOneClass(Object value,
            Collection<Class<?>> classes) {
        for (Class<?> valueClass : classes) {
            if (valueClass.isAssignableFrom(value.getClass())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the number of generations between the specified subclass or
     * subinterface and superclass or superinterface. It is assumed that the
     * latter is indeed assignable from the former.
     * 
     * @param ancestor
     *            Superclass or superinterface.
     * @param descendant
     *            Subclass or subinterface.
     * @return Number of generations (1 or more) between the classes.
     * @throws IllegalArgumentException
     *             If the supposed descendant does not actually have the
     *             supposed ancestor as a superclass/superinterface.
     */
    public static int getGenerationsBetweenClasses(Class<?> ancestor,
            Class<?> descendant) {

        /*
         * If both are classes, simply iterate through the generations
         * separating them, counting said generations. Otherwise, iterate
         * through the interfaces implemented by the descendant and its ancestor
         * classes, and recursively through the interfaces extended by those
         * interfaces, finding the shortest generational path between the
         * ancestor and the descendant.
         */
        if (!ancestor.isInterface() && !descendant.isInterface()) {
            int generationDelta = 0;
            for (Class<?> aClass = descendant; aClass != null; aClass = aClass
                    .getSuperclass(), generationDelta++) {
                if (aClass.equals(ancestor)) {
                    return generationDelta;
                }
            }
        } else {
            int generationDelta = 0;
            int smallestGenerationDelta = -1;
            for (Class<?> aClass = descendant; aClass != null; aClass = aClass
                    .getSuperclass(), generationDelta++) {
                if (aClass.equals(ancestor)
                        || (smallestGenerationDelta == generationDelta)) {
                    return generationDelta;
                }
                for (Class<?> superInterface : aClass.getInterfaces()) {
                    if (ancestor.isAssignableFrom(superInterface)) {
                        int thisGenerationDelta = generationDelta
                                + 1
                                + getGenerationsBetweenClasses(ancestor,
                                        superInterface);
                        if (thisGenerationDelta == generationDelta) {
                            return generationDelta;
                        } else if ((smallestGenerationDelta == -1)
                                || (thisGenerationDelta < smallestGenerationDelta)) {
                            smallestGenerationDelta = thisGenerationDelta;
                        }
                    }
                }

            }
            if (smallestGenerationDelta > -1) {
                return smallestGenerationDelta;
            }
        }

        /*
         * If no familial relationship between the two was found, they are not
         * related.
         */
        throw new IllegalArgumentException(descendant
                + " is not a descendant of " + ancestor);
    }
}
