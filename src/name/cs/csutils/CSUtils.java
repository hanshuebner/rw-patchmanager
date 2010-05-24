/**
 * Copyright (c) 2009, Christian Schneider
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the names of the authors nor the names of its contributors may
 *    be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package name.cs.csutils;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.JComponent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import name.cs.csutils.Platform.OS;

public final class CSUtils {

    private static final Log log = LogFactory.getLog(CSUtils.class);
    
    public static final String UNASSIGNED_STRING = "[Unassigned]";

    public static final String EMPTY_STRING = "";

    private static final
        SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");

    private CSUtils() {
        super();
    }

    public static byte[] arrayCopyOf(byte[] original, int newLength) {
        byte[] copy = new byte[newLength];
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T[] arrayCopyOf(T[] original, int newLength) {
        return (T[]) arrayCopyOf(original, newLength, original.getClass());
    }
    
    @SuppressWarnings("unchecked")
    public static <T,U> T[] arrayCopyOf(U[] original, int newLength, Class<? extends T[]> newType) {
        T[] copy = ((Object)newType == (Object)Object[].class)
            ? (T[]) new Object[newLength]
            : (T[]) Array.newInstance(newType.getComponentType(), newLength);
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }
    
    public static final boolean equals(Object a, Object b) {
        return (a==b) || (a != null && a.equals(b));
    }
    
    public static String limitString(String str, int maxlen) {
        return limitString(str, maxlen, "...");
    }
    
    public static class ProcessResult {
        public int status;
        public boolean timeout;
        public boolean interrupted;
        public boolean exited;
        public InterruptedException interruptedException;
    }
    
    public static ProcessResult waitFor(Process proc, long timeout, long resolution) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("timeout<0");
        }
        if (resolution <= 0) {
            throw new IllegalArgumentException("resolution<0");
        }
        resolution = Math.min(timeout, resolution);
        
        ProcessResult res = new ProcessResult();
        res.status = 1;
        res.exited = false;
        res.timeout = false;
        res.interrupted = false;
        res.interruptedException = null;
        while (true) {
            if (timeout<=0) {
                res.timeout = true;
                break;
            }
            try {
                res.status = proc.exitValue();
                res.exited = true;
                break;
            } catch (IllegalThreadStateException ex) {
                // no op
            }
            
            try {
                Thread.sleep(resolution);
            } catch (InterruptedException ex) {
                // remember the interrupted state
                res.interrupted = true;
                res.interruptedException = ex;
            }
            // we can't do this in the try-clause since the
            // Interrupted exception might cause that the timeout
            // is never decremented and then this look will block forever
            timeout -= resolution;
        }
        return res;
    }
    
    public static String limitString(String str, int maxlen, String limitSuffix) {
        if (str.length()<=maxlen) {
            return str;
        }
        if (limitSuffix == null) {
            limitSuffix = EMPTY_STRING;
        }
        return str.substring(0, Math.max(0,Math.min(str.length()-3,maxlen)))+limitSuffix; 
    }

    private static int colorComponent(float value) {
        return Math.max(0, Math.min(255, (int)value));
    }
    
    public static Color newColor(Color c, float factor) {
        return new Color(colorComponent(c.getRed()*factor), 
            colorComponent(c.getGreen()*factor),
            colorComponent(c.getBlue() *factor));
    }

    public static Color change(Color c, float factor) {
        return new Color(
                colorComponent(c.getRed()+0xFf*factor*2-0xFf),
                colorComponent(c.getGreen()+0xFf*factor*2-0xFf),
                colorComponent(c.getBlue()+0xFf*factor*2-0xFf));
    }

    public static Color change(Color c, float rfactor, float gfactor, float bfactor) {
        return new Color(
                colorComponent(c.getRed()+0xFf*rfactor*2-0xFf),
                colorComponent(c.getGreen()+0xFf*gfactor*2-0xFf),
                colorComponent(c.getBlue()+0xFf*bfactor*2-0xFf));
    }

    public static String toHTMLColor(Color c) {
        return "#"+Integer.toHexString(c.getRGB()).substring(2);
    }
    
    /**
     * Returns the last path component of the specified path.
     * The components are separated using the slash character '/'
     * or the '\' character.
     * @param path the path
     * @return the last path component of the specified path
     */
    public static String getLastPathComponent(String path) {
        int separator = Math.max(path.lastIndexOf('/'),path.lastIndexOf('\\'));
        if (separator>=0) { // keep name if it contains only a single separator
            if (separator == path.length()-1) { // path ends with separator which is no good
                return getLastPathComponent(path.substring(0, path.length()-1));
            }
            return path.substring(separator+1);
        }
        return path;
    }

    public static File getApplicationConfigDir(String name) {
        File userhome = new File(System.getProperty(SystemProperties.USER_HOME));
        if (Platform.isFlavor(OS.UnixFlavor) || Platform.isFlavor(OS.MacOSFlavor)) {
            return new File(userhome,'.'+name);
        } else {
            return new File(userhome,name);
        }
    }
    
    public static Date now() {
        return Calendar.getInstance().getTime();
    }
    
    public static String dateToString(Date date) {
        return dateformat.format(date);
    }

    public static Font changeFontSize(Font font, float factor) {
        int size = (int)Math.round(factor*font.getSize());
        if (size<6) size = Math.max(6, font.getSize());
        return new Font(font.getName(), font.getStyle(), size);
    }

    public static Font changeFontStyle(Font font, int style) {
        return new Font(font.getName(), style, font.getSize());
    }

    public static Font changeFontName(Font font, String name) {
        return new Font(name, font.getStyle(), font.getSize());
    }
    
    public static void changeFontSize(JComponent component, float factor) {
        component.setFont(changeFontSize(component.getFont(), factor));
    }

    public static void changeFontStyle(JComponent component, int style) {
        component.setFont(changeFontStyle(component.getFont(), style));
    }

    public static void changeFontName(JComponent component, String name) {
        component.setFont(changeFontName(component.getFont(), name));
    }
    
    public static Date parseDate(String str) {
        SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return dateformat.parse(str);
        } catch (ParseException e) {
            return null;
        }
        /*
        // yyyy-mm-dd
        if (str.length() != 10 || 
                str.charAt(4)!='-' || str.charAt(7)!='-') {
            return null;
        }
        int year, month, date;
        try {
            year = Integer.parseInt(str.substring(0,4));
            month = Integer.parseInt(str.substring(5,5+2));
            date = Integer.parseInt(str.substring(8,8+2));
        } catch (NumberFormatException ex) {
            return null;
        }
        Calendar cal = new GregorianCalendar(tz);

        cal.set(year, month, date, 0, 0, 0);  
        
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // TODO 
        //if (!dateToString(cal.getTime()).equals(str))  
        //  throw new RuntimeException(str+","+dateToString(cal.getTime()));
        return cal.getTime();*/
    }
        
    public static <T> String join(String delimiter, T ... values) {
        if (delimiter == null) {
            delimiter = EMPTY_STRING;
        }
        if (values.length == 0) {
            return EMPTY_STRING;
        } else if (values.length == 1) {
            return String.valueOf(values[0]);
        } else if (values.length == 2) {
            return String.valueOf(values[0])
            +delimiter+String.valueOf(values[1]);
        }
        StringBuilder joined = new StringBuilder();
        joined.append(String.valueOf(values[0]));
        for (int i=1;i<values.length;i++) {
            joined.append(delimiter).append(values[i]);
        }
        return joined.toString();
    }

    public static String join(String delimiter, Iterator<?> iterator) {
        if (delimiter == null) {
            delimiter = EMPTY_STRING;
        }
        if (!iterator.hasNext()) {
            return EMPTY_STRING;
        }
        Object first = iterator.next();
        if (!iterator.hasNext()) {
            return String.valueOf(first);
        }
        StringBuilder joined = new StringBuilder();
        joined.append(first);
        while (iterator.hasNext()) {
            joined.append(delimiter).append(iterator.next());
        }
        return joined.toString();
    }
    
    public static String format(String format, Object ... args) {
        return format(null, format, args);
    }

    public static 
        String format(StringBuilder builder, String format, Object ... args) {
        if (builder == null) {
            builder = new StringBuilder();
        }
        new Formatter(builder).format(format, args);
        return builder.toString();
    }
    

    public static void copy(File src, File dst, boolean deleteDstOnError) throws IOException {
        try {
            InputStream is = new BufferedInputStream(new FileInputStream(src));
            try {
                copy(is, dst, false);
            } finally {
                is.close();
            }
        } catch (IOException ex) {
            if (deleteDstOnError) {
                dst.delete();
            }
        }
    }
    
    public static void copy(File file, OutputStream out) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            copy(in, out);
        } finally {
            in.close();
        }
    }

    public static void copy(InputStream in, File fout, boolean deleteFileOnError) throws IOException {
        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(fout));
            try {
                copy(in, out);
                out.flush();
            } finally {
                out.close();
            }
        } catch (IOException ex) {
            if (deleteFileOnError) {
                fout.delete();
            }
            throw ex;
        }
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        if (in == null) {
            throw new IllegalArgumentException("in == null");
        }
        if (out == null) {
            throw new IllegalArgumentException("out == null");
        }
        
        byte[] buf = new byte[1024];
        int r;
        while ((r=in.read(buf))!=-1) {
            out.write(buf, 0, r);
        }
        out.flush();
    }

    public static UnsupportedOperationException notImplementedYet() {
        return new UnsupportedOperationException("TODO: not implemented yet");
    }

    public static String removeFileExtension(String name) {
        if (name.startsWith(".")) {
            return name;
        }
        int idx = name.lastIndexOf('.');
        return idx<0 ? name : name.substring(0, idx);
    }
    
    public static File addMissingFileNameSuffix(File file, String suffix) {
        String name = file.getName();
        if (name.indexOf('.')<0) {
            return new File(file.getParentFile(), name+suffix);
        }
        return file;
    }

    public static boolean storeProperties(Properties properties, File dst) {
        return storeProperties(properties, dst, "generated file - do not modify");
    }
    public static boolean storeProperties(Properties properties,
            File dst, String comments) {
        if (log.isDebugEnabled()) {
            log.debug("Storing property file: "+dst);
        }
        OutputStream out = null;
        try {
            try {
                out = new BufferedOutputStream(new FileOutputStream(dst));
                properties.store(out, comments);
                out.flush();
            } finally {
                if (out != null) {
                    out.close();
                    out = null;
                }
            }
            return true;
        } catch (IOException ex) {
            if (log.isDebugEnabled()) {
                log.debug("Error while storing property file: "+dst, ex);
            }   
            dst.delete(); // delete corrupted property file
            return false;
        }
    }

    public static CSProperties loadProperties(File src) {
        CSProperties properties = new CSProperties();
        InputStream is = null;
        try {
            try {
                is = new BufferedInputStream(new FileInputStream(src));
                properties.load(is);
            } finally {
                if (is != null) {
                    is.close();
                    is = null;
                }
            }
        } catch (IOException ex) {
            if (log.isDebugEnabled()) {
                log.debug("Error while reading property file: "+src, ex);
            }
        }
        return properties;
    }

    public static CSProperties loadProperties(URL url) {
        if (url != null) {
            try {
                return loadProperties(new File(url.toURI()));
            } catch (URISyntaxException ex) {
                if (log.isDebugEnabled()) {
                    log.debug("could not load properties: "+url, ex);
                }
            }
        }
        return new CSProperties();
    }

}
