package com.glaurung.batMap.controller;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.glaurung.batMap.io.AreaDataPersister;
import com.glaurung.batMap.vo.AreaSaveObject;
import com.glaurung.batMap.vo.Room;

/**
 * Keeps the room descs of all saved maps in memory, so searching doesn't load every map each time.
 * Maps are reloaded only when their file has been saved since they were indexed.
 */
public class SearchIndex {

    private static class IndexedRoom {
        final Room room;
        final String shortDesc;
        final String longDesc;

        IndexedRoom( Room room ) {
            this.room = room;
            this.shortDesc = room.getShortDesc().toLowerCase();
            this.longDesc = room.getLongDesc().toLowerCase();
        }
    }

    private static class IndexedArea {
        final long saveTime;
        final List<IndexedRoom> rooms;

        IndexedArea( long saveTime, List<IndexedRoom> rooms ) {
            this.saveTime = saveTime;
            this.rooms = rooms;
        }
    }

    private Map<String, IndexedArea> areas = new LinkedHashMap<String, IndexedArea>();

    /**
     * Rooms whose short or long desc contains the text, ignoring case.
     */
    public synchronized List<Room> search( String baseDir, String text ) {
        refresh( baseDir );
        String lowerText = text.toLowerCase();
        List<Room> found = new LinkedList<Room>();
        for (IndexedArea area : areas.values()) {
            for (IndexedRoom indexed : area.rooms) {
                if (indexed.longDesc.contains( lowerText ) || indexed.shortDesc.contains( lowerText )) {
                    found.add( indexed.room );
                }
            }
        }
        return found;
    }

    /**
     * Brings the index up to date with the saved maps: loads new and changed ones, drops deleted ones.
     */
    public synchronized void refresh( String baseDir ) {
        Map<String, IndexedArea> refreshed = new LinkedHashMap<String, IndexedArea>();
        for (Map.Entry<String, Long> saved : AreaDataPersister.listAreaSaveTimes( baseDir ).entrySet()) {
            IndexedArea area = areas.get( saved.getKey() );
            if (area == null || area.saveTime != saved.getValue()) {
                area = load( baseDir, saved.getKey(), saved.getValue() );
            }
            refreshed.put( saved.getKey(), area );
        }
        areas = refreshed;
    }

    private IndexedArea load( String baseDir, String areaName, long saveTime ) {
        List<IndexedRoom> rooms = new LinkedList<IndexedRoom>();
        try {
            AreaSaveObject aso = AreaDataPersister.loadData( baseDir, areaName );
            for (Room room : aso.getGraph().getVertices()) {
                if (room.getShortDesc() == null || room.getLongDesc() == null) {
                    // in case of bad room data, just skip it
                    continue;
                }
                rooms.add( new IndexedRoom( room ) );
            }
        } catch (IOException | ClassNotFoundException e) {
            // an unreadable map is left out, until it is saved again
            System.out.println( "batMap: can't index area " + areaName + ": " + e );
        }
        return new IndexedArea( saveTime, rooms );
    }

}
