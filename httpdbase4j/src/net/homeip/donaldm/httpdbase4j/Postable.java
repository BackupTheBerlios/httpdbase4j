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

import com.sun.net.httpserver.HttpExchange;

/**
 * An interface that must be implemented by classes processing POST requests.
 * @see Httpd#addPostHandler
 * @author Donald Munro
 */
public interface Postable
//=======================
{
   /**
    * Called when the server is about to perform a POST. May be used by an 
    * overiding  class to generate dynamic content. The overiding class should 
    * return a new File or if it does not handle the post then it should 
    * return null. The resulting file will be processed as a GET request,
    * @param id Unique transaction id
    * @param ex The HttpExchange instance for this request.
    * @param request The Request 
    * @param response Response instance
    * @param dir - Directory where output can be created
    * @param extraParameters Extra parameters that can be used by overiding 
    *        classes
    * @return return either an HttpResponse instance or a new File instance 
    * within the base directory structure of the Httpd instance. If a File is
    * returned it will be processed as a GET request.
    */
   public Object onHandlePost(long id, HttpExchange ex, Request request, 
                            HttpResponse response, File dir, 
                            Object... extraParameters);
}
