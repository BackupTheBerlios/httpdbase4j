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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.Date;
import java.util.TreeSet;

/**
 * Encapsulates an HTTP request for a resource in a local file system 
 * @see net.homeip.donaldm.httpdbase4j.Request
 * @author Donald Munro
 */
public class FileRequest extends Request implements DirItemInterface, Cloneable
//=============================================================================        
{
   /**
    * The base directory containing the resource
    */
   protected File m_homeDir     = null;

   /**
    * The full path of the resource.
    */
   protected File m_requestFile = null;

   /**
    * Constructs a FileRequest.
    * @param httpd The Httpd instance within which the request occurred.
    * @see net.homeip.donaldm.httpdbase4j.Httpd
    * @param ex The HttpExchange instance for this request.
    * @see com.sun.net.httpserver.HttpExchange
    * @param homeDir The base directory containing the resource
    * @throws UnsupportedEncodingException
    * @throws IOException
    */
   public FileRequest(Httpd httpd, HttpExchange ex, File homeDir)
            throws UnsupportedEncodingException, IOException
   //------------------------------------------------------------
   {
      super(httpd, ex);
      m_homeDir = homeDir;
      m_requestFile = new File(m_homeDir, m_path.replace('/',
               File.separatorChar));
   }

   /**
    * Constructs a FileRequest.
    * @param httpd The Httpd instance within which the request occurred.
    * @see net.homeip.donaldm.httpdbase4j.Httpd
    * @param ex The HttpExchange instance for this request.
    * @see com.sun.net.httpserver.HttpExchange
    * @param homeDir The base directory containing the resource
    * @param f The request file 
    * @throws UnsupportedEncodingException
    * @throws IOException
    */
   public FileRequest(Httpd httpd, HttpExchange ex, File homeDir, File f)
            throws UnsupportedEncodingException, IOException
   //--------------------------------------------------------------------
   {
      super(httpd, ex);
      m_homeDir = homeDir;
      String home = m_homeDir.getAbsolutePath();
      String filePath = "";
      if (f != null)
         filePath = f.getPath();
      if (filePath.startsWith(home))
      {
         if (filePath.length() > home.length())
            filePath = filePath.substring(home.length() + 1);
         else
            filePath = "";
      }
      m_path = filePath;
      m_path = m_path.replace(File.separatorChar, '/');
      if (m_path.startsWith("/")) m_path = m_path.substring(1);
      m_requestFile = new File(m_homeDir, m_path.replace('/',
               File.separatorChar));
   }
   
   /**
    * Copy constructs a FileRequest with a new base directory and file
    * @param request The Request to copy
    * @param homeDir The base directory for the request
    * @param f The request file 
    * @throws UnsupportedEncodingException
    * @throws IOException
    * @throws URISyntaxException
    */
   public FileRequest(Request request, File homeDir, File f)
            throws UnsupportedEncodingException, IOException,
            URISyntaxException
   //-------------------------------------------------------------------------
   {
      super(request);
      m_homeDir = homeDir;
      String home = m_homeDir.getAbsolutePath();
      String filePath = "";
      if (f != null)
         filePath = f.getPath();
      else
         filePath = request.getPath();
      filePath = filePath.replace('/', File.separatorChar);
      if ( (filePath.compareTo(".") == 0) || 
           (filePath.startsWith("." + File.separatorChar)) )
      {
         String requestResource = request.getAbsolutePath();
         if (! request.isDirectory())
            requestResource = request.getDir();
         filePath = filePath.replaceFirst("\\.", requestResource);
      }
      if (filePath.startsWith(home))
      {
         if (filePath.length() > home.length())
            filePath = filePath.substring(home.length() + 1);
         else
            filePath = "";
      }
      m_path = filePath;
      m_path = m_path.replace(File.separatorChar, '/');
      m_uri = new URI(m_uri.getScheme(), m_uri.getUserInfo(), m_uri.getHost(),
               m_uri.getPort(), m_path, m_uri.getQuery(), m_uri.getFragment());
      m_requestFile = new File(m_homeDir, filePath);
   }

   /**
    * Copy constructs a FileRequest with a new file.
    * If defaultFileName is not null then assumes that request is a directory. 
    * @param request The FileRequest instance to copy
    * @param fileName The new file for the request.
    * @throws UnsupportedEncodingException
    * @throws IOException
    * @throws URISyntaxException
    */
   public FileRequest(FileRequest request, String fileName)
            throws UnsupportedEncodingException, IOException,
            URISyntaxException
   //-------------------------------------------------------------------------
   {
      super(request);
      m_homeDir = request.m_homeDir;
      String homeDir = m_homeDir.getAbsolutePath();      
      if (fileName != null)
      {
         fileName = fileName.replace('/', File.separatorChar);
         if ( (fileName.compareTo(".") == 0) || 
              (fileName.startsWith("." + File.separatorChar)) )
         {
            String requestResource = request.m_requestFile.getPath();
            if (! request.isDirectory())
               requestResource = request.getDir();
            fileName = fileName.replaceFirst("\\.", requestResource);
         }
         
         int p =-1;
         if (fileName.startsWith(homeDir))
            p = fileName.indexOf(homeDir) + homeDir.length();
         else
         {
            String s = m_homeDir.getPath();
            if (fileName.startsWith(s))
               p = fileName.indexOf(s) + s.length();
         }
         if (p >= 0)
            fileName = fileName.substring(p);         
      }
      else
         fileName = "";
      fileName = fileName.trim();
      if (fileName.length() == 0)
         fileName = "/";
      if ( (fileName.startsWith("/")) || (fileName.startsWith(File.separator)) )
      {
         if (fileName.length() > 1)
            m_path = fileName.substring(1);
         else
            m_path = "";
      }
      else
         m_path = m_path + 
                 (( (m_path.length() > 0) && (! m_path.endsWith("/"))) ? "/" : "") 
                 + fileName;
      m_requestFile = new File(m_homeDir, m_path);
      m_path = m_path.replace(File.separatorChar, '/');
      m_uri = new URI(m_uri.getScheme(), m_uri.getUserInfo(), m_uri.getHost(),
               m_uri.getPort(), m_path, m_uri.getQuery(), m_uri.getFragment());
   }

   /**
    *  @inheritDoc
    */
   @Override
   public boolean exists()
   //---------------------
   {
      return m_requestFile.exists();
   }

   /**
    *  @inheritDoc
    */
   @Override
   public boolean isReadable()
   //-------------------------
   {
      return m_requestFile.canRead();
   }

   /**
    *  @inheritDoc
    */
   @Override
   public long getContentLength()
   //---------------------------
   {
      if (m_cacheFile != null) 
         m_contentLength = m_cacheFile.length();
      else
         m_contentLength = m_requestFile.length();
      return m_contentLength;
   }

   /**
    *  @inheritDoc
    */
   @Override
   public boolean isDirectory()
   //-----------------------------
   {
      return m_requestFile.isDirectory();
   }

   /**
    *  @inheritDoc
    */
   @Override
   public String getAbsolutePath()
   //-----------------------------
   {
      return m_requestFile.getAbsolutePath();
   }

   /**
    *  @inheritDoc
    */
   @Override
   public String getName()
   //-----------------------------
   {
      return m_requestFile.getName();
   }

   /**
    *  @inheritDoc
    */
   @Override
   public String getExtension()
   //--------------------------
   {
      String name = m_requestFile.getName();
      int p = name.lastIndexOf('.');
      String ext = "";
      if (p >= 0)
         ext = name.substring(p);
      return ext;
   }
   
   /**
    *  @inheritDoc
    */
   @Override
   public String getDir()
   //--------------------
   {
      String s = m_requestFile.getParent();
      return (s == null) ? "" : s;
   }

   /**
    *  @inheritDoc
    */
   @Override
   public Request getDirRequest()
   //----------------------------
   {
      try
      {
         return new FileRequest(this, getDir());
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
   public Request getChildRequest(String name) 
   //---------------------------------------------
    {
      if (! this.isDirectory()) return null;
      try
      {
         return new FileRequest(this, name);
      }
      catch (Exception e)
      {
         return null;
      }              
   }
   
   static class DirItem implements DirItemInterface
   //==============================================
   {
      File m_file;

      public DirItem(File f)  {  m_file = f; }

      public String getName()  { return m_file.getPath();   }

      public long getSize() { return m_file.length(); }

      public Date getDate() { return new Date(m_file.lastModified()); }

      public boolean isDirectory() { return m_file.isDirectory();  }
      
      public InputStream getStream()
      {
         try
         {
            return new FileInputStream(m_file);
         }
         catch (FileNotFoundException ex)
         {
            return null;
         }
      }
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
      DirItemInterface f = new DirItem(m_requestFile);
      m_eTag = Http.eTag(f);
      return m_eTag;
   }
   
   private TreeSet<DirItemInterface> _readDir(File directory,
            final boolean isDirs, final DirItemInterface.SORTBY sortBy)
   //------------------------------------------------------------------------
   {
      File[] files = directory.listFiles(new FileFilter()
      {
         public boolean accept(File f)
         {
            if (isDirs)
               return f.isDirectory();
            else
               return f.isFile();
         }
      });

      TreeSet<DirItemInterface> set = new TreeSet<DirItemInterface>(
      new Comparator<DirItemInterface>()
      //--------------------------------
      {
         public int compare(DirItemInterface di1, DirItemInterface di2)
         {
            switch (sortBy)
            {
               case NAME:
                  return di1.getName().compareTo(di2.getName());

               case SIZE:
                  if (di1.getSize() < di2.getSize())
                     return -1;
                  else
                     if (di1.getSize() > di2.getSize())
                        return 1;
                     else
                        return 0;

               case DATE:
                  return (di1.getDate().compareTo(di2.getDate()));
            }
            return 0;
         }
      });
      for (int i = 0; i < files.length; i++)
      {
         File f = files[i];
         DirItem dirItem = new DirItem(f);
         set.add(dirItem);
      }
      return set;
   }

   /**
    *  @inheritDoc
    */
   @Override
   public TreeSet<DirItemInterface> getDirListFiles(
            DirItemInterface.SORTBY sortBy)
   //----------------------------------------------------------------------------
   {
      if (!isDirectory()) return null;
      return _readDir(m_requestFile, false, sortBy);
   }

   /**
    *  @inheritDoc
    */
   @Override
   public TreeSet<DirItemInterface> getDirListDirectories(
            DirItemInterface.SORTBY sortBy)
   //----------------------------------------------------------------------------
   {
      if (!isDirectory()) return null;
      return _readDir(m_requestFile, true, sortBy);
   }

   /**
    *  @inheritDoc
    */
   @Override
   protected HttpHandleable getHandler()
   //-----------------------------------
   {
      HttpHandleable handler = null;
      String extension = Http.getExtension(m_requestFile);
      if ((extension != null) && (extension.trim().length() > 0))
         handler = m_httpd.getHandler(extension);
      if (handler == null) handler = m_httpd;
      return handler;
   }

   /**
    *  @inheritDoc
    */
   @Override
   protected Postable getPostHandler()
   //---------------------------------
   {
      Postable postHandler = null;
      String extension = Http.getExtension(m_requestFile);
      if ((extension != null) && (extension.trim().length() > 0))
         postHandler = m_httpd.m_postHandlerMap.get(extension);
      System.out.println(m_uri.getPath());
      Postable pst = m_httpd.m_postHandlerMap.get(m_uri.getPath());
      if (pst == null) // be nice to people who forget abouty the leading slash
         pst = m_httpd.m_postHandlerMap.get("/" + m_uri.getPath());
      if (pst != null) // url handler has priority over extension
         postHandler = pst;
      if (postHandler == null) postHandler = m_httpd;
      return postHandler;
   }

   /**
    *  @inheritDoc
    */
   public Object clone() throws CloneNotSupportedException
   //-----------------------------------------------------
   {
      FileRequest klone = (FileRequest) super.clone();
      klone.m_homeDir = m_homeDir;
      klone.m_requestFile = m_requestFile;
      return klone;
   }
   
   /**
    *  @inheritDoc
    */
   @Override
   public InputStream getStream(boolean isEncoded)
   //---------------------------------------------
   {      
      if ( (! isEncoded) || (m_cacheFile == null) )
      {
         try
         {
            return new FileInputStream(m_requestFile);
         }
         catch (Exception e)
         {
            return null;
         }
      }
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
            try
            {
               return new FileInputStream(m_requestFile);
            }
            catch (Exception ee)
            {
               return null;
            }
         }
      }
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
   
   @Override
   public String toString()
   //----------------------
   {
      StringBuffer sb = new StringBuffer();
      sb.append("Base :" + m_homeDir.getAbsolutePath()); sb.append(Httpd.EOL);
      sb.append("Path :" + m_requestFile.getAbsolutePath()); sb.append(Httpd.EOL);
      return super.toString() + Httpd.EOL + sb.toString();
   }

   public Date getDate()
   //--------------------
   {
      return new Date(m_requestFile.lastModified());
   }

   public long getSize()
   //-------------------
   {
      return m_requestFile.length();
   }
}
