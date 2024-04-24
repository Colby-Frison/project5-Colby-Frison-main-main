import javax.swing.*;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class Driver {

    // Declare class data
    private static final String FILENAME = "triplog.csv";
    private static final String WINDOW_TITLE = "Your Name - Trip Animation";
    private static final int DEFAULT_ANIMATION_SPEED = 1;

    private static JMapViewer mapViewer;
    private static boolean enableStops = false;
    private static int animationSpeed = DEFAULT_ANIMATION_SPEED;
    private static ArrayList<TripPoint> tripPoints;

    public static void main(String[] args) {
        // Read file and call stop detection
        try {
            TripPoint.readFile(FILENAME);
            tripPoints = TripPoint.getTrip();
            int stops = TripPoint.h1StopDetection(); // You can choose which stop detection method to use
            System.out.println("Detected stops: " + stops);
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }

        // Set up frame, include your name in the title
        JFrame frame = new JFrame(WINDOW_TITLE);

        // Set up Panel for input selections
        JPanel topPanel = new JPanel();

        // Play Button
        JButton playButton = new JButton("Play");

        // CheckBox to enable/disable stops
        JCheckBox stopCheckBox = new JCheckBox("Enable Stops");

        // ComboBox to pick animation time
        JComboBox<String> timeComboBox = new JComboBox<>(new String[]{"1x", "2x", "3x"});
        timeComboBox.setSelectedIndex(DEFAULT_ANIMATION_SPEED - 1);

        // Add all to top panel
        topPanel.add(playButton);
        topPanel.add(stopCheckBox);
        topPanel.add(timeComboBox);

        // Set up mapViewer
        mapViewer = new JMapViewer();

        // Add listeners for GUI components
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                animateTrip();
            }
        });
        stopCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableStops = stopCheckBox.isSelected();
                handleStopCheckbox(enableStops);
            }
        });
        timeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedSpeed = (String) timeComboBox.getSelectedItem();
                animationSpeed = Integer.parseInt(selectedSpeed.substring(0, 1));
            }
        });

        // Set the map center and zoom level
        mapViewer.setDisplayPosition(new Coordinate(0, 0), 2);

        // Add components to the frame
        frame.getContentPane().add(BorderLayout.NORTH, topPanel);
        frame.getContentPane().add(BorderLayout.CENTER, mapViewer);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setVisible(true);
    }

    // Animate the trip based on selections from the GUI components
    // Animate the trip based on selections from the GUI components
    private static void animateTrip() {
        if (tripPoints == null || tripPoints.isEmpty()) {
            System.out.println("No trip points available.");
            return;
        }
    
        // Clear existing markers and lines
        mapViewer.removeAllMapMarkers();
        mapViewer.removeAllMapPolygons();
    
        // Create list of coordinates for the trip path
        ArrayList<Coordinate> pathCoordinates = new ArrayList<>();
        for (TripPoint point : tripPoints) {
            pathCoordinates.add(new Coordinate(point.getLat(), point.getLon()));
        }
    
        // Create a line representing the trip path
        MapPolygonImpl pathLine = new MapPolygonImpl(new ArrayList<>());
        pathLine.setColor(Color.BLUE);
        pathLine.setBackColor(null); // Set background color (transparent)
    
        // Add the line to the map
        mapViewer.addMapPolygon(pathLine);
    
        // Timer for animating the trip
        Timer timer = new Timer(100, new ActionListener() {
            int index = 0;
    
            @Override
            public void actionPerformed(ActionEvent e) {
                if (index < pathCoordinates.size()) {
                    pathLine.getPoints().add(pathCoordinates.get(index));
                    mapViewer.repaint();
                    index++;
                } else {
                    // Stop the timer when all points have been added
                    ((Timer) e.getSource()).stop();
                }
            }
        });
    
        // Start the timer
        timer.start();
    }

    private static void handleStopCheckbox(boolean enableStops) {
        if (enableStops) {
            try {
                int stops = TripPoint.h1StopDetection(); // Detect stops
                System.out.println("Detected stops: " + stops);
                // Show stops on the map
                ArrayList<TripPoint> stoppedPoints = TripPoint.getMovingTrip();
                for (TripPoint point : stoppedPoints) {
                    Coordinate coordinate = new Coordinate(point.getLat(), point.getLon());
                    mapViewer.addMapMarker(new MapMarkerDot(coordinate));
                }
            } catch (FileNotFoundException e) {
                System.err.println("File not found: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
            }
        } else {
            // Clear stops from the map
            mapViewer.removeAllMapMarkers();
            // If you want to show all trip points again, uncomment the following line
            // animateTrip();
        }
    }
}