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

import com.sun.net.httpserver.Headers;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper for com.sun.net.httpserver.Headers that implements the Clonable
 * interface and a copy constructor
 * @author Donald Munro
 */
public class CloneableHeaders extends Headers implements Cloneable
//----------------------------------------------------------------
{
   /**
    * Create a new CloneableHeaders instance
    */
   public CloneableHeaders()
   {
      super();
   }

   /**
    * Copy a new CloneableHeaders instance
    * @param h Headers instance to copy
    */
   public CloneableHeaders(Headers h)
   {
      super();
      if (h != null)
      {
         Set<Map.Entry<String, List<String>>> entries = h.entrySet();
         _copyTo(entries, this);
      }
   }

   /**
    *  @inheritDoc
    */
   protected Object clone() throws CloneNotSupportedException
   //--------------------------------------------------------
   {
      Headers klone = (Headers) super.clone();
      Set<Map.Entry<String, List<String>>> entries = entrySet();
      return _copyTo(entries, klone);
   }

   private Headers _copyTo(Set<Map.Entry<String, List<String>>> entries,
            Headers klone)
   //-------------------------------------------------------------------
   {
      klone.clear();
      for (Iterator<Map.Entry<String, List<String>>> i = entries.iterator(); i
               .hasNext();)
      {
         Map.Entry<String, List<String>> e = i.next();
         String k = e.getKey();
         List<String> l = e.getValue();
         for (Iterator<String> j = l.iterator(); j.hasNext();)
            klone.add(k, j.next());
      }
      return klone;
   }

}
