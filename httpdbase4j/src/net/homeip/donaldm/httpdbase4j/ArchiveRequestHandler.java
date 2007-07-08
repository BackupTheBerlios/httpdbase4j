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

import java.util.Iterator;

import net.homeip.donaldm.httpdbase4j.Httpd.LogLevel;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import de.schlichtherle.io.File;

/**
 * An abstraction of a handler for archive or classpath based HTTP requests.
 * @author Donald Munro
 */
public class ArchiveRequestHandler extends RequestHandler 
                               implements HttpHandler
//==========================================================================
{   
   /**
    * The base directory within the archive containing the web content. 
    */
   protected File             m_homeDir = null;   
   
   /**
    * A directory on the file system that can be used for creating dynamic
    * content
    */
   protected java.io.File     m_localHomeDir = null;
   
   /**
    * Create a ArchiveRequestHandler.
    * @param httpd - The Httpd instance
    * @param homeDir The base directory within the compressed file containing
    * the web resources. Must be an instance of a TrueZip File ie 
    * de.schlichtherle.io.File 
    * @param localHomeDir - A directory on the file system that can be used for creating 
    * dynamic content. If null then the alternate home in JatHttpd is used and if that is
    * null then a directory is created in the temporary directory.
    * @param isVerbose
    */
   public ArchiveRequestHandler(ArchiveHttpd httpd,  File homeDir, 
                            java.io.File localHomeDir, boolean isVerbose)
   //---------------------------------------------------------------------------
   {
      super(httpd, isVerbose);
      m_homeDir = homeDir;
      if (localHomeDir == null)
         localHomeDir = httpd.getAltFileSystemHome();
      if (localHomeDir == null)
      {
         _createLocalFileHome();
         localHomeDir = m_localHomeDir;
      }
      if (! localHomeDir.exists())
         localHomeDir.mkdirs();
      if ( (localHomeDir.exists()) && (localHomeDir.canWrite()) )         
         m_localHomeDir = localHomeDir;
      else
         _createLocalFileHome();               
   }
      
   public java.io.File getAltFileSystemHome() { return m_localHomeDir; }
   
   private void _createLocalFileHome()
   //---------------------------------
   {
      java.io.File tmpDir = new java.io.File(System.getProperty("java.io.tmpdir"));
            
      if ( (tmpDir.exists()) && (tmpDir.canWrite()) )
      {
         tmpDir = new File(tmpDir, "HttpdBase4J");
         tmpDir.mkdirs();
         if ( (! tmpDir.exists()) || (! tmpDir.canWrite()) )
         {
            tmpDir = new File(System.getProperty("java.io.tmpdir"));
            m_localHomeDir = new java.io.File(tmpDir, "HttpdBase4J-AltDocs");
         }
         else
            m_localHomeDir = new java.io.File(tmpDir, "AlternateDocs");
         m_localHomeDir.mkdirs();
         if (! m_localHomeDir.exists())
            m_localHomeDir = tmpDir;
         if (! m_localHomeDir.canWrite())
            m_localHomeDir = tmpDir;
      }
      else
         m_localHomeDir = null;
   }
           
   @Override
   public void handle(HttpExchange ex)
   //--------------------------------
   {
      m_ex =  ex;         
      //System.out.println(this.toString());      
      Request request = null;
      FileRequest altRequest = null;
      try
      {
          if (isCombinedRequest(ex.getRequestURI().getPath()))
            request = new ArchiveCombinedRequest(m_httpd, ex, m_homeDir); 
         else
            request = new ArchiveRequest(m_httpd, ex, m_homeDir);         
      }
      catch (Exception e)
      {
         Httpd.Log(LogLevel.ERROR, "Creating request for " + 
                     ex.getRequestURI().toASCIIString(), e);
         return;
      }
      try
      {         
         if (m_verbose)
            Httpd.Log(Httpd.LogLevel.INFO, "Received " + 
                     request.getMethodString() + " " + request.getPath() + 
                     " request from " + ex.getRemoteAddress().toString(),null);
         if ( (request.isGETorHEAD()) && (request.isDirectory()) )         
         {
            ArchiveRequest req = null;
            for (Iterator<String> i=m_httpd.m_defaultFiles.iterator(); 
                 i.hasNext();)
            {
               String defaultName = i.next();
               req = new ArchiveRequest((ArchiveRequest) request, defaultName);
               if (req.exists()) break;
               req = null;
            }
            if (req != null)
               request = req;
            else
            {
               browseDirCheck(ex, request);
               return;
            }   
         }

         HttpResponse r = new HttpResponse(ex);
         boolean isProcessAsGet = false;
         long id = Httpd.getNextSequence();

         if (request.getMethod() == Request.HTTP_METHOD.POST)
         {
            Postable postHandler = request.getPostHandler();   
            if (postHandler == null) postHandler = m_httpd;
            Object o = postHandler.onHandlePost(id, ex, request, r, m_localHomeDir);
            if (( o != null) && (o instanceof HttpResponse) )
            {
               r = (HttpResponse) o;
               r.send();
               return;
            }   
                        
            java.io.File f;
            if ( (o == null) || (o instanceof File) || (o instanceof java.io.File))
            {   
               if ( (o == null) || (m_localHomeDir == null) )
               {
                  String html = "<head><title>POST Error</title></head><body>"+
                                "<p>Service unavailable for POST " + 
                                request.getPath()+ "</p><hr>"
                              + "</body></html>";
                  r.setStatus(503);
                  r.setMimeType(Http.MIME_HTML);
                  r.setBody(html);
                  r.send();
                  if (m_verbose)
                     Httpd.Log(Httpd.LogLevel.INFO, "No POST handler for " + 
                              request.getPath() + " request from " + 
                              ex.getRemoteAddress().toString(), null);
                  return;
               }   
               else
               {
                  f = (java.io.File) o;
                  request = new FileRequest(request, m_localHomeDir, f);
                  isProcessAsGet = true;
               }
               if (m_verbose)
                     Httpd.Log(Httpd.LogLevel.INFO, "POST handler for " + 
                              request.getPath() + " request from " + 
                              ex.getRemoteAddress().toString() + 
                              " generated " + request.getAbsolutePath(), null);
            }
            else // if ( (o == null) || (o instanceof File) )
            {
               Httpd.Log(LogLevel.ERROR, "Invalid return type " + 
                        o.getClass().getName() + " from onHandlePost " +
                        ex.getRequestURI().toASCIIString(), null);
               HttpResponse.internalError(ex, request.getURI(), 
                                          ex.getRequestHeaders());
            }
         }

         if ( (request.isGETorHEAD()) || (isProcessAsGet) )
         {
            HttpHandleable handler = request.getHandler();
            if (request.isDirectory())
            {
               String dirHtml = handler.onListDirectory(request);
               if (dirHtml == null)
               {
                  r = HttpResponse.accessDenied(ex, request.getURI(), 
                                                ex.getRequestHeaders());
                  r.send();
                  return;
               }
               r.setMimeType(Http.MIME_HTML);
               r.setBody(dirHtml);
               r.send();
               return;
            }         
            
            String etag = null;
            if (handler.onIsCacheable(id, ex, request))
            {
               etag = request.getETag(false);
               if (request.checkClientCache())
               {
                  r.setStatus(304);
                  r.send();
                  return;
               }
            }

            if (! handler.onPreServe(id, ex, request))
            {
               HttpResponse.notFound(ex, request.getURI(), 
                                     ex.getRequestHeaders()).send();
               return;
            }

            if (! request.exists())
            {
               if ( (altRequest == null) && (m_localHomeDir != null) )
                  altRequest = new FileRequest(request, m_localHomeDir, null);
               if ( (altRequest != null) && (altRequest.exists()) )
               {
                  request = altRequest;
                  etag = request.getETag(true);
               }
               else
               {
                  if (m_verbose)
                     Httpd.Log(Httpd.LogLevel.INFO, "Request " + 
                              request.getURI().toASCIIString() + 
                              " not found", null);
                  Request newRequest = handler.onFileNotFound(id, ex, request);            
                  if ( (newRequest == null) ||  (! newRequest.exists()) )
                  {                     
                     HttpResponse.notFound(ex, 
                                     (newRequest == null) ? request.getURI()
                                                          : newRequest.getURI(), 
                                           ex.getRequestHeaders()).send();
                     return;
                  }
                  request = newRequest;
                  if (request.isCacheable())
                     etag = request.getETag(true);
               }               
            }

            sendResult(request, r, id, etag, ex);
            return;
         }
      }
      catch (Exception e)
      {
         Httpd.Log(Httpd.LogLevel.ERROR, "Error handling request", e);   
      }
      finally
      {
         if (ex != null) try { ex.close(); } catch (Exception e) {} 
      }
   }

   @Override
   public String toString()
   //----------------------
   {
      return super.toString() + Httpd.EOL + "Base Directory: " + 
             ((m_homeDir == null) ? "Unknown" : m_homeDir) + 
             Httpd.EOL + "File System Directory: " + 
             ((m_localHomeDir == null) ? "Unknown" : 
                                          m_localHomeDir.getAbsolutePath());
   }
}
