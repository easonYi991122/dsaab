import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class SeamCarverGUI extends JFrame {
    private MyImageLabel imageLabel;
    private BufferedImage currentImage;
    public String imagePath;
    public String saveProtectedPath;
    public String protectedMaskPath;
    public String revisedProtectedMaskPath;
    public boolean isSelected;
    private Rectangle selectedArea;

    class MyImageLabel extends JLabel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (selectedArea != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(2));
                g2d.draw(selectedArea);
                g2d.dispose();
            }
        }
    }

    public SeamCarverGUI() {
        super("Seam Carver GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton uploadButton = new JButton("Upload Image");
        JButton carveButton = new JButton("Carve Image");
        JButton selectButton = new JButton("Select Region");

        imageLabel = new MyImageLabel();
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
            saveProtectedPath = file.getAbsolutePath();
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
                    SeamCarver sc = new SeamCarver(imagePath, 600, 800, protectedMaskPath, "");
                    return null;
                }
            };
            worker.execute();
        }
    }


    private void selectRegion(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Directory to Save Mask");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File directory = fileChooser.getSelectedFile();
            String filename = "mask.jpg";
            protectedMaskPath = new File(directory, filename).getAbsolutePath();

            MouseAdapter mouseAdapter = new MouseAdapter() {
                Point start;

                @Override
                public void mousePressed(MouseEvent e) {
                    start = e.getPoint();
                    selectedArea = new Rectangle();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    updateSelectionArea(e);
                    imageLabel.repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    updateSelectionArea(e);
                    generateMaskAndSave(saveProtectedPath);
                    imageLabel.removeMouseListener(this);
                    imageLabel.removeMouseMotionListener(this);
                    selectedArea = null; // Clear the rectangle after saving
                    imageLabel.repaint();
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
        if (currentImage != null) {
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
                File outputFile = new File(protectedMaskPath);
                ImageIO.write(maskImage, "jpg", outputFile);
                System.out.println("Mask image saved as " + protectedMaskPath);
            } catch (IOException ex) {
                System.err.println("Error saving the mask image: " + ex.getMessage());
            }

            // revisedMaskPath = "file:///" + maskPath.replace("\\", "/");
        }
    }
}
