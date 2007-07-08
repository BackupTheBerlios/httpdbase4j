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
import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class providing various useful static methods 
 * @author Donald Munro
 */
public class Http
//===============
{
   static private HashMap<Integer, String> m_httpErrors = 
           new HashMap<Integer, String>();
   
   static public int HTTP_CONTINUE = 100, HTTP_OK = 200, HTTP_REDIRECT = 301, 
                     HTTP_UNAUTHORISED = 401,  HTTP_FORBIDDEN = 403,
                     HTTP_NOTFOUND = 404, HTTP_BADREQUEST = 400,
                     HTTP_METHOD = 405, HTTP_LENGTH = 411,
                     HTTP_INTERNALERROR = 500, HTTP_NOTIMPLEMENTED = 501;
   
   public static final String MIME_PLAINTEXT = "text/plain",
                              MIME_HTML = "text/html", MIME_XML = "text/xml", 
                              MIME_BINARY = "application/octet-stream",
                              MIME_ICON = "image/x-icon";
   
   static private HashMap<String, String> m_mimeExtensionMap = null;
      
   public static final DateFormat m_dateFormats[] = 
   {
      //new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",  Locale.US);
      new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
      new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
      new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US) 
   };

   static
   {
      m_httpErrors.put( new Integer(100), "Continue" );
      m_httpErrors.put( new Integer(101), "Switching Protocols" );
      m_httpErrors.put( new Integer(200), "OK" );
      m_httpErrors.put( new Integer(201), "Created" );
      m_httpErrors.put( new Integer(202), "Accepted" );
      m_httpErrors.put( new Integer(203), "Non-Authoritative Information" );
      m_httpErrors.put( new Integer(204), "No Content" );
      m_httpErrors.put( new Integer(205), "Reset Content" );
      m_httpErrors.put( new Integer(206), "Partial Content" );
      m_httpErrors.put( new Integer(300), "Multiple Choices" );
      m_httpErrors.put( new Integer(301), "Moved Permanently" );
      m_httpErrors.put( new Integer(302), "Moved Temporarily" );
      m_httpErrors.put( new Integer(303), "See Other" );
      m_httpErrors.put( new Integer(304), "Not Modified" );
      m_httpErrors.put( new Integer(305), "Use Proxy" );
      m_httpErrors.put( new Integer(400), "Bad Request" );
      m_httpErrors.put( new Integer(401), "Unauthorized" );
      m_httpErrors.put( new Integer(402), "Payment Required" );
      m_httpErrors.put( new Integer(403), "Forbidden" );
      m_httpErrors.put( new Integer(404), "Not Found" );
      m_httpErrors.put( new Integer(405), "Method Not Allowed" );
      m_httpErrors.put( new Integer(406), "Not Acceptable" );
      m_httpErrors.put( new Integer(407), "Proxy Authentication Required" );
      m_httpErrors.put( new Integer(408), "Request Time-out" );
      m_httpErrors.put( new Integer(409), "Conflict" );
      m_httpErrors.put( new Integer(410), "Gone" );
      m_httpErrors.put( new Integer(411), "Length Required" );
      m_httpErrors.put( new Integer(412), "Precondition Failed" );
      m_httpErrors.put( new Integer(413), "Request Entity Too Large" );
      m_httpErrors.put( new Integer(414), "Request-URI Too Large" );
      m_httpErrors.put( new Integer(415), "Unsupported Media Type" );
      m_httpErrors.put( new Integer(500), "Server Error" );
      m_httpErrors.put( new Integer(501), "Not Implemented" );
      m_httpErrors.put( new Integer(502), "Bad Gateway" );
      m_httpErrors.put( new Integer(503), "Service Unavailable" );
      m_httpErrors.put( new Integer(504), "Gateway Time-out" );
      m_httpErrors.put( new Integer(505), "HTTP Version not supported");
      
      for (int i=0; i<m_dateFormats.length; i++)
      {
         m_dateFormats[i].setTimeZone(new SimpleTimeZone(0, "GMT"));
         m_dateFormats[i].setLenient(true);
      }
   }
   
   /**
    * Maps a HTTP error code onto an error description.
    * @param code The HTTP error code
    * @return  An error description.
    */
   static public String getErrorMessage(int code)
   //--------------------------------------------
   {
      String m = m_httpErrors.get(code);
      if (m == null) return "";
      return m;
   }
   
   static private Pattern m_extPattern = Pattern.compile(".+\\.(.+)$");
   /**
    * @param file File to get the file extension for.
    * @return The file extention 
    */
   public static String getExtension(File file)
   //------------------------------------------
   {  
      String path = file.getAbsolutePath();
      int p = path.lastIndexOf(File.separatorChar);
      if (p < 0)
         p = path.lastIndexOf('/');
      if ( (p++ >= 0) && (p < path.length()) )
         path = path.substring(p);
      p = path.indexOf("?");  
      if (p < 0)
         p = path.indexOf("&");  
      if (p > 0)
         path = path.substring(0, p);
      Matcher matcher = m_extPattern.matcher(path);
      String ext = "";
      if (matcher.matches())
         ext = matcher.group(1);
      if (! ext.startsWith("."))
         ext = "." + ext;
      return ext;
   }
   
   static private void loadMimeMap()
   //-------------------------------
   {
      m_mimeExtensionMap = new HashMap<String, String>();
      m_mimeExtensionMap.put("html", "text/html" );
      m_mimeExtensionMap.put("st", "text/html" );
      m_mimeExtensionMap.put("zip", "application/x-zip-compressed");
      m_mimeExtensionMap.put("gif", "image/gif" );
      m_mimeExtensionMap.put("jpeg", "image/jpeg" );
      m_mimeExtensionMap.put("jpg", "image/jpeg" );
      m_mimeExtensionMap.put("png", "image/png" );
      m_mimeExtensionMap.put("css", "text/css" );
      m_mimeExtensionMap.put("pdf", "application/pdf" );
      m_mimeExtensionMap.put("doc", "application/msword");
      m_mimeExtensionMap.put("gz", "application/x-gzip");
      m_mimeExtensionMap.put("zip", "application/zip");
      m_mimeExtensionMap.put("js", "application/x-javascript");
      m_mimeExtensionMap.put("xml", "application/xml");
      m_mimeExtensionMap.put("dtd", "application/xml-dtd");
      m_mimeExtensionMap.put("txt", "text/plain");
      
      BufferedReader br = null;
      File mimeTypes = null;
      try
      {  // TODO:HttpdBase4J-00002 Presumably Windoze must have a mime.types 
         // equivalent in the registry ??
         if (! System.getProperty("os.name").toLowerCase().contains("windows"))
         {              
            mimeTypes = new File("/etc/mime.types");
            if (mimeTypes.exists())
            {
               br = new BufferedReader(new FileReader(mimeTypes));
               String s = br.readLine();
               while (s != null)
               {
                  s = s.trim();
                  if ( (s.length() > 0) && (! s.startsWith("#")) )
                  {
                     String[] as = s.split("\\s+");
                     if (as.length >= 2)
                     {
                        String v = as[0].trim();
                        for (int i=1; i<as.length; i++)
                        {
                           String k = as[i].trim();
                           if (k.length() > 0)
                              m_mimeExtensionMap.put(k, v);
                        }
                     }                     
                  }
                     
                  s = br.readLine();
               }
               
            }
         }
      }
      catch (Exception e)
      {
         
      }
      finally
      {
         if (br != null)
            try { br.close(); }  catch (Exception e) {}
      }
      /*
      Set<Entry<String, String>> set = m_mimeExtensionMap.entrySet();
      for (Iterator<Entry<String, String>> it = set.iterator(); it.hasNext();)
      {
         Entry<String, String> entry = it.next();
         System.out.println(entry.getKey() + " = " + entry.getValue());
      }
      */
   }
   
   /**
    * @param r Request instance for which to look up the MIME type
    * @return The mime type 
    */
   static public String getMimeType(Request r)
   //-----------------------------------------
   {
      String ext = r.getExtension();
      if (m_mimeExtensionMap == null)
         loadMimeMap();
      if (m_mimeExtensionMap != null)
      {
         if (ext.trim().startsWith("."))
            ext = ext.substring(1);
         return m_mimeExtensionMap.get(ext);
      }
      return null;
   }
   
   static public String strDate(Date dte)
   //----------------------------
   {
      if (dte == null)
         return m_dateFormats[0].format(new Date());
      return m_dateFormats[0].format(dte);
   }
   
   public static final Date getDate(String date)
   //-------------------------------------------
   {      
      Date dte = null;
      for (int i = 0; i < m_dateFormats.length; i++)
      {
         try
         {
            dte = m_dateFormats[i].parse(date);            
            break;
            
         }
         catch (ParseException e)
         {
            dte = null;
            continue;
         }
      }
      return dte;      
   }
   
   /**
    * Read a stream in a byte array.
    * @param is - The stream to read
    * @param data - The byte array into which to read the data.
    * If null the array will be allocated
    * @param len - The number of bytes to read
    * @return A count of the bytes read.
    * @throws IOException
    */
   static public int readStream(InputStream is, byte[] data, int len) 
                        throws IOException
   //-------------------------------------------------------------------------
   {
      if (len < 0) return 0;
      if (data == null)
         data = new byte[len];
      int p = is.read(data);      
      int cb = p;
      while ( (cb >= 0) && (p < len) )
      {
         cb = is.read(data, p, len - p);
         p += cb;
      }
      return p;
   }
   
   /**
    * Copy an input stream to an output stream
    * @param is - The input stream
    * @param os - The output stream 
    * @return The number of bytes copied
    * @throws IOException
    */
   static public long readWriteStream(InputStream is, OutputStream os) 
                     throws IOException
   //----------------------------------------------------------------
   {
      byte[] data = new byte[4096];
      long total = 0;
      int cb = is.read(data, 0, 4096);
      while (cb >= 0)
      {
         total += cb;
         os.write(data, 0, cb);
         cb = is.read(data, 0, 4096);         
      }
      return total;
   }
   
   /**
    * Create a String representation of an HttpExchange object.
    * @param ex The HttpExchange object.
    * @return A String representation of ex.
    */
   static protected String strExchange(HttpExchange ex)
   //------------------------------------------------
   {
      StringBuffer sb = new StringBuffer();
      if (ex != null)
      {
         sb.append("Method: " + ex.getRequestMethod());
         sb.append(Httpd.EOL);
         Headers headerMap = ex.getRequestHeaders();
         Set<Map.Entry<String,List<String>>> headers = headerMap.entrySet();
         for (Iterator<Map.Entry<String,List<String>>> i=headers.iterator();
         i.hasNext();)
         {
            Map.Entry<String,List<String>> e = i.next();
            sb.append("Header: " + e.getKey() + ": ");
            List<String> hv = e.getValue();
            for (Iterator<String> j=hv.iterator(); j.hasNext();)
               sb.append(j.next() + " ");
            sb.append(Httpd.EOL);
         }
         URI uri = ex.getRequestURI().normalize();
         if (uri != null)
         {
            sb.append(uri.toASCIIString());
            sb.append("Path " + uri.getPath()); sb.append(Httpd.EOL);
            sb.append("Host " + uri.getHost()); sb.append(Httpd.EOL);
            sb.append("Port " + uri.getPort()); sb.append(Httpd.EOL);
            sb.append("Fragment " + uri.getFragment()); sb.append(Httpd.EOL);
            sb.append("Query " + uri.getQuery()); sb.append(Httpd.EOL);
            sb.append("Scheme " + uri.getScheme()); sb.append(Httpd.EOL);
         }
//         InputStream is = null;
//         sb.append("Content:"); sb.append(nl);
//         try
//         {
//            is = ex.getRequestBody();
//            int ch;
//            while ( (ch = is.read()) != -1)
//               sb.append(ch);
//         }
//         catch (Exception e)
//         {
//         }
//         finally
//         {
//            if (is != null) try {is.close(); } catch (Exception e) {}
//         }
//         sb.append(nl);
      }
      return sb.toString();
   }
   
   /**
    * Calculate the e-tag for a list of files
    * @param files List of files to calculate e-tag for
    * @return A String containing the e-tag.
    */
   public static String eTag(DirItemInterface... files)
   //-------------------------------------------
   {
      MessageDigest messageDigest = null;
      try 
      {
         messageDigest = MessageDigest.getInstance("SHA-1");
      }
      catch (NoSuchAlgorithmException e)               
      {
         Httpd.Log(Httpd.LogLevel.ERROR, "Error computing ETAG hash", e);
         return null;
      }
      
      BufferedInputStream bis = null;
      byte[] buffer = new byte[4096];
      DirItemInterface currentFile = null;
      try
      {
         for (DirItemInterface file : files)
         {
            currentFile = file;
            InputStream is = file.getStream();
            if (is == null) continue;
            bis = new BufferedInputStream(is);
            while (true)
            {
               int cb = bis.read(buffer); 
               if (cb < 0) break;
               messageDigest.update(buffer, 0, cb);
            }            
         }
         byte digest[] = messageDigest.digest();
         StringBuffer sb = new StringBuffer(digest.length * 2 + 16);
         for (int i = 0; i < digest.length; i++) 
         {
            int v = digest[i] & 0xff;
            if (v < 16) sb.append('0');
            sb.append(Integer.toHexString(v));
         }
         return sb.toString();
      }
      catch (IOException e)
      {
         Httpd.Log(Httpd.LogLevel.ERROR, "Error computing ETAG hash " +
                   ((currentFile == null) ? "" : currentFile.getName()), e);
         messageDigest.digest();
         return null;
      }
      finally
      {
         if (bis != null)
            try { bis.close(); } catch (Exception e) {}
      }
   }
}
