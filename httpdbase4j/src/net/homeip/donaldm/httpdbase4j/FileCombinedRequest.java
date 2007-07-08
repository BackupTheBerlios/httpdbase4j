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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;

/**
 * <p>
 * Support for combining several style sheets or javascripts for file system based
 * requests into one request. 
 * For example the client could specify:
 * </p><br>
 * <code>
 * <link rel="stylesheet" type="text/css" media="all" href="styles/1.css,2.css,
 * morestyles/3.css" /> 
 * </code>
 * <p>
 * Different stylesheets/scripts are separated by commas; if the stylesheet name 
 * includes a / then it is assumed to be a full directory name otherwise it
 * uses the previous entries directory. If the previoud directory did not have
 * a directory then / is assumed eg
 * </p><br>
 * <code>
 * <script type="text/javascript" src="1.js,2.js,3.js">
 * </code><br>
 *  * 1.js, 2.js and 3.js are assumed to be in /.<br>
 * <p>
 * <b>This is non-standard HTML</b>, although it can be done in for example 
 * Apache using rewriting and server side scripts. 
 * (@see http://rakaz.nl/item/make_your_pages_load_faster_by_combining_and_compressing_javascript_and_css_files)
 * </p>
 * @author Donald Munro
 */
public class FileCombinedRequest extends CombinedRequest implements Cloneable
//=========================================================================
{
   
   /**
    * The base directory containing the resource
    */
   protected File                                  m_homeDir = null;
   
   /**
    * Array of full paths of the resource.
    */
   protected  ArrayList<File>                      m_requestFiles = 
                                                          new ArrayList<File>();
   
   public FileCombinedRequest(Httpd httpd, HttpExchange ex, File homeDir)
            throws UnsupportedEncodingException, IOException
   //------------------------------------------------------------
   {
      super(httpd, ex);
      m_homeDir = homeDir;
      splitUp();
   }
   
   /**
    *  @inheritDoc
    */
   @Override
   protected void addFile(String file)
   //---------------------------------
   {
      m_requestFiles.add(new File(m_homeDir, file.replace('/', 
                                      File.separatorChar)));
   }
   
   /**
    *  @inheritDoc
    */
   @Override
   protected int getCount() { return m_requestFiles.size(); }
      
   /**
    *  @inheritDoc
    */
   @Override
   protected InputStream getItemStream(int i)
   //----------------------------------------
   {
      try
      {
         return new FileInputStream(m_requestFiles.get(i));
      }
      catch (Exception e)
      {
         return null;
      }
   }
   
   /**
    *  @inheritDoc
    */
   @Override
   public boolean exists()
   //---------------------
   {
      for (int i=0;i <m_requestFiles.size(); i++)
      {
         if (! m_requestFiles.get(i).exists())
         {
            if (m_strict) return false;
         }
         else
            return true;
      }
      return false;
   }

   /**
    *  @inheritDoc
    */
   @Override   
   public boolean isReadable()
   //-------------------------
   {
      for (int i=0;i <m_requestFiles.size(); i++)
      {
         if (! m_requestFiles.get(i).canRead())
         {
            if (m_strict) return false;
         }
         else
            return true;
      }
      return false;
   }


   /**
    *  @inheritDoc
    */
   @Override   
   public Date getDate()
   //-------------------
   {
      long maxt = 0;
      for (int i=0;i <m_requestFiles.size(); i++)
      {
          File f = m_requestFiles.get(i);
          long t = f.lastModified();
          if (t > maxt) maxt = t;
      }
      return new Date(maxt);
   }

   /**
    *  @inheritDoc
    */
   @Override
   public String getName()
   //--------------------
   {
      StringBuffer names = new StringBuffer();
      for (int i=0;i <m_requestFiles.size(); i++)
      {         
         String name = m_requestFiles.get(i).getName();
         if (names.length() > 0)
         {
            names.append(name);
            names.append(m_delimiter);
         }                    
      }
      return names.toString();
   }
   
   /**
    *  @inheritDoc
    */
   @Override
   public String getETag(boolean refresh)
   //---------------------
   {
      if ( (! refresh) && (m_eTag != null) )
         return m_eTag;
      DirItemInterface[] items = new DirItemInterface[m_requestFiles.size()];
      for (int i=0;i <m_requestFiles.size(); i++)
         items[i] = new FileRequest.DirItem(m_requestFiles.get(i));
      m_eTag = Http.eTag(items);
      return m_eTag;
   }
   
}
