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

import java.io.File;
import java.util.Iterator;

import net.homeip.donaldm.httpdbase4j.Httpd.LogLevel;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * An abstraction of a handler for local filesystem based HTTP requests.
 * @author Donald Munro
 */
public class FileRequestHandler extends RequestHandler implements HttpHandler
//===========================================================================
{   
   /**
    * The home directory within the file system for this request.
    */
   protected File             m_homeDir = null;   
   
   /**
    * Create a FileRequestHandler.
    * @param httpd - The Httpd instance
    * @param homeDir - The home directory within the file system for this request.
    * @param isVerbose - Logging verbosity
    */
   public FileRequestHandler(Httpd httpd, File homeDir, boolean isVerbose)
   //-----------------------------------------------------------------
   {
      super(httpd, isVerbose);
      m_homeDir = homeDir;
   }
   
   /**
    *  @inheritDoc
    */    
   @Override
   public void handle(HttpExchange ex)
   //--------------------------------
   {      
      m_ex =  ex;               
      Request request = null;
      try
      {
         if (isCombinedRequest(ex.getRequestURI().getPath()))
            request = new FileCombinedRequest(m_httpd, ex, m_homeDir); 
         else
            request = new FileRequest(m_httpd, ex, m_homeDir);
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
            FileRequest req = null;
            for (Iterator<String> i=m_httpd.m_defaultFiles.iterator(); i.hasNext();)
            {
               String defaultName = i.next();
               req = new FileRequest((FileRequest) request, defaultName);
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
            Object o = postHandler.onHandlePost(id, ex, request, r, m_homeDir);
            if (( o != null) && (o instanceof HttpResponse) )
            {
               r = (HttpResponse) o;
               r.send();
               return;
            }   
                        
            File f;
            if ( (o == null) || (o instanceof File) )
            {               
               if (o == null)
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
                  f = (File) o;
                  request = new FileRequest(request, m_homeDir, f);
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
               if (m_verbose)
                  Httpd.Log(Httpd.LogLevel.INFO, "Request " + 
                            request.getURI().toASCIIString() + 
                            " not found (" + request + ")", null);
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
               etag = request.getETag(true);
            }

            if (! request.isReadable())
            {
               HttpResponse.accessDenied(ex, request.getURI(), 
                                         ex.getRequestHeaders()).send();
               return;
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

   /**
    *  @inheritDoc
    */       
   @Override
   public String toString()
   //----------------------
   {
      return super.toString() + Httpd.EOL + "Base Directory: " + 
             ((m_homeDir == null) ? "Unknown" : m_homeDir);
   }      
}
