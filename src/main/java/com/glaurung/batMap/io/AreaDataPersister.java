package com.glaurung.batMap.io;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
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
             ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
            return (AreaSaveObject) objectInputStream.readObject();
        }
    }

    public static List<String> listAreaNames( String basedir ) {
        File newDir = new File( basedir, NEW_PATH );
        newDir = new File( newDir, PATH );
        File folder = newDir;
        File[] files = folder.listFiles();
        LinkedList<String> names = new LinkedList<String>();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (name.endsWith( SUFFIX )) {
                    names.add( name.substring( 0, name.length() - SUFFIX.length() ) );
                }
            }
        }
        return names;
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
            if (oldDirFiles.size() == 0) {
                Files.deleteIfExists( oldDir.toPath() );
                return;
            }
            Files.createDirectories( newDir.toPath() );
            for (File mapfile : oldDirFiles) {
                // never overwrite a map that already exists in the new location
                File target = new File( newDir, mapfile.getName() );
                if (! target.exists()) {
                    Files.move( mapfile.toPath(), target.toPath() );
                }
            }
            //all files moved to new place now, can safely delete old directory
            if (listFiles( oldDir ).size() == 0) {
                Files.deleteIfExists( oldDir.toPath() );
            }

        } catch (IOException e) {
            e.printStackTrace();
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
