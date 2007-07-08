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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/*
 * A simple sessions manager. May be used to maintain state for simple
 * web applications.
 * @author Donald Munro
 */ 
public class SimpleSessionManager
//===============================
{

   /*
    * Unique session id used to identify a session
    */
   static private AtomicLong           m_sessionSequence = new AtomicLong(0);
   
   /*
    * Session Map. Maps a session id onto another Map that maps a 
    * variable name (String) onto a value (Object).
    */
   static protected Map<Long, Map<String, Object>> m_sessionMap = null;           
   
   private static final class SingletonHolder
   {
      static final SimpleSessionManager singleton = new SimpleSessionManager();
   }

   private SimpleSessionManager() {}
   
   /**
    * Return the single SimpleSessionManager instance.
    * @return The singleton SimpleSessionManager instance
    */
   static public SimpleSessionManager getSimpleSessionManager() 
   //----------------------------------------------------------
   {
      return SingletonHolder.singleton;
   }
   
   /**
    * 
    * @return The next available session id
    */
   static public long getNextSessionId() 
   //-----------------------------------
   { 
      return m_sessionSequence.getAndIncrement(); 
   }
   
   /**
    * Set a session variable.
    * @param sessionId The session id for the session
    * @param varName The name of the session variable
    * @param value The variable value
    */
   static public void setSessionVariable(long sessionId, String varName, 
                                         Object value)
   //---------------------------------------------------------------------
   {
      if (m_sessionMap == null)
         m_sessionMap = new HashMap<Long, Map<String, Object>>();
      Map<String, Object> m = m_sessionMap.get(sessionId);
      if (m == null)
      {
         m = new HashMap<String, Object>();
         m_sessionMap.put(sessionId, m);
      }
      m.put(varName, value);
   }
   
   /**
    * Get a session variable.
    * @param sessionId The session id for the session
    * @param varName The name of the session variable
    * @return The value of the session variable <i>varName</i> or null
    * if no such session or no such variable
    */
   static public Object getSessionVariable(long sessionId, String varName)
   //---------------------------------------------------------------------
   {
      if (m_sessionMap == null) return null;
      Map<String, Object> m = m_sessionMap.get(sessionId);
      if (m == null) return null;
      return m.get(varName);
   }
   
   /**
    * Delete a session variable.
    * @param sessionId The session id for the session
    * @param varName The name of the session variable
    * @return The value of the session variable <i>varName</i> or null
    * if no such session or no such variable
    */
   static public Object removeSessionVariable(long sessionId, String varName)
   //-----------------------------------------------------------------------
   {
      if (m_sessionMap == null) return null;
      Map<String, Object> m = m_sessionMap.get(sessionId);
      if (m == null) return null;
      return m.remove(varName);
   }
   
   /**
    * Delete a session.
    * @param sessionId The session id for the session
    */
   static public void clearSession(long sessionId)
   //---------------------------------------------
   {
      if (m_sessionMap == null) return;
      Map<String, Object> m = m_sessionMap.remove(sessionId);
      m.clear();
      m = null;
   }
}
