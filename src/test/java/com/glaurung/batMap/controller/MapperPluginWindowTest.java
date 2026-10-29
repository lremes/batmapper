package com.glaurung.batMap.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.mythicscape.batclient.interfaces.BatWindow;
import com.mythicscape.batclient.interfaces.ClientGUI;
import com.mythicscape.batclient.interfaces.PluginManager;

/**
 * Loads the plugin against a fake client, like batclient does, and drives it with console commands.
 */
public class MapperPluginWindowTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private MapperPlugin plugin;
    private boolean windowVisible;
    private boolean windowCreated;
    private List<String> console;

    @BeforeClass
    public static void headless() {
        System.setProperty( "java.awt.headless", "true" );
    }

    @Before
    public void setUp() {
        console = new ArrayList<String>();
        plugin = new MapperPlugin();
        plugin.init( fake( PluginManager.class, null ), null, null, null, null, fake( ClientGUI.class, new InvocationHandler() {
            public Object invoke( Object proxy, Method method, Object[] args ) {
                switch (method.getName()) {
                    case "getBaseDirectory":
                        return temp.getRoot().getPath();
                    case "createBatWindow":
                        windowCreated = true;
                        return window();
                    case "printText":
                        console.add( (String) args[1] );
                        return null;
                    default:
                        return null;
                }
            }
        } ) );
    }

    private BatWindow window() {
        return fake( BatWindow.class, new InvocationHandler() {
            public Object invoke( Object proxy, Method method, Object[] args ) {
                switch (method.getName()) {
                    case "setVisible":
                        windowVisible = (Boolean) args[0];
                        return null;
                    case "isVisible":
                        return windowVisible;
                    default:
                        return null;
                }
            }
        } );
    }

    @SuppressWarnings("unchecked")
    private <T> T fake( Class<T> type, InvocationHandler handler ) {
        if (handler == null) {
            handler = new InvocationHandler() {
                public Object invoke( Object proxy, Method method, Object[] args ) {
                    return null;
                }
            };
        }
        return (T) Proxy.newProxyInstance( getClass().getClassLoader(), new Class<?>[] { type }, handler );
    }

    private void command( String command ) throws Exception {
        plugin.process( command );
        SwingUtilities.invokeAndWait( new Runnable() {
            public void run() {
                // wait for the window change queued on the swing thread
            }
        } );
    }

    @Test
    public void hideAndShowTheWindow() throws Exception {
        plugin.loadPlugin();
        assertTrue( windowCreated );
        assertTrue( windowVisible );

        command( "hide" );
        assertFalse( windowVisible );

        command( "show" );
        assertTrue( windowVisible );
    }

    @Test
    public void toggleFlipsTheWindow() throws Exception {
        plugin.loadPlugin();

        command( "toggle" );
        assertFalse( windowVisible );

        command( "TOGGLE" );
        assertTrue( windowVisible );
    }

    @Test
    public void helpListsTheWindowCommands() {
        plugin.process( null );

        String help = console.toString();
        assertTrue( help, help.contains( "hide/show" ) );
        assertTrue( help, help.contains( "toggle" ) );
    }

    @Test
    public void commandBeforeWindowIsOpenReportsError() throws Exception {
        command( "show" );

        assertTrue( console.toString(), console.toString().contains( "mapper window isn't open" ) );
    }

}
