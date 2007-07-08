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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.TreeSet;

import com.sun.net.httpserver.HttpExchange;

/**
 * <p>
 * Support for combining several style sheets or javascripts into one  
 * request. For example the client could specify:
 * </p><br>
 * <code>
 * <link rel="stylesheet" type="text/css" media="all" 
 *  href="styles/1.css!+!2.css!+!morestyles/3.css" /> 
 * </code>
 * <p>
 * Different stylesheets/scripts are separated by a separator string eg !+! in 
 * this case; if the stylesheet name includes a / then it is assumed to be a 
 * full directory name otherwise it uses the directory of the last entry that 
 * had a directory. </p><br>
 * <code
 * <script type="text/javascript" src="/scripts/1.js!+!2.js!+!3.js">
 * </code>
 * <p>
 * 2.js and 3.js will be assumed to be in /scripts. </p>/<br>
 * <p>
 * If the previous directory did not have a directory then / is assumed eg
 * </p><br>
 * <code>
 * <script type="text/javascript" src="1.js!+!2.js!+!3.js">
 * </code><br>
 *  * 1.js, 2.js and 3.js are assumed to be in /.<br>
 * <p>
 * <b>This is non-standard HTML</b>, although it can be done in for example 
 * Apache using rewriting and server side scripts. 
 * (@see http://rakaz.nl/item/make_your_pages_load_faster_by_combining_and_compressing_javascript_and_css_files)
 * </p>
 * @see ArchiveCombinedRequest
 * @see FileCombinedRequest
 * @author Donald Munro
 */
abstract public class CombinedRequest extends Request implements Cloneable
//=========================================================================
{   
   /**
    * File that contains all the individual request files combined. 
    */
   protected File                                  m_combinedFile = null;
   
   /**
    * Buffer to store the contents of all the individual request files combined
    * in the event that a temporary combined file could not be created.
    */
   protected byte[]                                m_combinedArray = null;
   
   /**
    * Delimiter to separate files in script or style tag.
    */
   protected String                                m_delimiter = null;
   
   /**
    * The extension for this CombinedRequest. (Extensions must be 
    * homogenous for a given CombinedRequest).
    */
   protected String                                m_extension = "";
   
   /**
    * File extensions supported by this CombinedRequest.
    */
   protected String[]                              m_extensions = null;
   
   /**
    *  If true if one of the resources does not exist then exists returns
    *  false otherwise it only returns false if all of the resources don't exist
    */
   protected boolean                               m_strict = true;   
      
   /**
    * Create a CombinedRequest.
    * @param httpd - The Httpd instance
    * @param ex - The HttpExchange for the HTTP exchange this request 
    * is involved in.
    * @throws UnsupportedEncodingException
    * @throws IOException
    */
   public CombinedRequest(Httpd httpd, HttpExchange ex) 
          throws UnsupportedEncodingException, IOException
   //---------------------------------------------------------------------
   { 
      this(httpd, ex, RequestHandler.COMBINED_REQUEST_DELIMITER, 
           RequestHandler.COMBINED_REQUEST_EXTENSIONS);
   }
   
   /**
    * Create a CombinedRequest.
    * @param httpd - The Httpd instance
    * @param ex - The HttpExchange for the HTTP exchange this request 
    * is involved in.
    * @param delimiter - The delimiter that separates files in the 
    * request tag. It is used as a regular expression so regular
    * expression escapping should be used where applicable.
    * @param extensions - The extensions supported by this request
    * @throws UnsupportedEncodingException
    * @throws IOException
    */
   public CombinedRequest(Httpd httpd, HttpExchange ex, String delimiter,
                          String... extensions) 
          throws UnsupportedEncodingException, IOException
   //---------------------------------------------------------------------
   { 
      super(httpd, ex);
      m_delimiter = delimiter;
      m_extensions = new String[extensions.length];
      for (int i=0; i<extensions.length; i++)  m_extensions[i] = extensions[i];
   }
   
   /**
    * Split the request into the individual request files and add
    * files to list of files for this request. Calls addFile for 
    * each file.
    * @throws UnsupportedEncodingException
    */
   protected void splitUp() throws UnsupportedEncodingException
   //-----------------------------------------------------------
   {
      String[] reqs = m_uri.getPath().split(m_delimiter);
      String cwd  = "";
      m_extension = Http.getExtension(new File(reqs[0]));
      int i;
      for (i=0; i<m_extensions.length; i++)
      {
         String extension = m_extensions[i].trim();
         if (! extension.startsWith("."))
            extension = "." + extension;
         if (m_extension.compareToIgnoreCase(extension) == 0)
            break;            
      }   
      if (i >= m_extensions.length)
         throw new UnsupportedEncodingException("Unsupported extension " + 
                                                m_extension);
      for (i=0; i<reqs.length; i++)
      {
         String s = reqs[i];
         if (m_extension.compareToIgnoreCase(Http.getExtension(new File(s))) != 0)
            throw new UnsupportedEncodingException("Only files of the same " 
                    + " extension on the same line are supported (" + 
                    m_extension + " " + s + ")");
         int p = s.lastIndexOf('/');
         if (p >= 0)
            cwd = s.substring(0, p);
         else
            s = cwd + "/" + s;
         addFile(s);
      }
   }
   
   /**
    * Add a file to the list of files for this CombinedRequest.
    * @param file
    */
   abstract protected void addFile(String file);
      
   public boolean isDirectory()  { return false; }

   abstract public Date getDate();
   
    /**
    *  @inheritDoc
    */
   @Override
   public long getContentLength()
   //----------------------------
   {  
      if (m_cacheFile != null) 
      {
         m_contentLength = m_cacheFile.length();
         return m_contentLength;
      }      
      
      if ( (m_combinedFile != null) && (m_combinedFile.exists()) )
      {
         m_contentLength = m_combinedFile.length();
         return m_contentLength;
      }
      
      if (m_combinedArray != null) 
      {
         m_contentLength = m_combinedArray.length;
         return m_contentLength;
      }   
      return -1;
   } 

   /**
    *  @inheritDoc
    */
   @Override
   public InputStream getStream()
   //----------------------------
   {      
      return getStream(true);
   }
   
    /**
    *  @inheritDoc
    */
   @Override
   public InputStream getStream(boolean isEncoded)
   //---------------------------------------------
   {      
      if ( (! isEncoded) || (m_cacheFile == null) )
         return _getRawStream();
      else
      {
         try
         {
            return new FileInputStream(m_cacheFile);
         }
         catch (Exception e)
         {
            m_requestHeaders.add("Pragma", "no-cache");
            m_encoding = null;
            return _getRawStream();
         }
      }
   }
   
   private InputStream _getRawStream()
   //---------------------------------
   {
      if (! combineFiles()) return null;
      if (m_combinedFile != null)
      {
         try
         {
            return new FileInputStream(m_combinedFile);
         }
         catch (Exception e)
         {            
            m_combinedFile = null;
         }
      }
      if (m_combinedArray != null)
         return new ByteArrayInputStream(m_combinedArray);
      return null;
   }

   /**
    *  @inheritDoc
    */
   @Override
   public String getExtension()
   //--------------------------
   {
      return m_extension;
   }
   
   public String getAbsolutePath()
   {
      throw new UnsupportedOperationException("Not supported.");
   }

   public String getName()
   {
      throw new UnsupportedOperationException("Not supported.");
   }

   public String getDir()
   {
      throw new UnsupportedOperationException("Not supported.");
   }

   public Request getDirRequest()
   {
      throw new UnsupportedOperationException("Not supported.");
   }

   public Request getChildRequest(String name)
   {
      throw new UnsupportedOperationException("Not supported.");
   }

   protected HttpHandleable getHandler()
   //-----------------------------------
   {
      return m_httpd;
   }

   protected Postable getPostHandler()
   {
      throw new UnsupportedOperationException("Not supported.");
   }

   public TreeSet<DirItemInterface> getDirListFiles(SORTBY sortBy)
   {
      throw new UnsupportedOperationException("Not supported.");
   }

   public TreeSet<DirItemInterface> getDirListFiles(boolean isLength, 
                                                    SORTBY sortBy)
   {
      throw new UnsupportedOperationException("Not supported.");
   }

   public TreeSet<DirItemInterface> getDirListDirectories(SORTBY sortBy)
   {
      throw new UnsupportedOperationException("Not supported.");
   }
   
   abstract protected int getCount();
   
   abstract protected InputStream getItemStream(int i);
    
   private boolean combineFiles()
   //----------------------------
   {
      BufferedInputStream bis = null;
      BufferedOutputStream bos = null;
      ByteArrayOutputStream combinedBuffer = null;
      byte[] buffer = new byte[4096];
      try
      {
         m_combinedFile = null;
         if ( (m_cacheDir != null) && (m_cacheDir.exists()) )
         {
            try
            {
               m_combinedFile = File.createTempFile("combine", ".tmp", m_cacheDir);
            }
            catch (Exception e)
            {
               m_combinedFile = null;
            }
            if (m_combinedFile != null)
               bos = new BufferedOutputStream(new FileOutputStream(m_combinedFile));            
         }
         if (m_combinedFile == null)
         {
            combinedBuffer = new ByteArrayOutputStream();  
            bos = new BufferedOutputStream(combinedBuffer);
         }
         for (int i=0;i<getCount(); i++)
         {     
            try
            {
               bis = new BufferedInputStream(getItemStream(i));
               while (true)
               {
                  int cb = bis.read(buffer);
                  if (cb == -1) break;
                  bos.write(buffer, 0, cb);
               }
               bos.write(13);
               bis.close();
               bis = null;            
            }
            catch (Exception e)
            {
               Httpd.Log(Httpd.LogLevel.INFO, "Combining files", e);
               continue;
            }
         }
         bos.close();
         bos = null;
         if (m_combinedFile == null)
            m_combinedArray = combinedBuffer.toByteArray();
      }
      catch (Exception e)
      {
         Httpd.Log(Httpd.LogLevel.ERROR, "Combining files", e);
         return false;
      }
      finally
      {
         if (bis != null)
            try { bis.close(); } catch (Exception e) {}
         if (bos != null)
            try { bos.close(); } catch (Exception e) {}
      }
      return true;
   }

   public String getETag(boolean refresh)
   //------------------------------------
   {
      throw new UnsupportedOperationException("Not supported");
   }

   public long getSize()
   {
      throw new UnsupportedOperationException("Not supported yet.");
   }
}
