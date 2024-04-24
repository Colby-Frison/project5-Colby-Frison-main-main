import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;

public class Driver {
    private static TripPoint[] tripPoints;
    private static TripPoint[] animatedTrip;
    private static JFrame frame;
    private static JMapViewer mapViewer;
    private static JComboBox<Integer> animationTimeComboBox;
    private static JCheckBox stopsCheckBox;
    private static JButton playButton;
    private static JLabel titleLabel;

    public static void main(String[] args) throws FileNotFoundException, IOException {
        // Read file and call stop detection
        TripPoint.readFile("triplog.csv");
        // Perform stop detection
        TripPoint.h2StopDetectionSimplified();

        // Set up frame, include your name in the title
        frame = new JFrame("Map Animation by [Your Name]");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Set up Panel for input selections
        JPanel topPanel = new JPanel();

        // Play Button
        playButton = new JButton("Play");
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                animateTrip();
            }
        });

        // CheckBox to enable/disable stops
        stopsCheckBox = new JCheckBox("Include Stops");

        // ComboBox to pick animation time
        Integer[] animationTimes = {15, 30, 60, 90};
        animationTimeComboBox = new JComboBox<>(animationTimes);

        // Title Label
        titleLabel = new JLabel("Map Animation");
        titleLabel.setPreferredSize(new Dimension(150, 25));

        // Add all to top panel
        topPanel.add(titleLabel);
        topPanel.add(playButton);
        topPanel.add(stopsCheckBox);
        topPanel.add(new JLabel("Animation Time (s):"));
        topPanel.add(animationTimeComboBox);

        // Set up mapViewer
        mapViewer = new JMapViewer();

        // Add components to frame
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(mapViewer, BorderLayout.CENTER);

        // Set the map center and zoom level (Assuming center coordinates and zoom level)
        //mapViewer.setMapCenter(new Coordinate(/** Center Latitude **/, /** Center Longitude **/));
        mapViewer.setZoom(10);

        // Set frame size and visibility
        frame.setSize(800, 600);
        frame.setVisible(true);
    }

    // Animate the trip based on selections from the GUI components
    private static void animateTrip() {
        // Retrieve selected options
        int animationTime = (int) animationTimeComboBox.getSelectedItem();
        boolean includeStops = stopsCheckBox.isSelected();

        // Clear previous animation
        mapViewer.removeAll();

        // Get animated trip points based on selected options
        animatedTrip = includeStops ? TripPoint.getTrip().toArray(new TripPoint[0]) : TripPoint.getMovingTrip().toArray(new TripPoint[0]);

        // Calculate interval between each animation step
        int interval = animationTime * 1000 / animatedTrip.length;


        // Start animation
        new Thread(new Runnable() {
            MapMarker prevMarker = null;
            
            @Override
            public void run() {
                for (int i = 0; i < animatedTrip.length; i++) {
                    TripPoint currentPoint = animatedTrip[i];
                    Coordinate coordinate = new Coordinate(currentPoint.getLat(), currentPoint.getLon());
                    IconMarker curr = new IconMarker(coordinate, new ImageIcon("raccoon.png").getImage());
                    mapViewer.addMapMarker(curr);

                    MapMarker currMarker = new MapMarkerDot(coordinate);

                    // Connect the current point with the previous point
                    if (prevMarker != null) {
                        drawLine(prevMarker.getCoordinate(), currMarker.getCoordinate());
                    }
                    prevMarker = currMarker;

                    SwingUtilities.invokeLater(() -> mapViewer.repaint());

                    // Wait for next animation step
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mapViewer.addMapMarker(new MapMarkerDot(coordinate));
                    mapViewer.removeMapMarker(curr);
                }
            }
        }).start();
    }

    // Method to draw a line between two coordinates
    private static void drawLine(Coordinate start, Coordinate end) {
        MapPolygonImpl line = new MapPolygonImpl(start, end);
        mapViewer.addMapPolygon(line);
    }
}