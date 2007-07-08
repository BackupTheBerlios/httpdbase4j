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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.sun.net.httpserver.BasicAuthenticator;

/**
* Implements a Basic Authenticator for authentication from a JDBC 
* source.
* @author Donald Munro 
*/
public class JdbcBasicAuthenticator extends BasicAuthenticator
//============================================================
{
   String m_jdbcUrl = null;
   String m_sql = null;
   BasicAuthenticator m_nextAuthenticator = null;
   
   /**
    * Create a JdbcBasicAuthenticator from a JDBC source.
    * @param realm The authorisation realm.
    * @param jdbcUrl The URL to connect to the database (assumes that the
    * JDBC driver has already been loaded.
    * @param sql The SQL statement to validate the user. Parameters should be 
    * placed in the SQL in the form of a ? character ie as for a 
    * PreparedStatement.<br>
    * The SQL should read :<br>
    * <code>
    *  SELECT HASH_FUNCTION(Entered-Password), Hashed-Password <br>
    *  FROM Users-Table <br>
    *  WHERE USER = Entered-User <br>
    * </code>
    *  where HASH_FUNCTION is the databases password hashing function.<br>
    *
    *  For example for MySQL you could have:
    *  SELECT PASSWORD(?), Passwd
    *  FROM Users
    *  WHERE User = ?
    *  The row should have been inserted using the hashing function eg
    *  INSERT INTO Users(User, Passwd) VALUES ('idiot', PASSWORD('moron'))    
    *
    */
   public JdbcBasicAuthenticator(String realm, String jdbcUrl, String sql)
   //---------------------------------------------------------------------
   {
      super(realm);
      m_jdbcUrl = jdbcUrl;
      m_sql = sql;
   }
   
   /**
    * Create a JdbcBasicAuthenticator from a JDBC source.
    * @param realm The authorisation realm.
    * @param jdbcUrl The URL to connect to the database (assumes that the
    * JDBC driver has already been loaded).
    * @param sql The SQL statement to validate the user. Parameters should be 
    * placed in the SQL in the form of a ? character ie as for a 
    * PreparedStatement.<br>
    * The SQL should read :<br>
    * <code>
    *  SELECT HASH_FUNCTION(Entered-Password), Hashed-Password <br>
    *  FROM Users-Table <br>
    *  WHERE USER = Entered-User <br>
    * </code>
    *  where HASH_FUNCTION is the databases password hashing function.<br>
    *
    *  For example for MySQL you could have:
    *  SELECT PASSWORD(?), Passwd
    *  FROM Users
    *  WHERE User = ?
    *  The row should have been inserted using the hashing function eg
    *  INSERT INTO Users(User, Passwd) VALUES ('idiot', PASSWORD('moron'))
    * @param nextAuthenticator If authorization fails (in the sense of not being
    * found or an exception occurring, but not in the case of the user being 
    * found but having entered the incorrect password) then forward the
    * authentication onto this authenticator.
    */
   public JdbcBasicAuthenticator(String realm, String jdbcUrl, String sql,
                                 BasicAuthenticator nextAuthenticator)
   //---------------------------------------------------------------------
   {
      super(realm);
      m_jdbcUrl = jdbcUrl;
      m_sql = sql;
      m_nextAuthenticator = nextAuthenticator;
   }
   
   /**
    * Overiding classes can overide this method if the database does not
    * have a password hashing function or they wish to implement their own
    * hashing.
    * @param password The password entered by the user
    * @return The hashed password. The default implementation simply returns
    * the incoming password parameter
    */
   protected String hashPassword(String password)
   //-------------------------------------------------
   {
      return password;
   }
   
   /**
    * Overiding classes can overide this method to open a JDBC 
    * connection. 
    * @param jdbcUrl The URL to connect to the database
    * @return A java.sql.Connection to the database. The default 
    * implementation returns DriverManager.getConnection(jdbcUrl)
    * @throws java.sql.SQLException 
    */
   protected Connection openConnection(String jdbcUrl) throws SQLException
   //---------------------------------------------------------------------
   {
      return DriverManager.getConnection(jdbcUrl);
   }
   
   /* (non-Javadoc)
    * @see com.sun.net.httpserver.BasicAuthenticator#checkCredentials(
    *                                        java.lang.String, java.lang.String)
    */
   @Override
   public boolean checkCredentials(String userEntered, 
                                   String passwordEntered)
   //-----------------------------------------------------------
   {
      if ( (m_sql == null) || (m_jdbcUrl == null) )
      {
         if (m_nextAuthenticator != null)
               return m_nextAuthenticator.checkCredentials(userEntered, 
                                                           passwordEntered);
      }
 
      Connection connection = null;
      PreparedStatement pst = null;
      ResultSet rs = null;      
      try
      {
         connection = openConnection(m_jdbcUrl);
         if (connection == null)
         {
            if (m_nextAuthenticator != null)
               return m_nextAuthenticator.checkCredentials(userEntered, 
                                                           passwordEntered);
            else
               return false;
         }
         pst = connection.prepareStatement(m_sql);
         pst.setString(1, passwordEntered);
         pst.setString(2, userEntered);
         rs = pst.executeQuery();
         if (rs.next())
         {
            String passwordHash = hashPassword(rs.getString(1).trim());
            String passwdHash = rs.getString(2).trim();
            return (passwdHash.compareTo(passwordHash) == 0);
         }
         else
            if (m_nextAuthenticator != null)
               return m_nextAuthenticator.checkCredentials(userEntered, 
                                                           passwordEntered);
      }
      catch (Exception e)
      {
         System.err.println("Exception chacking password");
         e.printStackTrace(System.err);
         if (m_nextAuthenticator != null)
            return m_nextAuthenticator.checkCredentials(userEntered, 
                                                        passwordEntered);
      }
      finally
      {
         if (rs != null) try { rs.close(); } catch (Exception e) {}
         if (pst != null)
         {
            try { pst.clearParameters(); pst.close();} catch (SQLException e) {}                     
         }
         if (connection != null) 
            try { connection.close(); } catch (Exception e) {}         
      }
      return false;
   }
}
