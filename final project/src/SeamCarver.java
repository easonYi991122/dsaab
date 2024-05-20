import edu.princeton.cs.algs4.Picture;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SeamCarver {
    private Picture inImage;
    private int inHeight;
    private int inWidth;
    private static Picture outImage;
    private int outHeight;
    private int outWidth;
    private boolean object;
    private boolean protect;
    private double[][] kernelX;
    private double[][] kernelYLeft;
    private double[][] kernelYRight;
    private double[][] mask;
    private final double constant = 10000;

    public SeamCarver(String filename, int outHeight, int outWidth, String protectMask, String objectMask) {
        this.outHeight = outHeight;
        this.outWidth = outWidth;
        this.inImage = new Picture(filename);
        this.inHeight = inImage.height();
        this.inWidth = inImage.width();
        this.outImage = new Picture(inImage.width(), inImage.height());
        for (int col = 0; col < inImage.width(); col++) {
            for (int row = 0; row < inImage.height(); row++) {
                outImage.set(col, row, inImage.get(col, row));
            }
        }

        this.object = !objectMask.isEmpty();
        this.protect = !protectMask.isEmpty();

        if (object) {
            this.mask = readMask(objectMask);
        } else if (protect) {
            this.mask = readMask(protectMask);
        }

        this.kernelX = new double[][]{{0, 0, 0}, {-1, 0, 1}, {0, 0, 0}};
        this.kernelYLeft = new double[][]{{0, 0, 0}, {0, 0, 1}, {0, -1, 0}};
        this.kernelYRight = new double[][]{{0, 0, 0}, {1, 0, 0}, {0, -1, 0}};

        frameini();
        start();
//        frame.dispose();
        saveResult("D:\\study\\dsaab\\final project\\out1.jpg");
    }

    public double[][] readMask(String objectMask) {
        try {
            // 使用ImageIO读取图像
            BufferedImage img = ImageIO.read(new File(objectMask));

            // 初始化mask数组
            double[][] mask = new double[img.getHeight()][img.getWidth()];

            // 遍历图像的每个像素，转换为灰度并存储到mask数组
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    Color color = new Color(img.getRGB(x, y));
                    // 计算灰度值
                    double gray = 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue();
                    if(gray > 1)
                        mask[y][x] = 1;
                    else
                        mask[y][x] = 0;
                }
            }

            return mask;
        } catch (IOException e) {
            e.printStackTrace();
            // 在发生异常时返回null或一个空数组可能更安全，具体取决于你如何处理这些情况
            return null;
        }
    }

    public void start() {
        if (this.object) {
            objectRemoval();
        } else {
            seamsCarving();
        }
    }

    public void seamsCarving() {
        int deltaRow = outHeight - inHeight;
        int deltaCol = outWidth - inWidth;

        // Remove columns
        if (deltaCol < 0) {
            seamsRemoval(-deltaCol, false);
        }
        // Insert columns
        else if (deltaCol > 0) {
            seamsInsertion(deltaCol, false);
        }

        // Remove rows
        if (deltaRow < 0) {
            outImage = rotateImage(outImage, true);
            if (protect) {
                mask = rotateMask(mask, true);
            }
            seamsRemoval(-deltaRow, true);
            outImage = rotateImage(outImage, false);
        }
        // Insert rows
        else if (deltaRow > 0) {
            outImage = rotateImage(outImage, true);
            if (protect) {
                mask = rotateMask(mask, true);
            }
            seamsInsertion(deltaRow, true);
            outImage = rotateImage(outImage, false);
        }
    }


    // object removal
    public void objectRemoval() {
        boolean rotate = false;
        int[] objectDimension = getObjectDimension();
        int objectHeight = objectDimension[0];
        int objectWidth = objectDimension[1];

        if (objectHeight < objectWidth) {
            outImage = rotateImage(outImage, true);
            mask = rotateMask(mask, true);
            rotate = true;
        }

        double[][] energyMap = calcEnergyMap();
        applyMaskToEnergyMap(energyMap, mask, -constant);

        while (maskHasNonZero()) {

            double[][] cumulativeMap = cumulativeMapBackward(energyMap);
            int[] seamIdx = findSeam(cumulativeMap);
            deleteSeam_draw(seamIdx, rotate);
            deleteSeamOnMask(seamIdx);
            energyMap = updateEnergyMap(energyMap, seamIdx);
        }

        int numPixels;
        if (!rotate) {
            numPixels = inWidth - outImage.width();
        } else {
            numPixels = inHeight - outImage.height();
        }

        seamsInsertion(numPixels, rotate);
        if (rotate) {
            outImage = rotateImage(outImage, false);
        }
    }

    private boolean maskHasNonZero() {
        for (int i = 0; i < mask.length; i++) {
            for (int j = 0; j < mask[i].length; j++) {
                if (mask[i][j] > 0) {
                    return true; // 如果找到一个非零元素，立即返回true
                }
            }
        }
        return false; // 如果遍历整个数组都没有找到非零元素，返回false
    }
    private void applyMaskToEnergyMap(double[][] energyMap, double[][] mask, double factor) {
        for (int i = 0; i < energyMap.length; i++) {
            for (int j = 0; j < energyMap[i].length; j++) {
                if (mask[i][j] > 0) {
                    energyMap[i][j] += 50;
                    energyMap[i][j] *= factor; // 将掩码非零位置的能量值乘以指定的因子
                }
            }
        }
    }

    // seams removal
    private void seamsRemoval(int numPixels, boolean rotate_state) {
        double[][] energyMap = calcEnergyMap();
        if (protect) {
            // 应用掩码，增加保护区域的能量值
            applyMaskToEnergyMap(energyMap, mask, constant);
        }
        for (int i = 0; i < numPixels; i++) {
            double[][] cumulativeMap = cumulativeMapBackward(energyMap);
            int[] seamIdx = findSeam(cumulativeMap);

            //System.out.println(Arrays.stream(seamIdx).sum());

            deleteSeam_draw(seamIdx, rotate_state);
            if (protect) {
                deleteSeamOnMask(seamIdx);
            }
            energyMap = updateEnergyMap(energyMap, seamIdx);
        }

        /*for (int i = 0; i < numPixels; i++) {
            double[][] energyMap = calcEnergyMap();
            if (protect) {
                // 应用掩码，增加保护区域的能量值
                applyMaskToEnergyMap(energyMap, mask, constant);
            }
            double[][] cumulativeMap = cumulativeMapForward(energyMap);
            int[] seamIdx = findSeam(cumulativeMap);

            //System.out.println(Arrays.stream(seamIdx).sum());

            deleteSeam_draw(seamIdx, rotate_state);
            if (protect) {
                deleteSeamOnMask(seamIdx);
            }
        }*/
    }

    private void seamsInsertion(int numPixels, boolean rotate_state) {
        Picture tempImage = new Picture(outImage.width(), outImage.height());
        for (int col = 0; col < outImage.width(); col++) {
            for (int row = 0; row < outImage.height(); row++) {
                tempImage.set(col, row, outImage.get(col, row));
            }
        }

        double[][] tempMask = null;
        if (protect) {
            tempMask = new double[mask.length][mask[0].length];
            for (int i = 0; i < mask.length; i++) {
                System.arraycopy(mask[i], 0, tempMask[i], 0, mask[i].length);
            }
        }

        List<int[]> seamsRecord = new ArrayList<>();

        double[][] energyMap = calcEnergyMap();
        if (protect) {
            applyMaskToEnergyMap(energyMap, mask, constant);
        }

        for (int i = 0; i < numPixels; i++) {

            double[][] cumulativeMap = cumulativeMapBackward(energyMap);
            int[] seamIdx = findSeam(cumulativeMap);
            seamsRecord.add(seamIdx);
            deleteSeam_pure(seamIdx);
            if (protect) {
                deleteSeamOnMask(seamIdx);
            }
            energyMap = updateEnergyMap(energyMap, seamIdx);
        }

        outImage = tempImage;
        if (protect) {
            mask = tempMask;
        }

        for (int i = 0; i < seamsRecord.size(); i++) {
            int[] seam = seamsRecord.remove(0);
            addSeam(seam, rotate_state);
            if (protect) {
                addSeamOnMask(seam);
            }
            seamsRecord = updateSeams(seamsRecord, seam);
        }
    }


    public double[][] calcEnergyMap() {
        int width = outImage.width();
        int height = outImage.height();
        double[][] energyMap = new double[height][width];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                energyMap[y][x] = calcEnergy(x, y);
            }
        }

        return energyMap;
    }
    public double[][] updateEnergyMap(double[][] energyMap, int[] seamIdx) {
        int m = outImage.height();
        int n = outImage.width();
        double[][] output = new double[m][n];

        for (int row = 0; row < m; row++) {
            int colToRemove = seamIdx[row];
            for (int col = 0; col < n; col++) {
                if (col < colToRemove) {
                    output[row][col] = energyMap[row][col];
                } else {
                    output[row][col] = energyMap[row][col + 1];
                }
            }

            int colToChange1 = colToRemove - 1;
            if(colToChange1 >= 0)
                if(Math.abs(output[row][colToChange1]) < constant)
                    output[row][colToChange1] = calcEnergy(colToChange1, row);

            int colToChange2 = colToRemove;
            if(colToChange2 < n)
                if(Math.abs(output[row][colToChange2]) < constant)
                    output[row][colToChange2] = calcEnergy(colToChange2, row);
        }

        return output;
    }

    private double calcEnergy(int x, int y) {
        // 边界像素的能量值设为1000，模拟无穷大的梯度
        if (x == 0 || x == outImage.width() - 1 || y == 0 || y == outImage.height() - 1) {
            return 1000;
        }

        // 计算颜色梯度
        int[] dx = {-1, 1};
        int[] dy = {-1, 1};

        double gradientSquared = 0;
        for (int d : dx) {
            gradientSquared += colorGradientSquared(x, y, x + d, y);
        }
        for (int d : dy) {
            gradientSquared += colorGradientSquared(x, y, x, y + d);
        }

        // 返回梯度的平方根作为能量值
        return Math.sqrt(gradientSquared);
    }

    private double colorGradientSquared(int x1, int y1, int x2, int y2) {
        int rgb1 = outImage.getRGB(x1, y1);
        int rgb2 = outImage.getRGB(x2, y2);

        int r = ((rgb1 >> 16) & 0xFF) - ((rgb2 >> 16) & 0xFF);
        int g = ((rgb1 >> 8) & 0xFF) - ((rgb2 >> 8) & 0xFF);
        int b = (rgb1 & 0xFF) - (rgb2 & 0xFF);

        return r * r + g * g + b * b;
    }


    public double[][] cumulativeMapBackward(double[][] energyMap) {
        int m = energyMap.length;
        int n = energyMap[0].length;
        double[][] output = new double[m][n];

        // 首行直接复制，因为它没有前一行
        System.arraycopy(energyMap[0], 0, output[0], 0, n);

        for (int row = 1; row < m; row++) {
            for (int col = 0; col < n; col++) {
                double minPrev = output[row - 1][col];
                if (col > 0) {
                    minPrev = Math.min(minPrev, output[row - 1][col - 1]);
                }
                if (col < n - 1) {
                    minPrev = Math.min(minPrev, output[row - 1][col + 1]);
                }
                output[row][col] = energyMap[row][col] + minPrev;
            }
        }

        return output;
    }

    public double[][] cumulativeMapForward(double[][] energyMap) {
        double[][] matrixX = calcNeighborMatrix(kernelX);
        double[][] matrixYLeft = calcNeighborMatrix(kernelYLeft);
        double[][] matrixYRight = calcNeighborMatrix(kernelYRight);

        int m = energyMap.length;
        int n = energyMap[0].length;
        double[][] output = new double[m][n];

        for (int i = 0; i < m; i++) {
            System.arraycopy(energyMap[i], 0, output[i], 0, n);
        }

        for (int row = 1; row < m; row++) {
            for (int col = 0; col < n; col++) {
                double eLeft = Double.MAX_VALUE;
                double eRight = Double.MAX_VALUE;
                double eUp = output[row - 1][col] + matrixX[row - 1][col];

                if (col > 0) {
                    eLeft = output[row - 1][col - 1] + matrixX[row - 1][col - 1] + matrixYLeft[row - 1][col - 1];
                }
                if (col < n - 1) {
                    eRight = output[row - 1][col + 1] + matrixX[row - 1][col + 1] + matrixYRight[row - 1][col + 1];
                }

                output[row][col] = energyMap[row][col] + Math.min(eUp, Math.min(eLeft, eRight));
            }
        }

        return output;
    }

    public double[][] calcNeighborMatrix(double[][] kernel) {
        int width = outImage.width();
        int height = outImage.height();
        double[][] output = new double[height][width];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                output[y][x] = applyKernel(x, y, kernel);
            }
        }

        return output;
    }

    private double applyKernel(int x, int y, double[][] kernel) {
        double result = 0;
        double dr = 0, dg = 0, db = 0;
        int kernelWidth = kernel.length;
        int kernelHeight = kernel[0].length;
        int kernelHalfWidth = kernelWidth / 2;
        int kernelHalfHeight = kernelHeight / 2;

        for (int i = -kernelHalfWidth; i <= kernelHalfWidth; i++) {
            for (int j = -kernelHalfHeight; j <= kernelHalfHeight; j++) {
                if (x + i >= 0 && x + i < outImage.width() && y + j >= 0 && y + j < outImage.height()) {
                    Color rgb = outImage.get(x + i, y + j);
                    int r = rgb.getRed();
                    int g = rgb.getGreen();
                    int b = rgb.getBlue();

                    double kernelValue = kernel[i + kernelHalfWidth][j + kernelHalfHeight];

                    dr += kernelValue * r;
                    dg += kernelValue * g;
                    db += kernelValue * b;
                }
            }
        }
        result = Math.sqrt(Math.pow(dr, 2) + Math.pow(dg, 2) + Math.pow(db, 2));
        return result;
    }
    public int[] findSeam(double[][] cumulativeMap) {
        int m = cumulativeMap.length;
        int n = cumulativeMap[0].length;
        int[] output = new int[m];

        // 找到最后一行中能量最小的点
        output[m - 1] = findMinIndex(cumulativeMap[m - 1]);

        // 从下到上回溯找到最小能量缝
        for (int row = m - 2; row >= 0; row--) {
            int prvX = output[row + 1];
            output[row] = prvX; // 默认为正上方
            double minVal = cumulativeMap[row][prvX];

            // 检查左上方
            if (prvX > 0 && cumulativeMap[row][prvX - 1] < minVal) {
                minVal = cumulativeMap[row][prvX - 1];
                output[row] = prvX - 1;
            }

            // 检查右上方
            if (prvX < n - 1 && cumulativeMap[row][prvX + 1] < minVal) {
                output[row] = prvX + 1;
            }
        }

        return output;
    }

    // 辅助方法：找到数组中最小值的索引
    private int findMinIndex(double[] array) {
        int minIndex = 0;
        double minValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < minValue) {
                minValue = array[i];
                minIndex = i;
            }
        }
        return minIndex;
    }

    public void deleteSeam_draw(int[] seamIdx, boolean rotate_state) {
        int m = outImage.height();
        int n = outImage.width();
        Picture output = new Picture(n - 1, m);

        // 创建一个临时的图片副本用于展示删除的seam
        Picture displayImage = new Picture(n, m);
        for (int row = 0; row < m; row++) {
            for (int col = 0; col < n; col++) {
                displayImage.set(col, row, outImage.get(col, row));
            }
        }

        // 在临时图片上将要删除的seam标记为黑色
        for (int row = 0; row < m; row++) {
            int colToRemove = seamIdx[row];
            displayImage.set(colToRemove, row, Color.BLACK);
        }

        if(rotate_state == true) {
            displayImage = rotateImage(displayImage, false);
        }
        // 展示这个标记了将要删除的seam的图片
        displayPicture(displayImage);

        for (int row = 0; row < m; row++) {
            int colToRemove = seamIdx[row];
            for (int col = 0, outputCol = 0; col < n; col++) {
                if (col != colToRemove) {
                    output.set(outputCol, row, outImage.get(col, row));
                    outputCol++;
                }
            }
        }

        outImage = output;
    }
    public void deleteSeam_pure(int[] seamIdx) {
        int m = outImage.height();
        int n = outImage.width();
        Picture output = new Picture(n - 1, m);

//        // 创建一个临时的图片副本用于展示删除的seam
//        Picture displayImage = new Picture(n, m);
//        for (int row = 0; row < m; row++) {
//            for (int col = 0; col < n; col++) {
//                displayImage.set(col, row, outImage.get(col, row));
//            }
//        }
//
//        // 在临时图片上将要删除的seam标记为黑色
//        for (int row = 0; row < m; row++) {
//            int colToRemove = seamIdx[row];
//            displayImage.set(colToRemove, row, Color.BLACK);
//        }
//
//        // 展示这个标记了将要删除的seam的图片
//        displayPicture(displayImage);

        for (int row = 0; row < m; row++) {
            int colToRemove = seamIdx[row];
            for (int col = 0, outputCol = 0; col < n; col++) {
                if (col != colToRemove) {
                    output.set(outputCol, row, outImage.get(col, row));
                    outputCol++;
                }
            }
        }

        outImage = output;
    }

    public void addSeam(int[] seamIdx, boolean rotate_state) {
        int m = outImage.height();
        int n = outImage.width();
        Picture output = new Picture(n + 1, m);

        Picture displayImage = new Picture(n, m);
        for (int row = 0; row < m; row++) {
            for (int col = 0; col < n; col++) {
                displayImage.set(col, row, outImage.get(col, row));
            }
        }

        // 在临时图片上将要删除的seam标记为黑色
        for (int row = 0; row < m; row++) {
            int colToRemove = seamIdx[row];
            displayImage.set(colToRemove, row, Color.BLACK);
        }

        if(rotate_state == true) {
            displayImage = rotateImage(displayImage, false);
        }
        // 展示这个标记了将要删除的seam的图片
        displayPicture(displayImage);

        for (int row = 0; row < m; row++) {
            int col = seamIdx[row];
            for (int currentCol = 0; currentCol <= n; currentCol++) {
                if (currentCol < col) {
                    // 直接复制原始图像中的像素到新图像
                    output.set(currentCol, row, outImage.get(currentCol, row));
                } else if (currentCol == col) {
                    Color leftColor = outImage.get(currentCol - 1 < 0 ? 0 : currentCol - 1, row);
                    Color rightColor = outImage.get(currentCol, row);
                    Color avgColor = new Color(
                            (leftColor.getRed() + rightColor.getRed()) / 2,
                            (leftColor.getGreen() + rightColor.getGreen()) / 2,
                            (leftColor.getBlue() + rightColor.getBlue()) / 2);
                    output.set(currentCol, row, avgColor);
                } else {
                    // 由于添加了新的列，所以复制剩余的像素时需要将列索引减一
                    output.set(currentCol, row, outImage.get(currentCol - 1, row));
                }
            }
        }

        outImage = output;
    }

    public List<int[]> updateSeams(List<int[]> remainingSeams, int[] currentSeam) {
        List<int[]> updatedSeams = new ArrayList<>();

        for (int[] seam : remainingSeams) {
            int[] updatedSeam = new int[seam.length];
            for (int i = 0; i < seam.length; i++) {
                if (seam[i] >= currentSeam[i]) {
                    updatedSeam[i] = seam[i] + 2;
                } else {
                    updatedSeam[i] = seam[i];
                }
            }
            updatedSeams.add(updatedSeam);
        }

        return updatedSeams;
    }

    public Picture rotateImage(Picture image, boolean ccw) {
        int m = image.height();
        int n = image.width();
        Picture output = new Picture(m, n); // 注意，旋转后的宽度和高度会交换

        if (ccw) {
            // 逆时针旋转
            for (int x = 0; x < n; x++) {
                for (int y = 0; y < m; y++) {
                    Color color = image.get(x, y);
                    output.set(y, n - 1 - x, color); // 逆时针旋转并翻转
                }
            }
        } else {
            // 顺时针旋转
            for (int x = 0; x < n; x++) {
                for (int y = 0; y < m; y++) {
                    Color color = image.get(x, y);
                    output.set(m - 1 - y, x, color); // 顺时针旋转
                }
            }
        }

        return output;
    }

    public double[][] rotateMask(double[][] mask, boolean ccw) {
        int m = mask.length;
        int n = mask[0].length;
        double[][] output = new double[n][m]; // 旋转后的尺寸与原尺寸交换

        if (ccw) {
            // 逆时针旋转
            for (int x = 0; x < m; x++) {
                for (int y = 0; y < n; y++) {
                    output[n - 1 - y][x] = mask[x][y]; // 逆时针旋转
                }
            }
        } else {
            // 顺时针旋转
            for (int x = 0; x < m; x++) {
                for (int y = 0; y < n; y++) {
                    output[y][m - 1 - x] = mask[x][y]; // 顺时针旋转
                }
            }
        }

        return output;
    }
    public void deleteSeamOnMask(int[] seamIdx) {
        int m = mask.length;
        int n = mask[0].length;
        double[][] output = new double[m][n - 1]; // 创建一个新的遮罩数组，每行的长度减1

        for (int row = 0; row < m; row++) {
            int colToRemove = seamIdx[row];
            for (int col = 0, newCol = 0; col < n; col++) {
                if (col != colToRemove) {
                    output[row][newCol] = mask[row][col];
                    newCol++;
                }
            }
        }

        mask = output; // 更新遮罩数组
    }
    public void addSeamOnMask(int[] seamIdx) {
        int m = mask.length;
        int n = mask[0].length;
        double[][] output = new double[m][n + 1]; // 创建一个新的遮罩数组，每行的长度加1

        for (int row = 0; row < m; row++) {
            int col = seamIdx[row];
            // 计算要插入的像素的值
            double p;
            if (col == 0) {
                p = (mask[row][col] + mask[row][Math.min(col + 1, n - 1)]) / 2.0;
            } else {
                p = (mask[row][col - 1] + mask[row][col]) / 2.0;
            }

            // 复制插入点之前的部分
            for (int j = 0; j < col; j++) {
                output[row][j] = mask[row][j];
            }
            // 插入新的像素
            output[row][col] = p;
            // 复制插入点及之后的部分
            for (int j = col; j < n; j++) {
                output[row][j + 1] = mask[row][j];
            }
        }

        mask = output; // 更新遮罩数组
    }

    public int[] getObjectDimension() {
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;
        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;

        for (int row = 0; row < mask.length; row++) {
            for (int col = 0; col < mask[row].length; col++) {
                if (mask[row][col] > 0) {
                    if (row < minRow) minRow = row;
                    if (row > maxRow) maxRow = row;
                    if (col < minCol) minCol = col;
                    if (col > maxCol) maxCol = col;
                }
            }
        }

        int height = maxRow - minRow + 1;
        int width = maxCol - minCol + 1;

        return new int[]{height, width}; // 返回高度和宽度
    }

    public static void saveResult(String filename) {
        // 将Picture转换为BufferedImage
        BufferedImage bufferedImage = new BufferedImage(outImage.width(), outImage.height(), BufferedImage.TYPE_INT_RGB);
        for (int col = 0; col < outImage.width(); col++) {
            for (int row = 0; row < outImage.height(); row++) {
                bufferedImage.setRGB(col, row, outImage.get(col, row).getRGB());
            }
        }

        // 准备保存图像的文件对象
        File outputFile = new File(filename);

        // 确保父目录存在
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs(); // 如果父目录不存在，则创建它
        }

        // 尝试保存图像
        try {
            ImageIO.write(bufferedImage, "jpg", outputFile); // 假设文件名以.jpg结尾
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public JFrame frame;

    public void frameini() {
        // 初始化JFrame
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void displayPicture(Picture image) {
        BufferedImage bufferedImage = new BufferedImage(image.width(), image.height(), BufferedImage.TYPE_INT_RGB);
        for (int col = 0; col < image.width(); col++) {
            for (int row = 0; row < image.height(); row++) {
                bufferedImage.setRGB(col, row, image.get(col, row).getRGB());
            }
        }

        // 设置窗口大小
        int frameWidth = 800;
        int frameHeight = 600;
        frame.setSize(frameWidth+100, frameHeight+100);

        // 缩放图片以适应窗口大小
        Image scaledImage = bufferedImage.getScaledInstance(frameWidth, frameHeight, Image.SCALE_SMOOTH);
        ImageIcon imageIcon = new ImageIcon(scaledImage);

        // 创建一个带有图片的JLabel
        JLabel jLabel = new JLabel(imageIcon);

        // 每次显示新图片前，清除旧的内容
        frame.getContentPane().removeAll();
        frame.getContentPane().add(jLabel); // 添加新的内容

        frame.setVisible(true); // 使窗口可见
    }
}

