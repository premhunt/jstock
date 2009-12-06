/*
 * JStock - Free Stock Market Software
 * Copyright (C) 2009 Yan Cheng Cheok <yccheok@yahoo.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.yccheok.jstock.gui;

import com.thoughtworks.xstream.XStream;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import org.yccheok.jstock.engine.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JRadioButton;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.TimeTCPClient;
import org.yccheok.jstock.analysis.Connection;
import org.yccheok.jstock.analysis.DoubleConstantOperator;
import org.yccheok.jstock.analysis.EqualityOperator;
import org.yccheok.jstock.analysis.Indicator;
import org.yccheok.jstock.analysis.OperatorIndicator;
import org.yccheok.jstock.analysis.SinkOperator;
import org.yccheok.jstock.analysis.StockOperator;
import org.yccheok.jstock.internationalization.MessagesBundle;

/**
 *
 * @author yccheok
 */
public class Utils {
    /** Creates a new instance of Utils */
    private Utils() {
    }

    public static java.util.Date getNTPDate() {
        List<String> hosts = getNTPServers();

        for (String host : hosts) {
            TimeTCPClient client = new TimeTCPClient();
            // We want to timeout if a response takes longer than 5 seconds
            client.setDefaultTimeout(5000);
            try {
                client.connect(host);
                java.util.Date ntpDate = client.getDate();
                client.disconnect();
                // Just to be extra caution.
                if (ntpDate != null) {
                    return ntpDate;
                }
            }
            catch (java.net.SocketException exp) {
                log.error(host, exp);
            }
            catch (java.io.IOException exp) {
                log.error(host, exp);
            }
        }
        return null;
    }

    public static boolean extractZipFile(String zipFilePath, boolean overwrite) {
        return extractZipFile(new File(zipFilePath), overwrite);
    }

    public static boolean extractZipFile(File zipFilePath, boolean overwrite) {
        InputStream inputStream = null;
        ZipInputStream zipInputStream = null;
        boolean status = true;

        try {
            inputStream = new FileInputStream(zipFilePath);

            zipInputStream = new ZipInputStream(inputStream);
            final byte[] data = new byte[1024];

            while (true) {
                ZipEntry zipEntry = null;
                FileOutputStream outputStream = null;

                try {
                    zipEntry = zipInputStream.getNextEntry();

                    if (zipEntry == null) break;

                    final String destination = Utils.getUserDataDirectory() + zipEntry.getName();

                    if (overwrite == false) {
                        if (Utils.isFileOrDirectoryExist(destination)) continue;
                    }

                    if (zipEntry.isDirectory())
                    {
                        Utils.createCompleteDirectoryHierarchyIfDoesNotExist(destination);
                    }
                    else
                    {
                        final File file = new File(destination);
                        // Ensure directory is there before we write the file.
                        Utils.createCompleteDirectoryHierarchyIfDoesNotExist(file.getParentFile());

                        int size = zipInputStream.read(data);

                        if (size > 0) {
                            outputStream = new FileOutputStream(destination);

                            do {
                                outputStream.write(data, 0, size);
                                size = zipInputStream.read(data);
                            } while(size >= 0);
                        }
                    }
                }
                catch (IOException exp) {
                    log.error(null, exp);
                    status = false;
                    break;
                }
                finally {
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        }
                        catch (IOException exp) {
                            log.error(null, exp);
                            break;
                        }
                    }

                    if (zipInputStream != null) {
                        try {
                            zipInputStream.closeEntry();
                        }
                        catch (IOException exp) {
                            log.error(null, exp);
                            break;
                        }
                    }
                }

            }   // while(true)
        }
        catch (IOException exp) {
            log.error(null, exp);
            status = false;
        }
        finally {
            if (zipInputStream != null) {
                try {
                    zipInputStream.close();
                } catch (IOException ex) {
                    log.error(null, ex);
                }
            }

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    log.error(null, ex);
                }
            }
        }
        return status;
    }

    // A value obtained from server, to ensure all JStock's users are getting
    // same value for same key.
    public static String getUUIDValue(String url, String key) {
        final org.yccheok.jstock.gui.Utils.InputStreamAndMethod inputStreamAndMethod = org.yccheok.jstock.gui.Utils.getResponseBodyAsStreamBasedOnProxyAuthOption(url);

        if (inputStreamAndMethod.inputStream == null) {
            inputStreamAndMethod.method.releaseConnection();
            return null;
        }

        Properties properties = new Properties();
        try {
            properties.load(inputStreamAndMethod.inputStream);
        }
        catch (IOException exp) {
            log.error(null, exp);
            return null;
        }
        catch (IllegalArgumentException exp) {
            log.error(null, exp);
            return null;
        }
        finally {
            try {
                inputStreamAndMethod.inputStream.close();
            } catch (IOException exp) {
                log.error(null, exp);
            }
            inputStreamAndMethod.method.releaseConnection();
        }
        final String _id = properties.getProperty("id");
        if (_id == null) {
            log.info("UUID not found");
            return null;
        }

        final String id = org.yccheok.jstock.gui.Utils.decrypt(_id);
        if (id.equals(org.yccheok.jstock.gui.Utils.getJStockUUID()) == false) {
            log.info("UUID doesn't match");
            return null;
        }

        final String value = properties.getProperty(key);
        if (value == null) {
            log.info("Value not found");
            return null;
        }
        return value;
    }

    public static Map<String, String> getUUIDValue(String url) {
        Map<String, String> map = new HashMap<String, String>();
        final org.yccheok.jstock.gui.Utils.InputStreamAndMethod inputStreamAndMethod = org.yccheok.jstock.gui.Utils.getResponseBodyAsStreamBasedOnProxyAuthOption(url);

        if (inputStreamAndMethod.inputStream == null) {
            inputStreamAndMethod.method.releaseConnection();
            return java.util.Collections.EMPTY_MAP;
        }

        Properties properties = new Properties();
        try {
            properties.load(inputStreamAndMethod.inputStream);
        }
        catch (IOException exp) {
            log.error(null, exp);
            return java.util.Collections.EMPTY_MAP;
        }
        catch (IllegalArgumentException exp) {
            log.error(null, exp);
            return java.util.Collections.EMPTY_MAP;
        }
        finally {
            try {
                inputStreamAndMethod.inputStream.close();
            } catch (IOException exp) {
                log.error(null, exp);
            }
            inputStreamAndMethod.method.releaseConnection();
        }
        final String _id = properties.getProperty("id");
        if (_id == null) {
            log.info("UUID not found");
            return java.util.Collections.EMPTY_MAP;
        }

        final String id = org.yccheok.jstock.gui.Utils.decrypt(_id);
        if (id.equals(org.yccheok.jstock.gui.Utils.getJStockUUID()) == false) {
            log.info("UUID doesn't match");
            return java.util.Collections.EMPTY_MAP;
        }

        for (Object key : properties.keySet()) {
            if (key != null) {
                map.put(key.toString(), properties.getProperty(key.toString()));
            }
        }
        return map;
    }

    private static List<String> getNTPServers()
    {
        // The list is obtained from Windows Vista, Internet Time Server List itself.
        // The complete server list can be obtained from http://tf.nist.gov/tf-cgi/servers.cgi
        final List<String> defaultServer = java.util.Collections.unmodifiableList(java.util.Arrays.asList("time-a.nist.gov", "time-b.nist.gov", "time-nw.nist.gov"));
        List<String> servers = Utils.NTPServers;
        if (servers != null) {
			// We already have the server list.
            return servers;
        }

        final String server = getUUIDValue(org.yccheok.jstock.engine.Utils.getJStockStaticServer() + "servers_information/ntp_server.txt", "server");
        if (server != null) {
            String[] s = server.split(",");
            if (s.length > 0) {
                List<String> me = java.util.Collections.unmodifiableList(java.util.Arrays.asList(s));
				// Save it! So that we need not to ask for server list again next time.
                Utils.NTPServers = me;
                return me;
            }
        }
        
		// Use default servers, so that we need not to ask for server list again next time.
        Utils.NTPServers = defaultServer;
        return defaultServer;
    }

    public static void launchWebBrowser(String address) {
        if (Desktop.isDesktopSupported())
        {
            final Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE))
            {
                URL url = null;
                String string = address;
                try {
                    url = new URL(string);
                } catch (MalformedURLException ex) {
                    return;
                }
                try {
                    desktop.browse(url.toURI());
                }
                catch (URISyntaxException ex) {
                }
                catch (IOException ex) {
                }
            }
        }
    }

    public static void launchWebBrowser(javax.swing.event.HyperlinkEvent evt) {
        if (HyperlinkEvent.EventType.ACTIVATED.equals(evt.getEventType())) {
            URL url = evt.getURL();
        if (Desktop.isDesktopSupported())
        {
            final Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE))
            {
                    if (url == null) {
                        // www.yahoo.com considered an invalid URL. Hence, evt.getURL() returns null.
                        String string = "http://" + evt.getDescription();
                        try {
                            url = new URL(string);
                        } catch (MalformedURLException ex) {
                            return;
                        }
                    }
                try {
                    desktop.browse(url.toURI());
                }
                catch (URISyntaxException ex) {
                }
                catch (IOException ex) {
                }
            }
        }
        }
    }

    public static java.awt.Image getScaledImage(Image image, int maxWidth, int maxHeight) {
        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();
        
        final int imgWidth = image.getWidth(null);
        final int imgHeight = image.getHeight(null);
                        
        final int preferredWidth = Math.min(imgWidth, maxWidth);
        final int preferredHeight = Math.min(imgHeight, maxHeight);
        
        final double scaleX = (double)preferredWidth / (double)imgWidth;
        final double scaleY = (double)preferredHeight / (double)imgHeight;
        
        final double bestScale = Math.min(scaleX, scaleY);
    
        return image.getScaledInstance((int)((double)imgWidth * bestScale), (int)((double)imgHeight * bestScale), Image.SCALE_SMOOTH);
    }
    
    // This method returns true if the specified image has transparent pixels
    private static boolean hasAlpha(Image image) {
        // If buffered image, the color model is readily available
        if (image instanceof BufferedImage) {
            BufferedImage bimage = (BufferedImage)image;
            return bimage.getColorModel().hasAlpha();
        }
    
        // Use a pixel grabber to retrieve the image's color model;
        // grabbing a single pixel is usually sufficient
         PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
        }
    
        // Get the image's color model
        ColorModel cm = pg.getColorModel();
        
        return cm.hasAlpha();
    }
    
    // This method returns a buffered image with the contents of an image
    public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage)image;
        }
    
        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();
    
        // Determine if the image has transparent pixels; for this method's
        // implementation, see e661 Determining If an Image Has Transparent Pixels
        boolean hasAlpha = hasAlpha(image);
    
        // Create a buffered image with a format that's compatible with the screen
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;
            if (hasAlpha) {
                transparency = Transparency.BITMASK;
            }
    
            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(
                image.getWidth(null), image.getHeight(null), transparency);
        } catch (HeadlessException e) {
            // The system does not have a screen
        }
    
        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;
            if (hasAlpha) {
                type = BufferedImage.TYPE_INT_ARGB;
            }
            bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        }
    
        // Copy image to buffered image
        Graphics g = bimage.createGraphics();
    
        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();
    
        return bimage;
    }
    
    // Deletes all files and subdirectories under dir.
    // Returns true if all deletions were successful.
    // If a deletion fails, the method stops attempting to delete and returns false.
    public static boolean deleteDir(File dir, boolean deleteRoot) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]), true);
                if (!success) {
                    return false;
                }
            }
        }
    
        // The directory is now empty so delete it
        if(deleteRoot) {
            return dir.delete();
        }
        
        return true;
    }

    public static boolean createCompleteDirectoryHierarchyIfDoesNotExist(String directory) {
        return createCompleteDirectoryHierarchyIfDoesNotExist(new File(directory));
    }
    
    private static boolean createCompleteDirectoryHierarchyIfDoesNotExist(File f) {
        if (f == null) return true;
                
        if (false == createCompleteDirectoryHierarchyIfDoesNotExist(f.getParentFile())) {
            return false;
        }
        
        String path = null;
        
        try {
            path = f.getCanonicalPath();
        } catch (IOException ex) {
            log.error(null, ex);
            return false;
        }
        
        return createDirectoryIfDoesNotExist(path);
    }
    
    public static boolean isFileOrDirectoryExist(String fileOrDirectory) {
        java.io.File f = new java.io.File(fileOrDirectory);
        return f.exists();
    }
    
    public static boolean createDirectoryIfDoesNotExist(String directory) {
        java.io.File f = new java.io.File(directory);
        
        if(f.exists() == false) {
            if(f.mkdir())
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        
        return true;
    }
    
    public static String getUserDataDirectory() {
        return System.getProperty("user.home") + File.separator + ".jstock" + File.separator + getApplicationVersionString() + File.separator;
    }

    public static boolean migrateFrom104jTo105() {
        // File (or directory) with old name
        final File oldDirectory = new File(System.getProperty("user.home") + File.separator + ".jstock" + File.separator + "1.0.4" + File.separator);

        // File (or directory) with new name
        final File newDirectory = new File(getUserDataDirectory());

        // Migrate already?
        if (newDirectory.isDirectory() && newDirectory.exists()) {
            return true;
        }

        // No 1.0.4j found?
        if (oldDirectory.isDirectory() == false || oldDirectory.exists() == false) {
            return true;
        }

        // Rename file (or directory)
        boolean success = oldDirectory.renameTo(newDirectory);
        if (!success) {
            return false;
        }

        // Indicator directory no longer backward compataible.
        final File indicatorDirectory = new File(getUserDataDirectory() + "indicator" + File.separator);
        final boolean status = deleteDir(indicatorDirectory, true);
        return status;
    }

    public static Color getColor(double price, double referencePrice) {
        if(price < referencePrice) {
            return JStockOptions.DEFAULT_LOWER_NUMERICAL_VALUE_FOREGROUND_COLOR;
        }
        
        if(price > referencePrice) {
            return JStockOptions.DEFAULT_HIGHER_NUMERICAL_VALUE_FOREGROUND_COLOR;
        }
        
        return JStockOptions.DEFAULT_NORMAL_TEXT_FOREGROUND_COLOR;
    }
    
    // Deletes all files and subdirectories under dir.
    // Returns true if all deletions were successful.
    // If a deletion fails, the method stops attempting to delete and returns false.
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
    
        // The directory is now empty so delete it
        return dir.delete();
    }
    
    public static Stock getEmptyStock(Code code, Symbol symbol) {
        return new Stock(   code,
                            symbol,
                            "",
                            Stock.Board.Unknown,
                            Stock.Industry.Unknown,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            0,
                            0.0,
                            0.0,
                            0,
                            0.0,
                            0,
                            0.0,
                            0,
                            0.0,
                            0,
                            0.0,
                            0,
                            0.0,
                            0,
                            0.0,
                            0,
                            Calendar.getInstance()                                        
                            );                
    } 

    public static void deleteAllOldFiles(File dir, int days) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                deleteAllOldFiles(new File(dir, children[i]), days);
            }

            // Delete empty directory
            if(dir.list().length == 0)
            {
                dir.delete();
            }
        } else {
            final long today = System.currentTimeMillis();
            final long timeStamp = dir.lastModified();

            final long difMil = today - timeStamp;
            final long milPerDay = 1000*60*60*24;
            final long d = difMil / milPerDay;

            if(d >= days)
            {
                dir.delete();
            }
        }
    }

    public static String getApplicationVersionString() {
        return APPLICATION_VERSION_STRING;
    }

    // If you try to call this method at different time with same source, the
    // resultant encrypted string will be different. The best part is, it is still
    // able to be decrypted back to the original source.
    public static String encrypt(String source)
    {
        if (source.length() <= 0)
            return "";

        org.jasypt.encryption.pbe.PBEStringEncryptor pbeStringEncryptor = new org.jasypt.encryption.pbe.StandardPBEStringEncryptor();
        pbeStringEncryptor.setPassword(getJStockUUID());
        return pbeStringEncryptor.encrypt(source);
    }

    public static String decrypt(String source)
    {
        if (source.length() <= 0) {
            return "";
        }

        org.jasypt.encryption.pbe.PBEStringEncryptor pbeStringEncryptor = new org.jasypt.encryption.pbe.StandardPBEStringEncryptor();
        pbeStringEncryptor.setPassword(getJStockUUID());
        try {
            return pbeStringEncryptor.decrypt(source);
        }
        catch(org.jasypt.exceptions.EncryptionOperationNotPossibleException exp) {
            log.error(null, exp);
        }

        return "";
    }

    public static String getJStockUUID() {
        return "fe78440e-e0fe-4efb-881d-264a01be483c";
    }

    public static boolean isWindows() {
        String windowsString = "Windows";
        String osName = System.getProperty("os.name");

        if (osName == null) return false;

        return osName.regionMatches(true, 0, windowsString, 0, windowsString.length());
    }

    public static Executor getZoombiePool()
    {
        return zombiePool;
    }

    public static Indicator getLastPriceRiseAboveIndicator(double lastPrice)
    {
        final StockOperator stockOperator = new StockOperator();
        stockOperator.setType(StockOperator.Type.LastPrice);
        final DoubleConstantOperator doubleConstantOperator = new DoubleConstantOperator();
        doubleConstantOperator.setConstant(lastPrice);
        final EqualityOperator equalityOperator = new EqualityOperator();
        equalityOperator.setEquality(EqualityOperator.Equality.GreaterOrEqual);
        final SinkOperator sinkOperator = new SinkOperator();

        final Connection stockToEqualityConnection = new Connection();
        final Connection doubleConstantToEqualityConnection = new Connection();
        final Connection equalityToSinkConnection = new Connection();

        stockOperator.addOutputConnection(stockToEqualityConnection, 0);
        equalityOperator.addInputConnection(stockToEqualityConnection, 0);

        doubleConstantOperator.addOutputConnection(doubleConstantToEqualityConnection, 0);
        equalityOperator.addInputConnection(doubleConstantToEqualityConnection, 1);

        equalityOperator.addOutputConnection(equalityToSinkConnection, 0);
        sinkOperator.addInputConnection(equalityToSinkConnection, 0);

        final OperatorIndicator operatorIndicator = new OperatorIndicator();
        operatorIndicator.setName("RiseAbove");
        operatorIndicator.add(stockOperator);
        operatorIndicator.add(doubleConstantOperator);
        operatorIndicator.add(equalityOperator);
        operatorIndicator.add(sinkOperator);

        assert(operatorIndicator.getType() == OperatorIndicator.Type.AlertIndicator);
        operatorIndicator.preCalculate();

        return operatorIndicator;
    }

    public static Indicator getLastPriceFallBelowIndicator(double lastPrice)
    {
        final StockOperator stockOperator = new StockOperator();
        stockOperator.setType(StockOperator.Type.LastPrice);
        final DoubleConstantOperator doubleConstantOperator = new DoubleConstantOperator();
        doubleConstantOperator.setConstant(lastPrice);
        final EqualityOperator equalityOperator = new EqualityOperator();
        equalityOperator.setEquality(EqualityOperator.Equality.LesserOrEqual);
        final SinkOperator sinkOperator = new SinkOperator();

        final Connection stockToEqualityConnection = new Connection();
        final Connection doubleConstantToEqualityConnection = new Connection();
        final Connection equalityToSinkConnection = new Connection();

        stockOperator.addOutputConnection(stockToEqualityConnection, 0);
        equalityOperator.addInputConnection(stockToEqualityConnection, 0);

        doubleConstantOperator.addOutputConnection(doubleConstantToEqualityConnection, 0);
        equalityOperator.addInputConnection(doubleConstantToEqualityConnection, 1);

        equalityOperator.addOutputConnection(equalityToSinkConnection, 0);
        sinkOperator.addInputConnection(equalityToSinkConnection, 0);

        final OperatorIndicator operatorIndicator = new OperatorIndicator();
        operatorIndicator.setName("FallBelow");
        operatorIndicator.add(stockOperator);
        operatorIndicator.add(doubleConstantOperator);
        operatorIndicator.add(equalityOperator);
        operatorIndicator.add(sinkOperator);

        assert(operatorIndicator.getType() == OperatorIndicator.Type.AlertIndicator);
        operatorIndicator.preCalculate();
        
        return operatorIndicator;
    }

    public static void setDefaultLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (java.lang.ClassNotFoundException exp) {
            log.error(null, exp);
        }
        catch (java.lang.InstantiationException exp) {
            log.error(null, exp);
        }
        catch (java.lang.IllegalAccessException exp) {
            log.error(null, exp);
        }
        catch (javax.swing.UnsupportedLookAndFeelException exp) {
            log.error(null, exp);
        }
    }

    public static class CloudFile {
        public final File file;
        public final long checksum;
        public final long date;
        public final int version;
        private CloudFile(File file, long checksum, long date, int version) {
            this.file = file;
            this.checksum = checksum;
            this.date = date;
            this.version = version;
        }

        public static CloudFile newInstance(File file, long checksum, long date, int version) {
            return new CloudFile(file, checksum, date, version);
        }
    }

    public static CloudFile loadFromCloud(String username, String password) {
        final String url = "https://jstock-cloud.appspot.com/DownloadServlet";
        final PostMethod post = new PostMethod(url);
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            org.yccheok.jstock.engine.Utils.setHttpClientProxyFromSystemProperties(httpClient);
            org.yccheok.jstock.gui.Utils.setHttpClientProxyCredentialsFromJStockOptions(httpClient);

            NameValuePair[] data = {
                new NameValuePair("Email", username),
                new NameValuePair("Passwd", password)
            };
            post.setRequestBody(data);          
            // No ProxyAuth support yet. I do not know how to do so.
            httpClient.executeMethod(post);
            final Header header = post.getResponseHeader("Content-Type");
            if (header == null || header.getValue() == null || false == header.getValue().equalsIgnoreCase("application/octet-stream")) {
                return null;
            }

            String _checksum = post.getResponseHeader("jstock-custom-checksum").getValue();
            String _date = post.getResponseHeader("jstock-custom-date").getValue();
            String _version = post.getResponseHeader("jstock-custom-version").getValue();
            if (_checksum == null || _date == null || _version == null) {
                return null;
            }

            long checksum = Long.parseLong(_checksum);
            long date = Long.parseLong(_date);
            int version = Integer.parseInt(_version);

            inputStream = post.getResponseBodyAsStream();
            final File temp = File.createTempFile(Utils.getJStockUUID(), ".zip");
            temp.deleteOnExit();
            outputStream = new FileOutputStream(temp);
            byte buf[] = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            return CloudFile.newInstance(temp, checksum, date, version);
        }
        catch (FileNotFoundException ex) {
            log.error(null, ex);
            return null;
        }
        catch (IOException ex) {
            log.error(null, ex);
            return null;
        }
        catch(NumberFormatException ex) {
            log.error(null, ex);
            return null;
        }
        finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ex) {
                    log.error(null, ex);
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    log.error(null, ex);
                }
            }
            post.releaseConnection();
        }
    }

    public static boolean saveToCloud(String username, String password, File file) {
        final String url = "https://jstock-cloud.appspot.com/UploadServlet";
        final PostMethod post = new PostMethod(url);

        try {
            org.yccheok.jstock.engine.Utils.setHttpClientProxyFromSystemProperties(httpClient);
            org.yccheok.jstock.gui.Utils.setHttpClientProxyCredentialsFromJStockOptions(httpClient);
            Part[] parts = {
                new StringPart("Email", username),
                new StringPart("Passwd", password),
                new StringPart("Date", new Date().getTime() + ""),
                new StringPart("Checksum", org.yccheok.jstock.analysis.Utils.getChecksum(file) + ""),
                new StringPart("Version", org.yccheok.jstock.gui.Utils.getApplicationVersionID() + ""),
                new FilePart("file", file)
            };
            post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));

            // No ProxyAuth support yet. I do not know how to do so.
            httpClient.executeMethod(post);
            final String respond = post.getResponseBodyAsString();
            return (respond != null && respond.equals("OK"));
        } 
        catch (FileNotFoundException ex) {
            log.error(null, ex);
            return false;
        }
        catch (IOException ex) {
            log.error(null, ex);
            return false;
        }
        finally {
            post.releaseConnection();
        }
    }

    public static boolean isCompatible(int applicationVersionID) {
        if (applicationVersionID == 1051) {
            return true;
        }
        else if (applicationVersionID == 1052) {
            return true;
        }
        else if (applicationVersionID == 1053) {
            return true;
        }
        else if (applicationVersionID == APPLICATION_VERSION_ID) {
            return true;
        }        
        return false;
    }

    /**
     * Get response body through non-standard POST method.
     * Please refer to <url>http://stackoverflow.com/questions/1473255/is-jakarta-httpclient-sutitable-for-the-following-task/1473305#1473305</url>
     *
     * @param uri For example, http://X/%5bvUpJYKw4QvGRMBmhATUxRwv4JrU9aDnwNEuangVyy6OuHxi2YiY=%5dImage?
     * @param formData For example, [SORT]=0,1,0,10,5,0,KL,0&[FIELD]=33,38,51
     * @return the response body. null if fail.
     */
    public static String getPOSTResponseBodyAsStringBasedOnProxyAuthOption(String uri, String formData) {
        org.yccheok.jstock.engine.Utils.setHttpClientProxyFromSystemProperties(httpClient);
        org.yccheok.jstock.gui.Utils.setHttpClientProxyCredentialsFromJStockOptions(httpClient);

        final PostMethod method = new PostMethod(uri);
        final RequestEntity entity;
        try {
            entity = new StringRequestEntity(formData, "application/x-www-form-urlencoded", "UTF-8");
        } catch (UnsupportedEncodingException exp) {
            log.error(null, exp);
            return null;
        }
        method.setRequestEntity(entity);
        method.setContentChunked(false);

        final JStockOptions jStockOptions = MainFrame.getInstance().getJStockOptions();
        String respond = null;
        try {
            if (jStockOptions.isProxyAuthEnabled()) {
                /* WARNING : This chunck of code block is not tested! */
                method.setFollowRedirects(false);
                httpClient.executeMethod(method);

                int statuscode = method.getStatusCode();
                if ((statuscode == HttpStatus.SC_MOVED_TEMPORARILY) ||
                    (statuscode == HttpStatus.SC_MOVED_PERMANENTLY) ||
                    (statuscode == HttpStatus.SC_SEE_OTHER) ||
                    (statuscode == HttpStatus.SC_TEMPORARY_REDIRECT)) {
                    //Make new Request with new URL
                    Header header = method.getResponseHeader("location");
                    /* WARNING : Correct method to redirect? Shall we use POST? How about form data? */
                    HttpMethod RedirectMethod = new GetMethod(header.getValue());
                    // I assume it is OK to release method for twice. (The second
                    // release will happen in finally block). We shouldn't have an
                    // unreleased method, before executing another new method.
                    method.releaseConnection();
                    // Do RedirectMethod within try-catch-finally, so that we can have a
                    // exception free way to release RedirectMethod connection.
                    // #2836422
                    try {
                        httpClient.executeMethod(RedirectMethod);
                        respond = RedirectMethod.getResponseBodyAsString();
                    }
                    catch (HttpException exp) {
                        log.error(null, exp);
                        return null;
                    }
                    catch (IOException exp) {
                        log.error(null, exp);
                        return null;
                    }
                    finally {
                        RedirectMethod.releaseConnection();
                    }
                }
                else {
                    respond = method.getResponseBodyAsString();
                } // if statuscode = Redirect
            }
            else {
                httpClient.executeMethod(method);
                respond = method.getResponseBodyAsString();
            } //  if jStockOptions.isProxyAuthEnabled()
        }
        catch (HttpException exp) {
            log.error(null, exp);
            return null;
        }
        catch (IOException exp) {
            log.error(null, exp);
            return null;
        }
        finally {
            method.releaseConnection();
        }
        return respond;
    }

    // We prefer to have this method in gui package instead of engine. This is because it requires
    // access to JStockOptions.
    // Returns null if fail.
    public static String getResponseBodyAsStringBasedOnProxyAuthOption(String request) {
        org.yccheok.jstock.engine.Utils.setHttpClientProxyFromSystemProperties(httpClient);
        org.yccheok.jstock.gui.Utils.setHttpClientProxyCredentialsFromJStockOptions(httpClient);

        final HttpMethod method = new GetMethod(request);
        final JStockOptions jStockOptions = MainFrame.getInstance().getJStockOptions();
        String respond = null;
        try {
            if (jStockOptions.isProxyAuthEnabled()) {
                method.setFollowRedirects(false);
                httpClient.executeMethod(method);

                int statuscode = method.getStatusCode();
                if ((statuscode == HttpStatus.SC_MOVED_TEMPORARILY) ||
                    (statuscode == HttpStatus.SC_MOVED_PERMANENTLY) ||
                    (statuscode == HttpStatus.SC_SEE_OTHER) ||
                    (statuscode == HttpStatus.SC_TEMPORARY_REDIRECT)) {
                    //Make new Request with new URL
                    Header header = method.getResponseHeader("location");
                    HttpMethod RedirectMethod = new GetMethod(header.getValue());
                    // I assume it is OK to release method for twice. (The second
                    // release will happen in finally block). We shouldn't have an
                    // unreleased method, before executing another new method.
                    method.releaseConnection();
                    // Do RedirectMethod within try-catch-finally, so that we can have a
                    // exception free way to release RedirectMethod connection.
                    // #2836422
                    try {
                        httpClient.executeMethod(RedirectMethod);
                        respond = RedirectMethod.getResponseBodyAsString();
                    }
                    catch (HttpException exp) {
                        log.error(null, exp);
                        return null;
                    }
                    catch (IOException exp) {
                        log.error(null, exp);
                        return null;
                    }
                    finally {
                        RedirectMethod.releaseConnection();
                    }
                }
                else {
                    respond = method.getResponseBodyAsString();
                } // if statuscode = Redirect
            }
            else {
                httpClient.executeMethod(method);
                respond = method.getResponseBodyAsString();
            } //  if jStockOptions.isProxyAuthEnabled()
        }
        catch (HttpException exp) {
            log.error(null, exp);
            return null;
        }
        catch (IOException exp) {
            log.error(null, exp);
            return null;
        }
        finally {
            method.releaseConnection();
        }
        return respond;
    }

    public static class InputStreamAndMethod {
        public final InputStream inputStream;
        public final HttpMethod method;
        public InputStreamAndMethod(InputStream inputStream, HttpMethod method) {
            this.inputStream = inputStream;
            this.method = method;
        }
    }

    // Unlike getResponseBodyAsStringBasedOnProxyAuthOption, method must be closed
    // explicitly by caller. If not, the returned input stream will not be valid.
    // The returned InputStreamAndMethod and InputStreamAndMethod.method will always be non-null.
    //
    // InputStreamAndMethod.inputStream will be null if we fail to get any respond.
    //
    // We must always remember to close InputStreamAndMethod.method, after finish
    // reading InputStreamAndMethod.inputStream.
    public static InputStreamAndMethod getResponseBodyAsStreamBasedOnProxyAuthOption(String request) {
        org.yccheok.jstock.engine.Utils.setHttpClientProxyFromSystemProperties(httpClient);
        org.yccheok.jstock.gui.Utils.setHttpClientProxyCredentialsFromJStockOptions(httpClient);

        final GetMethod method = new GetMethod(request);
        final JStockOptions jStockOptions = MainFrame.getInstance().getJStockOptions();
        InputStreamAndMethod inputStreamAndMethod = null;
        InputStream respond = null;
        HttpMethod methodToClosed = method;

        try {
            if (jStockOptions.isProxyAuthEnabled()) {
                method.setFollowRedirects(false);
                httpClient.executeMethod(method);

                int statuscode = method.getStatusCode();
                if ((statuscode == HttpStatus.SC_MOVED_TEMPORARILY) ||
                    (statuscode == HttpStatus.SC_MOVED_PERMANENTLY) ||
                    (statuscode == HttpStatus.SC_SEE_OTHER) ||
                    (statuscode == HttpStatus.SC_TEMPORARY_REDIRECT)) {
                    //Make new Request with new URL
                    Header header = method.getResponseHeader("location");
                    GetMethod RedirectMethod = new GetMethod(header.getValue());
                    methodToClosed = RedirectMethod;
                    method.releaseConnection();
                    // Do RedirectMethod within try-catch-finally, so that we can have a
                    // exception free way to release RedirectMethod connection.
                    // #2836422                    
                    try {
                        httpClient.executeMethod(RedirectMethod);
                        respond = RedirectMethod.getResponseBodyAsStream();
                    }
                    catch (HttpException exp) {
                        log.error(null, exp);
                    }
                    catch (IOException exp) {
                        log.error(null, exp);
                    }
                }
                else {
                    methodToClosed = method;
                    respond = method.getResponseBodyAsStream();
                } // if statuscode = Redirect
            }
            else {
                methodToClosed = method;
                httpClient.executeMethod(method);
                respond = method.getResponseBodyAsStream();
            } //  if jStockOptions.isProxyAuthEnabled()
        }
        catch (HttpException exp) {
            log.error(null, exp);
        }
        catch (IOException exp) {
            log.error(null, exp);
        }
        finally {
            inputStreamAndMethod = new InputStreamAndMethod(respond, methodToClosed);
        }

        return inputStreamAndMethod;
    }

    // We prefer to have this method in gui package instead of engine. This is because it requires
    // access to JStockOptions.
    private static void setHttpClientProxyCredentialsFromJStockOptions(HttpClient httpClient) {
        final JStockOptions jStockOptions = MainFrame.getInstance().getJStockOptions();
        if (jStockOptions.isProxyAuthEnabled() == false) {
            httpClient.getState().clearCredentials();
        }
        else {
            httpClient.getState().setProxyCredentials(AuthScope.ANY, jStockOptions.getCredentials());
        }
    }

    /*
     * Get the extension of a file.
     */
    public static String getFileExtension(String s) {
        String ext = "";
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }

    /*
     * Get the extension of a file.
     */
    public static String getFileExtension(File f) {
        return getFileExtension(f.getName());
    }

    public static ApplicationInfo getLatestApplicationInfo()
    {
        final String request = org.yccheok.jstock.engine.Utils.getJStockStaticServer() + "version_information/index.txt";

        final org.yccheok.jstock.gui.Utils.InputStreamAndMethod inputStreamAndMethod = org.yccheok.jstock.gui.Utils.getResponseBodyAsStreamBasedOnProxyAuthOption(request);

        if (inputStreamAndMethod.inputStream == null) {
            inputStreamAndMethod.method.releaseConnection();
            return null;
        }

        Properties properties = new Properties();
        try {
            properties.load(inputStreamAndMethod.inputStream);
        }
        catch (IOException exp) {
            log.error(null, exp);
            return null;
        }
        catch (IllegalArgumentException exp) {
            log.error(null, exp);
            return null;

        }
        finally {
            try {
                inputStreamAndMethod.inputStream.close();
            } catch (IOException exp) {
                log.error(null, exp);
            }
            inputStreamAndMethod.method.releaseConnection();
        }

        final String applicationVersionID = properties.getProperty("applicationVersionID");
        final String windowsDownloadLink = properties.getProperty("windowsDownloadLink");
        final String linuxDownloadLink = properties.getProperty("linuxDownloadLink");
        final String macDownloadLink = properties.getProperty("macDownloadLink");
        final String solarisDownloadLink = properties.getProperty("solarisDownloadLink");
        if (applicationVersionID == null || windowsDownloadLink == null || linuxDownloadLink == null || macDownloadLink == null || solarisDownloadLink == null) {
            return null;
        }

        final int version;
       	try {
            version = Integer.parseInt(applicationVersionID);
        }
        catch (NumberFormatException exp) {
            log.error(null, exp);
			return null;
        }
		return new ApplicationInfo(version, windowsDownloadLink, linuxDownloadLink, macDownloadLink, solarisDownloadLink);
    }

    public static int getApplicationVersionID() {
        return Utils.APPLICATION_VERSION_ID;
    }

    public static String toHTML(String plainText) {
        plainText = plainText.replace(System.getProperty("line.separator"), "<br>");
        return "<html><head></head><body>" + plainText + "</body></html>";
    }

    public static <A> A fromXML(Class c, InputStream inputStream) {
        XStream xStream = new XStream();

        try {
            Object object = xStream.fromXML(inputStream);
            if (c.isInstance(object)) {
                return (A)object;
            }
        }
        catch (Exception exp) {
            log.error(null, exp);
        }
        finally {
            /* The caller shall close input stream explicitly. */
            //if (inputStream != null) {
            //    try {
            //        inputStream.close();
            //        inputStream = null;
            //    }
            //    catch (java.io.IOException exp) {
            //        log.error(null, exp);
            //        return null;
            //    }
            //}
        }

        return null;
    }

    public static <A> A fromXML(Class c, File file) {
        XStream xStream = new XStream();
        InputStream inputStream = null;

        try {
            inputStream = new java.io.FileInputStream(file);
            Object object = xStream.fromXML(inputStream);
            if (c.isInstance(object)) {
                return (A)object;
            }
        }
        catch (Exception exp) {
            log.error(null, exp);
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                    inputStream = null;
                }
                catch (java.io.IOException exp) {
                    log.error(null, exp);
                    return null;
                }
            }
        }

        return null;
    }

    public static <A> A fromXML(Class c, String filePath) {
        return (A)fromXML(c, new File(filePath));
    }

    public static boolean toXML(Object object, File file) {
        XStream xStream = new XStream();
        OutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(file);
            xStream.toXML(object, outputStream);
        }
        catch (Exception exp) {
            log.error(null, exp);
            return false;
        }
        finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                    outputStream = null;
                }
                catch (java.io.IOException exp) {
                    log.error(null, exp);
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean toXML(Object object, String filePath) {
        return toXML(object, new File(filePath));
    }

    public static String getExtraDataDirectory() {
        return org.yccheok.jstock.gui.Utils.getUserDataDirectory() + "extra" + File.separator;
    }

    public static String toHTMLFileSrcFormat(String fileName) {
        try {
            return new File(fileName).toURI().toURL().toString();
        } catch (MalformedURLException ex) {
            log.error(null, ex);
        }
        // http://www.exampledepot.com/egs/javax.swing/checkbox_AddIcon.html
        return "file:" + fileName;
    }

   public static class FileEx {
       public final File file;
       public final org.yccheok.jstock.file.Statement.Type type;
       public FileEx(File file, org.yccheok.jstock.file.Statement.Type type) {
           this.file = file;
           this.type = type;
       }
   }

    // Calling to this method will affect state of JStockOptions.
    // Returns null if no file being selected.
    public static FileEx promptSavePortfolioCSVAndExcelJFileChooser(final String suggestedFileName) {
        final JStockOptions jStockOptions = MainFrame.getInstance().getJStockOptions();
        final JFileChooser chooser = new JFileChooser(jStockOptions.getLastFileIODirectory());
        final FileNameExtensionFilter csvFilter = new FileNameExtensionFilter("CSV Documents (*.csv)", "csv");
        final FileNameExtensionFilter xlsFilter = new FileNameExtensionFilter("Microsoft Excel (*.xls)", "xls");
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(csvFilter);
        chooser.addChoosableFileFilter(xlsFilter);        

        final org.yccheok.jstock.gui.file.PortfolioSelectionJPanel portfolioSelectionJPanel = new org.yccheok.jstock.gui.file.PortfolioSelectionJPanel();
        chooser.setAccessory(portfolioSelectionJPanel);
        chooser.addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY, new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final boolean flag = ((FileNameExtensionFilter)evt.getNewValue()).equals(csvFilter);
                portfolioSelectionJPanel.setEnabled(flag);
                chooser.setSelectedFile(chooser.getFileFilter().getDescription().equals(csvFilter.getDescription()) ? new File(portfolioSelectionJPanel.getSuggestedFileName()) : new File(suggestedFileName));
            }
            
        });
        portfolioSelectionJPanel.addJRadioButtonsActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                chooser.setSelectedFile(new File(portfolioSelectionJPanel.getSuggestedFileName()));
            }

        });
        final java.util.Map<String, FileNameExtensionFilter> map = new HashMap<String, FileNameExtensionFilter>();
        map.put(csvFilter.getDescription(), csvFilter);
        map.put(xlsFilter.getDescription(), xlsFilter);

        final FileNameExtensionFilter filter = map.get(jStockOptions.getLastSavedFileNameExtensionDescription());
        if (filter != null) {
            chooser.setFileFilter(filter);
        }

        // Only enable portfolioSelectionJPanel, if CSV is being selected.
        portfolioSelectionJPanel.setEnabled(chooser.getFileFilter().getDescription().equals(csvFilter.getDescription()));
        chooser.setSelectedFile(chooser.getFileFilter().getDescription().equals(csvFilter.getDescription()) ? new File(portfolioSelectionJPanel.getSuggestedFileName()) : new File(suggestedFileName));

        while (true) {
            final int returnVal = chooser.showSaveDialog(MainFrame.getInstance());
            if (returnVal != JFileChooser.APPROVE_OPTION) {
                return null;
            }

            File file = chooser.getSelectedFile();
            if (file == null) {
                return null;
            }

            // Ensure the saved file is in correct extension. If user provide correct
            // file extension explicitly, leave it as is. If not, mutate the filename.
            final String extension = Utils.getFileExtension(file);
            if (extension.equals("csv") == false && extension.equals("xls") == false) {
                if (chooser.getFileFilter().getDescription().equals(csvFilter.getDescription())) {
                    try {
                        file = new File(file.getCanonicalPath() + ".csv");
                    } catch (IOException ex) {
                        log.error(null, ex);
                    }
                }
                else if (chooser.getFileFilter().getDescription().equals(xlsFilter.getDescription())) {
                    try {
                        file = new File(file.getCanonicalPath() + ".xls");
                    } catch (IOException ex) {
                        log.error(null, ex);
                    }
                }
                else {
                    // Impossible.
                    return null;
                }
            }

            if (file.exists()) {
                final String output = MessageFormat.format(MessagesBundle.getString("question_message_replace_old_template"), file.getName());

                final int result = javax.swing.JOptionPane.showConfirmDialog(MainFrame.getInstance(), output, MessagesBundle.getString("question_title_replace_old"), javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.QUESTION_MESSAGE);
                if (result != javax.swing.JOptionPane.YES_OPTION) {
                    continue;
                }
            }

            final String parent = chooser.getSelectedFile().getParent();
            if (parent != null) {
                jStockOptions.setLastFileIODirectory(parent);
            }

            if (Utils.getFileExtension(file).equals("csv")) {
                jStockOptions.setLastFileNameExtensionDescription(csvFilter.getDescription());                
            }
            else if (Utils.getFileExtension(file).equals("xls")) {
                jStockOptions.setLastFileNameExtensionDescription(xlsFilter.getDescription());                
            }
            else {
                // Impossible.
                return null;
            }
            
            return new FileEx(file, portfolioSelectionJPanel.getType());
        }
    }

    // This method returns the selected radio button in a button group
    public static JRadioButton getSelection(ButtonGroup group) {
        for (Enumeration e = group.getElements(); e.hasMoreElements(); ) {
            JRadioButton b = (JRadioButton)e.nextElement();
            if (b.getModel() == group.getSelection()) {
                return b;
            }
        }
        return null;
    }

    private static File promptOpenJFileChooser(FileNameExtensionFilter... fileNameExtensionFilters) {
        final JStockOptions jStockOptions = MainFrame.getInstance().getJStockOptions();
        final JFileChooser chooser = new JFileChooser(jStockOptions.getLastFileIODirectory());
        chooser.setAcceptAllFileFilterUsed(false);
        for (FileNameExtensionFilter fileNameExtensionFilter : fileNameExtensionFilters) {
            chooser.addChoosableFileFilter(fileNameExtensionFilter);
        }
        final java.util.Map<String, FileNameExtensionFilter> map = new HashMap<String, FileNameExtensionFilter>();
        for (FileNameExtensionFilter fileNameExtensionFilter : fileNameExtensionFilters) {
            map.put(fileNameExtensionFilter.getDescription(), fileNameExtensionFilter);
        }
        final FileNameExtensionFilter filter = map.get(jStockOptions.getLastSavedFileNameExtensionDescription());
        if (filter != null) {
            chooser.setFileFilter(filter);
        }
        int returnVal = chooser.showOpenDialog(MainFrame.getInstance());

        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File file = chooser.getSelectedFile();
        if (file == null || !file.exists()) {
            return null;
        }
        final String parent = chooser.getSelectedFile().getParent();
        if (parent != null) {
            jStockOptions.setLastFileIODirectory(parent);
        }
        final String extension = Utils.getFileExtension(file);
        for (FileNameExtensionFilter fileNameExtensionFilter : fileNameExtensionFilters) {
            final String[] extensions = fileNameExtensionFilter.getExtensions();
            if (extensions.length <= 0) {
                continue;
            }
            if (extension.equals(extensions[0])) {
                jStockOptions.setLastFileNameExtensionDescription(fileNameExtensionFilter.getDescription());
                return file;
            }
        }
        return null;
    }

    // Calling to this method will affect state of JStockOptions.
    // Returns null if no file being selected.
    public static File promptOpenCSVAndExcelJFileChooser() {
        final FileNameExtensionFilter csvFilter = new FileNameExtensionFilter("CSV Documents (*.csv)", "csv");
        final FileNameExtensionFilter xlsFilter = new FileNameExtensionFilter("Microsoft Excel (*.xls)", "xls");
        return promptOpenJFileChooser(csvFilter, xlsFilter);
    }

    public static File promptOpenZippedJFileChooser() {
        final FileNameExtensionFilter zippedFilter = new FileNameExtensionFilter("Zipped Files (*.zip)", "zip");
        return promptOpenJFileChooser(zippedFilter);
    }

    private static File promptSaveJFileChooser(String suggestedFileName, FileNameExtensionFilter... fileNameExtensionFilters) {
        final JStockOptions jStockOptions = MainFrame.getInstance().getJStockOptions();
        final JFileChooser chooser = new JFileChooser(jStockOptions.getLastFileIODirectory());
        chooser.setAcceptAllFileFilterUsed(false);
        for (FileNameExtensionFilter fileNameExtensionFilter : fileNameExtensionFilters) {
            chooser.addChoosableFileFilter(fileNameExtensionFilter);
        }
        chooser.setSelectedFile(new File(suggestedFileName));
        final java.util.Map<String, FileNameExtensionFilter> map = new HashMap<String, FileNameExtensionFilter>();
        for (FileNameExtensionFilter fileNameExtensionFilter : fileNameExtensionFilters) {
            map.put(fileNameExtensionFilter.getDescription(), fileNameExtensionFilter);
        }
        final FileNameExtensionFilter filter = map.get(jStockOptions.getLastSavedFileNameExtensionDescription());
        if (filter != null) {
            chooser.setFileFilter(filter);
        }
        while (true) {
            final int returnVal = chooser.showSaveDialog(MainFrame.getInstance());
            if (returnVal != JFileChooser.APPROVE_OPTION) {
                return null;
            }

            File file = chooser.getSelectedFile();
            if (file == null) {
                return null;
            }
            // Ensure the saved file is in correct extension. If user provide correct
            // file extension explicitly, leave it as is. If not, mutate the filename.
            final String extension = Utils.getFileExtension(file);
            boolean found = false;
            root:
            for (FileNameExtensionFilter fileNameExtensionFilter : fileNameExtensionFilters) {
                String[] extensions = fileNameExtensionFilter.getExtensions();
                for (String e : extensions) {
                    if (e.equals(extension)) {
                        found = true;
                        break root;
                    }
                }
            }
            if (!found) {
                for (FileNameExtensionFilter fileNameExtensionFilter : fileNameExtensionFilters) {
                    String[] extensions = fileNameExtensionFilter.getExtensions();
                    if (extensions.length <= 0) {
                        continue;
                    }
                    final String e = extensions[0];
                    if (chooser.getFileFilter().getDescription().equals(fileNameExtensionFilter.getDescription())) {
                        try {
                            if (e.startsWith(".")) {
                                file = new File(file.getCanonicalPath() + e);
                            }
                            else {
                                file = new File(file.getCanonicalPath() + "." + e);
                            }
                        } catch (IOException ex) {
                            log.error(null, ex);
                        }
                        break;
                    }
                }
            }
            if (file.exists()) {
                final String output = MessageFormat.format(MessagesBundle.getString("question_message_replace_old_template"), file.getName());

                final int result = javax.swing.JOptionPane.showConfirmDialog(MainFrame.getInstance(), output, MessagesBundle.getString("question_title_replace_old"), javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.QUESTION_MESSAGE);
                if (result != javax.swing.JOptionPane.YES_OPTION) {
                    continue;
                }
            }

            final String parent = chooser.getSelectedFile().getParent();
            if (parent != null) {
                jStockOptions.setLastFileIODirectory(parent);
            }
            final String e = Utils.getFileExtension(file);
            for (FileNameExtensionFilter fileNameExtensionFilter : fileNameExtensionFilters) {
                String[] extensions = fileNameExtensionFilter.getExtensions();
                if (extensions.length <= 0) {
                    continue;
                }
                if (e.equals(extensions[0])) {
                    jStockOptions.setLastFileNameExtensionDescription(fileNameExtensionFilter.getDescription());
                    break;
                }
            }
            return file;
        }
    }

    public static File promptSaveZippedJFileChooser(String suggestedFileName) {
        final FileNameExtensionFilter zippedFilter = new FileNameExtensionFilter("Zipped Files (*.zip)", "zip");
        return promptSaveJFileChooser(suggestedFileName, zippedFilter);
    }

    // Calling to this method will affect state of JStockOptions.
    // Returns null if no file being selected.
    public static File promptSaveCSVAndExcelJFileChooser(String suggestedFileName) {
        final FileNameExtensionFilter csvFilter = new FileNameExtensionFilter("CSV Documents (*.csv)", "csv");
        final FileNameExtensionFilter xlsFilter = new FileNameExtensionFilter("Microsoft Excel (*.xls)", "xls");
        return promptSaveJFileChooser(suggestedFileName, csvFilter, xlsFilter);
    }

    public static class ApplicationInfo
    {
        public final int applicationVersionID;
        public final String windowsDownloadLink;
        public final String linuxDownloadLink;
        public final String macDownloadLink;
        public final String solarisDownloadLink;

        public ApplicationInfo(int applicationVersionID, String windowsDownloadLink, String linuxDownloadLink, String macDownloadLink, String solarisDownloadLink) {
            this.applicationVersionID = applicationVersionID;
            this.windowsDownloadLink = windowsDownloadLink;
            this.linuxDownloadLink = linuxDownloadLink;
            this.macDownloadLink = macDownloadLink;
            this.solarisDownloadLink = solarisDownloadLink;
        }
    }

    private static volatile List<String> NTPServers = null;

	// We will use this as directory name. Do not have space or special characters.
    private static final String APPLICATION_VERSION_STRING = "1.0.5";

    // For About box comparision on latest version purpose.
    // 1.0.5d
    // Remember to update isCompatible method.
    private static final int APPLICATION_VERSION_ID = 1054;

    private static Executor zombiePool = Executors.newFixedThreadPool(Utils.NUM_OF_THREADS_ZOMBIE_POOL);

    private static final int NUM_OF_THREADS_ZOMBIE_POOL = 4;

    private static final HttpClient httpClient;

    static {
        MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager = new MultiThreadedHttpConnectionManager();
        multiThreadedHttpConnectionManager.getParams().setMaxTotalConnections(128);
        multiThreadedHttpConnectionManager.getParams().setDefaultMaxConnectionsPerHost(128);
        httpClient = new HttpClient(multiThreadedHttpConnectionManager);
        multiThreadedHttpConnectionManager.getParams().setMaxConnectionsPerHost(httpClient.getHostConfiguration(), 128);
    }
    private static final Log log = LogFactory.getLog(Utils.class);
}
