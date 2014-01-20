package controllers.modules.search;

import java.util.List;

import play.Play;
import play.libs.Codec;
import play.modules.search.Search;
import play.modules.search.store.ManagedIndex;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;

public class Administration extends Controller {
    
    @Before
    protected static void check () {
        Http.Header auth = request.headers.get("authorization");
        if(auth != null) {
            String encodedPassword = auth.value().substring("Basic ".length());
            String password = new String(Codec.decodeBASE64(encodedPassword));
            String user = password.substring(0, password.indexOf(':'));
            String pwd = password.substring((user + ":").length());
            System.out.println("user is " + user + " , passwd is " + pwd);
            if (! pwd.equals(Play.configuration.getProperty("play.search.password","search")))
                    unauthorized("You are not authorized");
         } else {
             unauthorized("You are not authorized");
         }
    }
    
    public static void index () {
        List<ManagedIndex> indexes = Search.getCurrentStore().listIndexes();
        render(indexes);
    }
    
    public static void optimize (String name) {
        Search.getCurrentStore().optimize(name);
        index();
    }
    
    public static void reindex(String name) {
        Search.getCurrentStore().rebuild(name);
        index();
    }
    
    public static void reopen (String name) {
        Search.getCurrentStore().reopen(name);
        index();
    }
}
