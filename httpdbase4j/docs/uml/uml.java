
class HttpServer {}
class HttpContext {}
class HttpExchange {}

abstract interface HttpHandleable {}
abstract interface Templatable {}
abstract interface Postable {}
abstract interface DirItemInterface {}

/**
 * @composed 1 - 1 HttpServer
 * @composed 1 - 1 HttpContext
 * @has 1 m_handlerMap n HttpHandleable
 * @has 1 m_postHandlerMap n Postable
 */
abstract class Httpd implements HttpHandleable, Postable {}

/**
 * @extends Httpd
 */
class ArchiveHttpd implements HttpHandleable, Postable {}
/**
 * @extends Httpd
 */
class FileHttpd implements HttpHandleable, Postable {}

/**
 * @composed 1 - 1 Httpd
 */
abstract class RequestHandler implements HttpHandleable {}

/**
 * @composed 1 - 1 Httpd
 * @assoc 1 - 1 HttpExchange
 * @assoc 1 - 1  ArchiveRequest
 * @assoc 1 - 1  FileRequest 
 * @assoc 1 - 1  StringTemplateHandler
 */
class ArchiveRequestHandler extends RequestHandler implements HttpHandleable {}

/**
 * @composed 1 - 1 Httpd
 * @assoc 1 - 1 HttpExchange
 * @assoc 1 - 1  FileRequest
 * @assoc 1 - 1  StringTemplateHandler
 */
class FileRequestHandler extends RequestHandler implements HttpHandleable {}

class CloneableHeaders {}

/**
 * @composed 1 - 1 Httpd
 * @composed 1 - 1 HttpExchange 
 * @has 1 m_requestHeaders n CloneableHeaders
 * @has 1 m_getParameters n CloneableHeaders
 * @has 1 m_postParameters n CloneableHeaders
 */
abstract class Request implements DirItemInterface {}

/**
 * @assoc 1 - 1 HttpHandleable
 * @assoc 1 - 1 Postable
  */
class FileRequest extends Request implements DirItemInterface {}

/**
 * @assoc 1 - 1 HttpHandleable
 * @assoc 1 - 1 Postable
 */
class ArchiveRequest extends Request implements DirItemInterface {}

abstract class CombinedRequest extends Request implements DirItemInterface {}

class ArchiveCombinedRequest extends CombinedRequest {}

class FileCombinedRequest extends CombinedRequest {}

abstract class StringTemplateHandler implements HttpHandleable, Postable, Templatable {}