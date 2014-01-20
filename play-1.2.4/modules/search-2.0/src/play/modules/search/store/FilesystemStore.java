package play.modules.search.store;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

import play.Logger;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.db.jpa.JPA;
import play.db.jpa.JPABase;
import play.exceptions.UnexpectedException;
import play.libs.Files;
import play.modules.search.Indexed;
import play.modules.search.Search;

public class FilesystemStore implements Store {

    protected Map<String, IndexWriter> indexWriters = new HashMap<String, IndexWriter>();

    protected Map<String, IndexSearcher> indexSearchers = new HashMap<String, IndexSearcher>();

    public static String DATA_PATH;

    public static boolean sync = true;

    public void unIndex(Object object) {
        try {
            if (!(object instanceof JPABase))
                return;
            if (object.getClass().getAnnotation(Indexed.class) == null)
                return;
            JPABase jpaBase = (JPABase ) object;
            String index = object.getClass().getName();
            getIndexWriter(index).deleteDocuments(new Term("_docID", ConvertionUtils.getIdValueFor(jpaBase) + ""));
            if (sync) {
                getIndexWriter(index).commit();
                dirtyReader(index);
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public void index(Object object, String index) {
        try {
            if (!(object instanceof JPABase)) {
                Logger.warn("Unable to index " + object + ", unsupported class type. Only play.db.jpa.JPABase classes are supported.");
                return;
            }
            JPABase jpaABase = (JPABase ) object;
            Document document = ConvertionUtils.toDocument(object);
            if (document == null)
                return;
            getIndexWriter(index).deleteDocuments(new Term("_docID", ConvertionUtils.getIdValueFor(jpaABase) + ""));
            getIndexWriter(index).addDocument(document);
            if (sync) {
                getIndexWriter(index).commit();
                dirtyReader(index);
            } else {
                if (getIndexWriter(index).ramSizeInBytes() > 1024 * 1024 * 48) {
                    getIndexWriter(index).commit();
                    dirtyReader(index);
                }
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
    
    public IndexSearcher getIndexSearcher(String name) {
        try {
            if (!indexSearchers.containsKey(name)) {
                synchronized (this) {
                    File root = new File(DATA_PATH, name);
                    if (!root.exists())
                        getIndexWriter(name);
                    IndexSearcher reader = new IndexSearcher(FSDirectory.open(root));
                    indexSearchers.put(name, reader);
                }
            }
            return indexSearchers.get(name);
        } catch (Exception e) {
            throw new UnexpectedException("Cannot open index", e);
        }
    }

    /**
     * Used to synchronize reads after writes
     *
     * @param name of the reader to be reopened
     */
    public void dirtyReader(String name) {
        synchronized (this) {
            try {
                if (indexSearchers.containsKey(name)) {
                    IndexReader rd = indexSearchers.get(name).getIndexReader();
                    indexSearchers.get(name).close();
                    indexSearchers.remove(name);
                }
            } catch (Exception e) {
                throw new UnexpectedException("Can't reopen reader", e);
            }
        }
    }

    private IndexWriter getIndexWriter(String name) {
        try {
            if (!indexWriters.containsKey(name)) {
                synchronized (this) {
                    File root = new File(DATA_PATH, name);
                    if (!root.exists())
                        root.mkdirs();
                    if (new File(root, "write.lock").exists())
                        new File(root, "write.lock").delete();
                    IndexWriter writer = new IndexWriter(FSDirectory.open(root), Search.getAnalyser(), MaxFieldLength.UNLIMITED);
                    indexWriters.put(name, writer);
                }
            }
            return indexWriters.get(name);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public void rebuildAllIndexes() throws Exception {
        stop();
        File fl = new File(DATA_PATH);
        Files.deleteDirectory(fl);
        fl.mkdirs();
        List<ApplicationClass> classes = Play.classes.getAnnotatedClasses(Indexed.class);
        for (ApplicationClass applicationClass : classes) {
            List<JPABase> objects = JPA.em().createQuery(
                                            "select e from " + applicationClass.javaClass.getCanonicalName() + " as e")
                                            .getResultList();
            for (JPABase jpaBase : objects) {
                index(jpaBase, applicationClass.javaClass.getName());
            }
        }
        Logger.info("Rebuild index finished");
    }

    public List<ManagedIndex> listIndexes() {
        List<ManagedIndex> indexes = new ArrayList<ManagedIndex>();
        List<ApplicationClass> classes = Play.classes.getAnnotatedClasses(Indexed.class);
        for (ApplicationClass applicationClass : classes) {
            ManagedIndex index = new ManagedIndex();
            index.name = applicationClass.javaClass.getName();
            index.optimized = getIndexSearcher(index.name).getIndexReader().isOptimized();
            index.documentCount = getIndexSearcher(index.name).getIndexReader().numDocs();
            index.jpaCount =  (Long ) JPA.em().createQuery("select count (*) from " + applicationClass.javaClass.getCanonicalName()+ ")").getSingleResult();
            indexes.add(index);
        }
        return indexes;
    }

    public void start() {
        if (Play.configuration.containsKey("play.search.path"))
            DATA_PATH = Play.configuration.getProperty("play.search.path");
        else
            DATA_PATH = Play.applicationPath.getAbsolutePath() + "/data/search/";
        Logger.trace("Search module repository is in " + DATA_PATH);
        sync = Boolean.parseBoolean(Play.configuration.getProperty("play.search.synch", "true"));
        Logger.trace("Write operations sync: " + sync);
    }

    public void stop() throws Exception {
        for (IndexWriter writer : indexWriters.values()) {
            writer.close();
        }
        for (IndexSearcher searcher : indexSearchers.values()) {
            searcher.close();
        }
        indexWriters.clear();
        indexSearchers.clear();
    }

    public void optimize(String name) {
        try {
            getIndexWriter(name).optimize(true);
            getIndexWriter(name).commit();
            dirtyReader(name);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public void rebuild(String name) {
        String id = UUID.randomUUID().toString();
        File oldFolder = new File(DATA_PATH, name);
        File newFolder = new File(DATA_PATH, name + id);
        Class cl = Play.classes.getApplicationClass(name).javaClass;
        List<JPABase> objects = JPA.em().createQuery("select e from " + cl.getCanonicalName() + " as e").getResultList();
        String index = cl.getName() + id;
        IndexWriter indexWriter = getIndexWriter(index);
        try {
            for (JPABase jpaBase : objects) {
                Document document = ConvertionUtils.toDocument(jpaBase);
                if (document == null)
                    return;
                indexWriter.addDocument(document);
            }

            getIndexWriter(index).commit();
            dirtyReader(index);
            getIndexSearcher(name).close();
            indexSearchers.remove(name);
            getIndexWriter(name).close();
            indexWriters.remove(name);
            Files.deleteDirectory(oldFolder);
            newFolder.renameTo(oldFolder);
        } catch (IOException e) {
            throw new UnexpectedException(e);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public void reopen(String name) {
        dirtyReader(name);
    }

    public void delete(String name) {
        synchronized (this) {
            try {
                if (indexSearchers.containsKey(name)) {
                    IndexReader rd = indexSearchers.get(name).getIndexReader();
                    indexSearchers.get(name).close();
                    indexSearchers.remove(name);
                }
                if (indexWriters.containsKey(name)) {
                    indexWriters.get(name).close();
                    indexWriters.remove(name);
                }
                File target = new File(DATA_PATH, name);
                if (target.exists() && target.isDirectory())
                    Files.deleteDirectory(target);
            } catch (Exception e) {
                throw new UnexpectedException("Can't reopen reader", e);
            }
        }
    }

    public void deleteAll() {
        File root = new File(DATA_PATH);
        if (root.exists() && root.isDirectory()) {
            File[] indexes = root.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.isDirectory();
                }
            });
            for (File file : indexes) {
                delete(file.getName());
            }
        }
    }

    public boolean hasIndex(String name) {
        return new File(DATA_PATH, name).exists();
    }
}
