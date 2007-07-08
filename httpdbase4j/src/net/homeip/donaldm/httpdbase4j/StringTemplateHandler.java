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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Provides a base handler class for templating based on the StringTemplate 
 * library (http://www.stringtemplate.org/)
 * The Templates (which must have an extension of '.st') may be 
 * processed in three ways:
 * <ul>
 *    <li>
 *       <p>
 *       The user may specify a template processing class that implements the 
 *       Templatable interface. The Templatable.processTemplate method of that class 
 *       will then be called for each template.
 *       </p>
 *    </li>
 *    <li>
 *    <p>
 *       If a template processing class is not specified in the constructor then 
 *       the  StringTemplateHandler class uses itself as a template processing 
 *       class. The default Templatable handler method loads a class from 
 *       the Java package in m_templateJavaPackage (which must be specified in the 
 *       constructor) with the same name as the template. This class must have an 
 *       empty public constructor and must implement the Templatable interface.
 *       The class name (excluding the extension) must either be exactly the same 
 *       as the template (ie have the same case too) or the first letter can be 
 *       uppercase. For example if the the template is listpersons.st then 
 *       listpersons.class or Listpersons.class will be found.
 *    </p>
 *    </li>
 *    <li>
 *    <p>
 *       The user can overide the StringTemplateHTTPD class and provide his own 
 *       processTemplate method
 *    </p>
 *    </li>
 * </ul>
 * The StringTemplate library (@see http://www.stringtemplate.org) requires 
 * stringtemplate.jar and antlr-2.7.7.jar be in the classpath.
 * @see FileStringTemplateHandler
 * @see ArchiveStringTemplateHandler
 * @author Donald Munro
 */
abstract public class StringTemplateHandler 
         implements HttpHandleable, Postable, Templatable
//========================================================
{
   /**
    * Template group for the home directory (also handles sub-dirs of the 
    * home dir */
   protected StringTemplateGroup  m_templateGroup = null;
   
   /**
    * Template processing and generation class.
    */
   protected Templatable          m_templateProcessor = null;
   
   /**
    * The package name for per-template processing and generation Java classes.
    **/
   protected String               m_templateJavaPackage = "";

   protected Map<Long, Object>    m_resultMap = Collections.synchronizedMap(
                                                   new HashMap<Long, Object>());
   
   protected Map<Long, File>      m_postMap = Collections.synchronizedMap(
                                                   new HashMap<Long, File>());
     
   protected Httpd                m_httpd = null;
   
   private static Pattern m_htmlPattern = 
           Pattern.compile(".*<.*\\s*html\\s*([^>]*)\\s*>.*", 
                           Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
   
   private static Pattern m_xmlPattern = 
     Pattern.compile(".*<.*\\s*\\?xml\\s*([^\\?>]*)\\s*\\?>.*", 
                     Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
   
   protected boolean m_isCacheable = false;   
   
   /**
    * Constructor with template package. 
    * @param httpd The Httpd instance
    * @param templateJavaPackage The Java package where template processing 
    *                            classes are located.
    * @throws java.io.IOException Throws an IO exception if the homeDir  
    *                             directory is not accessible.
    */
   public StringTemplateHandler(Httpd httpd, String templateJavaPackage) 
          throws IOException
   //------------------------------------------------------------------
   {
      if (templateJavaPackage == null)
         throw new IllegalArgumentException("Template package is null");
      m_httpd = httpd;
      m_templateJavaPackage = templateJavaPackage;
      m_templateProcessor = this;
   }
   
   /**
    * Constructor with a template processor class that implements
    * the Templatable interface. 
    * @param httpd The Httpd instance
    * @param templateProcessor A Java class that implements the Templatable 
    * interface. 
    * @throws java.io.IOException Throws an IO exception if the homeDir 
    *                             directory not accessible.
    * @throws java.lang.IllegalArgumentException Throws an 
    * IllegalArgumentException exception if templateProcessor is null 
   */
   public StringTemplateHandler(Httpd httpd, Templatable templateProcessor)
          throws IOException, IllegalArgumentException
   //------------------------------------------------------------------
   {
      if (templateProcessor == null)
         throw new IllegalArgumentException("Template processor is null");
      m_httpd = httpd;
      m_templateProcessor = templateProcessor;
   }
   
   abstract protected StringTemplate getTemplate(Request request);
   abstract protected Templatable getTemplateInstance(String templateName); 
   
   /**
    * @param b true to enable debug mode (Disables use of template cache)
    */
   public void setDebug(boolean b) 
   //-----------------------------
   { if (b) 
        m_templateGroup.setRefreshInterval(0);
     else
        m_templateGroup.setRefreshInterval(Integer.MAX_VALUE);
   }   
   
   /**
    * By default the StringTemplateHandler derived classes return m_isCacheable
    * which defaults to false to disallow cacheing for template based resources.
    * Use this method to enable/disable caching for this handler.
    * @param isCacheable true to enable cahceing for this handler.
    */
   public void setCacheable(boolean isCacheable) 
   { 
      m_isCacheable = isCacheable;
   }
   
   /**
    * @return true if cacheing is enabled for StringTemplate resources, else
    * false
    */
   public boolean getCacheable() { return m_isCacheable; } 
   
   public HttpResponse onServeHeaders(long id, HttpExchange ex, Request request)
   //---------------------------------------------------------------------------
   {
      StringTemplate template = getTemplate(request);
      if (template == null) return null; 
      
      HttpResponse r = new HttpResponse(ex, Http.HTTP_OK);
      /* Content-Length 0 == chunked 
       *r.addHeader("Content-Length", "0"); 
       *or  */
      StringBuffer mimeType = new StringBuffer();
      String s = m_templateProcessor.templateString(template, request, mimeType);
      if (s == null) return null;
      
      if (mimeType.length() > 0)
         r.addHeader("Content-Type", mimeType.toString());
      else
      {     
         Matcher matcher = m_htmlPattern.matcher(s);
         if (matcher.matches())
            r.addHeader("Content-Type", Http.MIME_HTML);
         else
         {
            matcher = m_xmlPattern.matcher(s);
            if (matcher.matches())
               r.addHeader("Content-Type", Http.MIME_XML);
            else
               r.addHeader("Content-Type", Http.MIME_PLAINTEXT);
         }
      }
      ByteArrayOutputStream baos = null;
      if (request.m_encoding != null)
      {         
         if (request.m_cacheFile != null)
            request.m_cacheFile.delete();
         BufferedInputStream bis = null;
         BufferedOutputStream bos = null, boss = null;         
         baos = new ByteArrayOutputStream();
         byte[] buffer = new byte[4096];

         try
         {
            bis = new BufferedInputStream(new ByteArrayInputStream(s.getBytes())); 
            if (request.m_encoding.compareTo("gzip") == 0)
            {
               if (request.m_cacheFile != null)
                  bos = new BufferedOutputStream(
                            new GZIPOutputStream(
                                new FileOutputStream(request.m_cacheFile)));
               boss = new BufferedOutputStream(new GZIPOutputStream(baos));
            }
            if (request.m_encoding.compareTo("deflate") == 0)
            {
               if (request.m_cacheFile != null)
                  bos = new BufferedOutputStream(
                            new DeflaterOutputStream(
                                new FileOutputStream(request.m_cacheFile)));
               boss = new BufferedOutputStream(new DeflaterOutputStream(baos));
            }
            while (true)
            {
               int cb = bis.read(buffer);
               if (cb == -1) break;
               if (bos != null) bos.write(buffer, 0, cb);
               boss.write(buffer, 0, cb);
            }
            boss.close();
            boss = null;
         }
         catch (Exception e)
         {
            Httpd.Log(Httpd.LogLevel.ERROR, 
                      "Compressing StringTemplate output", e);
            request.m_cacheFile = null;
            request.m_encoding = null;
            if (baos != null)
               try { baos.close(); } catch (Exception ee) {}
            baos = null;
         }
         finally
         {
            if (bis != null)
               try { bis.close(); } catch (Exception e) {}
            if (bos != null)
               try { bos.close(); } catch (Exception e) {}
            if (boss != null)
               try { boss.close(); } catch (Exception e) {}
         }
      }      
      
      if (baos != null)
      {
         r.addHeader("Content-Length", Integer.toString(baos.size())); 
         m_resultMap.put(id, baos);
      }
      else
      {
         r.addHeader("Content-Length", Integer.toString(s.length())); 
         m_resultMap.put(id, s);
      }
      return r;
   }
   
   /**
  * @inheritDoc
  */
   public InputStream onServeBody(long id, HttpExchange ex, Request request)
  //---------------------------------------------------------------------------
   {
      try
      {
         Object o = m_resultMap.get(id);      
         if (o != null)
         {
            if (o instanceof String)
            {
               String s = (String) o;
               return new BufferedInputStream(new ByteArrayInputStream(
                                                                 s.getBytes()));
            }
            else
            {
               if (o instanceof ByteArrayOutputStream)
               {
                  ByteArrayOutputStream baos = (ByteArrayOutputStream) o;
                  return new BufferedInputStream(new ByteArrayInputStream(
                                                            baos.toByteArray()));
               }
            }                
         }
      }
      catch (Exception e)
      {
         Httpd.Log(Httpd.LogLevel.ERROR, "Error creating input stream of " +
                     "template output string", e);
         return null;
      }
      finally
      {
         m_resultMap.remove(id);      
      }
     return null; 
   }         
   
   /**
    * The method processes templates and returns the file generated from the 
    * template. The default behaviour when this class is the template processor
    * is to load a class from m_templateJavaPackage with the same name as the 
    * template. This class must have an empty public constructor and must 
    * implement the Templatable interface.
    * Overiding classes can modify this behaviour to change the way templates 
    * content is generated.
    * @param  template The template
    * @param request The Request instance.
    * @return The file generated from the template.   */
   public File templateFile(StringTemplate template, Request request, 
                            StringBuffer mimeType, File dir)
   //----------------------------------------------------------------------
   {           
      Templatable instance = getTemplateInstance(template.getName());
      if (instance == null)
         return null;
      return instance.templateFile(template, request, mimeType, dir);
   }

   /**
    * The method processes templates and returns the file generated from the 
    * template. The default behaviour when this class is the template processor
    * is to load a class from m_templateJavaPackage with the same name as the 
    * template. This class must have an empty public constructor and must 
    * implement the Templatable interface.
    * Overiding classes can modify this behaviour to change the way templates 
    * content is generated.
    * @param  template The template
    * @param request The Request instance.
    * @return The String generated from the template.   */
   public String templateString(StringTemplate template, Request request, 
                                StringBuffer mimeType)
   //-------------------------------------------------------------------------
   {
      Templatable instance = getTemplateInstance(template.getName());
      if (instance == null)
         return null;
      return instance.templateString(template, request, mimeType);
   }
   
   /**
    * The method processes templates and returns the file generated from the 
    * template. The default behaviour when this class is the template processor
    * is to load a class from m_templateJavaPackage with the same name as the 
    * template. This class must have an empty public constructor and must 
    * implement the Templatable interface.
    * Overiding classes can modify this behaviour to change the way templates 
    * content is generated.
    * <b>Note</b> With the current implementation only templateString (and 
    * templateFile for POST processing) is used so this method need not be 
    * implemented, although if the implementation changes for example to use 
    * chunked encoding and not to generate a string in onServeHeaders, but 
    * instead to generate a stream in onServeBody then templateStream would be 
    * required.
    * @param  template The template
    * @param request The Request instance.
    * @return The InputStream generated from the template.   */
   public InputStream templateStream(StringTemplate template, Request request, 
                                     StringBuffer mimeType)
   //-------------------------------------------------------------------------
   {
      Templatable instance = getTemplateInstance(template.getName());
      if (instance == null)
         return null;
      return instance.templateStream(template, request, mimeType);
   }
   
   
   public boolean onPreServe(long id, HttpExchange ex, Request request)
   //------------------------------------------------------------------
   {
      return true;
   }

   public void onPostServe(long id, HttpExchange ex, Request request,
                           boolean isOK)
   {
   }

   public Request onFileNotFound(long id, HttpExchange ex, Request request)
   {
      return null;
   }

   public Object onHandlePost(long id, HttpExchange ex, Request request, 
                            HttpResponse response, File dir, 
                            Object... extraParameters)
   //----------------------------------------------------------------------
   {
      StringTemplate template = getTemplate(request);
      if (template == null) return null; 
      Templatable instance = null;
      if (m_templateProcessor == this) // Get class of same name as template
         instance = getTemplateInstance(template.getName());
      else // Use supplied (single class) template processor 
         instance = m_templateProcessor;
      
      // In either case check if the class supports Postable 
      if (! (instance instanceof Postable)) return null;
      
      // If it does then call onHandlePost
      Postable postProcessor = (Postable) instance;
      return postProcessor.onHandlePost(id, ex, request, response, dir, template);

   }

   public String onListDirectory(Request request)
   {
      return m_httpd.onListDirectory(request);
   }

}
