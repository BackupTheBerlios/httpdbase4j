package net.homeip.donaldm.testng.templates;

public class SelItem
{
   String key, value;
   boolean selected = false;
   
   public SelItem(String key, String value)
   {
      this.key = key;
      this.value = value;
   }
   
   public SelItem(String key, String value, boolean isSelected)
   {
      this.key = key;
      this.value = value;
      this.selected = isSelected;
   }
   
   public String getKey() { return key; }
   
   public String getValue() { return value; }
   
   public boolean isSelected() { return selected; } 
}