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
import java.io.InputStream;
import org.antlr.stringtemplate.StringTemplate;

/**
 * An interface that must be implemented by template populating class
 * used by StringTemplateHandler.
 * @see StringTemplateHandler
 * @author Donald Munro
 */
public interface Templatable
//==========================
{
   /**
    * Called by the StringTemplateHandler to populate a file based template.
    * @param template The StringTemplate instance
    * @param request The Request instance.
    * @param mimeType Return the mime type of the generated content in this.
    * parameter. NOTE: Can be null so check to avoid Exceptions
    * @param dir Directory in which to create return file. Can be null
    * @return A temporary file of the contents of the template output or null.
    */
   public File templateFile(StringTemplate template, Request request, 
                            StringBuffer mimeType, File dir);
   
   /**
   * Called by the StringTemplateHandler to populate a string based template.
   * @param template The StringTemplate instance
   * @param request The Request instance.
   * @param mimeType Return the mime type of the generated content in this. 
   * parameter. NOTE: Can be null so check to avoid Exceptions
   * @return A temporary file of the contents of the template output or null.
   */
   public String templateString(StringTemplate template, Request request, 
                                StringBuffer mimeType);
   
   /**
   * Called by the StringTemplateHandler to populate a stream based template.
   * @param template The StringTemplate instance
   * @param request The Request instance.
   * @param mimeType Return the mime type of the generated content in this. 
   * parameter. NOTE: Can be null so check to avoid Exceptions
   * @return A temporary file of the contents of the template output or null.
   */
   public InputStream templateStream(StringTemplate template, Request request, 
                                     StringBuffer mimeType);
}
