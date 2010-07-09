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
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import net.homeip.donaldm.httpdbase4j.Httpd.LogLevel;

/**
 * An abstraction of a handler for HTTP requests.
 * @author Donald Munro
 */
abstract public class RequestHandler implements HttpHandler
//==========================================================
{
   
   /**
   The Httpd instance for this request.
   */
   protected Httpd            m_httpd = null;

   /**
   The HttpExchange instance for this request.
   * @see com.sun.net.httpserver.HttpExchange
   */
   protected HttpExchange m_ex = null;
   
   /**
    * Logging verbosity on or off.
    */
   protected boolean m_verbose = true;
   
   /**
    * Delimiter to use between files in a combined request. Must be a regular
    * expression, hence the \\+.
    * @see CombinedRequest
    */
   static protected String COMBINED_REQUEST_DELIMITER = "!\\+!";
   
   static protected String[] COMBINED_REQUEST_EXTENSIONS = { ".css", ".js" };
                           
   /**
    * Create a RequestHandler
    * @param httpd The HttpExchange instance for this request.
    * @see com.sun.net.httpserver.HttpExchange
    * @param isVerbose Logging verbosity on/off
    */
   public RequestHandler(Httpd httpd, boolean isVerbose)
   //--------------------------------------
   {
      m_httpd = httpd;
      m_verbose = isVerbose;
   }
   
   static public boolean isCombinedRequest(String path)
   //--------------------------------------------------
   {
      if (! path.matches(".*" + COMBINED_REQUEST_DELIMITER + ".*")) return false;
      for (int i=0; i<COMBINED_REQUEST_EXTENSIONS.length; i++)
         if (path.contains(COMBINED_REQUEST_EXTENSIONS[i])) return true;
      return false;
   }
   
   @Override
   public String toString()
   //----------------------
   {  
      
      return super.toString() + Http.strExchange(m_ex);
   }
      
   /**
    * Permissions check for directory browse 
    * @param ex The HttpExchange instance for this request.
    * @param request The request to check
    */
   protected void browseDirCheck(HttpExchange ex, Request request)
   //-------------------------------------------------------------
   {
      if (! request.isDirectory())
      {
         try
         {
            HttpResponse.internalError(ex, request.getURI(), 
                                  ex.getRequestHeaders()).send();
         }
         catch (Exception e)
         {
            Httpd.Log(LogLevel.ERROR, "Creating URI /", e);
         }
         return;
      }
      
      URI uri = null;
      try
      {
         uri = new URI(request.getURI().toString() + "/");
      }
      catch (Exception e)
      {
         uri = null;
      }
      if (m_httpd.onAllowDirectoryBrowse(request.getAbsolutePath()))
      {            
         if (! request.getPath().endsWith("/"))
         {                     
            if (uri != null)
               HttpResponse.reDirect(ex, uri, 
                                     ex.getRequestHeaders()).send();
            else
            {
               try
               {
                  HttpResponse.internalError(ex, request.getURI(), 
                                        ex.getRequestHeaders()).send();
               }
               catch (Exception e)
               {
                  Httpd.Log(LogLevel.ERROR, "Creating URI /", e);
               }
            }
            return;
         }
      }
      else
      {
         HttpResponse.accessDenied(ex, uri, 
                                   ex.getRequestHeaders()).send();
         return;
      }   
   }
   
   protected void sendResult(Request request, HttpResponse r, long id, 
                             String etag, HttpExchange ex)
   //-------------------------------------------------------------------------
   {
      HttpHandleable handler = request.getHandler(); 
      if (! request.getContent(id, handler))
      {
         HttpResponse.internalError(ex, request.getURI(),
                 ex.getRequestHeaders()).send();
         return;
      }
      long len = request.getContentLength();
      boolean isModified = false;
      HttpResponse userResponse = handler.onServeHeaders(id, ex, request);
      if (userResponse == null)
      {
         String mimeType = Http.getMimeType(request);
         if (mimeType == null)
            mimeType = "text/plain";
         List<String> l = ex.getRequestHeaders().get("Accept");
         boolean isFound = false;
         for (Iterator<String>it=l.iterator(); it.hasNext();)
         {
            String accept = it.next();            
            if (accept == null) continue;
            accept = accept.trim();                     
            if ( (accept != null) && (mimeType != null) &&
                 ( (accept.contains(mimeType)) || (accept.compareTo("*/*") == 0) ) )
            {
               r.addHeader("Content-Type", mimeType);
               isFound = true;
               break;
            }
         }
         if (! isFound)
            r.addHeader("Content-Type", mimeType);
      }
      else
      {
         isModified = true;
         r = userResponse;
      }
      if (request.isCacheable())
      {
         if (etag != null)
            r.addHeader("ETag", etag);
         r.addHeader("Last-Modified", Http.strDate(request.getDate()));
      }
      if (request.m_encoding != null)
         r.addHeader("Content-Encoding", request.m_encoding);
      r.addHeader("Connection", "close");
      if (request.getMethod() == Request.HTTP_METHOD.HEAD)
         r.sendHeaders(-1);
      else
      {
         InputStream is = handler.onServeBody(id, ex, request);
         if (is != null)
         {
            String s = r.getHeader("Content-Length", 0);
            try
            { len = Long.parseLong(s); }
            catch (Exception e)
            { len = -1; }
            if ( (! isModified) || (len < 0) )
            {
               Httpd.Log(LogLevel.ERROR, "Content-Length not set in "
                       + "header in onGETHeaders.", null);
               HttpResponse.internalError(ex, request.getURI(),
                       ex.getRequestHeaders()).send();
               return;
            }
         }
         if (r.sendHeaders(len))
         {
            BufferedInputStream bis = null;
            boolean ok = false;
            try
            {
               if (is == null)
               {
                  bis = new BufferedInputStream(request.getStream());
                  ok = r.sendData(bis);
               }
               else
                  ok = r.sendData(is);
               
            }
            catch (Exception e)
            {
               Httpd.Log(Httpd.LogLevel.ERROR, "Error reading file", e);
               ok = false;
            }
            finally
            {
               if (bis != null) try
               { bis.close(); }
               catch (Exception e)
               {}
               if (is != null) try
               { is.close(); }
               catch (Exception e)
               {}
            }
            handler.onPostServe(id, ex, request, ok);
         }
      }
   }
   
   /* (non-Javadoc)
    * @see com.sun.net.httpserver.HttpHandler#handle(com.sun.net.httpserver.HttpExchange)
    */
   @Override abstract public void handle(HttpExchange exchange) throws IOException;
}
