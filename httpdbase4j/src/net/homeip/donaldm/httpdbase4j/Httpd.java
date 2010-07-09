/*
HttpdBase4J: An embeddable Java web server framework that supports HTTP, HTTPS, 
templated content and serving content from inside a jar or archive.
Copyright (C) 2007 Donald Munro

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not,see http://www.gnu.org/licenses/lgpl.txt
*/

package net.homeip.donaldm.httpdbase4j;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import de.schlichtherle.io.FileInputStream;
import java.net.URL;
import java.security.SecureRandom;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * An embeddable Java web server that supports HTTP, HTTPS, templated
 * content and serving content from inside a jar or archive (Supported types: jar, zip, 
 * tar, tar.gz, tar.bz2).
 * Based on the com.sun.net.httpserver.classes only available in Java 1.6. 
 * (@see com.sun.net.httpserver or @link http://java.sun.com/javase/6/docs/jre/api/net/httpserver/spec/overview-summary.html
 * Also supports templated generation of content using the StringTemplate 
 * library @link http://www.stringtemplate.org
 * </p>
 * <b>Usage</b><br>
 * <p>
 * To create a simple embedded web server on port 8088 with a home directory
 * at homeDir in the local filesystem and a root url at / ie / maps onto homeDir:
 * </p><br>
 * <code>
 * homeDir = new java.io.File("./htdocs");
 * httpd = new FileHttpd(homeDir, 10); // Creates a server with a threadpool of 10
 * httpd.setLogger(System.err); // Log to console 
 * httpd.start(8088, "/");
 * </code>
 * <p>
 * To create a HTTPS embedded web server on port 8088 with a home directory
 * at homeDir in the local filesystem and a root url at / ie / maps onto homeDir:
 * </p><br>
 * <code>
 * homeDir = new java.io.File("./htdocs");
 * httpd = new FileHttpd(homeDir, 10);      
 * m_httpd.start(8089, "/", keystore, password);
 * </code><br>
 * <p>
 * HttpdBase4J also supports serving content from inside the class path ie from
 * in the jar file. 
 * </p>
 * <p>
 * The code below creates a server with content located in directory
 * resources/htdocs in a jar in the classpath (normally the main jar).
 * </p>
 * <code>
 * httpd = new ArchiveHttpd("/resources/htdocs", 10);
 * httpd.start(8088, "/");
 * </code><br>
 * <p>
 * HttpdBase4J also supports serving content from a specified archive file
 * Supported formats: jar, zip, tar, tar.gz and tar.bz2.  
 * </p>
 * The code below creates a server with content located in directory 
 * resources/htdocs in archive file content.zip.
 * </p>
 * <code>
 * httpd = new ArchiveHttpd(new File("content.zip"), "/resources/htdocs", 10);
 * httpd.start(8088, "/");
 * </code><br>
 * <p>
 * Templated content can also be created. Currently the StringTemplate library
 * (@see http://www.stringtemplate.org) is used but it should be relatively
 * easy to create user derived classes for other template implementations.
 * </p>
 * <p> 
 * To create an HTTP embedded web server on port 8088 serving templated content 
 * from resources/htdocs in the classpath and having template file handler 
 * (A Java class implenting the Templatable interface that is used to fill the 
 * templates) in net.homeip.donaldm.test.templates:
 * </p><br>
 * <code>
 * httpd = new ArchiveHttpd("resources/htdocs", 10);
 * StringTemplateHandler stHandler = new ArchiveStringTemplateHandler(httpd, 
                                        "net.homeip.donaldm.test.templates");
 * httpd.addHandler(".st", stHandler); // .st extension = template files
 * httpd.start(m_port, "/");
 * </code><br>
 * <p>
 * To implement a embedded web server with POST handler for URL /post:
 * </p><br>
 * <code>
 * m_httpd = new FileHttpd(homeDir, 10);
 * httpd.addPostHandler("/post", postHandler); postHandler implements Postable
 * httpd.start(8088, "/");
 * </code>
 * <br>
 * <p>The Httpd class also provides many overidable methods:</p><br>
 * <code>
 * httpd = new TestOverideHttpd(m_homeDir, 10);
 * httpd.start(m_port, "/");
 * <br>
 * class TestOverideHttpd
 * {
 *    public HttpResponse onServeHeaders(long id, HttpExchange ex, Request request)
 *    {
 *       //Create or amend content 
 *    }
 *    public InputStream onServeBody(long id, HttpExchange ex, Request request)
 *    {
 *       // Return amended or created content
 *    }
 * 
 * }
 * </code><br>
 * <p>
 * Some of the overidable methods include: onAllowDirectoryBrowse, onCreateExecutor,
 * onCreateRequestHandler, onFileNotFound, onAllowDirectoryBrowse, onListDirectory,
 * onPreServe, onPostServe etc. See the documention of HttpHandleable</p><br>
 * @see FileHttpd
 * @see ArchiveHttpd
 * @see HttpHandleable
 * @see Postable
 * @author Donald Munro
 */ 
abstract public class Httpd implements HttpHandleable, Postable
//==============================================================
{
   /**
    * Thread model for the server. SINGLE, MULTI or POOL.  
    */
   public enum ThreadModel 
   {
      SINGLE, MULTI, POOL
   };   
   
   public static String EOL = System.getProperty("line.separator");
   
   /**
    * The HTTP server class. May be either HttpServer or HttpsServer
    */
   protected HttpServer                  m_http             = null;

   protected int                         m_port             = 8080;

   protected HttpContext                 m_context          = null;

   protected HttpHandler                 m_requestHandler   = null;

   /**
    * Verbose logging switch
    */
   protected boolean                     m_isVerbose        = false;

   /**
    * The file names recognised as default to use if no filename is
    * specified in the URI. Defaults to index.html, index.htm
    */
   protected ArrayList<String>           m_defaultFiles     = new ArrayList<String>();

   /**
    * Maps file extensions onto handlers
    * @see Httpd#addHandler
    */
   protected Map<String, HttpHandleable> m_handlerMap       = 
                                          new HashMap<String, HttpHandleable>();
   /**
    * Maps POST handlers onto URLs or extensions.
    * @see Httpd#addPostHandler
    */
   protected Map<String, Postable>       m_postHandlerMap   = 
                                                new HashMap<String, Postable>();   
   
   protected boolean                     m_mustCache = true;
   
   /*
    * The threading model used by this server.
    */
   protected ThreadModel                 m_threadModel      = ThreadModel.MULTI;

   /*
    * The executor used to create server threads.
    */
   protected ExecutorService             m_executor         = null;

   protected int                         m_poolMax          = 50;

   protected int                         m_poolSize         = 5;
   
   protected boolean                     m_isStarted        = false;

   static private AtomicLong             m_sequence         = new AtomicLong(0);

   public Httpd()
   //------------
   {
      m_defaultFiles.add("index.html");
      m_defaultFiles.add("index.htm");
   }
   
   public boolean isStarted() { return m_isStarted; }
   
   public void setCaching(boolean b) { m_mustCache = b; }
   
   public boolean getCaching() { return m_mustCache; }
   
   abstract public String getHomePath();
   
   protected void setDefaultPoolSizes()
   //-----------------------------------
   {
      switch (m_threadModel)
      {
         case MULTI:
            m_poolSize = 5;
            m_poolMax = Integer.MAX_VALUE;
            break;

         case POOL:
            m_poolSize = 10;
            m_poolMax = 10;
            break;
      }
   }
   

   /**
    * 
    * @param b true to set verbose mode on otherwise false
    */
   public void setVerbose(boolean b)
   {
      m_isVerbose = b;
   }

   /**
    * 
    * @return verbose mode
    */
   public boolean getVerbose()
   {
      return m_isVerbose;
   }

   /**
    * 
    * @return The next id to use as a transaction id
    */
   public static long getNextSequence()
   {
      return m_sequence.getAndIncrement();
   }

   /**
    * @return The TCP port for this server
    */
   public int getPort()
   //------------------
   {
      return m_port;
   }
   
   /**
    * Set the Authenticator class used for HTTP authentication.
    * @param authenticator The authenticator to use 
    */ 
   public void setAuthenticator(Authenticator authenticator)
   //-------------------------------------------------------
   {
      if (m_context != null) m_context.setAuthenticator(authenticator);
   }
   
   /**
    * Should only be used before calling start.
    * @param threadModel The threading model to use
    */
   public void setThreadModel(ThreadModel threadModel)
   //-------------------------------------------------
   {
      if (m_isStarted) return;
      m_threadModel = threadModel;
      m_poolSize = 5;
      m_poolMax = 50;
   }

   /**
    * Sets thread model to POOL and sets the constant pool size to the specified 
    * value.
    * Should only be used before calling start.
    * @param size The size of the fixed size thread pool
    */
   public void setThreadPool(int size)
   //---------------------------------
   {
      if (m_isStarted) return;
      m_threadModel = ThreadModel.POOL;
      m_poolSize = size;
      m_poolMax = size;
   }

   /**
    * Sets thread model to POOL and sets the pool size and max pool size to the 
    * specified values.
    * Should only be used before calling start.
    * @param size The size of the thread pool
    * @param max The maximum size of the thread pool
    */
   public void setThreadPool(int size, int max)
   //-----------------------------------------
   {
      if (m_isStarted) return;
      m_threadModel = ThreadModel.POOL;
      m_poolSize = size;
      m_poolMax = max;
   }

   /**
    * Add a handler for an extension. 
    * @param extension The file extension (including the .)
    * @return The handler for <i>extension</i> or null if no handler found
    **/
   public HttpHandleable getHandler(String extension)
   //------------------------------------------------
   {
      extension = extension.trim();
      if (! extension.startsWith("."))
         extension = "." + extension;
      return m_handlerMap.get(extension);
   }
   
   /**
    * Add a handler for an extension. 
    * @param extension The file extension (including the .)
    * @param handler A class implementing the HttpHandleable interface.
    **/
   public void addHandler(String extension, HttpHandleable handler)
   //----------------------------------------------
   {
      extension = extension.trim();
      if (! extension.startsWith("."))
         extension = "." + extension;
      m_handlerMap.put(extension, handler);
   }

   /**
    * Remove a handler for an extension. 
    * @param extension The file extension (including the .)
    * @return The handler that was removed
    **/
   public HttpHandleable removeHandler(String extension)
   //----------------------------------------------
   {
      extension = extension.trim();
      if (! extension.startsWith("."))
         extension = "." + extension;
      return m_handlerMap.remove(extension);
   }
   
   /**
    * Add a POST handler. If the name parameter starts with a full stop ('.')
    * then it is assumed to be a file extension and maps all requests with that 
    * extension to the supplied handler. If name does not start with a . then
    * it maps a specific request URI onto the supplied handler.<br>
    * EG httpd.addPostHandler(".st", myHandler);<br>
    *    httpd.addPostHandler("/invoices/new.st", myHandler);<br>
    * @param name A file extension (including the .) or a full request uri
    * @param handler A class implementing the Postable interface.
    *
    **/
   public void addPostHandler(String name, Postable handler)
   //------------------------------------------------------------------
   {
      m_postHandlerMap.put(name, handler);
   }

   /**
    * Remove a POST handler.
    * @see Httpd#addPostHandler
    * @param name A file extension (including the .) or a full request uri
    * @return The POST handler that was removed
    */
   public Postable removePostHandler(String name)
   //-------------------------------------------------
   {
      return m_postHandlerMap.remove(name);
   }

   /**
    * Start a standard (non HTTPS) server on the supplied port using the 
    * supplied path as the root URI path. 
    * @param port The TCP port for the server
    * @param root The root URI path. The first character of path must be '/'.
    * @return true if the server started successfully otherwise false
    * @throws java.io.IOException 
    * @throws java.lang.NoSuchFieldException 
    */
   public boolean start(int port, String root) throws IOException,
            NoSuchFieldException
   //-----------------------------------------
   {
      return start(port, root, null);
   }

   /**
    * Start a standard (non HTTPS) server on the supplied port using the 
    * supplied path as the root URI path and the supplied Authenticator class
    * for HTTP authentication. 
    * @param port The TCP port for the server
    * @param root The root URI path. The first character of path must be '/'.
    * @param authenticator The Authenticator derived class to use for 
    * authentication.
    * @see com.sun.net.httpserver.Authenticator or 
    * @link http://java.sun.com/javase/6/docs/jre/api/net/httpserver/spec/overview-summary.html
    * @return true if the server started successfully otherwise false
    * @throws java.io.IOException 
    * @throws java.lang.NoSuchFieldException 
    */
   public boolean start(int port, String root, Authenticator authenticator)
            throws IOException, NoSuchFieldException
   //----------------------------------------------------------------------
   {
      m_http = HttpServer.create(new InetSocketAddress(port), 20);
      m_http.setExecutor(onCreateExecutor());
      m_requestHandler = onCreateRequestHandler();
      m_context = m_http.createContext(root, m_requestHandler);
      if (authenticator != null) m_context.setAuthenticator(authenticator);
      m_http.start();
      m_port = port;
      m_isStarted = true;
      return true;
   }

   /**
    * Start an HTTPS server on the supplied port using the 
    * supplied root as the root URI path and the supplied keystore and password
    * for SSL certificates.
    * @param port The TCP port for the server
    * @param root The root URI path. The first character of path must be '/'.
    * @param keystore SSL certificate keystore
    * @param password SSL certificate 
    * @return true if the server started successfully otherwise false
    * @throws java.security.KeyStoreException 
    * @throws java.security.NoSuchAlgorithmException 
    * @throws java.security.cert.CertificateException 
    * @throws java.security.UnrecoverableKeyException 
    * @throws java.security.KeyManagementException 
    * @throws java.lang.NoSuchFieldException 
    * @throws java.io.FileNotFoundException 
    * @throws java.io.IOException 
    * @see javax.net.ssl.SSLContext
    */
   public boolean start(int port, String root, String keystore, String password)
            throws KeyStoreException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException,
            KeyManagementException, NoSuchFieldException,
            FileNotFoundException, IOException
   //--------------------------------------------------------------------------
   {
      return start(port, root, null, keystore, password, "SSL");
   }

   /**
    * Start an HTTPS server on the supplied port using the
    * supplied root as the root URI path and the supplied keystore and password
    * for SSL certificates.
    * @param port The TCP port for the server
    * @param root The root URI path. The first character of path must be '/'.
    * @param authenticator The Authenticator derived class to use for
    * authentication.
    * @see com.sun.net.httpserver.Authenticator or
    * @link http://java.sun.com/javase/6/docs/jre/api/net/httpserver/spec/overview-summary.html
    * @param keystore SSL certificate keystore
    * @param password SSL certificate
    * @return true if the server started successfully otherwise false
    * @throws java.security.KeyStoreException
    * @throws java.security.NoSuchAlgorithmException
    * @throws java.security.cert.CertificateException
    * @throws java.security.UnrecoverableKeyException
    * @throws java.security.KeyManagementException
    * @throws java.lang.NoSuchFieldException
    * @throws java.io.FileNotFoundException
    * @throws java.io.IOException
    * @see javax.net.ssl.SSLContext
    */
   public boolean start(int port, String root, Authenticator authenticator,
            String keystore, String password) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException,
            UnrecoverableKeyException, KeyManagementException,
            NoSuchFieldException, FileNotFoundException, IOException
   //--------------------------------------------------------------------------
   {
      return start(port, root, authenticator, keystore, password, "SSL");
   }

   /**
    * Start an HTTPS server on the supplied port using the 
    * supplied root as the root URI path and the supplied keystore and password
    * for SSL certificates.
    * @param port The TCP port for the server
    * @param root The root URI path. The first character of path must be '/'.
    * @param authenticator The Authenticator derived class to use for 
    * authentication.
    * @see com.sun.net.httpserver.Authenticator or 
    * @link http://java.sun.com/javase/6/docs/jre/api/net/httpserver/spec/overview-summary.html
    * @param keystore SSL certificate keystore
    * @param password SSL certificate
    * @param SSL encryption type (SSL, TLS, SSLv3)
    * @return true if the server started successfully otherwise false
    * @throws java.security.KeyStoreException 
    * @throws java.security.NoSuchAlgorithmException 
    * @throws java.security.cert.CertificateException 
    * @throws java.security.UnrecoverableKeyException 
    * @throws java.security.KeyManagementException 
    * @throws java.lang.NoSuchFieldException 
    * @throws java.io.FileNotFoundException 
    * @throws java.io.IOException 
    * @see javax.net.ssl.SSLContext
    */
   public boolean start(int port, String root, Authenticator authenticator,
            String keystore, String password, final String sslType) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException,
            UnrecoverableKeyException, KeyManagementException,
            NoSuchFieldException, FileNotFoundException, IOException
   //--------------------------------------------------------------------------
   {
      SSLContext ssl = onCreateSSLConfiguration(keystore, password, sslType);
      if (ssl == null) return false;
      
      m_http = HttpsServer.create(new InetSocketAddress(port), 20);
      HttpsConfigurator configurator = new HttpsConfigurator(ssl)
      {

         @Override
         public void configure(HttpsParameters params)
         {
            SSLContext c = getSSLContext();
            params.setNeedClientAuth(false);
            params.setWantClientAuth(false);
            onSetSSLParameters(c, params);
         }
      };

      ((HttpsServer) m_http).setHttpsConfigurator(configurator);
      m_http.setExecutor(onCreateExecutor());
      m_requestHandler = onCreateRequestHandler();
      m_context = m_http.createContext(root, m_requestHandler);
      if (authenticator != null) m_context.setAuthenticator(authenticator);
      m_http.start();
      m_port = port;
      m_isStarted = true;
      return true;
   }

   public java.io.File createKeystore(String sslType)
          throws NoSuchAlgorithmException, KeyStoreException, FileNotFoundException, IOException,
                 CertificateException, UnrecoverableKeyException, KeyManagementException
   //---------------------------------------------------------------------------------------------
   {
      return createKeystore(sslType, null, "*", null);
   }

   final static public String DEFAULT_KEYSTORE_PASSWORD = "stupidwasteoftime";

  /*
   * Attempts to create a SSL certificate file
  */
   public java.io.File createKeystore(String sslType, java.io.File keyStoreFile, String host,
                                      String password)
   //---------------------------------------------------------------------------------------------
   {
      if (password == null)
         password = DEFAULT_KEYSTORE_PASSWORD;
      
      final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
      if (keyStoreFile == null)
      {
         String home = System.getProperty("user.home", System.getProperty("user.dir", null));
         java.io.File homeDir = new java.io.File(home, ((isWindows) ? "" : ".") + "HttpBase4J");
         keyStoreFile = new java.io.File(homeDir, "ssl-cert");
      }
      String jdkHome = System.getProperty("java.home", null);
      java.io.File binDir = new java.io.File(jdkHome, "bin");
      java.io.File keytool = null;
      if (isWindows)
         keytool = new java.io.File(binDir, "keytool.exe");
      else
         keytool = new java.io.File(binDir, "keytool"); // mac ?
      if (! keytool.exists())
      {
         java.io.File old = keytool;
         keytool = new java.io.File(binDir, "jre");
         keytool = new java.io.File(binDir, "keytool");
         if (! keytool.exists())
         {
            Httpd.Log(LogLevel.ERROR, "Could not find keytool executable in. Tried " +
                      old.getAbsolutePath() + " and " + keytool.getAbsolutePath(), null);
            return null;
         }
      }

      Runtime r = Runtime.getRuntime();
      Process p = null;
      
      if (keyStoreFile.isDirectory())
         Http.deleteDir(keyStoreFile);
      else
         keyStoreFile.delete();
      keyStoreFile.mkdirs();
      keyStoreFile = new java.io.File(keyStoreFile, "ssl-key");
      keyStoreFile.delete();

      String command = keytool.getAbsolutePath() +
                       " -selfcert -keystore " + keyStoreFile.getAbsolutePath() +
                       " -genkey -dname cn=" + host + ",ou=AlQueada,o=AlQueada,c=US " +
                       "-alias HttpdBase4J -keyalg RSA " +
                       "-storepass " + password + " -keypass " + password;
      StringBuilder stdout = new StringBuilder(),stderr = new StringBuilder();
      if (r != null)
      {
         BufferedReader input = null, error = null;
         int status = 1;
         try
         {
            p = r.exec(command);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line;
            if (stdout != null) while ((line = input.readLine()) != null)
               stdout.append(line);
            if (stderr != null) while ((line = error.readLine()) != null)
               stderr.append(line);
            p.waitFor();
            status = p.exitValue();
            if (status != 0)
            {
               Httpd.Log(LogLevel.ERROR, "Error creating SSL certificate:" +
                                         System.getProperty("line.separator") +
                                         command +
                                         System.getProperty("line.separator") +
                                         stdout.toString() +
                                         System.getProperty("line.separator") +
                                         stderr.toString(), null);
               return null;
            }
         }
         catch (Exception e)
         {
            Httpd.Log(LogLevel.ERROR, "Exception creating SSL certificate: " +
                      command +
                      System.getProperty("line.separator") +
                      stdout.toString() +
                      System.getProperty("line.separator") +
                      stderr.toString(), null);
            return null;
         }
         finally
         {
            if (input != null) try { input.close(); } catch (Exception e) {}
            if ( error != null) try { error.close(); } catch (Exception e) {}            
         }
      }

      // keytool -selfcert -keystore cert/ssl-key -genkey
      //         -dname "cn=Ossama,ou=AlQueada,o=AlQueada,c=US"
      //         -alias HttpdBase4J -keyalg RSA -storepass mypassword -keypass mypassword
      if (! keyStoreFile.exists())
      {
         Httpd.Log(LogLevel.ERROR, "Error creating keystore file " + keyStoreFile.getAbsolutePath(),
                   null);
         return null;
      }
      System.setProperty("javax.net.ssl.trustStore", keyStoreFile.getAbsolutePath());
      System.setProperty("javax.net.ssl.trustStorePassword", password);
      return keyStoreFile;
   }

   /**
    * Overide to create SSLContext.
    * @param keystore SSL certificate keystore
    * @param password SSL certificate 
    * @return The SSLContext to use for HTTPS connections.
    * @throws java.security.NoSuchAlgorithmException 
    * @throws java.security.KeyStoreException 
    * @throws java.io.FileNotFoundException 
    * @throws java.io.IOException 
    * @throws java.security.cert.CertificateException 
    * @throws java.security.UnrecoverableKeyException 
    * @throws java.security.KeyManagementException 
    */   
   protected SSLContext onCreateSSLConfiguration(String keystore, String password, String sslType)
             throws NoSuchAlgorithmException, KeyStoreException, FileNotFoundException, IOException,
                    CertificateException, UnrecoverableKeyException, KeyManagementException
   //-----------------------------------------------------------------------------------------------
   {
      SSLContext ssl = null;
      if ( (password == null) || (password.length() < 6) )
         password = DEFAULT_KEYSTORE_PASSWORD;
      if (sslType == null)
         sslType = "SSL";
      if (keystore == null)
      {         
//         TrustManager[] trustAllCerts = new TrustManager[]
//         //===============================================
//         {
//            new X509TrustManager()
//            {
//               @Override
//               public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
//
//               @Override
//               public void checkClientTrusted(
//                        java.security.cert.X509Certificate[] certs, String authType) {}
//
//               @Override
//               public void checkServerTrusted(
//                        java.security.cert.X509Certificate[] certs, String authType) {}
//            }
//         };
//         SSLContext sc = SSLContext.getInstance("SSL");
//         //SSLContext sc = SSLContext.getInstance("TLS");
//         sc.init(null, trustAllCerts, new SecureRandom());
//         HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
//         //ssl = SSLContext.getInstance("SSLv3");
//         //ssl = SSLContext.getInstance("TLS");
//         //ssl.init(null, trustAllCerts, new java.security.SecureRandom());
//         //HttpsURLConnection.setDefaultSSLSocketFactory(ssl.getSocketFactory());
//         HttpsURLConnection.setDefaultHostnameVerifier(
//         new HostnameVerifier()
//         {
//            @Override
//            public boolean verify(String host, SSLSession sslSession) {  return true; }
//         });
         keystore = createKeystore(sslType, null, "*", password).getAbsolutePath();
      }

      char[] passphrase = password.toCharArray();
      KeyStore ks = KeyStore.getInstance("JKS");
      ks.load(new FileInputStream(keystore), passphrase);
      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(ks, passphrase);
      TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
      tmf.init(ks);
      ssl = SSLContext.getInstance(sslType);
      ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
      return ssl;
   } 

   /** Called every time a HTTPS connection is made to set the SSL 
    * parameters. Overide to customize these parameters.
    * 
    * @param c The SSLContext for this connection
    * @param params The HTTPS paramaters for this connection
    */
   protected void onSetSSLParameters(SSLContext c, HttpsParameters params)
   //---------------------------------------------------------------------
   {
      //InetSocketAddress remote = params.getClientAddress()  
      SSLParameters sslparams = c.getDefaultSSLParameters();
      //if (remote.equals (...) ) 
      params.setSSLParameters(sslparams);
   }

   /**
    * Attempt to stop this server.
    * 
    * @param timeout Amount of time (in seconds) to wait for the server to stop.
    * @return true if stopped succesfully, otherwise false
    */
   public boolean stop(int timeout)
   //------------------------------
   {
      if (! m_isStarted) return false;
      if (timeout < 0) timeout = 5;
      m_http.stop(timeout);
      ServerSocket ss = null;
      try
      {
         ss = new ServerSocket(m_port, 5);         
      }
      catch (IOException ex)
      {
         return false;
      }
      finally
      {
         if (ss != null)
            try { ss.close(); } catch (Exception e) {}
      }
      m_isStarted = false; 
      return true;
   }

   /**
    * Overide to create the request handler
    * @return The request handler
    */
   abstract protected HttpHandler onCreateRequestHandler();

   private LinkedBlockingQueue<Runnable> m_blockingQueue = 
                                          new LinkedBlockingQueue<Runnable>();
   
   private ThreadFactory m_threadFactory = new ThreadFactory()
   //=========================================================
   {
      public Thread newThread(Runnable r)
      {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("Httpd:" + m_port);
        return t;

      }
   };

   /**
    * Overide to create the thread executor.
    * @return The ExecutorService thread executor service to use for thread
    * creation.
    * @throws java.lang.NoSuchFieldException 
    */
   protected ExecutorService onCreateExecutor() throws NoSuchFieldException
   //---------------------------------------------------------------------
   {
      switch (m_threadModel)
      {
         case SINGLE:
            m_executor = Executors.newSingleThreadExecutor(m_threadFactory);
            break;

         case MULTI:
            //m_executor = Executors.newCachedThreadPool();
            m_blockingQueue = new LinkedBlockingQueue<Runnable>();
            m_executor = new ThreadPoolExecutor(m_poolSize, Integer.MAX_VALUE,
                     50000L, TimeUnit.MILLISECONDS, m_blockingQueue,
                     m_threadFactory);
            break;

         case POOL:
            //m_executor = Executors.newFixedThreadPool(m_threadPoolCount);
            m_blockingQueue = new LinkedBlockingQueue<Runnable>();
            m_executor = new ThreadPoolExecutor(m_poolSize, m_poolMax, 50000L,
                     TimeUnit.MILLISECONDS, m_blockingQueue, m_threadFactory);
            break;

         default:
            m_executor = null;
            throw new NoSuchFieldException("Invalid thread model "
                     + m_threadModel.toString());
      }
      return m_executor;
   }

   /**
    * Default directory list. Overide to customise the directory listing
    * output.
    * @param request The Request instance
    * @return A String ncontaining the HTML for the directory listing output.
    */
   @Override
   public String onListDirectory(Request request)
   //--------------------------------------------
   {
      StringBuilder html = new StringBuilder("<html><head><title>"
               + "Directory listing: " + request.getURI().toASCIIString()
               + "</title></head><body>");
      html.append("<H1>");
      html.append("Directory: ");
      html.append(request.getName());
      html.append("</H1>");

      TreeSet<DirItemInterface> set = request
               .getDirListDirectories(DirItemInterface.SORTBY.NAME);
      for (Iterator<DirItemInterface> i = set.iterator(); i.hasNext();)
      {
         DirItemInterface di = i.next();
         html.append("<a href=\"");
         html.append(di.getName());
         html.append("\">");
         html.append(" [");
         html.append(di.getName());
         html.append("] </a><br>");
      }

      set = request.getDirListFiles(DirItemInterface.SORTBY.NAME);
      for (Iterator<DirItemInterface> i = set.iterator(); i.hasNext();)
      {
         DirItemInterface di = i.next();
         html.append("<a href=\"");
         html.append(di.getName());
         html.append("\">");
         html.append(" [");
         html.append(di.getName());
         html.append("] </a><br>");
      }

      html.append("</body></html>");
      return html.toString();
   }
   
   @Override
   public boolean onIsCacheable(long id, HttpExchange ex, Request request)
   //---------------------------------------------------------------------
   {
      if (! m_mustCache) return false;
      return request.isCacheable();
   }
   
   @Override
   public java.io.File onGetCachedFile(long id, HttpExchange ex, Request request)
   //--------------------------------------------------------------------
   {
      return null;
   }
   
   @Override
   public boolean onPreServe(long id, HttpExchange ex, Request request)
   {
      return true;
   }

   @Override
   public HttpResponse onServeHeaders(long id, HttpExchange ex, Request request)
   {
      return null;
   }

   @Override
   public InputStream onServeBody(long id, HttpExchange ex, Request request)
   {
      return null;
   }

   @Override
   public void onPostServe(long id, HttpExchange ex, Request request,
            boolean isOK)
   {}

   @Override
   public Request onFileNotFound(long id, HttpExchange ex, Request request)
   {
      return null;
   }

   @Override
   public Object onHandlePost(long id, HttpExchange ex, Request request,
            HttpResponse response, java.io.File dir, Object... extraParameters)
   {
      return null;
   }

   public boolean onAllowDirectoryBrowse(String directory)
   //-------------------------------------------------
   {
      return true;
   }

   public void addDefaultFile(String file)
   //----------------------------------------
   {
      if (m_defaultFiles == null)
         m_defaultFiles = new ArrayList<String>();
      file = file.trim();
      if (! m_defaultFiles.contains(file))
         m_defaultFiles.add(file);
   }

   static private PrintStream m_logStream   = null;
   static protected Object    m_logger      = null;
   static protected Method    m_errorMethod = null;
   static protected Method    m_infoMethod  = null;
   static protected Method    m_debugMethod = null;

   /**
    * Logger level. ERROR, INFO or DEBUG
    */
   static protected enum LogLevel 
   {
      ERROR, INFO, DEBUG
   }

   /**
    * Set a logger to use. The logger must either implement the slf4j interface 
    * (@link http://www.slf4j.org) or be a PrintStream instance for logging. 
    * This method uses reflection to invoke logging methods so applications 
    * which don't require logging don't need any logging jar files in their 
    * classpath. Note: this probably won't be a good idea in high volume 
    * applications.
    *
    * @param ologger The logger to use (must implement the slf4j interface 
    * (http://www.slf4j.org) (or be an instance of PrintStream for simple
    * "logging" to the console. Note if setLogger is called twice, once with a
    * PrintStream and once with an slf4j logger then it will print to the 
    * stream and log although the same effect can normally be achieved by 
    * configuring the logger.
    *
    * @return true if logger successfully set otherwise false.
    */
   @SuppressWarnings("unchecked")
   public boolean setLogger(Object ologger)
   //--------------------------------------
   {
      if (ologger instanceof PrintStream)
      {
         m_logStream = (PrintStream) ologger;
         return true;
      }
      try
      {
         Class loggerClass = Class.forName("org.slf4j.Logger");
         m_logger = ologger;
         Class[] parameterTypes = new Class[2];
         parameterTypes[0] = Class.forName("java.lang.String");
         parameterTypes[1] = Class.forName("java.lang.Throwable");
         m_errorMethod = loggerClass.cast(m_logger).getClass().getMethod(
                  "error", parameterTypes);
         m_infoMethod = loggerClass.cast(m_logger).getClass().getMethod("info",
                  parameterTypes);
         m_debugMethod = loggerClass.cast(m_logger).getClass().getMethod(
                  "debug", parameterTypes);
      }
      catch (Throwable t)
      {
         System.err.println("setLogger: Error getting logging methods "
                  + " Check org.slf4j.Logger is in class path ("
                  + t.getMessage() + ")");
         t.printStackTrace(System.err);
         m_logger = null;
         return false;
      }
      return true;
   }

   /**
    * Log errors, information or warnings.    
    *
    * @param level The log level chosen from the LogLevelenum viz ERROR, INFO or
    *              DEBUG 
    * @param message - The message to be logged
    * @param e - The Java exception to be logged. Can be null.
    */
   static protected void Log(LogLevel level, String message, Throwable e)
   //--------------------------------------------------------------------
   {
      if (m_logStream != null)
      {
         synchronized (m_logStream)
         {
            m_logStream.println(level.toString() + ": " + message);
            if (e != null) e.printStackTrace(m_logStream);
         }
      }
      if (m_logger == null) return;
      try
      {
         Object[] parameters = new Object[2];
         parameters[0] = message;
         parameters[1] = e;
         switch (level)
         {
            case ERROR:
               m_errorMethod.invoke(m_logger, parameters);
               break;
            case INFO:
               m_infoMethod.invoke(m_logger, parameters);
               break;
            case DEBUG:
               m_debugMethod.invoke(m_logger, parameters);
               break;
         }
      }
      catch (Exception ee)
      {
         System.err.println("Could not invoke logger method ("
                  + ee.getMessage() + ")");
         ee.printStackTrace(System.err);
      }
   }
   
   /*
   public static void main(String[] args) throws UnrecoverableKeyException,
            KeyManagementException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException,
            FileNotFoundException, NoSuchFieldException, IOException
   //--------------------------------------
   {
      if (args.length <= 0)
      {
         System.err.println("Home directory must be specified as first arg");
         System.exit(1);
      }
      java.io.File homeDir = new java.io.File(args[0]);

      int port = 8088;
      if (args.length > 1) port = Integer.parseInt(args[1]);
      Httpd httpsd = new Httpd(homeDir, 10);
      httpsd.setVerbose(true);
      httpsd.setLogger(System.err);
      //httpsd.start(port, "/", "cert/ssl-key", "bigwaves");
      httpsd.start(port, "/", null, null);
      System.out.println("Httpds running on port " + port);
      
      Httpd httpd = new Httpd(homeDir, 10);
      httpd.setVerbose(true);
      httpd.setLogger(System.err);
      httpd.start(port-1, "/");
      System.out.println("Httpd running on port " + (port-1));
   }

    public static void main( String[] args )
    {
    java.io.File propertiesFile = null, homeDir =new java.io.File("."), 
    SSLhomeDir =new java.io.File("."), 
    templateDir =new java.io.File(homeDir, "templates");
    int port = 6502, sslport = 8086;
    // keytool -selfcert -keystore cert/ssl-key -genkey -dname "cn=Me, ou=ALQueada, o=AlQueada, c=US" -alias NanoHTTPD -keyalg RSA
    String keystore = "cert/ssl-key", password = "bigwaves";
    if (args.length > 0)
    {
    propertiesFile = new java.io.File(args[0]);
    if (! propertiesFile.exists())
    {
    System.err.println("Property file " + 
    propertiesFile.getAbsolutePath() +" not found");
    return;
    }
    else
    {
    Properties properties = new Properties();
    try 
    {
    properties.load(new FileInputStream(propertiesFile));
    }
    catch (Exception e)
    {
    System.err.println("Error reading from properties file " + 
    propertiesFile.getAbsolutePath() + ". " + 
    e.getMessage());
    return;
    }
    homeDir = new java.io.File(properties.getProperty("home", "."));
    SSLhomeDir = new java.io.File(properties.getProperty("sslhome", "."));
    templateDir =new java.io.File(properties.getProperty("templates", 
    new java.io.File(homeDir, "templates").getAbsolutePath())); 
    port = Integer.parseInt(properties.getProperty("port", "6502"));
    sslport = Integer.parseInt(properties.getProperty("sslport", "8086"));
    keystore = properties.getProperty("keystore", "cert/ssl-key");
    password = properties.getProperty("password", "bigwaves");
    }
    }
    
    StringTemplateHandler httpd =  null, httpsd = null;
    try
    {
    httpd = new StringTemplateHandler(homeDir, 
    "net.homeip.donaldm.TestNG.handlers",
    10, 20);
    httpsd = new StringTemplateHandler(homeDir, 
    "net.homeip.donaldm.TestNG.handlers",
    10, 20);
    }
    catch (Exception e)
    {
    System.err.println("Exception creating server");
    e.printStackTrace(System.err);
    return;
    }
    httpd.setLogger(System.out);
    httpsd.setLogger(System.out);
    
    try
    {
    if ( (port > 0) && (httpd.start(port, "/")) )
    System.out.println("HTTP server running on port " + port + ". Home "
    + homeDir.getAbsolutePath());
    if ( (sslport > 0) && (httpsd.start(sslport, "/", password, keystore )) )
    System.out.println("HTTPS server running on port " + sslport + 
    ". Home " + homeDir.getAbsolutePath());
    }
    catch (Exception e)
    {
    System.err.println("Exception starting server on  port " + port);
    e.printStackTrace(System.err);
    return;
    }
    
    
    try { System.in.read(); } catch( Throwable t ) {};
    httpd.stop(10000);
    httpsd.stop(10000);
    }

    }*/
}
