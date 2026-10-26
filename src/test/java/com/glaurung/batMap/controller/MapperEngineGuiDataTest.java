package com.glaurung.batMap.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.glaurung.batMap.io.GuiDataPersister;
import com.glaurung.batMap.vo.GuiData;
import com.mythicscape.batclient.interfaces.BatWindow;

public class MapperEngineGuiDataTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private MapperEngine engine;
    private File confFile;

    @BeforeClass
    public static void headless() {
        System.setProperty( "java.awt.headless", "true" );
    }

    @Before
    public void setUp() {
        new File( temp.getRoot(), "conf" ).mkdirs();
        confFile = new File( new File( temp.getRoot(), "conf" ), "batMap.conf" );
        engine = new MapperEngine( null );
        engine.setBaseDir( temp.getRoot().getPath() );
        engine.setBatWindow( window( new Point( 120, 80 ), new Dimension( 900, 600 ) ) );
    }

    private BatWindow window( final Point location, final Dimension size ) {
        return (BatWindow) Proxy.newProxyInstance( getClass().getClassLoader(), new Class<?>[] { BatWindow.class },
                new InvocationHandler() {
                    public Object invoke( Object proxy, Method method, Object[] args ) {
                        if (method.getName().equals( "getLocation" )) {
                            return location;
                        }
                        if (method.getName().equals( "getSize" )) {
                            return size;
                        }
                        return null;
                    }
                } );
    }

    @Test
    public void resizeIsNotSavedImmediately() {
        engine.componentResized( null );
        assertFalse( confFile.exists() );
    }

    @Test
    public void flushSavesPendingResize() {
        engine.componentResized( null );

        engine.flushGuiData();

        GuiData saved = GuiDataPersister.load( temp.getRoot().getPath() );
        assertEquals( 120, saved.getX() );
        assertEquals( 80, saved.getY() );
        assertEquals( 900, saved.getWidth() );
        assertEquals( 600, saved.getHeight() );
    }

    @Test
    public void flushSavesPendingMove() {
        engine.componentMoved( null );

        engine.flushGuiData();

        assertEquals( 120, GuiDataPersister.load( temp.getRoot().getPath() ).getX() );
    }

    @Test
    public void flushWithoutPendingChangeSavesNothing() {
        engine.flushGuiData();

        assertFalse( confFile.exists() );
        assertNull( GuiDataPersister.load( temp.getRoot().getPath() ) );
    }

}
