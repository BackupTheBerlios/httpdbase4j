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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

import de.schlichtherle.io.File;
import de.schlichtherle.io.FileInputStream;

/**
 * StringTemplateGroup (from org.antlr.stringtemplate) subclass to load templates from 
 * inside an archive file using TrueZip.
 */
public class ArchiveStringTemplateGroup extends StringTemplateGroup
//=============================================================
{
   
   private File m_jarDirectory = null;
   
   public ArchiveStringTemplateGroup(String name, File jarDirectory)
   //-----------------------------------------------------------
   {
      super(name, 
            (jarDirectory.getEnclArchive() == null) 
               ? jarDirectory.getAbsolutePath()
               : jarDirectory.getEnclArchive().getAbsolutePath());
      m_jarDirectory = jarDirectory;
   }
   
   public ArchiveStringTemplateGroup(String name, java.io.File jarDirectory)
   //--------------------------------------------------------------------
   {
      super(name, jarDirectory.getAbsolutePath());
      m_jarDirectory = new File(jarDirectory);
   }
   
   
   public ArchiveStringTemplateGroup(String name, java.io.File archiveFile, 
                                 String archiveDir)
   //-----------------------------------------------------------
   {
      super(name, archiveFile.getAbsolutePath());
      m_jarDirectory = new File(archiveFile, archiveDir);
   }
   
   public ArchiveStringTemplateGroup(String name, File archiveFile, 
                                 String archiveDir)
   //-----------------------------------------------------------
   {
      super(name, archiveFile.getAbsolutePath());
      m_jarDirectory = new File(archiveFile, archiveDir);
   }
   
   /**
    *  @inheritDoc
    */
   @Override
   protected StringTemplate loadTemplate(String templateName, String fileName)
   //-----------------------------------------------------------------
   {
      String archivePath = (m_jarDirectory.getEnclArchive() == null) 
                              ? m_jarDirectory.getAbsolutePath()
                              : m_jarDirectory.getEnclArchive().getAbsolutePath();
      String archiveDir = "/";
      int p = m_jarDirectory.getAbsolutePath().indexOf(archivePath);
      if (p >= 0)
         archiveDir = m_jarDirectory.getAbsolutePath().substring(p+
                                                         archivePath.length());
      if (archiveDir.length() == 0)
         archiveDir = "/";
      p = fileName.indexOf(archivePath);
      if (p >= 0)
         fileName = fileName.substring(p+archivePath.length());         
      File f = new File(m_jarDirectory, fileName);
      if (! f.exists())
      {
         System.err.println("WARNING:" + fileName + " not found in " + archivePath);
         p = fileName.indexOf(archiveDir);
         if (p >= 0)
            fileName = fileName.substring(p+archiveDir.length());         
         else
            fileName = archiveDir + "/" + fileName;
         f = new File(m_jarDirectory, fileName);
         if (! f.exists())
         {
            System.err.println("WARNING:" + fileName + " not found in " + archivePath);
            p = fileName.lastIndexOf(File.separatorChar);
            if (p < 0)
               p = fileName.lastIndexOf('/');            
            if ( (p >= 0) && (++p < fileName.length()) )
            {
               fileName = fileName.substring(p);
               f = new File(m_jarDirectory, fileName);
               if (! f.exists())
               {
                  System.err.println("ERROR:" + fileName + " not found in " + archivePath);
                  return null;
               }
            }
         }
      }
      BufferedReader br = null;
      StringTemplate template = null;
      InputStream fin = null;
      InputStreamReader isr = null;
      try
      {
         fin = new FileInputStream(f);
         isr = getInputStreamReader(fin);
         br = new BufferedReader(isr);
         template = loadTemplate(templateName, br);         
      }
      catch (IOException ioe)
      {
         ioe.printStackTrace(System.err);
      }
      finally
      {
         if (br != null)
            try { br.close(); } catch (Exception e) {}
         if (isr != null)
            try { isr.close(); } catch (Exception e) {}
         if (fin != null)
            try { fin.close(); } catch (Exception e) {}         
      }
      return template;
   }   
 }
