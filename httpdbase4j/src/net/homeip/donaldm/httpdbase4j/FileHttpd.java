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

import com.sun.net.httpserver.HttpHandler;
import java.io.File;

/**
 * Implementation of the abstract Httpd class for web content in the
 * file system.
 * @see Httpd 
 * @author Donald Munro
 */
public class FileHttpd  extends Httpd implements HttpHandleable, Postable
//=======================================================================
{
   /**
    * Home directory for the HTTP server
    */
   protected File               m_httpHomeDir      = null;
   
    /**
    * Constructor with a home directory.
    * @param homeDir Home directory for the server
    */
   public FileHttpd(File homeDir)
   //------------------------
   {
      super();
      m_httpHomeDir = homeDir;
   }

   /**
    * Constructor with a home directory and thread model
    * @param homeDir Home directory for the server
    * @param threadModel The thread model to use (SINGLE, MULTI or POOL)
    */
   public FileHttpd(File homeDir, ThreadModel threadModel)
   //-------------------------------------------------
   {
      super();
      m_httpHomeDir = homeDir;
      m_threadModel = threadModel;
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
    * Constructor for a fixed size thread pool based server. Defaults to 
    * threadpool threading model.
    * @param homeDir Home directory for the server
    * @param poolSize Size of the thread pool
    */
   public FileHttpd(java.io.File homeDir, int poolSize)
   //-------------------------------------
   {
      super();
      m_httpHomeDir = homeDir;
      m_threadModel = ThreadModel.POOL;
      m_poolSize = poolSize;
      m_poolMax = poolSize;
   }

   /**
    * Constructor for a thread pool based server/
    * @param homeDir Home directory for the server
    * @param poolSize Size of the thread pool
    * @param maxPoolSize Maximum Size of the thread pool
    */
   public FileHttpd(java.io.File homeDir, int poolSize, int maxPoolSize)
   //-------------------------------------------------------
   {
      super();
      m_httpHomeDir = homeDir;
      m_threadModel = ThreadModel.POOL;
      m_poolSize = poolSize;
      m_poolMax = maxPoolSize;
   }

   /**
    * Get the home directory in the file system as a File.
    * @return Home directory 
    */
   public java.io.File getHomeDir()  { return m_httpHomeDir;  }

   /**
    * Get the home directory in the file system as a String.
    * @return Home directory 
   */
   @Override
   public String getHomePath() { return m_httpHomeDir.getAbsolutePath(); }
   
   /**
    * Creates a FileRequestHandler for handling file system
    * based requests. Can be overidden to provide a user specified
    * request handler.
    */
   @Override
   protected HttpHandler onCreateRequestHandler()
   //--------------------------------------------
   {
      return new FileRequestHandler(this, m_httpHomeDir, m_isVerbose);
   }

   @Override
   public String toString()
   {
      return "FileHttpd{" + "m_httpHomeDir=" + m_httpHomeDir + '}';
   }
}
