package play.modules.search.store.mime;

import java.util.HashMap;
import java.util.Map;

import play.db.jpa.Blob;

public class ExtensionGuesser implements MimeGuesser {
    public static Map<String, String> extensions = new HashMap<String, String> ();
    static {
        extensions.put("pdf", "application/pdf");
        //FIXME: complete
    }
    
    public String guess (Blob blob) {
        if(blob != null)
            return blob.type();
        return null;
    }
}
