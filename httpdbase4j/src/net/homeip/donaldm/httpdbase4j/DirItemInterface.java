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

import java.io.InputStream;
import java.util.Date;

/**
 * The Request classes returns sortable lists of classes implementing the 
 * DirItemInterface to implement directory listings.
 * @author Donald Munro
 */
public interface DirItemInterface
//===============================
{  
   /**
    * Used as a parameter to specify how the directory contents should be sorted
    */
   static public enum SORTBY { NAME, SIZE, DATE }
   
   /**
    * @return The directory item name.
    */ 
   public String getName();
   
   /**
    * @return The directory item size.
    */ 
   public long getSize();
   
   /**
    * @return The directory item date.
    */ 
   public Date getDate();
   
   /**
    * @return true if the directory item is a directory.
    */ 
   public boolean isDirectory();
   
   /**
    * 
    * @return return an InputStream for the item, or return null 
    */
   public InputStream getStream();
}
