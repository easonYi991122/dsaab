import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class SeamCarverGUI extends JFrame {
    private MyImageLabel imageLabel;
    private BufferedImage currentImage;
    private double scaleX, scaleY;
    public String imagePath;
    public String protectedMaskPath;
    public boolean isSelected;
    private String protectMask = "";
    private Rectangle selectedArea;
    private boolean[][] brushSelection;

    class MyImageLabel extends JLabel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (selectedArea != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(2));
                g2d.draw(selectedArea);
                g2d.dispose();
            }
            if (brushSelection != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(new Color(255, 0, 0, 128));
                for (int x = 0; x < brushSelection.length; x++) {
                    for (int y = 0; y < brushSelection[x].length; y++) {
                        if (brushSelection[x][y]) {
                            g2d.fillRect((int) (x / scaleX), (int) (y / scaleY), 1, 1);
                        }
                    }
                }
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
            imagePath = "file:///" + file.getAbsolutePath().replace("\\", "/");
            try {
                currentImage = ImageIO.read(file);

                int width = imageLabel.getWidth();
                int height = imageLabel.getHeight();

                // Calculate scaling factors
                scaleX = (double) currentImage.getWidth() / width;
                scaleY = (double) currentImage.getHeight() / height;

                Image scaledImage = currentImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaledImage));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void carveImage(ActionEvent e) {
        if (imagePath != null) {
            long startTime = System.nanoTime();
            SwingWorker<Void, BufferedImage> worker = new SwingWorker<Void, BufferedImage>() {
                @Override
                protected Void doInBackground() throws Exception {
                    String maskPath = protectedMaskPath;
                    if(protectedMaskPath==null)    maskPath = "";
                    SeamCarver sc = new SeamCarver(imagePath, 600, 800, maskPath, "");
                    return null;
                }
            };
            worker.execute();
            long endTime = System.nanoTime();
            TimeMeasure(startTime, endTime);
        }
    }

    private static void TimeMeasure(long startTime, long endTime){

        // 计算耗时（单位：纳秒）
        long duration = endTime - startTime;

        // 将纳秒转换为毫秒
        double durationInMillis = duration / 1_000_000.0;

        // 输出耗时
        System.out.println("Function execution time: " + duration + " nanoseconds");
        System.out.println("Function execution time: " + durationInMillis + " milliseconds");
    }

    private void selectRegion(ActionEvent e) {
        // Show dialog to choose selection method
        JDialog selectionDialog = new JDialog(this, "Select Method", true);
        selectionDialog.setLayout(new FlowLayout());
        JButton rectButton = new JButton("Rectangular Selection");
        JButton brushButton = new JButton("Brush Selection");

        rectButton.addActionListener(ev -> {
            selectionDialog.dispose();
            initiateRectangularSelection();
        });

        brushButton.addActionListener(ev -> {
            selectionDialog.dispose();
            initiateBrushSelection();
        });

        selectionDialog.add(rectButton);
        selectionDialog.add(brushButton);
        selectionDialog.pack();
        selectionDialog.setLocationRelativeTo(this);
        selectionDialog.setVisible(true);
    }

    private void initiateRectangularSelection() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Directory to Save Mask");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File directory = fileChooser.getSelectedFile();
            protectedMaskPath = new File(directory, "mask.jpg").getAbsolutePath();
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
                    generateMaskAndSave(protectedMaskPath);
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

    private void initiateBrushSelection() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Directory to Save Mask");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File directory = fileChooser.getSelectedFile();
            protectedMaskPath = new File(directory, "maskBrush.jpg").getAbsolutePath();
            brushSelection = new boolean[currentImage.getWidth()][currentImage.getHeight()];

            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    updateBrushSelection(e.getPoint());
                    imageLabel.repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    generateBrushMaskAndSave(protectedMaskPath);
                    imageLabel.removeMouseListener(this);
                    imageLabel.removeMouseMotionListener(this);
                    brushSelection = null; // Clear the selection after saving
                    imageLabel.repaint();
                }

                private void updateBrushSelection(Point point) {
                    int brushSize = 10;
                    int xStart = Math.max(0, (int) (point.x * scaleX) - brushSize / 2);
                    int xEnd = Math.min(currentImage.getWidth(), (int) (point.x * scaleX) + brushSize / 2);
                    int yStart = Math.max(0, (int) (point.y * scaleY) - brushSize / 2);
                    int yEnd = Math.min(currentImage.getHeight(), (int) (point.y * scaleY) + brushSize / 2);

                    for (int x = xStart; x < xEnd; x++) {
                        for (int y = yStart; y < yEnd; y++) {
                            brushSelection[x][y] = true;
                        }
                    }
                }
            };

            imageLabel.addMouseListener(mouseAdapter);
            imageLabel.addMouseMotionListener(mouseAdapter);}
        else {
            System.out.println("Directory selection was cancelled.");
        }
    }

    private void generateBrushMaskAndSave(String filename) {
        if (currentImage != null) {
            int imgWidth = currentImage.getWidth();
            int imgHeight = currentImage.getHeight();
            double[][] mask = new double[imgWidth][imgHeight];
            BufferedImage maskImage = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);

            for (int x = 0; x < imgWidth; x++) {
                for (int y = 0; y < imgHeight; y++) {
                    if (brushSelection[x][y]) {
                        mask[x][y] = 1.0;
                        maskImage.setRGB(x, y, Color.WHITE.getRGB());
                    } else {
                        mask[x][y] = 0.0;
                        maskImage.setRGB(x, y, Color.BLACK.getRGB());
                    }
                }
            }

            try {
                File outputFile = new File(protectedMaskPath);
                ImageIO.write(maskImage, "jpg", outputFile);
                System.out.println("Mask image saved as " + filename);
            } catch (IOException ex) {
                System.err.println("Error saving the mask image: " + ex.getMessage());
            }
        }
    }

    private void generateMaskAndSave(String filename) {
        if (currentImage != null) {
            int imgWidth = currentImage.getWidth();
            int imgHeight = currentImage.getHeight();
            double[][] mask = new double[imgWidth][imgHeight];
            BufferedImage maskImage = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);

            int boxX = (int) (selectedArea.x * scaleX);
            int boxY = (int) (selectedArea.y * scaleY);
            int boxWidth = (int) (selectedArea.width * scaleX);
            int boxHeight = (int) (selectedArea.height * scaleY);

            for (int x = 0; x < imgWidth; x++) {
                for (int y = 0; y < imgHeight; y++) {
                    if (x >= boxX && x < boxX + boxWidth && y >= boxY && y < boxY + boxHeight) {
                        mask[x][y] = 1.0;
                        maskImage.setRGB(x, y, Color.WHITE.getRGB());
                    } else {
                        mask[x][y] = 0.0;
                        maskImage.setRGB(x, y, Color.BLACK.getRGB());
                    }
                }
            }

            try {
                File outputFile = new File(protectedMaskPath);
                ImageIO.write(maskImage, "jpg", outputFile);
                System.out.println("Mask image saved as " + filename);
            } catch (IOException ex) {
                System.err.println("Error saving the mask image: " + ex.getMessage());
            }
        }
    }
}