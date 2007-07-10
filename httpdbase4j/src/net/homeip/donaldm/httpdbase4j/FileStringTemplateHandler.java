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

import com.sun.net.httpserver.HttpExchange;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

/**
 * Handler for StringTemplate requests from a local filesystem.
 * @see StringTemplateHandler
 * @see Templatable
 * @author Donald Munro
 */
public class FileStringTemplateHandler extends StringTemplateHandler
       implements HttpHandleable, Postable, Templatable 
//==================================================================
{
   /**
    * Constructor with template package.
    * @param httpd - The Httpd instance
    * @param templateJavaPackage - The Java package where template processing 
    * classes are located.
    * @throws IOException
    */
   public FileStringTemplateHandler(Httpd httpd, String templateJavaPackage) 
           throws IOException
   //----------------------------------------------------------------------
   {
      super(httpd, templateJavaPackage);
      m_templateGroup = new StringTemplateGroup("StringTemplateDir",
                                                m_httpd.getHomePath());
   }
   
   /**
    * Constructor with a template processor class that implements
    * the Templatable interface.     
    * @param httpd - The Httpd instance
    * @param templateProcessor - A Java class that implements the Templatable 
    * interface. 
    * @throws IOException
    */
   public FileStringTemplateHandler(Httpd httpd, Templatable templateProcessor)
           throws IOException
   //----------------------------------------------------------------------
   {
      super(httpd, templateProcessor);
      m_templateGroup = new StringTemplateGroup("StringTemplateDir",
                                                m_httpd.getHomePath());
   }
   
   /**
    *  @inheritDoc
    */   
   @Override
   protected StringTemplate getTemplate(Request request)
   //--------------------------------------------------
   {
      String templateName = request.getName();
      File f = new File(templateName);
      String ext = Http.getExtension(f);
      int p = templateName.lastIndexOf(ext);
      if (p >= 0)
         templateName = templateName.substring(0, p);
      StringTemplate template = null;
      if (request.exists())
         template = m_templateGroup.getInstanceOf(templateName);
      return template;
   }
   
   /**
    *  @inheritDoc
    */   
   @SuppressWarnings("unchecked")
   @Override
   protected Templatable getTemplateInstance(String templateName)
   //-----------------------------------------------------------
   {
      if (templateName.compareTo("--POST_GENERATED--") == 0)
         return new TemplatableAdapter();
      String classPackage = m_templateJavaPackage;
      if (classPackage == null) classPackage = "";
      classPackage = classPackage.trim();
      if (classPackage.length() > 0)
      {
         if (! classPackage.endsWith("."))
            classPackage += ".";
      }
      
      int p =-1;
      p = templateName.lastIndexOf(File.separatorChar);
      if (p == -1)
         p = templateName.lastIndexOf('/');
      if (p >= 0)
         templateName = templateName.substring(p+1);
      try
      {
         String className = templateName;
         Class C = _instantiateTemplateClass(classPackage + templateName);         
         
         if (C == null) 
         {  // Try first letter uppercased 
            templateName = templateName.substring(0, 1).toUpperCase() + 
                           templateName.substring(1).toLowerCase();
            C = _instantiateTemplateClass(classPackage + templateName);
         }   
         if (C == null) 
            return null;
         Templatable instance = (Templatable) C.newInstance();
         return instance;
      }
      catch (InstantiationException e)
      {
         Httpd.Log(Httpd.LogLevel.ERROR, "Class " + classPackage + 
                     templateName + " could not be instantiated", e);
         return null;
      }
      catch (ClassCastException e)
      {
         Httpd.Log(Httpd.LogLevel.ERROR, "Class " + classPackage + 
                     templateName + " must implement the Templatable interface", 
                     e);
         return null;
      }
      catch (Exception e) 
      {
         Httpd.Log(Httpd.LogLevel.ERROR, "Class " + classPackage + 
                     templateName + " threw an exception", e);
         return null;
      }         
      
   }
           
   /**
    * Called to determine whether the resource from a request should be cached.
    * To implement user defined cacheing cache the file but return false and
    * also overide @see onGetCachedFile to return the cached file.
    * @param id Unique id
    * @param ex The exchange instance for the current HTTP transaction.
    * @param request The request instance
    * @return true to cache the result, false to not cache. 
    * StringTemplateHandler derived classes default to returning false to 
    * force no caching.
    */
   public boolean onIsCacheable(long id, HttpExchange ex, Request request)
   //---------------------------------------------------------------------
   {
      return m_isCacheable; 
   }
   
   @Override
   public File onGetCachedFile(long id, HttpExchange ex, Request request)
   //--------------------------------------------------------------------
   {
      return null;
   }
   
   @Override
   public boolean onPreServe(long id, HttpExchange ex, Request request)
   //------------------------------------------------------------------
   {
      return super.onPreServe(id, ex, request);
   }

   @Override
   public HttpResponse onServeHeaders(long id, HttpExchange ex, Request request)
   //--------------------------------------------------------------------------
   {
      return super.onServeHeaders(id, ex, request);
   }

   @Override
   public InputStream onServeBody(long id, HttpExchange ex, Request request)
   //-----------------------------------------------------------------------
   {
      return super.onServeBody(id, ex, request);
   }

   @Override
   public void onPostServe(long id, HttpExchange ex, Request request, 
                           boolean isOK)
   //-----------------------------------------------------------------
   {
      super.onPostServe(id, ex, request, isOK);
   }

   @Override
   public Request onFileNotFound(long id, HttpExchange ex, Request request)
   //----------------------------------------------------------------------
   {
      return super.onFileNotFound(id, ex, request);
   }

   @Override
   public String onListDirectory(Request request)
   //--------------------------------------------
   {
      return super.onListDirectory(request);
   }

   @Override
   public Object onHandlePost(long id, HttpExchange ex, Request request, 
                            HttpResponse response, File dir, Object... extraParameters)
   //-----------------------------------------------------------------
   {
      return super.onHandlePost(id, ex, request, response, dir, extraParameters);
   }

   @Override
   public File templateFile(StringTemplate template, Request request, 
                            StringBuffer mimeType, File dir)
   //----------------------------------------------------------------
   {
      return super.templateFile(template, request, mimeType, dir);
   }

   @Override
   public String templateString(StringTemplate template, Request request, 
                                StringBuffer mimeType)
   //--------------------------------------------------------------------
   {
      return super.templateString(template, request, mimeType);
   }

   @Override
   public InputStream templateStream(StringTemplate template, Request request, 
                                     StringBuffer mimeType)
   //-------------------------------------------------------------------------
   {
      return super.templateStream(template, request, mimeType);
   }
}
