abstract interface Templatable {}
abstract interface HttpHandleable {}
abstract interface Postable {}

class StringTemplate {}
class StringTemplateGroup {} 

class ArchiveStringTemplateGroup extends StringTemplateGroup {}

abstract class Httpd implements HttpHandleable, Postable {}

abstract class Request {}

/**
 * @has 1 - 1 Httpd
 * @assoc 1 m_templateProcessor 1  Templatable
 * @assoc 1 getTemplate 1  Templatable
 * @assoc 1 - 1  Request
 */
abstract class StringTemplateHandler implements HttpHandleable, Postable, Templatable {}

/**
 * @assoc 1 - 1 Httpd
 * @assoc 1 - 1 ArchiveStringTemplateGroup 
 * @assoc 1 loadTemplate 1 StringTemplate
 */
class ArchiveStringTemplateHandler extends StringTemplateHandler implements HttpHandleable, Postable, Templatable {}

/**
 * @assoc 1 - 1 Httpd
 * @assoc 1 - 1 StringTemplateGroup 
 * @assoc 1 loadTemplate 1 StringTemplate
 */
class FileStringTemplateHandler extends StringTemplateHandler implements HttpHandleable, Postable, Templatable {}

class TemplatableAdapter implements Templatable {}