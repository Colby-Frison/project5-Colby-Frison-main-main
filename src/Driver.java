import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.JMapViewerTree;
import org.openstreetmap.gui.jmapviewer.MapMarkerCircle;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.MapRectangleImpl;
import org.openstreetmap.gui.jmapviewer.OsmTileLoader;
import org.openstreetmap.gui.jmapviewer.events.JMVCommandEvent;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.JMapViewerEventListener;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.openstreetmap.gui.jmapviewer.interfaces.MapPolygon;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.BingAerialTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;

public class Driver extends JFrame implements JMapViewerEventListener {
    
    
    //JMapViewer variable
	
	private static final long serialVersionUID = 1L;

    private final JMapViewerTree treeMap;

    private final JLabel zoomLabel;
    private final JLabel zoomValue;

    private final JLabel mperpLabelName;
    private final JLabel mperpLabelValue;

    //Driver variables

    private static JMapViewer mapViewer;
    private static JComboBox<Integer> animationTimeComboBox;
    private static JCheckBox stopsCheckBox;
    private static JButton playButton;
    private static boolean isPlaying = false;
    private static Image raccoonImage;

    private static boolean includeStops;
    private static int animationTime;
    private static int currentStep;
    private static int totalSteps;

    private static TripPoint tripData;
    private static JComboBox<String> timeComboBox;
    private static ArrayList<TripPoint> trip;
    private static ArrayList<TripPoint> movingTrip;

    private Thread animationThread;

    private List<? extends ICoordinate> points;

    private static TripPoint[] animatedTrip;

    public Driver() throws FileNotFoundException, IOException {
        
        super("Trip Animator By: Colby Frison");
        
        // Read file and call stop detection
        TripPoint.readFile("triplog.csv");
        TripPoint.h2StopDetectionSimplified();

        raccoonImage = Toolkit.getDefaultToolkit().getImage("raccoon.png");
        

        setSize(400, 400);

        treeMap = new JMapViewerTree("Zones");

        // Listen to the map viewer for user operations so components will
        // receive events and update
        map().addJMVListener(this);

        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        JPanel panel = new JPanel(new BorderLayout());
        JPanel panelTop = new JPanel();
        JPanel panelBottom = new JPanel();
        JPanel helpPanel = new JPanel();

        // add controls for Driver
        Integer[] animationTimes = { 15, 30, 60, 90 };
        animationTimeComboBox = new JComboBox<>(animationTimes);
        panelTop.add(animationTimeComboBox);
        
        animationTimeComboBox.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                animationTime = (int) animationTimeComboBox.getSelectedItem();
            }
        });

        

        // CheckBox to enable/disable stops
        stopsCheckBox = new JCheckBox("Include Stops", false);
        panelTop.add(stopsCheckBox);

        stopsCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                includeStops = stopsCheckBox.isSelected();
            }
        });

        // Play Button
        playButton = new JButton("Play/Reset");
        panelTop.add(playButton);
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isPlaying) {
                    // Stop the animation
                    isPlaying = false;

                    playButton.setText("Play");
                    animationTimeComboBox.setEnabled(true);
                    stopsCheckBox.setEnabled(true);
            
                    // Interrupt the animation thread if it's running
                    if (animationThread != null && animationThread.isAlive()) {
                        animationThread.interrupt();
                        map().removeAllMapMarkers();
                    }
                    map().removeAllMapMarkers();
                } else {
                    setMapCenterAndZoom();
                    // Start the animation
                    isPlaying = true;
                    playButton.setText("Reset");
            
                    animationTimeComboBox.setEnabled(false);
                    stopsCheckBox.setEnabled(false);
            
                    // Start a new animation thread
                    animationThread = new Thread(() -> animateTrip());
                    animationThread.start();
                }
            }
        });


        mperpLabelName = new JLabel("Meters/Pixels: ");
        mperpLabelValue = new JLabel(String.format("%s", map().getMeterPerPixel()));

        zoomLabel = new JLabel("Zoom: ");
        zoomValue = new JLabel(String.format("%s", map().getZoom()));

        add(panel, BorderLayout.NORTH);
        add(helpPanel, BorderLayout.SOUTH);
        panel.add(panelTop, BorderLayout.NORTH);
        panel.add(panelBottom, BorderLayout.SOUTH);
        JLabel helpLabel = new JLabel("Use right mouse button to move,\n "
                + "left double click or mouse wheel to zoom.");
        helpPanel.add(helpLabel);
        JButton button = new JButton("setDisplayToFitMapMarkers");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                map().setDisplayToFitMapMarkers();
            }
        });
        JComboBox<TileSource> tileSourceSelector = new JComboBox<>(new TileSource[] {

            /// these are all the version of maps the JMapViewer has avaialbe
            /// the ones commented out either dont work for an IDE reason or a loading error in the panel

            //new OsmTileSource.Mapnik(),
            //new OsmTileSource.CycleMap(),
            new OsmTileSource.TransportMap(),
            //new OsmTileSource.LandscapeMap(),
            //new OsmTileSource.OutdoorsMap(),
            new BingAerialTileSource()
        });

        tileSourceSelector.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                map().setTileSource((TileSource) e.getItem());
            }
        });
        JComboBox<TileLoader> tileLoaderSelector;
        tileLoaderSelector = new JComboBox<>(new TileLoader[] {new OsmTileLoader(map())});
        tileLoaderSelector.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                map().setTileLoader((TileLoader) e.getItem());
            }
        });
        map().setTileLoader((TileLoader) tileLoaderSelector.getSelectedItem());
        panelTop.add(tileSourceSelector);

        add(treeMap, BorderLayout.CENTER);


        map().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    map().getAttribution().handleAttribution(e.getPoint(), true);
                }
            }
        });

        map().addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                boolean cursorHand = map().getAttribution().handleAttributionCursor(p);
                if (cursorHand) {
                    map().setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    map().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
                //if (showToolTip.isSelected()) map().setToolTipText(map().getPosition(p).toString());
            }
        });

        trip = tripData.getTrip();

    }

    private JMapViewer map() {
        return treeMap.getViewer();
    }

    private static Coordinate c(double lat, double lon) {
        return new Coordinate(lat, lon);
    }

    /**
     * @param args Main program arguments
     * @throws IOException 
     * @throws FileNotFoundException 
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        new Driver().setVisible(true);

        
    }

    private void updateZoomParameters() {
        if (mperpLabelValue != null)
            mperpLabelValue.setText(String.format("%s", map().getMeterPerPixel()));
        if (zoomValue != null)
            zoomValue.setText(String.format("%s", map().getZoom()));
    }

    @Override
    public void processCommand(JMVCommandEvent command) {
        if (command.getCommand().equals(JMVCommandEvent.COMMAND.ZOOM) ||
                command.getCommand().equals(JMVCommandEvent.COMMAND.MOVE)) {
            updateZoomParameters();
        }
    }

    private void animateTrip() {
        // Retrieve selected options
        int animationTime = (int) animationTimeComboBox.getSelectedItem();
        boolean includeStops = stopsCheckBox.isSelected();

        // Clear previous animation
        map().removeAllMapMarkers();

        // Get animated trip points based on selected options
        animatedTrip = includeStops ? TripPoint.getTrip().toArray(new TripPoint[0]) : TripPoint.getMovingTrip().toArray(new TripPoint[0]);

        // Calculate interval between each animation step
        int interval = animationTime * 1000 / animatedTrip.length;


        // Start animation
        new Thread(new Runnable() {
            Coordinate previousCoordinate = null;

            IconMarker curr;

            @Override
            public void run() {
                Graphics g = map().getGraphics();

                for (int i = 0; i < animatedTrip.length; i++) {
                    if(isPlaying){
                        TripPoint currentPoint = animatedTrip[i];
                        Coordinate coordinate = new Coordinate(currentPoint.getLat(), currentPoint.getLon());
                        curr = new IconMarker(coordinate, new ImageIcon("raccoon.png").getImage());
                        map().addMapMarker(curr);

                        //points.add(new ICoordinate(coordinate.getLat(), coordinate.getLon()));

                        map().addMapPolygon(new MapPolygonImpl(points));
                        SwingUtilities.invokeLater(() -> map().repaint());

                        // Wait for next animation step
                        try {
                            Thread.sleep(interval);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        map().addMapMarker(new MapMarkerDot(coordinate));
                        map().removeMapMarker(curr);

                        previousCoordinate = coordinate;
                    }
                    else {
                        map().removeAllMapMarkers();
                    }
                }
                playButton.setText("Play");
                isPlaying = false;

                animationTimeComboBox.setEnabled(true);
                stopsCheckBox.setEnabled(true);
            }
        }).start();
    }

    // Method to draw a line between two coordinates
    private void drawLine(Graphics g, Coordinate coord1, Coordinate coord2) {
        g.setColor(Color.RED);
        Point pt1 = map().getMapPosition(coord1.getLat(), coord1.getLon(), false);
        Point pt2 = map().getMapPosition(coord2.getLat(), coord2.getLon(), false);
        g.drawLine(pt1.x, pt1.y, pt2.x, pt2.y);
    }

    // Set the map center and zoom level
    private void setMapCenterAndZoom() {
        // Calculate bounding box of the trip
        double minLat = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = Double.MIN_VALUE;
        for (TripPoint point : trip) {
            double lat = point.getLat();
            double lon = point.getLon();
            if (lat < minLat) minLat = lat;
            if (lat > maxLat) maxLat = lat;
            if (lon < minLon) minLon = lon;
            if (lon > maxLon) maxLon = lon;
        }

        // Calculate map center and zoom level
        double centerLat = (minLat + maxLat) / 2;
        double centerLon = (minLon + maxLon) / 2;
        int zoom = (int) Math.max(maxLat - minLat, maxLon - minLon) * 100;

        // Set map center and zoom level
        map().setDisplayPosition(new Coordinate(centerLat, centerLon), zoom);
    }
    
}