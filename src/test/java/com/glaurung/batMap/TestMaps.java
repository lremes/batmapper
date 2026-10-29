package com.glaurung.batMap;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;

import com.glaurung.batMap.gui.MapperLayout;
import com.glaurung.batMap.io.AreaDataPersister;
import com.glaurung.batMap.vo.Area;
import com.glaurung.batMap.vo.Exit;
import com.glaurung.batMap.vo.Room;

import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;

/**
 * Saves small maps for tests, the same way the mapper saves them.
 */
public class TestMaps {

    /**
     * Saves an area with one room per {id, short desc, long desc}, rooms connected in a row going east.
     */
    public static File saveArea( File baseDir, String areaName, String[]... rooms ) throws IOException {
        new File( baseDir, "conf" ).mkdirs(); // the client always has its conf dir
        Area area = new Area( areaName );
        SparseMultigraph<Room, Exit> graph = new SparseMultigraph<Room, Exit>();
        MapperLayout layout = new MapperLayout( graph );
        Room previous = null;
        for (int i = 0; i < rooms.length; i++) {
            Room room = new Room( rooms[i][1], rooms[i][0], area );
            room.setLongDesc( rooms[i][2] );
            graph.addVertex( room );
            layout.setLocation( room, new Point2D.Double( i * 120, 0 ) );
            if (previous != null) {
                graph.addEdge( new Exit( "e" ), previous, room, EdgeType.DIRECTED );
            }
            previous = room;
        }
        AreaDataPersister.save( baseDir.getPath(), graph, layout );
        return mapFile( baseDir, areaName );
    }

    public static File mapFile( File baseDir, String fileName ) {
        return new File( new File( new File( baseDir, "conf" ), "batMapAreas" ), fileName.replaceAll( "'", "" ).replaceAll( "/", "" ) + ".batmap" );
    }

}
