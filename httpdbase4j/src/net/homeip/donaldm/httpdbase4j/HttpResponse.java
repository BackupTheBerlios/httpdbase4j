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

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates sending HTTP responses and provides several static methods
 * for sending common HTTP error and informational responses.
 * @author Donald Munro
 */
public class HttpResponse
//=======================
{
   protected int                         m_status = 200;
   
   protected Headers                     m_headers = null;

   protected HttpExchange                m_exchange = null;

   private String                        m_body = "";

   private OutputStream                  m_responseStream = null;
   
   private long                          m_contentLength = 0;

   /**
    * Create a HttpResponse 
    * @param exchange The exchange instance for the current HTTP transaction.
    */
   public HttpResponse(HttpExchange exchange)
   //----------------------------------------
   {
      m_exchange = exchange;
      m_headers = exchange.getResponseHeaders();
   }

   /**
    * Create a HttpResponse 
    * @param exchange The exchange instance for the current HTTP transaction.
    * @param status The HTTP status code of the response
    */
   public HttpResponse(HttpExchange exchange, int status)
   //-------------------------------------------------------
   {
      m_status = status;
      m_exchange = exchange;
      m_headers = exchange.getResponseHeaders();
   }

   /**
    * Create a HttpResponse 
    * @param exchange The exchange instance for the current HTTP transaction.
    * @param status The HTTP status code of the response
    * @param mimeType The mime type for the response
    */
   public HttpResponse(HttpExchange exchange, int status, String mimeType)
   //------------------------------------------------------------------
   {
      m_status = status;
      m_exchange = exchange;
      m_headers = exchange.getResponseHeaders();
      if (mimeType != null)
         addHeader("Content-Type", mimeType);

   }
   
   /**
    * Create a HttpResponse 
    * @param exchange The exchange instance for the current HTTP transaction.
    * @param status The HTTP status code of the response
    * @param mimeType The mime type for the response
    * @param body The content of the response
    */
   public HttpResponse(HttpExchange exchange, int status, String mimeType,
           String body)
   //--------------------------------------------------------------
   {
      m_status = status;
      m_exchange = exchange;
      m_headers = exchange.getResponseHeaders();
      if (mimeType != null)
         addHeader("Content-Type", mimeType);
      m_body = body;
   }

   /**
    * Get a header value that was set for this response.
    * @param k The header key
    * @return The header string for key k
    */
   public String getHeader(String k)
   //--------------------------------------------
   {
      return m_headers.getFirst(k);
   }
   
   /**
    * Get a header value that was set for this response.
    * @param k The header key
    * @param index If there is more than one value for the header key then 
    * return the kth value.
    * @return The header for key k and index <i>index</i>
    */
   public String getHeader(String k, int index)
  //--------------------------------------------
   {
      List<String> l = null;
      try
      {
         l = m_headers.get(k);
         if (l == null) return null;
         return l.get(index);
      }
      catch (ClassCastException e)
      {
         return m_headers.getFirst(k);
      }        
      catch (IndexOutOfBoundsException e)
      {
         return null;
      }
   }
   
   /**
    * Add a header value.
    * @param k The header key to add a value for
    * @param v The header value
    */
   public void addHeader(String k, String v)
   //---------------------------------------
   {
      k = k.trim();
      if (v == null) v = "";
      List<String> l = m_headers.get(k);
      if (l == null)
      {
         l = new ArrayList<String>();
         l.add(v);
         m_headers.put(k, l);
      }
      else
         l.add(v);
   }

   public void setStatus(int status)  { m_status = status;  }

   public void setBody(String body) { m_body = body;  }
   
   public void setMimeType(String mimeType) 
   //---------------------------------------------
   { 
      addHeader("Content-Type", mimeType);  
   }
   
   /**
    * Send the Response headers
    * @return true if successful otherwise false
    */
   public boolean sendHeaders()
   //--------------------------
   {
      return sendHeaders(-1);
   }
   
   /**
    * Send the Response headers
    * @param contentLength The content length for the body text that will be 
    * sent with sendData. If contentLength is -1 then the length of the body
    * set in the constructor will be used
    * @return true if successfull otherwise false
    */
   public boolean sendHeaders(long contentLength)
   //-------------------------------------------
   {
      if (contentLength < 0)
         m_contentLength = m_body.length();
      else
         m_contentLength = contentLength;
      String date = m_headers.getFirst("Date");
      if (date == null)
         addHeader("Date",Http.strDate(null));
      try
      {
         if (m_contentLength >= 0)
         {
            m_exchange.sendResponseHeaders(m_status, m_contentLength);         
            m_responseStream = m_exchange.getResponseBody();
         }
         else
            return false;
      }
      catch (Exception e)
      {
         return false;
      }
      return true;
   }

   /**
    * Send the Response data
    * @return true if Response was successfully sent otherwise false
    */
   public boolean sendData()
   //-----------------------
   {
      if (m_responseStream == null) return false;
      if (m_body.length() == 0) return false;
      if (m_contentLength <= 0) return false;
      return sendData(m_body);
   }

   /**
    * Send the Response data
    * @param s The string to send as the Response body. Note: This must be the 
    * same length as the contentLength set in sendHeaders.
    * @return true if Response was successfully sent otherwise false
    */
   public boolean sendData(String s)
   //-------------------------------
   {
      if (m_responseStream == null) return false;
      if (s.length() == 0) return false;
      if (s.length() != m_contentLength) return false;
      
      InputStream data = new ByteArrayInputStream(s.getBytes());
      try
      {
         return sendData(data);
      }
      finally
      {
         if (data != null) try { data.close(); } catch (Exception e) {}
         if (m_responseStream != null) 
            try { m_responseStream.close(); } catch (Exception e) {}
      }
   }

   /**
    * Send the Response data
    * @param data The stream to send as the Response body. Note: This must be the 
    * same length as the contentLength set in sendHeaders.
    * @return true if Response was successfully sent otherwise false
    */
   public boolean sendData(InputStream data)
   //---------------------------------------
   {
      if (m_responseStream == null) return false;      
      try
      {
         Http.readWriteStream(data, m_responseStream);
         return true;
      }
      catch (Exception e)
      {
         Httpd.Log(Httpd.LogLevel.ERROR, "Error sending response data", e);
         return false;
      }
      finally
      {
         try { m_responseStream.close(); } catch (Exception e) {}
      }
   }

   /**
    * Send the Response using the body and/or status previously specified in the 
    * constructor. If the body is not specified then nol content is send only 
    * the headers.
    * @return true if Response was successfully sent otherwise false
    */
   public boolean send()
   //-------------------
   {
      if (sendHeaders(( (m_body != null) && (m_body.length() > 0) )
      ? m_body.length() : -1))
         sendData();
      else
         return false;
      return true;
   }

   /**
    * Create a HTTP NOT FOUND response
    * @param exchange The exchange instance for the current HTTP transaction.
    * @param uri The URI for the response
    * @param requestHeaders The request headers
    * @return An HttpResponse instance 
    */
   static public HttpResponse notFound(HttpExchange exchange, URI uri, 
                                       Headers requestHeaders)
   //----------------------------------------------------------------
   {
      String accept = requestHeaders.getFirst("Accept");
      boolean isHtml = accept.toLowerCase().contains("text/html");
      HttpResponse response = null;
      if ( (isHtml) && 
           (exchange.getRequestMethod().compareToIgnoreCase("head") != 0) )
      {
         String html = "<html>\n<head>\n"
             + "<title>Error: File not found " + uri.getPath() + "</title>\n"
             + "<body>\n<h1>File not Found<b>"
             + "</b></h1><br>\nThe requested URL <b>" + uri.toASCIIString()
             + " could not be located (" + uri.getPath() + ")\n<hr>";
         response = new HttpResponse(exchange, Http.HTTP_NOTFOUND, 
                                     Http.MIME_HTML, html);            
      }
      else
         response = new HttpResponse(exchange, Http.HTTP_NOTFOUND);       
      return response;
   }
   
   /**
    * Create a HTTP REDIRECT response
    * @param exchange The exchange instance for the current HTTP transaction.
    * @param uri The URI for the response
    * @param requestHeaders The request headers
    * @return An HttpResponse instance 
    */
   static public HttpResponse reDirect(HttpExchange exchange, URI uri, 
                                       Headers requestHeaders)
   //----------------------------------------------------------------
   {
      HttpResponse r = null;
      if (exchange.getRequestMethod().compareToIgnoreCase("head") != 0)
      {
         String html = "<html><body>Redirected. Click this link if you "
                       + "are not redirected <a href=\"" + uri.toString() 
                       + "\">" + uri.toASCIIString() + "</a></body></html>";
         r = new HttpResponse(exchange, Http.HTTP_REDIRECT, 
                                                 Http.MIME_HTML, html);
      }
      else
         r = new HttpResponse(exchange, Http.HTTP_REDIRECT);
      r.addHeader("Location", uri.toASCIIString());
      return r;
   }
   
   /**
    * Create a HTTP ACCESS DENIED response
    * @param exchange The exchange instance for the current HTTP transaction.
    * @param uri The URI for the response
    * @param requestHeaders The request headers
    * @return An HttpResponse instance 
    */
   static public HttpResponse accessDenied(HttpExchange exchange, URI uri, 
                                       Headers requestHeaders)
   //----------------------------------------------------------------
   {
      String accept = requestHeaders.getFirst("Accept");
      boolean isHtml = accept.toLowerCase().contains("text/html");
      HttpResponse response = null;
      if ( (isHtml) && 
           (exchange.getRequestMethod().compareToIgnoreCase("head") != 0) )
      {
         String html = "<html>\n<head>\n"
             + "<title>Error: Access denied:  " + uri.getPath() + "</title>\n"
             + "<body>\n<h1>Access denied<b>"
             + "</b></h1><br>\nAccess denied to URL <b>" + uri.toASCIIString()
             + "\n<hr>";
         response = new HttpResponse(exchange, Http.HTTP_FORBIDDEN, 
                                     Http.MIME_HTML, html);            
      }
      else
         response = new HttpResponse(exchange, Http.HTTP_FORBIDDEN);       
      return response;
   }
   
   /**
    * Create a HTTP INTERNAL ERROR response
    * @param exchange The exchange instance for the current HTTP transaction.
    * @param uri The URI for the response
    * @param requestHeaders The request headers
    * @return An HttpResponse instance 
    */
   static public HttpResponse internalError(HttpExchange exchange, URI uri, 
                                            Headers requestHeaders)
   //----------------------------------------------------------------
   {
      String accept = requestHeaders.getFirst("Accept");      
      boolean isHtml = accept.toLowerCase().contains("text/html");
      HttpResponse response = null;
      if ( (isHtml) && 
           (exchange.getRequestMethod().compareToIgnoreCase("head") != 0) )
      {
         String html = "<html>\n<head>\n"
             + "<title>Error: Internal error " + uri.getPath() + "</title>\n"
             + "<body>\n<h1>Internal error<b>"
             + "</b></h1><br>\nThe requested URL <b>" + uri.toASCIIString()
             + " caused an internal error (" + uri.getPath() + ")\n<hr>";
         response = new HttpResponse(exchange, Http.HTTP_INTERNALERROR, 
                                     Http.MIME_HTML, html);            
      }
      else
         response = new HttpResponse(exchange, Http.HTTP_INTERNALERROR);       
      return response;
   }
   
   /**
    * Create a HTTP NOTIFY CONTINUE response
    * @param exchange The exchange instance for the current HTTP transaction.
    * @return An HttpResponse instance 
    */
   static public HttpResponse notifyContinue(HttpExchange exchange)
   //----------------------------------------------------------------
   {
      if (exchange.getProtocol().contains("1.0")) return null;
      HttpResponse response = new HttpResponse(exchange, Http.HTTP_CONTINUE);       
      return response;      
   }   
}
