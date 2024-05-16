import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.awt.event.*;
import java.io.IOException;

public class SeamCarverGUI extends JFrame {
    private JLabel imageLabel;
    private BufferedImage currentImage;
    public String imagePath;
    public String savePath;
    private String maskPath;
    public String revisedMaskPath;
    public boolean isSelected;
    private String protectMask = "";
    private Rectangle selectedArea;

    public SeamCarverGUI() {
        super("Seam Carver GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        JPanel buttonPanel = new JPanel(new FlowLayout());

        JButton uploadButton = new JButton("Upload Image");
        JButton carveButton = new JButton("Carve Image");
        JButton selectButton = new JButton("Select Region");

        imageLabel = new JLabel();
        JScrollPane scrollPane = new JScrollPane(imageLabel);

        uploadButton.addActionListener(this::uploadImage);
        carveButton.addActionListener(this::carveImage);
        selectButton.addActionListener(this::selectRegion);

        buttonPanel.add(uploadButton);
        buttonPanel.add(carveButton);
        buttonPanel.add(selectButton);

        add(buttonPanel, BorderLayout.SOUTH);
        add(scrollPane, BorderLayout.CENTER);

        setVisible(true);
    }

    private void uploadImage(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            isSelected = true;
            File file = fileChooser.getSelectedFile();
            savePath = file.getAbsolutePath();
            imagePath = "file:///" + file.getAbsolutePath().replace("\\", "/");
            try {
                currentImage = ImageIO.read(file);

                int width = this.getWidth();
                int hight = this.getHeight();

                Image scaledimage = currentImage.getScaledInstance(width,hight,Image.SCALE_SMOOTH);

                imageLabel.setIcon(new ImageIcon(scaledimage));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void carveImage(ActionEvent e) {
        if (imagePath != null) {
            SwingWorker<Void, BufferedImage> worker = new SwingWorker<Void, BufferedImage>() {
                @Override
                protected Void doInBackground() throws Exception {
                    SeamCarver sc = new SeamCarver(imagePath, 600, 800, protectMask, "");
                    return null;
                }
            };
            worker.execute();
        }
    }

    private void selectRegion(ActionEvent e) {
        // Ask user to select a directory for saving the mask
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Directory to Save Mask");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File directory = fileChooser.getSelectedFile();

            // Prompt user for the filename
            String filename = "mask.jpg";
                maskPath = new File(directory, filename).getAbsolutePath();

                MouseAdapter mouseAdapter = new MouseAdapter() {
                    Point start; // Starting point of the drag

                    @Override
                    public void mousePressed(MouseEvent e) {
                        start = e.getPoint(); // Record the starting point when the mouse is pressed
                        selectedArea = new Rectangle();
                    }

                    @Override
                    public void mouseDragged(MouseEvent e) {
                        updateSelectionArea(e);
                        imageLabel.repaint(); // Refresh the image to show the selection rectangle
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        updateSelectionArea(e);
                        generateMaskAndSave(savePath);  // Save the mask to the new path
                        imageLabel.removeMouseListener(this); // Remove listener after selection
                        imageLabel.removeMouseMotionListener(this);
                    }

                    private void updateSelectionArea(MouseEvent e) {
                        int x = Math.min(start.x, e.getX());
                        int y = Math.min(start.y, e.getY());
                        int width = Math.abs(e.getX() - start.x);
                        int height = Math.abs(e.getY() - start.y);
                        selectedArea.setBounds(x, y, width, height);
                    }
                };

                imageLabel.addMouseListener(mouseAdapter);
                imageLabel.addMouseMotionListener(mouseAdapter);
            }
        else {
            System.out.println("Directory selection was cancelled.");
        }
    }

    private void generateMaskAndSave(String filename) {
        int imgWidth = currentImage.getWidth();
        int imgHeight = currentImage.getHeight();

        BufferedImage maskImage = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < imgWidth; x++) {
            for (int y = 0; y < imgHeight; y++) {
                if (selectedArea.contains(x, y)) {
                    maskImage.setRGB(x, y, Color.WHITE.getRGB());
                } else {
                    maskImage.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }

        try {
            File outputFile = new File(maskPath);
            ImageIO.write(maskImage, "jpg", outputFile);
        } catch (IOException ex) {
            System.err.println("Error saving the mask image: " + ex.getMessage());
        }

        // Used for passing to SeamCarver
        revisedMaskPath = "file:///" + maskPath.replace("\\", "/");
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
}
