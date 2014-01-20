package play.modules.search.store;

import java.util.ArrayList;
import java.util.List;

import play.Logger;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.db.jpa.Blob;
import play.modules.search.store.extractors.TextExtractor;
import play.modules.search.store.mime.ExtensionGuesser;
import play.modules.search.store.mime.MimeGuesser;

/**
 * This class performs Full text extraction from various
 * file formats
 * @author jfp
 */
public class FileExtractor {
    
    public static List<TextExtractor> extractors = new ArrayList<TextExtractor>();
    public static MimeGuesser mimeGuesser = new ExtensionGuesser();
    
    public static void init() {
        Logger.debug("init FileExtractor");
        List<ApplicationClass> classes = Play.classes.getAssignableClasses(TextExtractor.class);
        List<TextExtractor> extractors = new ArrayList<TextExtractor>();
        for (ApplicationClass applicationClass : classes) {
            try {
                Logger.trace("adding %s as a TextExtractor", applicationClass.name);
                extractors.add((TextExtractor) applicationClass.javaClass.newInstance());
            } catch (Exception e) {
                Logger.warn(e,"Could not instanciate text extractor %s",applicationClass.javaClass.getName());
            }
        }
        FileExtractor.extractors = extractors;
    }
    
    public static String getText (Blob blob) {
        // Guess mime
        String mime = mimeGuesser.guess (blob);
        // Invoke the handlers
        String fileName = blob.getFile().getName();
        for (TextExtractor extractor : extractors) {
            if (extractor.handles(mime)) {
                Logger.debug ("Using %s extractor to handle blob %s, mime=%s", extractor.getClass().getName(), fileName, blob.type());
                return extractor.extract(blob);
            }
        }
        Logger.warn("No handlers able to index %s mime type, file was %s", mime, fileName);
        return null;
    }
}
