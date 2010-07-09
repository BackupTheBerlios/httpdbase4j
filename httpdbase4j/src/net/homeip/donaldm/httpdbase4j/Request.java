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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import net.homeip.donaldm.httpdbase4j.Httpd.LogLevel;
import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsICharsetDetectionObserver;

/**
 * Abstraction of an HTTP request
 * @see ArchiveRequest
 * @see FileRequest
 * @see CombinedRequest
 * @author Donald Munro
 */
abstract public class Request implements DirItemInterface
//=======================================================
{
   /**
    * Enumeration of HTTP methods
    */
   static public enum HTTP_METHOD {
      GET, HEAD, POST, PUT, DELETE, UNKNOWN
   }

   /**
    The HttpExchange instance for this request.
    * @see com.sun.net.httpserver.HttpExchange
    */
   protected HttpExchange     m_ex             = null;

   /**
    The Httpd instance within which the request occurred.
    @see net.homeip.donaldm.httpdbase4j.Httpd
    */
   protected Httpd            m_httpd          = null;

   /**
    The request URI
    @see net.homeip.donaldm.httpdbase4j.Httpd
    */
   protected URI              m_uri            = null;

   /**
    The request headers
    */
   protected CloneableHeaders m_requestHeaders = null;

   /**
    The HTTP method
    */
   protected HTTP_METHOD      m_method         = null;

   /**
    The HTTP method as a String
    */
   protected String           m_methodString   = null;

   /**
    * Request URI path
    */
   protected String           m_path           = null;

   /**
    * Request GET parameters 
    */
   protected CloneableHeaders m_getParameters  = null;

   /**
    * Request POST parameters 
    */
   protected CloneableHeaders m_postParameters = null;

   protected String           m_encoding = null;
   
   protected long             m_contentLength = Long.MIN_VALUE;
   
   /**
    *  If true then the output will be compressed with gzip/deflate if the 
    *  client indicates that it supports compression.
    */
   protected boolean          m_compress = true;
   
   protected File             m_compressedFile = null;
   
   protected String           m_eTag = null;

   protected File             m_cacheFile = null;
   
   static protected File      m_cacheDir;
   
   static
   {
      File tmpDir = new File(System.getProperty("java.io.tmpdir"));
      if ( (tmpDir.exists()) && (tmpDir.canWrite()) )
      {    
         tmpDir = new File(tmpDir, "HttpdBase4J");
         tmpDir.mkdirs();
         if ( (! tmpDir.exists()) || (! tmpDir.canWrite()) )
         {
            tmpDir = new File(System.getProperty("java.io.tmpdir"));
            m_cacheDir = new File(tmpDir, "HttpdBase4J-Cache");
         }
         else
            m_cacheDir = new File(tmpDir, "Cache");
         m_cacheDir.mkdirs();
      }
      if ( (! m_cacheDir.exists()) || (! m_cacheDir.canWrite()) ) 
         m_cacheDir = null;
   }
   
   private boolean            m_isGet          = false;

   /**
    * Create a Request instance.
    * @param httpd The Httpd instance within which the request occurred.
    * @see net.homeip.donaldm.httpdbase4j.Httpd
    * @param ex The HttpExchange instance for this request.
    * @see com.sun.net.httpserver.HttpExchange
    * @throws UnsupportedEncodingException
    * @throws IOException
    */
   public Request(Httpd httpd, HttpExchange ex)
            throws UnsupportedEncodingException, IOException
   //-----------------------------------------------------------------
   {
      m_httpd = httpd;
      m_ex = ex;
      m_methodString = ex.getRequestMethod().trim().toUpperCase();
      if (m_methodString.compareTo("GET") == 0)
         m_method = HTTP_METHOD.GET;
      else
         if (m_methodString.compareTo("HEAD") == 0)
            m_method = HTTP_METHOD.HEAD;
         else
            if (m_methodString.compareTo("POST") == 0)
               m_method = HTTP_METHOD.POST;
            else
               if (m_methodString.compareTo("PUT") == 0)
                  m_method = HTTP_METHOD.PUT;
               else
                  if (m_methodString.compareTo("DELETE") == 0)
                     m_method = HTTP_METHOD.DELETE;
                  else
                     m_method = HTTP_METHOD.UNKNOWN;
      m_uri = ex.getRequestURI().normalize();
      m_path = m_uri.getPath();
      if (m_path.startsWith("/")) m_path = m_path.substring(1);
      m_requestHeaders = new CloneableHeaders(ex.getRequestHeaders());
      m_getParameters = processParameters(m_uri.getQuery());
      String requestBody = getRequestString(ex.getRequestBody(),
               m_requestHeaders);
      switch (m_method)
      {
         case GET:
         case HEAD:
            m_isGet = true;
            break;

         default:
            String contentType = getContentType();
            if ((contentType != null)
                     && (contentType.trim().toLowerCase()
                              .startsWith("application/x-www-form-urlencoded")))
               ;
            m_postParameters = processParameters(requestBody);
      }

   }

   public Request(Request request)
   //-----------------------------
   {
      m_httpd = request.m_httpd;
      m_ex = request.m_ex;
      m_method = request.m_method;
      m_methodString = request.m_methodString;
      m_uri = request.m_uri;
      m_path = request.m_path;
      
      try
      {
         m_requestHeaders = (CloneableHeaders) request.m_requestHeaders.clone();
         m_getParameters = (CloneableHeaders) request.m_getParameters.clone();
         switch (m_method)
         {
            case GET:
            case HEAD:
               m_isGet = true;
               break;

            default:
               m_postParameters = (CloneableHeaders) request.m_postParameters.clone();
         }
      }
      catch (CloneNotSupportedException e)
      {
         e.printStackTrace(System.err);
      }      
   }
   
   /**
    * Check whether the resource exists.
    * @return <i>true</i> if the resource exists, <i>false</i> if it does not
    */
   abstract public boolean exists();

   /**
    * Check whether the resource is readable
    * @return <i>true</i> if the resource is readable, 
    * <i>false</i> if it is not.
    */
   abstract public boolean isReadable();

   /**
    * Check whether the resource is a directory
    * @return <i>true</i> if the resource is a directory, 
    * <i>false</i> if it is not.
    */
   abstract public boolean isDirectory();
   
   /**
    * Return the length of the resource.
    * @return the resource length or -1 if the length could not
    * be determined.
    */
   abstract public long getContentLength();

   /**
    * Return the full path of the resource
    * @return the full path of the resource
    */
   abstract public String getAbsolutePath();

   /**
    * Return the name of the resource (ie the final component in the path after 
    * the final /)
    * @return the name of the resource
    */
   abstract public String getName();
   
   /**
    * Return the file extension of the resource (ie the text after the final . 
    * in the path)
    * @return the extension of the resource
    */
   abstract public String getExtension();
   
   /**
    * Return the date of the resource
    * @return the name of the resource
    */
   abstract public Date getDate();

   /**
    * Return the directory of the resource (ie all components of the path before 
    * the final /)
    * @return the path of the resource
    */
   abstract public String getDir();

   /**
    * Return the directory of the resource (ie all components of the path before 
    * the final / as a Request contructed from this Request)
    * @return A Request for the directory containing this request
    */
   abstract public Request getDirRequest();
   
   /**
    * Create a request from the current directory request combined with a 
    * file or directory in the current directory
    * 
    * @param name The name of a file or directory in the current request if the
    * current request is a directory
    * @return A new Request or null if request is not a directory
    */
   abstract public Request getChildRequest(String name);
   
   /**
    * Return the handler for this request (a class implementing the 
    * HttpHandleable interface).
    * @return The request handler
    * @see net.homeip.donaldm.httpdbase4j.HttpHandleable
    */
   abstract protected HttpHandleable getHandler();

   /**
    * Return the POST handler for this request (a class implementing the 
    * Postable interface).
    * @return The request POST handler or null if no POST handler is defined
    * @see Postable
    */
   abstract protected Postable getPostHandler();   
   
   /**
    * Return a list of files in a resource directory.
    * @param sortBy Specify how the files should be sorted. This is an instance
    * of DirItemInterface.SORTBY ie NAME, DATE or SIZE).
    * @return A TreeSet (sortable set) of DirItemInterface implementing
    * instances. Returns null if the resource is not a directory.
    * @see net.homeip.donaldm.httpdbase4j.DirItemInterface
    */
   abstract public TreeSet<DirItemInterface> getDirListFiles(
            DirItemInterface.SORTBY sortBy);

   /**
    * Return a list of directories in a resource directory.
    * @param sortBy Specify how the files should be sorted. This is an instance
    * of DirItemInterface.SORTBY ie NAME, DATE or SIZE).
    * @return A TreeSet (sortable set) of DirItemInterface implementing
    * instances. Returns null if the resource is not a directory.
    * @see net.homeip.donaldm.httpdbase4j.DirItemInterface
    */
   abstract public TreeSet<DirItemInterface> getDirListDirectories(
            DirItemInterface.SORTBY sortBy);

   /**
    * Return a stream of the resource contents. 
    * @param isEncoded If true will return a stream for the encoded (eg gzip or
    * deflate) resource or if cacheing is allowed for this request and the 
    * encoded resource is in the cache then a stream for the cached version. 
    * If false returns a stream for the unencoded resource. If there is no
    * encoding ie the client does not support encoding then both true and
    * false return a stream for the unencoded resource.
    * @return A stream of the resource contents. 
    */
   abstract public InputStream getStream(boolean isEncoded);

   /**
    * @return A stream of the resource contents (encoded content is returned if
    * available). 
    */
   
   /**
    * @return A stream of the resource contents (encoded content is returned if
    * available). 
    */
   public InputStream getStream()
   //----------------------------
   {      
      return getStream(true);
   }
   
   /**
    * @param refresh If true recalculate the tag hash even if it has already
    * been calculated, if false reuse the cached value
    * @return The ETag cacheing hash for this request
    */
   abstract public String getETag(boolean refresh);
   
   public boolean checkClientCache()
   //-------------------------
   {
      if (! m_httpd.getCaching()) return false;
      String modDateStr = m_requestHeaders.getFirst("If-Modified-Since");
      if (modDateStr != null)
      {
         Date modDate = Http.getDate(modDateStr);
         if (modDate != null)
         {
            Date reqDate = getDate();
            System.out.println(modDate.getTime() + " " + reqDate.getTime());
            if (reqDate != null)
               if (modDate.after(reqDate))
                  return true;
               else
                  return false;
         }
      }      
      
      String clientEtag = m_requestHeaders.getFirst("If-None-Match");      
      if (clientEtag != null)
      {
         clientEtag = clientEtag.replaceAll("\"", "");
         getETag(false);
         if (m_eTag.trim().compareTo(clientEtag.trim()) == 0)
            return true;
      }
            
      return false;         
   }
   
   /**
    * @return The request URI
    */
   public URI getURI()
   {
      return m_uri;
   }   
   
   /**
    * @return true if the Request result should be cached (checks for
    * Cache-Control and Pragma:NoCache headers)
    */
   public boolean isCacheable()
   //--------------------------
   {
      List<String> pragmas = m_requestHeaders.get("Pragma");
      if (pragmas != null)
      {
         for (Iterator<String>it=pragmas.iterator(); it.hasNext();)
         {
            String s = it.next().trim();
            if (s.compareToIgnoreCase("no-cache") == 0)
               return false;
         }
      }
      List<String> controls = m_requestHeaders.get("Cache-Control");
      if (controls != null)
      {
         for (Iterator<String>it=controls.iterator(); it.hasNext();)
         {
            String control = it.next().trim();
            if ( (control != null) && 
                 (control.compareToIgnoreCase("no-cache") == 0) )
               return false;
            if ( (control != null) && 
                 (control.compareToIgnoreCase("private") == 0) )
               return false;
         }
      }      
      /*
      if ( (m_requestHeaders.containsKey("If-None-Match")) ||
           (m_requestHeaders.containsKey("If-Modified-Since")) )
         return true;      */
      return true;
   }
   
   static private Pattern IE_PATTERN = 
                           Pattern.compile("Mozilla/.*MSIE ([0-9]\\.[0-9]).*");
   
   /**
    * Determine the compression encodings supported by the client.
    * @return A String array with the encodings accepted by the client, 
    * ordered by most desired encoding:
    * gzip = GZip encoded
    * deflate = Deflate encoded
    * txt = No compression
    */
   public String[] compressEncoding()
   //--------------------------------
   {
      String encoding = m_requestHeaders.getFirst("Accept-Encoding");
      if (encoding != null)
         encoding = encoding.toLowerCase();
      String[] encodings = null;
      String agent = m_requestHeaders.getFirst("User-Agent");          
      if ( (agent != null) && (agent.toLowerCase().indexOf("opera") < 0) )
      {
         Matcher matcher = IE_PATTERN.matcher(agent);
         String ver = null;
         if (matcher.matches())
            ver = matcher.group(1);
         double version = 0;
         if (ver != null)
         {
            try { version = Double.parseDouble(ver); } catch (Exception e){}
         }
         if ( (version < 6) ||
              ( (version == 6) && (agent.toUpperCase().indexOf("EV1") < 0) )
            )
            encoding = null;
      }
      if (encoding == null) 
      {
         encodings = new String[1];
         encodings[0] = "txt";
      }
      else
      { 
         String[] encs = encoding.split(",");
         encodings = new String[encs.length + 1];
         System.arraycopy(encs, 0, encodings, 0, encs.length);
         encodings[encs.length] = "txt";
      }
      return encodings;
   }
      
   public boolean getContent(long id, HttpHandleable handler)
   //--------------------------------------------------------
   {  
      m_compressedFile = m_cacheFile = null;            
      m_encoding = null;
      String[] encodings = compressEncoding();      
      if ( (encodings.length == 1) && (encodings[0].compareTo("txt") == 0) )
         return true;
               
      try
      {
         if (m_cacheDir != null)
            m_compressedFile = File.createTempFile("content", ".tmp", m_cacheDir);
      }
      catch (Exception e)
      {
         m_compressedFile = null;
      }
      if (m_compressedFile == null)
         return true;;
      BufferedInputStream bis = null;
      BufferedOutputStream bos = null;
      byte[] buffer = new byte[4096];      
      try
      {
         for (int i=0; i<encodings.length; i++)
         {
            m_encoding = encodings[i];
            m_cacheFile = new File(m_cacheDir,
                                 ((m_eTag == null) ? Long.toString(id) : m_eTag)
                                 + "." + m_encoding);
            if (handler.onIsCacheable(-1, m_ex, this))
            {
               File f = handler.onGetCachedFile(id, m_ex, this);
               if ( (f == null) && (m_cacheFile.exists()) )
               {
                  m_compressedFile = m_cacheFile;
                  break;
               }
               if (f != null) 
               {
                  m_compressedFile = f;
                  break;
               }
            }
            if (bis == null)
               bis = new BufferedInputStream(getStream(false));
            try
            {
               if ( (m_encoding.compareTo("gzip") == 0) && 
                    (m_compressedFile != null) )
               {
                  bos = new BufferedOutputStream(
                            new GZIPOutputStream(
                                new FileOutputStream(m_compressedFile)));
                  while (true)
                  {
                     int cb = bis.read(buffer);
                     if (cb == -1) break;
                     bos.write(buffer, 0, cb);                     
                  }                           
                  bos.close();
                  bos = null;
                  break;
               }

               if ( (m_encoding.compareTo("deflate") == 0) && 
                    (m_compressedFile != null) )
               {
                  bos = new BufferedOutputStream(
                            new DeflaterOutputStream(
                                new FileOutputStream(m_compressedFile)));
                  while (true)
                  {
                     int cb = bis.read(buffer);
                     if (cb == -1) break;
                     bos.write(buffer, 0, cb);                     
                  }               
                  bos.close();
                  bos = null;
                  break;
               }
               if ( (m_encoding.compareTo("txt") == 0) || 
                    (m_compressedFile == null) )
               {               
                  m_compressedFile = m_cacheFile = null;
                  return true;
               }
            }
            catch (IOException e)
            {
               m_compressedFile = null;
            }
            m_cacheFile = null;
         }
      }
      catch (Exception e)
      {
         Httpd.Log(Httpd.LogLevel.ERROR, "Request content encoding", e);
      }

      finally
      {
         if (bis != null)
            try { bis.close(); } catch (Exception e) {}
         if (bos != null) 
            try { bos.close(); } catch (Exception e) {}
      }
      if (handler.onIsCacheable(-1, m_ex, this))
      {
         if (m_cacheFile == null) return false;
         m_cacheFile.delete();
         m_compressedFile.renameTo(m_cacheFile);
         m_compressedFile = null;
      }
      else
      {
         m_cacheFile = m_compressedFile;
         //m_cacheFile.deleteOnExit();
      }
      return true;
   }
   
   /**
    * Return whether this request is an HTTP GET or HEAD
    * @return true if this request is an HTTP GET or HEAD
    */
   public boolean isGETorHEAD()
   {
      return m_isGet;
   }

   /**
    * @return The request method as an HTTP_METHOD enumeration
    */
   public HTTP_METHOD getMethod()
   {
      return m_method;
   }

   /**
    * @return The request method as a String
    */
   public String getMethodString()
   {
      return m_methodString;
   }

   /**
    * @return The request URI path 
    */
   public String getPath()
   {
      return m_path;
   }

   /**
    * @return The request GET parameters
    */
   public CloneableHeaders getGETParameters()
   {
      return m_getParameters;
   }

   /**
    * @return The request POST parameters
    */
   public CloneableHeaders getPOSTParameters()
   {
      return m_postParameters;
   }

   /**
    * @return The content type of the request 
    */
   public String getContentType()
   //----------------------------
   {
      return m_requestHeaders.getFirst("Content-Type");
   }

   protected byte[]  m_postData = null;
   
   /**
    * Get request contents (eg a POST request contents) as a String
    * @param is InputStream of request contents
    * @param headers Request headers
    * @return A String representation of the request contents or an empty string
    * @throws IOException
    * <b>Note:</b>Uses jchardet for charset detection (http://jchardet.sourceforge.net/)
    */
   protected String getRequestString(InputStream is, Headers headers)
            throws IOException
   //-----------------------------------------------------------
   {
      if (m_postData == null) 
         m_postData= getRequestBytes(is, headers);
      if (m_postData == null) return "";
      int len = m_postData.length;
      nsDetector charSetDetector = new nsDetector();
      final ArrayList<String> charsets = new ArrayList<String>();
      charSetDetector.Init(new nsICharsetDetectionObserver()
      {
         @Override
         public void Notify(String charset)
         {
            charsets.add(charset);
         }
      });
      charSetDetector.DoIt(m_postData, len, false);
      boolean isAscii = charSetDetector.isAscii(m_postData, len);
      charSetDetector.DataEnd();

      if (isAscii)
         return new String(m_postData);
      else
         if (charsets.size() <= 0)
            return new String(m_postData);
         else
         {
            for (int i = 0; i < charsets.size(); i++)
            {
               try
               {
                  return new String(m_postData, charsets.get(i));
               }
               catch (UnsupportedEncodingException e)
               {
                  Httpd.Log(LogLevel.INFO, "Could not create a String with " +
                            " charset " + charsets.get(i), e);
                  continue;
               }
               catch (Exception e)
               {
                  Httpd.Log(LogLevel.ERROR, "Error decoding request body. Could" +
                            "not create a String with charset " 
                            + charsets.get(i), e);
               }
            }
         }
      return new String(m_postData);
   }

   /**
    * Get request contents (eg a POST request contents) into a byte array.
    * @param is InputStream of request contents
    * @param headers Request headers
    * @return An array of bytes of the request contents or null 
    * @throws IOException
    * <b>Note:</b>Uses jchardet for charset detection (http://jchardet.sourceforge.net/)
    */
   protected byte[] getRequestBytes(InputStream is, Headers headers)
            throws IOException
   //-----------------------------------------------------------
   {
      String contentLen = headers.getFirst("Content-Length");
      if (contentLen == null) return null;
      int len = -1;
      try
      {
         len = Integer.parseInt(contentLen);
      }
      catch (Exception e)
      {
         len = -1;
      }
      if (len <= 0) return null;

      byte data[] = null;
      byte[] ret = data = new byte[len];
      int l = Http.readStream(is, data, len);
      if ((l >= 0) && (l != len)) ret = Arrays.copyOf(data, l);
      return ret;
   }

   /**
    * Parses parameters in the form key=value&key2=value2)
    * @param queryString
    * @return A key-value Map of the parameters 
    * @throws UnsupportedEncodingException
    */
   protected CloneableHeaders processParameters(String queryString)
            throws UnsupportedEncodingException
   //--------------------------------------------------------------------
   {
      CloneableHeaders parameters = new CloneableHeaders();
      if (queryString == null) return parameters;
      queryString = queryString.replace('+', ' ');
      StringTokenizer st = new StringTokenizer(queryString, "&");
      while (st.hasMoreTokens())
      {
         String field = st.nextToken();
         String k = null, v = "";
         int index = field.indexOf('=');
         if (index > 0)
         {
            k = URLDecoder.decode(field.substring(0, index), "UTF-8");
            v = URLDecoder.decode(field.substring(index + 1), "UTF-8").trim();
            if (v.startsWith("\"")) v = v.substring(1);
            if (v.endsWith("\"")) v = v.substring(0, v.length() - 1);
         }
         else
            k = URLDecoder.decode(field, "UTF-8");
         k = k.toLowerCase();
         parameters.add(k, v);
      }
      return parameters;
   }

   /**
    * Implementation of Cloneable interface for Request
    * @return The cloned Request
    */
   @Override
   public Object clone() throws CloneNotSupportedException
   //-----------------------------------------------------
   {
      Request klone = (Request) super.clone();

      klone.m_ex = m_ex;
      klone.m_httpd = m_httpd;
      klone.m_requestHeaders = (CloneableHeaders) m_requestHeaders.clone();
      klone.m_method = m_method;
      klone.m_getParameters = (CloneableHeaders) m_getParameters.clone();
      klone.m_postParameters = (CloneableHeaders) m_postParameters.clone();
      return klone;
   }

   private void _appendParams(CloneableHeaders m, StringBuffer sb)
   //-------------------------------------------------------------
   {
      if (m == null)
      {
         sb.append(Httpd.EOL);
         return;
      }
      Set<Map.Entry<String,List<String>>> headers = m.entrySet();
      for (Iterator<Map.Entry<String,List<String>>> i=headers.iterator();
         i.hasNext();)
         {
            Map.Entry<String,List<String>> e = i.next();
            sb.append(e.getKey()).append(": ");
            List<String> hv = e.getValue();
            for (Iterator<String> j=hv.iterator(); j.hasNext();)
               sb.append(j.next()).append(" ");
            sb.append(Httpd.EOL);
         }
   }
   
   private Object nullv(Object o)
   {
      if (o == null) return "";
      return o;
   }
   
   @Override
   protected void finalize() throws Throwable
   //----------------------------------------
   {
      super.finalize();
      if ( (m_cacheFile == m_compressedFile) && (m_cacheFile != null) )
         m_cacheFile.delete();
   }

   @Override
   public String toString()
   {
      return "Request {" + "m_uri=" + m_uri + ", m_path=" + m_path + ", m_requestHeaders=" +
              m_requestHeaders + ", m_method=" + m_method + ", m_methodString=" + m_methodString +
              ((m_isGet) ? ("[" + m_getParameters + "]") : ("[" + m_postParameters + "]")) +
              ", m_encoding=" + m_encoding + ", m_contentLength=" + m_contentLength + ", m_compress=" +
              m_compress + ", m_compressedFile=" + m_compressedFile + ", m_eTag=" + m_eTag +'}';
   }


}
