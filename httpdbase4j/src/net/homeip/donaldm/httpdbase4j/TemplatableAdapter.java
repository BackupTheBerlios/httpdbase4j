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
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import org.antlr.stringtemplate.StringTemplate;

/**
 * Convenience class for implementing template processing classes
 * @see Templatable
 * @author Donald Munro
 */
public class TemplatableAdapter implements Templatable
//====================================================
{   
   /**
    * An overidable super class implementation of templateFile. Overiding classes 
    * should populate the template and then return super.templateFile(...).
    * @param template The StringTemplate instance
    * @param request The Request instance
    * @return A temporary file of the contents of the template output or null.
    */
   @Override
   public File templateFile(StringTemplate template, Request request, 
                            StringBuffer mimeType, File dir)
   //---------------------------------------------------------------------
   {
      File tempFile =  null;
      BufferedWriter bw = null;
      try
      {
         if (dir == null)
            tempFile = File.createTempFile("TMP", ".tmp.html");
         else
            tempFile = File.createTempFile("TMP", ".tmp.html", dir);
         tempFile.deleteOnExit();
         bw = new BufferedWriter(new FileWriter(tempFile));
         bw.write(template.toString());
         if (mimeType != null) 
         {
            mimeType.setLength(0);
            mimeType.append(Http.MIME_HTML);
         }
      }
      catch (Exception e)
      {
         Httpd.Log(Httpd.LogLevel.ERROR, "Error creating temporary output file "
                   + tempFile.getAbsolutePath(), e);
         return null;
      }
      finally
      {
         if (bw != null) try { bw.close(); } catch (Exception e) {}
      }
      return tempFile;
   }

   /**
    * An overidable super class implementation of templateString. Overiding 
    * classes  should populate the template and then return 
    * super.templateString(...).
    * @param template The StringTemplate instance
    * @param request The Request instance
    * @return A string representing the contents of the template output or null.
    */
   @Override
   public String templateString(StringTemplate template, Request request, 
                                StringBuffer mimeType)
   //-----------------------------------------------------------------------
   {
      String s = null;
      try 
      { 
         s = template.toString(); 
      }
      catch (Throwable t)
      {
         Httpd.Log(Httpd.LogLevel.ERROR, "Error generating template string", t);
         return null;
      }
      return s;
   }

   /**
    * An overidable super class implementation of templateStream. Overiding 
    * classes  should populate the template and then return 
    * super.templateStream(...).
    * @param template The StringTemplate instance
    * @param request The Request instance
    * @return A stream representing the contents of the template output or null.
    */
   @Override
   public InputStream templateStream(StringTemplate template, Request request, 
                                     StringBuffer mimeType)
   //--------------------------------------------------------------------------
   {
      String s = null;
      try 
      { 
         s = template.toString(); 
      }
      catch (Throwable t)
      {
         Httpd.Log(Httpd.LogLevel.ERROR, "Error generating template string", t);
         return null;
      }
      if (s == null) return null;
      
      return new BufferedInputStream(new ByteArrayInputStream(
               s.getBytes()));
   }
   
}
