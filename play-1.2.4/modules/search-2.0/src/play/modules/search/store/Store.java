package play.modules.search.store;

import java.util.List;

import org.apache.lucene.search.IndexSearcher;


/**
 * Manages a search backed (indexes, searchers, readers...)
 * @author jfp
 */
public interface Store {
    public void start () throws Exception;
    public void stop () throws Exception;
    public void unIndex(Object object);
    public void index(Object object, String index);
    public void rebuildAllIndexes() throws Exception;
    public IndexSearcher getIndexSearcher (String searcherName);
    public List<ManagedIndex> listIndexes();
    public boolean hasIndex (String name);
    public void delete (String name);
    public void deleteAll ();
    public void optimize (String name);
    public void rebuild (String name);
    public void reopen (String name);
}
