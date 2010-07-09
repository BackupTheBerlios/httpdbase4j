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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;

import com.sun.net.httpserver.HttpExchange;

import de.schlichtherle.io.File;
import de.schlichtherle.io.FileInputStream;

/**
 * <p>
 * Support for combining several style sheets or javascripts for archive or classpath 
 * based requests into one request. 
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
public class ArchiveCombinedRequest extends CombinedRequest implements Cloneable
//============================================================================
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
            
   /**
    * Create an ArchiveCombinedRequest.
    * @param httpd The Httpd instance
    * @param ex The HttpExchange instance for this HTTP transaction
    * @param homeDir Directory in archive where combined request files
    * reside.
    * @throws UnsupportedEncodingException
    * @throws IOException
    */
   protected ArchiveCombinedRequest(Httpd httpd, HttpExchange ex, File homeDir) 
             throws UnsupportedEncodingException, IOException
   //---------------------------------------------------------------------
   { 
      super(httpd, ex);
      m_homeDir = homeDir;
      splitUp();
   }
   
   /**
    * Add a file to the list of files for this CombinedRequest
    */
   @Override
   protected void addFile(String file)
   //---------------------------------
   {
      m_requestFiles.add(new File(m_homeDir, file));
   }
   
   /**
    * @return true if all the files in the combined request exist.
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
   
   @Override
   public boolean isReadable() { return this.exists(); }

   /**
    * @return The maximum date of all the files in the combined 
    * request
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
      StringBuilder names = new StringBuilder();
      for (int i=0;i <m_requestFiles.size(); i++)
      {         
         File f = new File(m_requestFiles.get(i).getInnerEntryName());
         String name = f.getName();         
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
         items[i] = new ArchiveRequest.DirItem(m_requestFiles.get(i));
      m_eTag = Http.eTag(items);
      return m_eTag;
   }
   
   /**
    * @return The number of files in the combined request
    */
   @Override protected int getCount() { return m_requestFiles.size(); }
      
   
   /**
    * @param i Index of the file for which to return an InputStream. 
    * @return An InputStream for the <i>i</i>th file in the combined request 
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
}
