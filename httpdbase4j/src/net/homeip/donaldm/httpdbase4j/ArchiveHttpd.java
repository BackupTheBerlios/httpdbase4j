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

import de.schlichtherle.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLClassLoader;

import com.sun.net.httpserver.HttpHandler;
import java.lang.reflect.Method;
import java.net.URI;

/**
 * Implementation of the abstract Httpd class for web content in a
 * jar in the classpath or in an archive.
 * @see Httpd 
 * @author Donald Munro
 */
public class ArchiveHttpd extends Httpd implements HttpHandleable, Postable
//=====================================================================
{
   /**
    * Compressed file containing web content. Prefarably jar but can also be
    * zip, tar, tar.gz or tar.bz2
    */
   protected File                        m_archiveDirectory =null;   
      
   protected java.io.File                m_altFileSystemHome = null;

      
   /**
    * Constructor with a home resource location. Assumes the content is in a jar 
    * in the classpath and will search the classpath to attempt to find the jar.
    * @param homeDir The base directory in the jar where content is located
    * @throws java.io.FileNotFoundException 
    */
   public ArchiveHttpd(String homeDir) throws FileNotFoundException
   //----------------------------------------------------------
   {
      super();
      m_archiveDirectory = findJarInCP(homeDir);
      if (m_archiveDirectory == null)
         throw new FileNotFoundException(homeDir + " not found in classpath");
      }
   
   /**
    * Constructor with a jar file containing the resources and a home resource location
    * within the jar.
    * @param archiveFile A content jar, zip, tar, tar.gz or tar.bz2 file containing 
    * directory homeDir.    
    * @param archiveDirName The base directory in the content compressed file  
    * where content is located
    * @throws java.io.FileNotFoundException 
    */
   public ArchiveHttpd(java.io.File archiveFile, String archiveDirName) 
          throws FileNotFoundException 
   //---------------------------------------------------------------------------------
   {
      super();
      if ( (archiveDirName == null) || (archiveDirName.trim().length() == 0) )
         archiveDirName = "/";
      archiveDirName = archiveDirName.trim();
      m_archiveDirectory = new File(archiveFile, archiveDirName);
      if (! m_archiveDirectory.exists())
         throw new FileNotFoundException(archiveFile.getAbsolutePath() + " not found");
      if ( (archiveDirName.compareTo("/") != 0) &&
           (m_archiveDirectory.getEnclArchive() == null) )
         throw new FileNotFoundException(archiveDirName + " not found in " + 
                                         archiveFile.getName());
   }      
   
   /**
    * Constructor with a home resource location and a thread model. Assumes the content is in a jar 
    * in the classpath and will search the classpath to attempt to find the jar.
    * @param archiveDirName The base directory in the jar where content is located
    * @param threadModel The thread model to use (SINGLE, MULTI or POOL)
    */
   public ArchiveHttpd(String archiveDirName, ThreadModel threadModel)
          throws FileNotFoundException
   //--------------------------------------------------------------
   {
      super();
      m_archiveDirectory = findJarInCP(archiveDirName);
      m_threadModel = threadModel;
      setDefaultPoolSizes();
   }

   /**
    * Constructor with a jar file, home resource location and a thread model
    * @param archiveFile A content Jar file containing directory homeDir.
    * @param archiveDirName The base directory in the jar where content is located    
    * @param threadModel The thread model to use (SINGLE, MULTI or POOL)
    * @throws java.io.FileNotFoundException 
    */
   public ArchiveHttpd(java.io.File archiveFile, String archiveDirName, 
                   ThreadModel threadModel)
          throws FileNotFoundException
   //-------------------------------------------------------------------------
   {
      this(archiveFile, archiveDirName);
      m_threadModel = threadModel;
      setDefaultPoolSizes();
   }
   
   /**
    * Constructor for a fixed size thread pool based server. Assumes the content is in a jar 
    * in the classpath and will search the classpath to attempt to find the jar.
    * @param archiveDirName The base directory in the jar where content is located
    * @param poolSize Size of the thread pool
    */
   public ArchiveHttpd(String archiveDirName, int poolSize) 
          throws FileNotFoundException
   //---------------------------------------------------
   {
      super();
      m_archiveDirectory = findJarInCP(archiveDirName);
      m_threadModel = ThreadModel.POOL;
      m_poolSize = poolSize;
      m_poolMax = poolSize;
   }

   /**
    * Constructor for a thread pool based server. Assumes the content is in a jar 
    * in the classpath and will search the classpath to attempt to find the jar.
    * @param archiveDirName The base directory in the jar where content is located
    * @param poolSize Size of the thread pool
    * @param maxPoolSize Maximum Size of the thread pool
    */
   public ArchiveHttpd(String archiveDirName, int poolSize, int maxPoolSize)
          throws FileNotFoundException
   //-----------------------------------------------------------------
   {
      super();
      m_archiveDirectory = findJarInCP(archiveDirName);
      m_threadModel = ThreadModel.POOL;
      m_poolSize = poolSize;
      m_poolMax = maxPoolSize;
   }

   /**
    * Constructor for a thread pool based server.
    * @param archiveFile A content Jar file containing directory homeDir.
    * @param archiveDirName The base directory in the jar where content is located
    * @param poolSize Size of the thread pool
    * @param maxPoolSize Maximum Size of the thread pool
    * @throws java.io.FileNotFoundException 
    */
   public ArchiveHttpd(java.io.File archiveFile, String archiveDirName, 
                       int poolSize, int maxPoolSize)
          throws FileNotFoundException
   //-------------------------------------------------------
   {
      this(archiveFile, archiveDirName);            
      m_threadModel = ThreadModel.POOL;
      m_poolSize = poolSize;
      m_poolMax = maxPoolSize;
   }

   /**
    * Get the directory within the archive for the base content.
    * @return The directory within the archive for the base content.
    */
   public String getArchiveDirectoryName()
   //---------------------------
   {
      return m_archiveDirectory.getInnerEntryName();
   }
   
   /**
    * Get the archive file for the base content.
    * @return The archive file name for the base content.
    */
   public String getArchiveFileName() 
   //--------------------------
   { 
      return m_archiveDirectory.getTopLevelArchive().getAbsolutePath(); 
   }
   
   /**
    * Get the archive file for the base content.
    * @return The archive file for the base content.
    */
   public java.io.File getArchiveFile() 
   //--------------------------
   { 
      if ( (m_archiveDirectory != null) && 
           (m_archiveDirectory.getTopLevelArchive() != null) )
         return new File(m_archiveDirectory.getTopLevelArchive().getAbsolutePath()); 
      return null;
   }
   
   public void setJarDirectory(java.io.File archiveFile, String archiveDirName)
          throws FileNotFoundException
   //-----------------------------------------------------------------------
   {
      m_archiveDirectory = new de.schlichtherle.io.File(archiveFile, archiveDirName);
      if ( (! m_archiveDirectory.exists()) ||  (! m_archiveDirectory.isArchive()) )
         throw new FileNotFoundException(archiveDirName + " not found in " +
                                         archiveFile.getAbsolutePath());
   }
   
   @Override
   public String getHomePath() { return m_archiveDirectory.getInnerEntryName(); }
   
   public void setAltFileSystemHome(java.io.File altHome)
   //----------------------------------------------------
   {
      m_altFileSystemHome = altHome;
      
   }
   
   public  java.io.File getAltFileSystemHome() { return m_altFileSystemHome; }
   
   @Override
   protected HttpHandler onCreateRequestHandler()
   //--------------------------------------------
   {
      ArchiveRequestHandler handler = new ArchiveRequestHandler(this, m_archiveDirectory, 
                                                        m_altFileSystemHome,
                                                        m_isVerbose);
      if (m_altFileSystemHome == null)
         m_altFileSystemHome = handler.getAltFileSystemHome();
      return handler;
   }
   
   private File findJarInCP(String homeDir) throws FileNotFoundException
   //--------------------------------------------------------------------
   {
      ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
      URL[] urls = ((URLClassLoader)sysClassLoader).getURLs();
      String[] jars = new String[urls.length];
      for(int i=0; i< urls.length; i++)
         jars[i] = urls[i].getFile();
/*    String cp = System.getProperty("java.class.path");
      if ( (cp == null) || (cp.length() == 0) )
         return null;
      String[] jars = cp.split(File.pathSeparator);*/
      for (int i=0; i<jars.length; i++)
      {
         String jar = jars[i];
         if (jar == null) continue;
         jar = jar.trim();
         if (  (jar.length() == 0) || (jar.compareTo(".") == 0) ||
               (jar.compareTo("..") == 0) )
            continue;
         File f = new File(jar);
         if ( ! f.exists()) continue;
         if ( (f.isArchive()) || (f.isDirectory()) )
         {
            File home = new File(f, homeDir);
            if (home.exists()) 
               return home;         
         }
      }
      throw new FileNotFoundException(homeDir + " not found in classpath");
   }
   
   public static boolean addToClassPath(String jarFile) 
                         throws FileNotFoundException 
   //---------------------------------------------------------------------------
   {
      java.io.File f = new java.io.File(jarFile);
      if (! f.exists()) throw new FileNotFoundException(jarFile);
      Class[] parameters = new Class[] { URL.class };
      URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
      Class<URLClassLoader> sysclass = URLClassLoader.class;
      //String urlPath = "jar:file://" + f.getAbsolutePath() + "!/";      
      try
      {
         URI uri = f.toURI();
         URL url = uri.toURL();
         Method method = sysclass.getDeclaredMethod("addURL", parameters);
         method.setAccessible(true);
         
         method.invoke(sysloader, new Object[] { url });
      }
      catch (Throwable t)
      {
         t.printStackTrace();
         return false;
      }
      return true;
   }

   public static boolean isInClassPath(String jarFileName)
   //-----------------------------------------------------
   {
      ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
      URL[] urls = ((URLClassLoader)sysClassLoader).getURLs();
      for(int i=0; i< urls.length; i++)
      {
         java.io.File jarInPath = new java.io.File(urls[i].getFile());
         java.io.File jarFile = new java.io.File(jarFileName);
         System.out.println(jarFile.getName() + " " + jarInPath.getName());
         if (jarFile.getName().compareTo(jarInPath.getName()) == 0)
            return true;
      }
      return false;
   }

   public static String getClasspath()
   //-----------------------------
   {
      ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
      URL[] urls = ((URLClassLoader)sysClassLoader).getURLs();
      StringBuilder sb = new StringBuilder();
      for(int i=0; i< urls.length; i++)
      {
         sb.append(urls[i].getFile());
         sb.append(':');
      }
      return sb.toString();
   }
}
