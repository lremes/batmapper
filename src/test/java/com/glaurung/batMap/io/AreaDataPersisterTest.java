package com.glaurung.batMap.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.geom.Point2D;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.glaurung.batMap.gui.MapperLayout;
import com.glaurung.batMap.vo.Area;
import com.glaurung.batMap.vo.AreaSaveObject;
import com.glaurung.batMap.vo.Exit;
import com.glaurung.batMap.vo.Room;

import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;

public class AreaDataPersisterTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private File oldDir;
    private File newDir;

    @Before
    public void setUp() {
        oldDir = new File( temp.getRoot(), "batMapAreas" );
        newDir = new File( new File( temp.getRoot(), "conf" ), "batMapAreas" );
    }

    private File write( File dir, String name, String content ) throws IOException {
        dir.mkdirs();
        File file = new File( dir, name );
        Files.write( file.toPath(), content.getBytes( StandardCharsets.UTF_8 ) );
        return file;
    }

    private String read( File file ) throws IOException {
        return new String( Files.readAllBytes( file.toPath() ), StandardCharsets.UTF_8 );
    }

    @Test
    public void listsOnlyBatmapFilesWithoutSuffix() throws IOException {
        write( newDir, "arelium.batmap", "" );
        write( newDir, "the.dotted.area.batmap", "" );
        write( newDir, "arelium.batmap.0.bk", "" );
        write( newDir, "notes.txt", "" );
        write( newDir, "upper.BATMAP", "" );

        assertEquals( new HashSet<>( Arrays.asList( "arelium", "the.dotted.area" ) ),
                      new HashSet<>( AreaDataPersister.listAreaNames( temp.getRoot().getPath() ) ) );
    }

    @Test
    public void savedAreaLoadsBack() throws Exception {
        new File( temp.getRoot(), "conf" ).mkdirs(); // the client always has its conf dir
        Area area = new Area( "trader's house" );
        SparseMultigraph<Room, Exit> graph = new SparseMultigraph<Room, Exit>();
        MapperLayout layout = new MapperLayout( graph );
        Room hall = new Room( "Hall", "hall", area );
        Room shop = new Room( "Shop", "shop", area );
        graph.addEdge( new Exit( "n" ), hall, shop, EdgeType.DIRECTED );
        layout.setLocation( hall, new Point2D.Double( 0, 0 ) );
        layout.setLocation( shop, new Point2D.Double( 0, -120 ) );

        AreaDataPersister.save( temp.getRoot().getPath(), graph, layout );
        AreaSaveObject loaded = AreaDataPersister.loadData( temp.getRoot().getPath(), "trader's house" );

        assertEquals( new HashSet<>( Arrays.asList( "traders house" ) ),
                      new HashSet<>( AreaDataPersister.listAreaNames( temp.getRoot().getPath() ) ) );
        assertEquals( 2, loaded.getGraph().getVertexCount() );
        assertEquals( 1, loaded.getGraph().getEdgeCount() );
        assertEquals( "trader's house", loaded.getGraph().getVertices().iterator().next().getArea().getName() );
        for (Map.Entry<Room, Point2D> location : loaded.getLocations().entrySet()) {
            if (location.getKey().getId().equals( "shop" )) {
                assertEquals( new Point2D.Double( 0, -120 ), location.getValue() );
            }
        }
        assertEquals( 2, loaded.getLocations().size() );
    }

    @Test
    public void listingMissingDirectoryGivesNoAreas() {
        assertTrue( AreaDataPersister.listAreaNames( temp.getRoot().getPath() ).isEmpty() );
    }

    @Test
    public void migrationMovesAllFilesAndRemovesOldDirectory() throws IOException {
        write( oldDir, "a.batmap", "map a" );
        write( oldDir, "b.batmap", "map b" );

        AreaDataPersister.migrateFiles( oldDir, newDir );

        assertEquals( "map a", read( new File( newDir, "a.batmap" ) ) );
        assertEquals( "map b", read( new File( newDir, "b.batmap" ) ) );
        assertFalse( oldDir.exists() );
    }

    @Test
    public void migrationNeverOverwritesExistingMaps() throws IOException {
        write( oldDir, "a.batmap", "old a" );
        write( oldDir, "b.batmap", "old b" );
        write( newDir, "b.batmap", "new b" );

        AreaDataPersister.migrateFiles( oldDir, newDir );

        assertEquals( "old a", read( new File( newDir, "a.batmap" ) ) );
        assertEquals( "new b", read( new File( newDir, "b.batmap" ) ) );
        // the clashing map stays behind, so the old directory is kept too
        assertEquals( "old b", read( new File( oldDir, "b.batmap" ) ) );
        assertFalse( new File( oldDir, "a.batmap" ).exists() );
    }

    @Test
    public void migrationRemovesEmptyOldDirectory() {
        oldDir.mkdirs();

        AreaDataPersister.migrateFiles( oldDir, newDir );

        assertFalse( oldDir.exists() );
        assertFalse( newDir.exists() );
    }

    @Test
    public void migrationKeepsOldDirectoryWithSubfoldersQuietly() throws IOException {
        File subfolder = new File( oldDir, "subfolder" );
        write( subfolder, "kept.txt", "keep me" );
        write( oldDir, "a.batmap", "map a" );
        PrintStream originalErr = System.err;
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr( new PrintStream( err, true ) );
        try {
            AreaDataPersister.migrateFiles( oldDir, newDir );
        } finally {
            System.setErr( originalErr );
        }

        assertEquals( "", err.toString() );
        assertEquals( "map a", read( new File( newDir, "a.batmap" ) ) );
        assertEquals( "keep me", read( new File( subfolder, "kept.txt" ) ) );
    }

    @Test
    public void migrationDoesNothingWithoutOldDirectory() {
        AreaDataPersister.migrateFiles( oldDir, newDir );

        assertFalse( oldDir.exists() );
        assertFalse( newDir.exists() );
    }

}
