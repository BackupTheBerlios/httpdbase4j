package net.homeip.donaldm.testng.templates;

import java.io.File;
import java.io.InputStream;
import net.homeip.donaldm.httpdbase4j.Request;

import org.antlr.stringtemplate.StringTemplate;
import net.homeip.donaldm.httpdbase4j.TemplatableAdapter;

public class Pagetemplate extends TemplatableAdapter
//--------------------------------------------------
{
   private SelItem[] selList;
   
   public Pagetemplate()
   {
      selList = new SelItem[6];
      for (int i=0; i<5; i++)
         selList[i] = new SelItem("OPT" + i, "Option " + (i+1));
      selList[5] = new SelItem("OPT5", "Option 6", true);
      
   }
   
   private void setTemplateAttributes(StringTemplate template, Request request)
   // ---------------------------------------------------------
   {
      template.setAttribute("selList", selList);
      if (System.getProperty("os.name").toLowerCase().contains("windows"))
         template.setAttribute("welcome", "Welcome Dumbass");
      else
         template.setAttribute("welcome", "Welcome Master");
   }

   public java.io.File templateFile(StringTemplate template, Request request, 
                            StringBuffer mimeType, File dir)
   // ---------------------------------------------------------------------
   {
      setTemplateAttributes(template, request);
      return super.templateFile(template, request, mimeType, dir);
   }

   public String templateString(StringTemplate template, Request request, 
                                StringBuffer mimeType)
   // -----------------------------------------------------------------------
   {
      setTemplateAttributes(template, request);
      return super.templateString(template, request, mimeType);
   }

   public InputStream templateStream(StringTemplate template, Request request, 
                                     StringBuffer mimeType)
   // -------------------------------------------------------------------------
   {
      setTemplateAttributes(template, request);
      return super.templateStream(template, request, mimeType);
   }
}
