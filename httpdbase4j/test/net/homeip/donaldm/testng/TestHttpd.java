package net.homeip.donaldm.testng;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import net.homeip.donaldm.httpdbase4j.FileHttpd;
import net.homeip.donaldm.httpdbase4j.FileStringTemplateHandler;
import net.homeip.donaldm.httpdbase4j.Http;
import net.homeip.donaldm.httpdbase4j.HttpResponse;
import net.homeip.donaldm.httpdbase4j.Httpd;
import net.homeip.donaldm.httpdbase4j.ArchiveHttpd;
import net.homeip.donaldm.httpdbase4j.ArchiveStringTemplateHandler;
import net.homeip.donaldm.httpdbase4j.Postable;
import net.homeip.donaldm.httpdbase4j.Request;
import net.homeip.donaldm.httpdbase4j.TemplatableAdapter;
import net.homeip.donaldm.testng.templates.SelItem;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.testng.annotations.Test;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import de.schlichtherle.io.File;

public class TestHttpd
//====================
{  
   private java.io.File m_homeDir = new File("test/htdocs");
   private String m_homeArchiveDirName = "test/htdocs";
   private int m_port = 8088;
   private Httpd m_httpd = null;
   
   private static final String LF = System.getProperty("line.separator");
   
   @Test(groups = { "static", "All" }) 
   public void startStatic() throws IOException, NoSuchFieldException
   //----------------------------------------------------
   {
      m_httpd = new FileHttpd(m_homeDir, 10);
      m_httpd.setVerbose(true);
      m_httpd.setLogger(System.err);
      m_httpd.start(m_port, "/");
      
      setupStaticFiles(m_homeDir);
   }
   
   // Also tests combined css/js requests
   private void setupStaticFiles(java.io.File homeDir) throws FileNotFoundException
   //-----------------------------------------------------------------------
   {
      deleteDir(homeDir);
      homeDir.mkdirs();
      java.io.File f = new File(homeDir, "basic.html");
      f.delete();
      PrintWriter ps = new PrintWriter(f);
      ps.print(getBasicHTML());
      ps.close();
      
      f = new File(m_homeDir, "style.css");
      f.delete();
      ps = new PrintWriter(f);
      ps.print(getPageCSS(0));
      ps.close();
      
      f = new File(m_homeDir, "style2.css");
      f.delete();
      ps = new PrintWriter(f);
      ps.print(getPageCSS(1));
      ps.close();
      
      f = new File(m_homeDir, "page.html");
      f.delete();
      ps = new PrintWriter(f);
      ps.print(getPageHTML());
      ps.close();

   }
   
   @Test(groups = { "static", "All" }, dependsOnMethods = { "startStatic" })
   public void testStatic() throws IOException, NoSuchFieldException
   //----------------------
   {      
      testBasic(m_port, "http");
      testPage(m_port, "http");
      stopServer();
   }

   
   @Test(groups = { "staticssl", "All" })
   public void startSSL() throws IOException, NoSuchFieldException, 
               UnrecoverableKeyException, KeyManagementException, KeyStoreException, 
               NoSuchAlgorithmException, CertificateException
   //----------------------------------------------------
   {
      m_httpd = new FileHttpd(m_homeDir, 10);      
      m_httpd.setVerbose(true);  
      m_httpd.setLogger(System.err);
      m_httpd.start(m_port, "/", null, null);
      
      setupStaticFiles(m_homeDir);
   }
   
// Won't work until TODO: HttpdLite-00001 is resolved
   @Test(groups = { "staticssl", "All" }, dependsOnMethods = { "startSSL" })
   public void testStaticSSL() throws IOException, NoSuchFieldException
   //------------------------------------------------------------------
   {      
      testBasic(m_port, "https");
      testPage(m_port, "https");
      stopServer();
   }
   
   @Test(groups = { "overide", "All" }) 
   public void startOveride() throws IOException, NoSuchFieldException
   //----------------------------------------------------
   {
      m_httpd = new TestOverideHttpd(m_homeDir, 10);
      m_httpd.setVerbose(true);
      m_httpd.setLogger(System.err);
      m_httpd.start(m_port, "/");
      
      deleteDir(m_homeDir);
      m_homeDir.mkdirs();
      java.io.File f = new File(m_homeDir, "test-overide.txt");
      PrintWriter ps = new PrintWriter(f);
      ps.print("Overide test success");
      ps.close();
      f = new File(m_homeDir, "no.such.file");
      f.delete();
   }
   
   class TestOverideHttpd extends FileHttpd
   //==================================
   {
      private String content = null;
      
      
      public TestOverideHttpd(java.io.File homeDir, int poolSize)
      {
         super(homeDir, poolSize);         
      }
            
      public Request onFileNotFound(long id, HttpExchange ex, Request request)
      {
         if (request.getName().compareTo("no.such.file") == 0)
         {
            java.io.File f = new File(m_homeDir, "no.such.file");
            try
            {
               PrintWriter ps = new PrintWriter(f);
               ps.print("I think therefore I am.");
               ps.close();
            }
            catch (Exception e)
            {
               return null;               
            }
            return request;            
         }
         return null;
      }
      
      public HttpResponse onServeHeaders(long id, HttpExchange ex, Request request)
      {
         if (request.getName().compareTo("test-overide.txt") == 0)
         {
            BufferedReader br = new BufferedReader(
                                    new InputStreamReader(request.getStream()));
            content = "<HTML><HEAD></HEAD><BODY><P>";
            try
            {
               content += br.readLine() + "</P></BODY></HTML>";
            }
            catch (Exception e)
            {
               System.err.println("Error reading stream: " + e.getMessage());
               return null;
            }
            content += "</P></BODY></HTML>";
            HttpResponse r = new HttpResponse(ex, Http.HTTP_OK);
            r.addHeader("Content-Type", Http.MIME_HTML);
            r.addHeader("Content-Length", Integer.toString(content.length())); 
            return r;
         }
         return super.onServeHeaders(id, ex, request);
      }

      public InputStream onServeBody(long id, HttpExchange ex, Request request)
      {
         if (request.getName().compareTo("test-overide.txt") == 0)
            return new BufferedInputStream(new ByteArrayInputStream(
                                           content.getBytes()));
         return super.onServeBody(id, ex, request);
      }      
      
      public String getContent() { return content; } 
   }
   
   @Test(groups = { "overide", "All" }, dependsOnMethods = { "startOveride" })
   public void testOveride() throws IOException, NoSuchFieldException
   //------------------------------------------------------------------
   {      
      testOveride(m_port);
      testNotFound(m_port);
      stopServer();
   }
   
   /* Tests
    * 1. templates (using class per template) 
    * 2. Content within jar
    */
   @Test(groups = { "templatemultijar", "All" }) 
   public void startTemplateMulti() throws IOException, NoSuchFieldException
   //----------------------------------------------------
   {
      setupTemplateJarFiles();
      File jar = new File("test/htdocs.jar");
      assert (jar.exists() && (jar.isArchive())) 
              : "test/htdocs.jar is not a valid archive";
      java.io.File jarFile = new java.io.File("test/htdocs.jar");
      //m_httpd = new ArchiveHttpd(jarFile, m_homeArchiveDirName, 10, 10);
      m_httpd = new ArchiveHttpd(m_homeArchiveDirName, 10, 10);
      m_httpd.setVerbose(true);
      m_httpd.setLogger(System.err);
      // Java handlers for the template files are in testng/templeates
      ArchiveStringTemplateHandler stHandler = new ArchiveStringTemplateHandler(m_httpd, 
                                        "net.homeip.donaldm.testng.templates", 
                                        jar);
      stHandler.setDebug(true);
      m_httpd.addHandler(".st", stHandler);
      m_httpd.start(m_port, "/");      
   }
   
/* For testing purposes create a jar with the content and add it to the 
   classpath. Normally you would just create the content in a directory
   that would be added to the jar eg in Netbeans src/resources/htdocs */
   private void setupTemplateJarFiles() throws IOException
   //-----------------------------------------------------------------------
   {      
      java.io.File f = new File("test/htdocs.jar");
      f.delete();
      JarOutputStream jar = new JarOutputStream(new BufferedOutputStream(
                              new java.io.FileOutputStream(f))); 
      JarEntry jarEntry = new JarEntry("test/htdocs/pagetemplate.st");
      jar.putNextEntry(jarEntry);
      String s = getPageTemplateHtml(true);
      jar.write(s.getBytes());
            
      jarEntry = new JarEntry("test/htdocs/options.st");
      jar.putNextEntry(jarEntry);
      s = getPageTemplateMacro();
      jar.write(s.getBytes());
      jar.close();      
      
//    Load jar into classpath
      add2Cp(f);
      displayClasspath();
      //System.out.println("Path = " + getPathForResource("test/htdocs/options.st"));
   }
   
   @SuppressWarnings("unchecked")
   private boolean add2Cp(java.io.File f)
   //----------------------------
   {
      Class[] parameters = new Class[] { URL.class };
      URLClassLoader sysloader = (URLClassLoader) ClassLoader
      .getSystemClassLoader();
      Class<URLClassLoader> sysclass = URLClassLoader.class;
      //String urlPath = "jar:file://" + f.getAbsolutePath() + "!/";      
      try
      {
         URL url = new URL("file://"  + f.getAbsolutePath());
         Method method = sysclass.getDeclaredMethod("addURL", parameters);
         method.setAccessible(true);
         
         method.invoke(sysloader, new Object[] { url });
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         return false;
      }
      return true;
   }
   
   private void displayClasspath()
   //-----------------------------
   {
      ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
      URL[] urls = ((URLClassLoader)sysClassLoader).getURLs();
      for(int i=0; i< urls.length; i++)
         System.out.println(urls[i].getFile());
   }
   
   private String getPathForResource( String filename )
   {
       ClassLoader loader = this.getClass().getClassLoader();
       URL url = loader.getResource( filename );
       String path = null;
       if (url != null) {
           path = url.getFile();
       }
       return path;
   }
   
   @Test(groups = { "templatemultijar", "All" }, 
         dependsOnMethods = { "startTemplateMulti" })
   public void testTemplateMulti() throws IOException, NoSuchFieldException
   //------------------------------------------------------------------
   {      
      URL url = new URL("http", "localhost", m_port, "/pagetemplate.st");
      URLConnection conn = url.openConnection();
      int len = conn.getContentLength();
      
      java.io.File f = new java.io.File(System.getProperty("java.io.tmpdir", "tmp"));
      java.io.File dir = new java.io.File(f, "templates");
      deleteDir(dir);
      dir.mkdirs();
      StringTemplateGroup group = new StringTemplateGroup("testgroup", 
                                                dir.getAbsolutePath());
      f = new java.io.File(dir, "pagetemplate.st");
      java.io.BufferedWriter bw = new java.io.BufferedWriter(new java.io.FileWriter(f));
      bw.write(getPageTemplateHtml(true));
      bw.close();
      f = new java.io.File(dir, "test/htdocs");
      f.mkdirs();
      f = new java.io.File(f, "options.st");
      bw = new java.io.BufferedWriter(new java.io.FileWriter(f));
      bw.write(getPageTemplateMacro());
      bw.close();
      StringTemplate st = group.getInstanceOf("pagetemplate");
      SelItem[] selList = new SelItem[6];
      for (int i=0; i<5; i++)
         selList[i] = new SelItem("OPT" + i, "Option " + (i+1));
      selList[5] = new SelItem("OPT5", "Option 6", true);
      st.setAttribute("selList", selList);
      if (System.getProperty("os.name").toLowerCase().contains("windows"))
         st.setAttribute("welcome", "Welcome Dumbass");
      else
         st.setAttribute("welcome", "Welcome Master");
      String expectedContents = null;
      try
      {
         expectedContents = st.toString();
      }
      catch (Exception e)
      {
         System.err.println(e.getMessage());
         e.printStackTrace(System.err);
      }
      
      assert len > 0 : "Content length for URL " + url.toString() + " is 0";
      assert expectedContents.length() == len : "Content length for URL " + url.toString() + 
      " != length of content (" + len + " != " + expectedContents.length() + ")";
      Object o = conn.getContent();
      assert o != null : "URL " + url.toString() + " content null";
      assert o instanceof InputStream : "URL " + url.toString() + " content is not" +
                                   "an InputStream (" + o.getClass().getName() + ")";
      InputStream in = (InputStream) o;
      byte[] b = readArray(in, len);
      in.close();
      
      String contents = new String(b);
      assert expectedContents.compareTo(contents) == 0 
      : "page.html: Content does not match expected content" + LF + 
      expectedContents + LF + contents;

      stopServer();
   }
   
   @Test(groups = { "templatesingle", "All" }) 
   public void startTemplateSingle() throws IOException, NoSuchFieldException
   //----------------------------------------------------
   {
      setupTemplateFiles();
      m_httpd = new FileHttpd(m_homeDir, 10);
      m_httpd.setVerbose(true);
      m_httpd.setLogger(System.err);
      FileStringTemplateHandler stHandler = new FileStringTemplateHandler(m_httpd, 
      new TemplatableAdapter()
      {
         private SelItem[] selList = null;
         
         private void setTemplateAttributes(StringTemplate template, 
                                            Request request)
         // ---------------------------------------------------------
         {
            System.out.println(template.getName());
            if (template.getName().compareTo("pagetemplate") == 0)
            {
               if (selList == null)
               {
                  selList = new SelItem[6];
                  for (int i=0; i<5; i++)
                     selList[i] = new SelItem("OPT" + i, "Option " + (i+1));
                  selList[5] = new SelItem("OPT5", "Option 6", true);
               }
               template.setAttribute("selList", selList);
               if (System.getProperty("os.name").toLowerCase().contains("windows"))
                  template.setAttribute("welcome", "Welcome Dumbass");
               else
                  template.setAttribute("welcome", "Welcome Master");
            }
         }
         
         public java.io.File templateFile(StringTemplate template, Request request, 
                                          StringBuffer mimeType, File dir)
         {
            setTemplateAttributes(template, request);
            return super.templateFile(template, request, mimeType, dir);
         }
         
         public String templateString(StringTemplate template, Request request, 
                                      StringBuffer mimeType)
         {
            setTemplateAttributes(template, request);
            return super.templateString(template, request, mimeType);
         }
         
         public InputStream templateStream(StringTemplate template, 
                                           Request request,
                                           StringBuffer mimeType)
         {
            setTemplateAttributes(template, request);
            return super.templateStream(template, request, mimeType);
         }
      });
      stHandler.setDebug(true);
      m_httpd.addHandler(".st", stHandler);
      m_httpd.start(m_port, "/");      
   }
   
   private void setupTemplateFiles() throws IOException
   //--------------------------------------------------
   {  
      deleteDir(m_homeDir);
      m_homeDir.mkdirs();
      java.io.File f = new java.io.File(m_homeDir, "pagetemplate.st");
      f.delete();
      PrintWriter ps = new PrintWriter(f);
      ps.print(getPageTemplateHtml(false));
      ps.close();
      f = new java.io.File(m_homeDir, "include");
      f.mkdirs();
      f = new java.io.File(f, "options.st");
      f.delete();
      ps = new PrintWriter(f);
      ps.print(getPageTemplateMacro());
      ps.close();
   }

   @Test(groups = { "templatesingle", "All" }, 
            dependsOnMethods = { "startTemplateSingle" })
   public void testTemplateSingle() throws IOException, NoSuchFieldException
   //------------------------------------------------------------------
   {      
      URL url = new URL("http", "localhost", m_port, "/pagetemplate.st");
      URLConnection conn = url.openConnection();
      int len = conn.getContentLength();
      
      StringTemplateGroup group = new StringTemplateGroup("testgroup", 
                                                m_homeDir.getAbsolutePath());
      StringTemplate st = group.getInstanceOf("pagetemplate");
      SelItem[] selList = new SelItem[6];
      for (int i=0; i<5; i++)
         selList[i] = new SelItem("OPT" + i, "Option " + (i+1));
      selList[5] = new SelItem("OPT5", "Option 6", true);
      st.setAttribute("selList", selList);
      if (System.getProperty("os.name").toLowerCase().contains("windows"))
         st.setAttribute("welcome", "Welcome Dumbass");
      else
         st.setAttribute("welcome", "Welcome Master");
      String expectedContents = null;
      try
      {
         expectedContents = st.toString();
      }
      catch (Exception e)
      {
         System.err.println(e.getMessage());
         e.printStackTrace(System.err);
      }
      
      assert len > 0 : "Content length for URL " + url.toString() + " is 0";
      //assert expectedContents.length() == len : "Content length for URL " + url.toString() + 
      //" != length of content (" + len + " != " + expectedContents.length() + ")";
      Object o = conn.getContent();
      assert o != null : "URL " + url.toString() + " content null";
      assert o instanceof InputStream : "URL " + url.toString() + " content is not" +
                                   "an InputStream (" + o.getClass().getName() + ")";
      InputStream in = (InputStream) o;
      byte[] b = readArray(in, len);
      in.close();
      
      String contents = new String(b);
      assert expectedContents.compareTo(contents) == 0 
      : "page.html: Content does not match expected content" + LF + 
      expectedContents + LF + contents;

      stopServer();
   }

   @Test(groups = { "post", "All" }) 
   public void startPost() throws IOException, NoSuchFieldException
   //----------------------------------------------------
   {
      m_httpd = new FileHttpd(m_homeDir, 10);
      m_httpd.setVerbose(true);      
      m_httpd.setLogger(System.err);
      m_httpd.addPostHandler("/post", new Postable()
      {

         public Object onHandlePost(long id, HttpExchange ex, Request request, 
                                  HttpResponse response, java.io.File dir, 
                                  Object... extraParameters)
         {
            Headers getParameters = request.getGETParameters();
            String v1 = getParameters.getFirst("get1");
            String v2 = getParameters.getFirst("get2");
            java.io.BufferedWriter bw = null;            
            java.io.File f = new java.io.File(m_homeDir, "post.txt");
            try
            {
               bw = new java.io.BufferedWriter(new java.io.FileWriter(f));
               bw.write(v1); bw.newLine();
               bw.write(v2); bw.newLine();
            }
            catch (IOException e)
            {
               e.printStackTrace();
               return null;
            }
            finally
            {
               if (bw != null) try { bw.close(); } catch (Exception e) {}
            }
            
            Headers postParameters = request.getPOSTParameters();
            assert postParameters != null : "No POST parameters ";
            v1 = postParameters.getFirst("key1");
            v2 = postParameters.getFirst("key2");            
            try
            {
               bw = new java.io.BufferedWriter(new java.io.FileWriter(f, true));
               bw.write(v1); bw.newLine();
               bw.write(v2); bw.newLine();
            }
            catch (Exception e)
            {
               return null;
            }
            finally
            {
               if (bw != null) try { bw.close(); } catch (Exception e) {}
            }
            return f;            
         }         
      });
      m_httpd.start(m_port, "/"); 
   }
   
   @Test(groups = { "post", "All" }, dependsOnMethods = { "startPost" })
   public void testPost() throws IOException, NoSuchFieldException
   //------------------------------------------------------------------
   {
      String data = URLEncoder.encode("key1", "UTF-8") + "=" + 
                                      URLEncoder.encode("Value 1", "UTF-8");
      data += "&" + URLEncoder.encode("key2", "UTF-8") + "=" + 
                                      URLEncoder.encode("Value 2", "UTF-8");
  
      // Send data
      URL url = new URL("http://localhost:" + m_port + "/post?get1=" + 
               URLEncoder.encode("Get 1", "UTF-8") + "&get2=" + 
               URLEncoder.encode("Get 2", "UTF-8"));
      System.out.println(url.toExternalForm());
      URLConnection conn = url.openConnection();
      conn.setDoOutput(true);
      java.io.OutputStreamWriter wr = new java.io.OutputStreamWriter(
                                                        conn.getOutputStream());
      wr.write(data);
      wr.flush();
  
      // Get the response
      java.io.BufferedReader rd = new java.io.BufferedReader(
              new java.io.InputStreamReader(conn.getInputStream()));
      String line;
      int lc = 0;
      while ((line = rd.readLine()) != null) 
      {
         System.out.println(line);
         switch (lc++)
         {
            case 0:
               assert line.compareTo("Get 1") == 0 : "Invalid line 1" + line;
               break;
            case 1:
               assert line.compareTo("Get 2") == 0 : "Invalid line 2" + line;
               break;
            case 2:
               assert line.compareTo("Value 1") == 0 : "Invalid line 1" + line;
               break;
            case 3:
               assert line.compareTo("Value 2") == 0 : "Invalid line 2" + line;
               break;
            
         }
      }
      wr.close();
      rd.close();
   }
   
   private void stopServer()
   //-----------------------
   {
      try { Thread.sleep(1000); } catch (Exception e) {}
      m_httpd.stop(1);
      m_httpd = null;
      
   }
   
   private void testBasic(int port, String protocol) throws IOException
   //-------------------------------------------------------------------
   {
      URL url = new URL(protocol, "localhost", port, "/basic.html");
      URLConnection conn = url.openConnection();
      int len = conn.getContentLength();
      assert len > 0 : "Content length for URL " + url.toString() + " is " + len;
      String expectedContents = getBasicHTML();
      
      Object o = conn.getContent();
      assert o != null : "URL " + url.toString() + " content null";
      assert o instanceof InputStream : "URL " + url.toString() + " content is not" +
                                   "an InputStream (" + o.getClass().getName() + ")";
      java.io.InputStream in = (java.io.InputStream) o;
      byte[] b = readArray(in, len);
      in.close();
      
      String contents = new String(b);
      /*
      assert expectedContents.length() == len : "Content length for URL " + url.toString() + 
      " != length of content (" + len + " != " + expectedContents.length() + ")";*/
      System.out.printf("%d %d\n", expectedContents.length(), contents.length());
      assert (expectedContents.compareTo(contents) == 0) 
         : "basic.html: Content does not match expected content" + LF + 
         expectedContents + LF + contents;
   }
   
   private void testPage(int port, String protocol) throws IOException
   //------------------------------------------------------------------
   {
      URL url = new URL(protocol, "localhost", port, "/page.html");
      URLConnection conn = url.openConnection();
      int len = conn.getContentLength();
      String expectedContents = getPageHTML();
      assert len > 0 : "Content length for URL " + url.toString() + " is 0";
      assert expectedContents.length() == len : "Content length for URL " + url.toString() + 
      " != length of content (" + len + " != " + expectedContents.length() + ")";
      Object o = conn.getContent();
      assert o != null : "URL " + url.toString() + " content null";
      assert o instanceof java.io.InputStream : "URL " + url.toString() + " content is not" +
                                   "an InputStream (" + o.getClass().getName() + ")";
      java.io.InputStream in = (java.io.InputStream) o;
      byte[] b = readArray(in, len);
      in.close();
      
      String contents = new String(b);
      assert expectedContents.compareTo(contents) == 0 
      : "page.html: Content does not match expected content" + LF + 
      expectedContents + LF + contents;

   }
   
   private void testOveride(int port) throws IOException
   //-------------------------------------------------------------------
   {
      TestOverideHttpd ohttpd = (TestOverideHttpd) m_httpd;
      URL url = new URL("http", "localhost", port, "/test-overide.txt");
      URLConnection conn = url.openConnection();
      int len = conn.getContentLength();
      assert len > 0 : "Content length for URL " + url.toString() + " is " + len;
      String expectedContents = ohttpd.getContent();
      
      Object o = conn.getContent();
      assert o != null : "URL " + url.toString() + " content null";
      assert o instanceof InputStream : "URL " + url.toString() + " content is not" +
                                   "an InputStream (" + o.getClass().getName() + ")";
      InputStream in = (InputStream) o;
      byte[] b = readArray(in, len);
      in.close();
      
      String contents = new String(b);
      /*
      assert expectedContents.length() == len : "Content length for URL " + url.toString() + 
      " != length of content (" + len + " != " + expectedContents.length() + ")";*/
      System.out.printf("%d %d\n", expectedContents.length(), contents.length());
      assert (expectedContents.compareTo(contents) == 0) 
         : "basic.html: Content does not match expected content" + LF + 
         expectedContents + LF + contents;
   }
   
   private void testNotFound(int port)  throws IOException
   //------------------------------------------------------
   {
      URL url = new URL("http", "localhost", port, "/no.such.file");
      URLConnection conn = url.openConnection();
      int len = conn.getContentLength();
      assert len > 0 : "Content length for URL " + url.toString() + " is " + len;
      String expectedContents = "I think therefore I am.";
      
      Object o = conn.getContent();
      assert o != null : "URL " + url.toString() + " content null";
      assert o instanceof InputStream : "URL " + url.toString() + " content is not" +
                                   "an InputStream (" + o.getClass().getName() + ")";
      InputStream in = (InputStream) o;
      byte[] b = readArray(in, len);
      in.close();
      
      String contents = new String(b);
      /*
      assert expectedContents.length() == len : "Content length for URL " + url.toString() + 
      " != length of content (" + len + " != " + expectedContents.length() + ")";*/
      System.out.printf("%d %d\n", expectedContents.length(), contents.length());
      assert (expectedContents.compareTo(contents) == 0) 
         : "basic.html: Content does not match expected content" + LF + 
         expectedContents + LF + contents;
   }
   
   private byte[] readArray(InputStream in, int len) throws IOException
   //------------------------------------------------------------------
   {
      byte[] b = new byte[len]; 
      int l = in.read(b);
      int off = l;
      while (off < len)
      {
         l = in.read(b, off, len-off);
         if (l < 0) break;
         off += l;
      }
      return b;
   }
   
   private String getBasicHTML()
   //---------------------------
   {
      return 
"<HTML>" + LF + 
"  <HEAD>" + LF +
"  </HEAD>" + LF +
"  <BODY" + LF +
"     <P> Hello World !!!! </P>" + LF +
"  </BODY>" + LF + 
"  </HTML>";            
   }
   
   private String getPageCSS(int i)
   //------------------------------
   {
      switch (i)
      {  case 0:
            return
"hr {color: sienna}" + LF + 
"p " + LF + 
"{" + LF + 
"   margin-left: 20px;"+ LF + 
"   text-align: center;" + LF +
"   font-size: 10pt" + LF + 
"}" + LF;// +
//"body {background-image: url(\"images/notfound.gif\")}";
         case 1:
            return
"a { color:red; }" + LF;            
      }
      return "";
   }
   
   private String getPageHTML()
   //-------------------------
   {
      return
"<HTML>" + LF + 
"  <HEAD>" + LF +
"     <link rel=\"stylesheet\" type=\"text/css\" href=\"style.css!+!style2.css\" />" + LF +
"  </HEAD>" + LF +
"  <BODY>" + LF +
"     <P> Hello World !!!! </P>" + LF +
"     <HR>" + LF + 
"  </BODY>" + LF + 
"  </HTML>";            
   }
   
   
   public String getPageTemplateHtml(boolean isInJar)
   //-----------------------------------------------
   {
      return
"<HTML>" + LF + 
"  <HEAD>" + LF +
"  </HEAD>" + LF +
"  <BODY" + LF +
"     <P>$welcome$</P>" + LF +
"     <SELECT SIZE=\"1\" name=\"sel\" id=\"sel\">" + LF +
      ((isInJar) ?
"        $selList:test/htdocs/options()$" :
"        $selList:include/options()$") + LF +
"     </SELECT>" + LF +
"     <HR>" + LF + 
"  </BODY>" + LF + 
"  </HTML>";
   }

   public String getPageTemplateMacro()
   //---------------------------------
   {
      return 
"$if(it.selected)$" + LF +
"   <option value=\"$it.key$\" selected> $it.value$</option>" + LF +
"$else$" + LF +
"   <option value=\"$it.key$\"> $it.value$</option>" + LF +
"$endif$";
   }

   private boolean deleteDir(java.io.File dir)
   //---------------------------------------
   {  // a symbolic link has a different canonical path than its actual path,
      // unless it's a link to itself
      java.io.File candir;
      try { candir = dir.getCanonicalFile(); } catch (IOException e) { return false; }        
      if (!candir.equals(dir.getAbsoluteFile())) 
         return false;
  
      java.io.File[] files = candir.listFiles();
      if (files != null) 
         {  for (int i = 0; i < files.length; i++) 
               {  java.io.File file = files[i];
                  boolean deleted = file.delete();
                  if (! deleted) 
                     if (file.isDirectory()) 
                        deleteDir(file);
  
               }
         }
      return dir.delete();  
   }
   
}
