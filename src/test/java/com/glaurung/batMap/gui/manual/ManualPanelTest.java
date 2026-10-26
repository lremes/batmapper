package com.glaurung.batMap.gui.manual;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JTextArea;

import org.junit.BeforeClass;
import org.junit.Test;

public class ManualPanelTest {

    @BeforeClass
    public static void headless() {
        System.setProperty( "java.awt.headless", "true" );
    }

    private JTextArea findTextArea( Container container ) {
        for (Component component : container.getComponents()) {
            if (component instanceof JTextArea) {
                return (JTextArea) component;
            }
            if (component instanceof Container) {
                JTextArea found = findTextArea( (Container) component );
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private String manualResource() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream( "/manual.txt" )) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            int b;
            while (( b = stream.read() ) != - 1) {
                bytes.write( b );
            }
            return bytes.toString( "UTF-8" );
        }
    }

    @Test
    public void showsWholeManual() throws IOException {
        String manual = manualResource();
        assertTrue( manual.length() > 1000 );

        assertEquals( manual, findTextArea( new ManualPanel() ).getText() );
    }

}
