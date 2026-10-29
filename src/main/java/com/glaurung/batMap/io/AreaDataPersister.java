package com.glaurung.batMap.io;

import java.awt.geom.Point2D;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.glaurung.batMap.vo.AreaSaveObject;
import com.glaurung.batMap.vo.Exit;
import com.glaurung.batMap.vo.Room;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.SparseMultigraph;

public class AreaDataPersister {

    private static final String SUFFIX = ".batmap";
    private static final String PATH = "batMapAreas";
    private static final String NEW_PATH = "conf";


    public static void save( String basedir, SparseMultigraph<Room, Exit> graph, Layout<Room, Exit> layout ) throws IOException {
        AreaSaveObject saveObject = makeSaveObject( basedir, graph, layout );
        saveData( saveObject );

    }


    private static void saveData( AreaSaveObject saveObject ) throws IOException {
	String baseName = saveObject.getFileName();
	File baseFile = new File( saveObject.getFileName() );
	File target = null;
	long timestamp = Long.MAX_VALUE;
	if (baseFile.exists()) {
	    for (int i = 0; i < 5; i++) {
		File backup = new File ( saveObject.getFileName() + "." + i + ".bk" );
		if (!backup.exists()) {
		    target = backup;
		    break;
		} else if (backup.lastModified() < timestamp) {
		    timestamp = backup.lastModified();
		    target = backup;
		}
	    }
	    System.out.println("Renaming " + baseFile + " to " + target);
	    baseFile.renameTo(target);
	}

        try (FileOutputStream fileOutputStream = new FileOutputStream(new File(saveObject.getFileName()));
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
            objectOutputStream.writeObject(saveObject);
        }
    }


    public static AreaSaveObject loadData( String basedir, String areaName ) throws IOException, ClassNotFoundException {

        File dataFile = new File( getFileNameFrom( basedir, areaName ) );
//		System.out.println("\n\n+ndataFileForLoading\n\n\n"+dataFile);
        try (FileInputStream fileInputStream = new FileInputStream(dataFile);
             // ObjectInputStream reads a few bytes at a time, so buffer it or every read goes to the OS
             ObjectInputStream objectInputStream = new ObjectInputStream( new BufferedInputStream( fileInputStream ) )) {
            return (AreaSaveObject) objectInputStream.readObject();
        }
    }

    public static List<String> listAreaNames( String basedir ) {
        LinkedList<String> names = new LinkedList<String>();
        for (File file : listAreaFiles( basedir )) {
            names.add( areaNameOf( file ) );
        }
        return names;
    }

    /**
     * Area names with the time their map was last saved, so callers can tell which maps changed.
     */
    public static Map<String, Long> listAreaSaveTimes( String basedir ) {
        final Map<String, Long> saveTimes = new LinkedHashMap<String, Long>();
        Path dir = new File( new File( basedir, NEW_PATH ), PATH ).toPath();
        if (! Files.isDirectory( dir )) {
            return saveTimes;
        }
        try {
            // walking gets the file times along with the directory listing on Windows, instead of asking for each file
            Files.walkFileTree( dir, EnumSet.noneOf( FileVisitOption.class ), 1, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) {
                    if (file.getFileName().toString().endsWith( SUFFIX )) {
                        saveTimes.put( areaNameOf( file.toFile() ), attrs.lastModifiedTime().toMillis() );
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed( Path file, IOException e ) {
                    return FileVisitResult.CONTINUE;
                }
            } );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return saveTimes;
    }

    private static List<File> listAreaFiles( String basedir ) {
        File newDir = new File( basedir, NEW_PATH );
        newDir = new File( newDir, PATH );
        File folder = newDir;
        File[] files = folder.listFiles();
        LinkedList<File> areaFiles = new LinkedList<File>();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith( SUFFIX )) {
                    areaFiles.add( file );
                }
            }
        }
        return areaFiles;
    }

    private static String areaNameOf( File areaFile ) {
        String name = areaFile.getName();
        return name.substring( 0, name.length() - SUFFIX.length() );
    }

    private static AreaSaveObject makeSaveObject( String basedir, SparseMultigraph<Room, Exit> graph, Layout<Room, Exit> layout ) throws IOException {
        AreaSaveObject saveObject = new AreaSaveObject();
        saveObject.setGraph( graph );
        Map<Room, Point2D> locations = saveObject.getLocations();
        for (Room room : graph.getVertices()) {
            Point2D coord = layout.transform( room );
            locations.put( room, coord );
        }
        saveObject.setFileName( getFileNameFrom( basedir, graph.getVertices().iterator().next().getArea().getName() ) );
//		System.out.println("\n\n+nsaveobjectdone\n\n\n"+saveObject.getFileName());
        return saveObject;
    }

    private static String getFileNameFrom( String basedir, String areaName ) throws IOException {

        areaName = areaName.replaceAll( "'", "" );
        areaName = areaName.replaceAll( "/", "" );
        areaName = areaName + SUFFIX;
        File newDir = new File( basedir, NEW_PATH );
        newDir = new File( newDir, PATH );
//		File pathFile = new File(PATH);
        if (! newDir.exists()) {
            if (! newDir.mkdir()) {
                throw new IOException( PATH + " doesn't exist" );
            }
        }

        return new File( newDir, areaName ).getPath();
    }

    public static void migrateFilesToNewLocation( String basedir ) {
        File newDir = new File( basedir, NEW_PATH );
        newDir = new File( newDir, PATH );
        migrateFiles( new File( PATH ), newDir );
    }

    static void migrateFiles( File oldDir, File newDir ) {
        if (! oldDir.exists())
            return;
        List<File> oldDirFiles = listFiles( oldDir );

        try {
            if (oldDirFiles.size() > 0) {
                Files.createDirectories( newDir.toPath() );
            }
            for (File mapfile : oldDirFiles) {
                // never overwrite a map that already exists in the new location
                File target = new File( newDir, mapfile.getName() );
                if (! target.exists()) {
                    Files.move( mapfile.toPath(), target.toPath() );
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // only remove the old directory once everything in it has been moved
        deleteIfEmpty( oldDir );
    }

    private static void deleteIfEmpty( File dir ) {
        String[] contents = dir.list();
        if (contents != null && contents.length == 0) {
            dir.delete();
        }
    }

    private static List<File> listFiles( File dir ) {
        List<File> result = new LinkedList<File>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    result.add( file );
                }
            }
        }
        return result;
    }


}
