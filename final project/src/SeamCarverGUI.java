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
    private JLabel imageSizeLabel;
    private BufferedImage currentImage;
    private double maxScale;
    public String imagePath;
    public String savedImagePath; //Used in SeamCarver
    public String protectedMaskPath;
    public String removalMaskPath;
    private Rectangle selectedArea;
    private boolean[][] brushSelection;
    private boolean isProtect = false;
    private boolean isRemoval = false;

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
                g2d.setColor(new Color(255, 65, 65, 128));
                for (int x = 0; x < brushSelection.length; x++) {
                    for (int y = 0; y < brushSelection[x].length; y++) {
                        if (brushSelection[x][y]) {
                            g2d.fillRect((int) (x / maxScale), (int) (y / maxScale), 1, 1);
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
        JButton protectButton = new JButton("Protect Region");
        JButton removalButton = new JButton("Remove Region");

        imageLabel = new MyImageLabel();
        imageSizeLabel = new JLabel();      // Label for displaying image dimensions

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        rightPanel.add(imageSizeLabel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(imageLabel);
        JScrollPane originalScrollPane = new JScrollPane(rightPanel); // Scroll pane for the original-sized image
        originalScrollPane.setPreferredSize(new Dimension(800, 30));
        uploadButton.addActionListener(this::uploadImage);
        carveButton.addActionListener(this::carveImage);
        protectButton.addActionListener(this::protectRegion);
        removalButton.addActionListener(this::removeRegion);

        buttonPanel.add(uploadButton);
        buttonPanel.add(carveButton);
        buttonPanel.add(protectButton);
        buttonPanel.add(removalButton);

        add(buttonPanel, BorderLayout.SOUTH);
        add(scrollPane, BorderLayout.CENTER);
        add(originalScrollPane, BorderLayout.NORTH);

        setVisible(true);
    }

    private void uploadImage(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            imagePath = "file:///" + file.getAbsolutePath().replace("\\", "/");
            try {
                currentImage = ImageIO.read(file);

                //Read the original size of the uploaded image
                int width = currentImage.getWidth();
                int height = currentImage.getHeight();

                // Calculate scaling factors
                double scaleX = (double) width / imageLabel.getWidth();
                double scaleY = (double) height / imageLabel.getHeight();
                maxScale = Math.max(scaleX, scaleY);

                //Define the exhibited image size
                int imageX = (int) (currentImage.getWidth()/maxScale);
                int imageY = (int) (currentImage.getHeight()/maxScale);

                Image scaledImage = currentImage.getScaledInstance(imageX, imageY, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaledImage));

                // Update image size label
                imageSizeLabel.setText("  Image Size: " + width + " x " + height);
                imageSizeLabel.revalidate();
                imageSizeLabel.repaint();

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void carveImage(ActionEvent e) {
        if (imagePath != null) {
            // Show a dialog to input the desired size for the carved image
            JTextField widthField = new JTextField(5);
            JTextField heightField = new JTextField(5);
            JPanel sizePanel = new JPanel();
            sizePanel.add(new JLabel("Width:"));
            sizePanel.add(widthField);
            sizePanel.add(Box.createHorizontalStrut(15)); // a spacer
            sizePanel.add(new JLabel("Height:"));
            sizePanel.add(heightField);
            int originalWidth = currentImage.getWidth();
            int originalHeight = currentImage.getHeight();

            int result;
            if(!isRemoval) { //If we are not going to remove the image:
                result = JOptionPane.showConfirmDialog(null, sizePanel,
                        "Please Enter Desired Size", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.OK_OPTION) {
                    try {
                        int targetWidth = Integer.parseInt(widthField.getText());
                        int targetHeight = Integer.parseInt(heightField.getText());

                        // Expansion Guarantee
                        if (targetWidth > originalWidth * 1.5 || targetHeight > originalHeight * 1.5) {
                            JOptionPane.showMessageDialog(this, "We recommend new size within 1.5x of original size.", "Error", JOptionPane.ERROR_MESSAGE);
                        }

                        else ImageCarvingCall(targetHeight, targetWidth);

                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Invalid size entered.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
            //If we are going to remove some part:
            else    ImageCarvingCall(originalHeight, originalWidth);
        }
    }

    private void ImageCarvingCall(int targetHeight, int targetWidth){
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Directory to Save the Carved Image");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File directory = fileChooser.getSelectedFile();
            savedImagePath = new File(directory,"savedImage.jpg").getAbsolutePath();
        }

        long startTime = System.nanoTime();
        SwingWorker<Void, BufferedImage> worker = new SwingWorker<Void, BufferedImage>() {
            @Override
            protected Void doInBackground() throws Exception {
                if(protectedMaskPath == null) protectedMaskPath = "";
                if(removalMaskPath == null) removalMaskPath = "";
                SeamCarver sc = new SeamCarver(imagePath, targetHeight, targetWidth, protectedMaskPath, removalMaskPath);

                BufferedImage outputImage =sc.getOutputImage();
                ImageIO.write(outputImage,"jpg", new File(savedImagePath));
                JOptionPane.showMessageDialog(SeamCarverGUI.this, "Carve operation completed, carved picture is saved in the save_path.", "Information", JOptionPane.INFORMATION_MESSAGE);
                return null;
            }
        };
        worker.execute();
        long endTime = System.nanoTime();
        TimeMeasure(startTime, endTime);
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

    private void protectRegion(ActionEvent e) {
        // Show dialog to choose selection method
        isProtect = true;
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

    private void removeRegion(ActionEvent e) {
        isRemoval = true;
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
            if(isProtect) protectedMaskPath = new File(directory, "mask.jpg").getAbsolutePath();
            if(isRemoval) removalMaskPath = new File(directory, "mask.jpg").getAbsolutePath();
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
                    if(isProtect)   generateMaskAndSave(protectedMaskPath);
                    if(isRemoval)   generateMaskAndSave(removalMaskPath);
                    imageLabel.removeMouseListener(this);
                    imageLabel.removeMouseMotionListener(this);
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
            if(isProtect) protectedMaskPath = new File(directory, "maskBrush.jpg").getAbsolutePath();
            if(isRemoval) removalMaskPath = new File(directory, "maskBrush.jpg").getAbsolutePath();
            brushSelection = new boolean[currentImage.getWidth()][currentImage.getHeight()];

            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    updateBrushSelection(e.getPoint());
                    imageLabel.repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if(isProtect)   generateBrushMaskAndSave(protectedMaskPath);
                    if(isRemoval)   generateBrushMaskAndSave(removalMaskPath);
                    imageLabel.removeMouseListener(this);
                    imageLabel.removeMouseMotionListener(this);
                    imageLabel.repaint();
                }

                private void updateBrushSelection(Point point) {
                    int brushSize = 10;
                    int xStart = Math.max(0, (int) (point.x * maxScale) - brushSize / 2);
                    int xEnd = Math.min(currentImage.getWidth(), (int) (point.x * maxScale) + brushSize / 2);
                    int yStart = Math.max(0, (int) (point.y * maxScale) - brushSize / 2);
                    int yEnd = Math.min(currentImage.getHeight(), (int) (point.y * maxScale) + brushSize / 2);

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
            BufferedImage maskImage = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);

            try {
                File outputFile = new File(filename);
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

            int boxX = (int) (selectedArea.x * maxScale);
            int boxY = (int) (selectedArea.y * maxScale);
            int boxWidth = (int) (selectedArea.width * maxScale);
            int boxHeight = (int) (selectedArea.height * maxScale);

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
                File outputFile = new File(filename);
                ImageIO.write(maskImage, "jpg", outputFile);
                System.out.println("Mask image saved as " + filename);
            } catch (IOException ex) {
                System.err.println("Error saving the mask image: " + ex.getMessage());
            }
        }
    }
}