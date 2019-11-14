import utils.*;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * This example shows how to use Java parallel streams in conjunction
 * with the ManagedBlocker interface and the Java fork-join pool to
 * download multiple images from a remote server.
 */
public class ex20 {
    /**
     * Logging tag.
     */
    private static final String TAG = ex20.class.getName();

    /**
     * The JVM requires a static main() entry point to run the app.
     */
    public static void main(String[] args) {
        // Initializes the Options singleton.
        Options.instance().parseArgs(args);

        new ex20().run();
    }

    /**
     * Run the program.
     */
    private void run() {
        System.out.println("Entering the download tests with "
                           + Runtime.getRuntime().availableProcessors()
                           + " cores available");

        // Warm up the common fork-join pool.
        warmUpThreadPool();

        // Runs the tests using the using the Java fork-join
        // framework's default behavior, which does not add new worker
        // threads to the pool when blocking occurs.
        RunTimer.timeRun(() -> testDefaultDownloadBehavior(),
                         "testDefaultDownloadBehavior()");

        // Run the tests using the using the Java fork-join
        // framework's ManagedBlocker mechanism, which adaptively adds
        // new worker threads to the pool when blocking occurs.
        RunTimer.timeRun(() -> testAdaptiveDownloadBehavior(),
                         "testAdaptiveDownloadBehavior()");

        // Print the results.
        System.out.println(RunTimer.getTimingResults());

        System.out.println("Leaving the download tests");
    }

    /**
     * This method warms up the default thread pool.
     */
    private void warmUpThreadPool() {
        // Delete any the filtered images from the previous run.
        deleteDownloadedImages();

        // Get the list of files to the downloaded images.
        List<File> imageFiles = Options.instance().getUrlList()
            // Convert the URLs in the input list into a stream and
            // process them in parallel.
            .parallelStream()

            // Transform URL to a File by downloading each image via
            // its URL.  This call ensures the common fork/join thread
            // pool is expanded to handle the blocking image download.
            .map(this::downloadAndStoreImage)

            // Terminate the stream and collect the results into list
            // of images.
            .collect(Collectors.toList());

        // Let the system garbage collect.
        System.gc();
    }

    /**
     * This method runs the tests using the using the Java fork-join
     * framework's default behavior, which does not add new worker
     * threads to the pool when blocking occurs.
     */
    private void testDefaultDownloadBehavior() {
        // Delete any the filtered images from the previous run.
        deleteDownloadedImages();

        // Get the list of files to the downloaded images.
        List<File> imageFiles = Options.instance().getUrlList()
            // Convert the URLs in the input list into a stream and
            // process them in parallel.
            .parallelStream()

            // Transform URL to a File by downloading each image via
            // its URL.  This call ensures the common fork/join thread
            // pool is expanded to handle the blocking image download.
            .map(this::downloadAndStoreImage)

            // Terminate the stream and collect the results into list
            // of images.
            .collect(Collectors.toList());

        // Print the statistics for this test run.
        printStats(imageFiles.size());

        // Let the system garbage collect.
        System.gc();
    }

    /**
     * This method runs the tests using the using the Java fork-join
     * framework's ManagedBlocker mechanism, which adaptively adds new
     * worker threads to the pool when blocking occurs.
     */
    private void testAdaptiveDownloadBehavior() {
        // Delete any the filtered images from the previous run.
        deleteDownloadedImages();

        // Get the list of files to the downloaded images.
        List<File> imageFiles = Options.instance().getUrlList()
            // Convert the URLs in the input list into a stream and
            // process them in parallel.
            .parallelStream()

            // Transform URL to a File by downloading each image via
            // its URL.  This call ensures the common fork/join thread
            // pool is expanded to handle the blocking image download.
            .map(this::downloadAndStoreImageEx)

            // Terminate the stream and collect the results into list
            // of images.
            .collect(Collectors.toList());

        // Print the statistics for this test run.
        printStats(imageFiles.size());

        // Let the system garbage collect.
        System.gc();
    }

    /**
     * Transform URL to a File by downloading each image via its URL
     * and storing it using the Java fork-join framework's
     * ManagedBlocker mechanism, which adaptively adds new worker
     * threads to the pool when blocking occurs.
     */
    private File downloadAndStoreImageEx(URL url) {
        return BlockingTask
            // This call ensures the common fork/join thread pool
            // is expanded to handle the blocking image download.
            .callInManagedBlock(() -> downloadImage(url))

            // Store the image on the local device.
            .store();
    }

    /**
     * Transform URL to a File by downloading each image via its URL
     * and storing it *without* using the Java fork-join framework's
     * ManagedBlocker mechanism, i.e., the pool of worker threads will
     * not be expanded.
     */
    private File downloadAndStoreImage(URL url) {
        return 
            // Perform a blocking image download.
            downloadImage(url)

            // Store the image on the local device.
            .store();
    }

    /**
     * Factory method that retrieves the image associated with the @a
     * url and creates an Image to encapsulate it.
     */
    private Image downloadImage(URL url) {
        return new Image(url,
                         NetUtils.downloadContent(url));
    }

    /**
     * Display the statistics about the test.
     */
    private void printStats(int imageCount) {
        System.out.println(TAG + 
                           ": downloaded and stored "
                           + imageCount
                           + " images using "
                           + (ForkJoinPool.commonPool().getPoolSize() + 1)
                           + " threads in the pool with "
                           + ForkJoinPool.commonPool().getStealCount()
                           + " tasks stolen");
    }

    /**
     * Clears the filter directories.
     */
    private void deleteDownloadedImages() {
        int deletedFiles =
            deleteSubFolders(Options.instance().getDirectoryPath());

        if (Options.instance().diagnosticsEnabled())
            System.out.println(TAG
                               + ": "
                               + deletedFiles
                               + " previously downloaded file(s) deleted");
    }

    /**
     * Recursively delete files in a specified directory.
     */
    private int deleteSubFolders(String path) {
        int deletedFiles = 0;
        File currentFolder = new File(path);        
        File files[] = currentFolder.listFiles();

        if (files == null) 
            return 0;

        // Java doesn't delete a directory with child files, so we
        // need to write code that handles this recursively.
        for (File f : files) {          
            if (f.isDirectory()) 
                deletedFiles += deleteSubFolders(f.toString());
            f.delete();
            deletedFiles++;
        }

        // Don't delete the current folder.
        // currentFolder.delete();
        return deletedFiles;
    }
}
