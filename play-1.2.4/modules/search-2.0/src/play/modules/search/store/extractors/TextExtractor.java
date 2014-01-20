package play.modules.search.store.extractors;

import play.db.jpa.Blob;

public interface TextExtractor {
    public boolean handles (String mime);
    public String extract (Blob blob);
}
