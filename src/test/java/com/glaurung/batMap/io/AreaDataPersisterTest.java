package com.glaurung.batMap.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
    public void migrationDoesNothingWithoutOldDirectory() {
        AreaDataPersister.migrateFiles( oldDir, newDir );

        assertFalse( oldDir.exists() );
        assertFalse( newDir.exists() );
    }

}
