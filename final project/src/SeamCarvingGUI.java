import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class SeamCarvingGUI extends JFrame {
    private BufferedImage image;
    private JLabel imageLabel;
    private JPanel buttonPanel;
    private JButton uploadButton, carvingButton, selectButton;
    private JFileChooser fileChooser;
    private Rectangle selectedArea;

    public SeamCarvingGUI() {
        super("Seam Carving GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        JScrollPane scrollPane = new JScrollPane(imageLabel);
        add(scrollPane, BorderLayout.CENTER);

        buttonPanel = new JPanel(new FlowLayout());
        uploadButton = new JButton("Upload Image");
        carvingButton = new JButton("Start Carving");
        selectButton = new JButton("Select Region");
        buttonPanel.add(uploadButton);
        buttonPanel.add(carvingButton);
        buttonPanel.add(selectButton);
        add(buttonPanel, BorderLayout.SOUTH);

        fileChooser = new JFileChooser();

        uploadButton.addActionListener(e -> uploadImage());
        carvingButton.addActionListener(e -> showZoomOptions());
        selectButton.addActionListener(e -> enableSelection());

        setVisible(true);
    }

    private void uploadImage() {
        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                image = ImageIO.read(file);
                ImageIcon icon = new ImageIcon(image);
                imageLabel.setIcon(icon);
                this.pack(); // Fit JFrame size to the image
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error loading image: " + ex.getMessage());
            }
        }
    }

    private void showZoomOptions() {
        JButton zoomInButton = new JButton("Zoom In");
        JButton zoomOutButton = new JButton("Zoom Out");
        buttonPanel.add(zoomInButton);
        buttonPanel.add(zoomOutButton);
        zoomInButton.addActionListener(e -> startCarving(true));
        zoomOutButton.addActionListener(e -> startCarving(false));
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    private void startCarving(boolean zoomIn) {
        System.out.println("Carving started with zoom " + (zoomIn ? "In" : "Out"));
        // Placeholder for seam carving algorithm integration
    }

    private void enableSelection() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            Point start;

            @Override
            public void mousePressed(MouseEvent e) {
                start = e.getPoint();
                selectedArea = new Rectangle();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int x = Math.min(start.x, e.getX());
                int y = Math.min(start.y, e.getY());
                int width = Math.abs(e.getX() - start.x);
                int height = Math.abs(e.getY() - start.y);
                selectedArea.setBounds(x, y, width, height);
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // The derived value for x, y, width and height can be passed for the following processing.
                System.out.println("Region selected from (" + selectedArea.x + ", " + selectedArea.y +
                        ") to (" + (selectedArea.x + selectedArea.width) + ", " +
                        (selectedArea.y + selectedArea.height) + ")");
            }
        };
        imageLabel.addMouseListener(mouseAdapter);
        imageLabel.addMouseMotionListener(mouseAdapter);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (selectedArea != null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.BLACK);
            g2.draw(selectedArea);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SeamCarvingGUI::new);
    }
}
