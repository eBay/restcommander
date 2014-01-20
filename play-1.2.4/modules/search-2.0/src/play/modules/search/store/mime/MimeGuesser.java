package play.modules.search.store.mime;

import play.db.jpa.Blob;

public interface MimeGuesser {
    public String guess (Blob blob);
}
