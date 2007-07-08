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
import java.io.InputStream;

/**
 * Interface with all the overidable methods that handlers provide.
 * @author Donald Munro
 */
public interface HttpHandleable
//===============================
{
 
   /**
    * Called to determine whether the resource from a request should be cached.
    * To implement user defined cacheing cache the file but return false and
    * also overide @see onGetCachedFile to return the cached file.
    * @param id Unique id
    * @param ex The exchange instance for the current HTTP transaction.
    * @param request The request instance
    * @return true to cache the result, false to not cache.
    */
   public boolean onIsCacheable(long id, HttpExchange ex, Request request);
   
   /**
    * Called to retrieve a cached file if a user defined caching scheme is
    * used.
    * @param id Unique id
    * @param ex The exchange instance for the current HTTP transaction.
    * @param request The request instance
    * @return true to cache the result, false to not cache.
    */
   public File onGetCachedFile(long id, HttpExchange ex, Request request);
   
   /**
    * Called before the server serves a file. 
    * @param id Unique id
    * @param ex The exchange instance for the current HTTP transaction.
    * @param request The request instance
    * @return true to continue processing the request
    */
   public boolean onPreServe(long id, HttpExchange ex, Request request);
      
   /**
    * Called as the server is about to perform a GET. May be used by an 
    * overiding  class to modify the response headers. The overiding class should 
    * return a HttpResponse or null to use the default HttpResponse.<br>
    * <b>NOTE:</b> If the overiding class is going to alter the response body
    * using <i>onGETBody</i> then it should set the Content-Length header in
    * this method.
    * @param id Unique transaction id
    * @param ex The exchange instance for the current HTTP transaction.
    * @param request The request instance
    * @return Return null to process normally, otherwise return a new 
    * HttpResponse to be send to the client
    */
   public HttpResponse onServeHeaders(long id, HttpExchange ex, Request request);
   
   /**
    * Called as the server is about to send the GET data. May be used by an 
    * overiding  class to modify the data. The overiding class should 
    * return a InputStream from which to read the  or null to use the default.
    * @param id Unique transaction id
    * @param ex The exchange instance for the current HTTP transaction.
    * @param request The request instance
    * @return  Return null to process normally, else return the  HttpResponse 
    * to be send to the client
    */ 
   public InputStream onServeBody(long id, HttpExchange ex, Request request);
  
   /**
    * Called before the server performs a GETon a file. May be used by an 
    * overiding  class to generate dynamic content. The overiding class should 
    * return a new File or if it does not generate a new file then it should 
    * return the input File parameter
    * @param id Unique transaction id
    * @param ex The exchange instance for the current HTTP transaction.
    * @param request The request instance    
    * @param isOK true if the request has been successfully sent
    */
   public void onPostServe(long id, HttpExchange ex, Request request,
                           boolean isOK);   
   
   /**
    * Called when a request file is not found.
    * @param id Unique transaction id
    * @param ex The exchange instance for the current HTTP transaction.
    * @param request The request instance    
    * @return Return null to process normally ie return a 404 error, otherwise 
    * return a new Request to be processed which will be used instead of request. 
    */
   public Request onFileNotFound(long id, HttpExchange ex, Request request);
   
   /**
    * Called to list a directory request.
    * @param request
    * @return A String containing the HTML for a page listing the 
    * directory contents for a directory request.
    */
   public String onListDirectory(Request request);
  
}
