import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;

public class SeamCarverGUI extends JFrame {
    private JLabel imageLabel;
    private BufferedImage currentImage;
    public String imagePath;
    public String savePath;
    public boolean isSelected;
    private String protectMask = "";

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

//    private void carveImage(ActionEvent e) {
//        if (imagePath != null) {
//            SeamCarver sc = new SeamCarver(imagePath, 600, 800, protectMask, "");
//            // Assuming SeamCarver can update its processing to a Picture and display it
//            // For now, just open a new frame or update the existing JLabel with new Picture
//
//        }
//    }
    private void carveImage(ActionEvent e) {
        if (imagePath != null) {
            SwingWorker<Void, BufferedImage> worker = new SwingWorker<Void, BufferedImage>() {
                @Override
                protected Void doInBackground() throws Exception {
                    SeamCarver sc = new SeamCarver(imagePath, 600, 800, protectMask, "");
                    return null;
                }

//                @Override
//                protected void process(List<BufferedImage> chunks) {
//                    for (BufferedImage image : chunks) {
//                        imageLabel.setIcon(new ImageIcon(image));
//                    }
//                }
            };
            worker.execute();
        }
    }


    private void selectRegion(ActionEvent e) {
        // Implement selection functionality, maybe using MouseAdapter to drag and select region
        // Convert this region to a suitable mask format
    }

    //public static void main(String[] args) {
        //SwingUtilities.invokeLater(SeamCarverGUI::new);}
}