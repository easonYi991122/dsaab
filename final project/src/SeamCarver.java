import edu.princeton.cs.algs4.Picture;
import java.util.List;
import java.util.ArrayList;

public class SeamCarver {
    private Picture inImage;
    private int inHeight;
    private int inWidth;
    private Picture outImage;
    private int outHeight;
    private int outWidth;
    private boolean object;
    private boolean protect;
    private double[][] kernelX;
    private double[][] kernelYLeft;
    private double[][] kernelYRight;
    private double[][] mask;
    private final double constant = 1000;

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

        start();
    }

    private void start() {
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
            seamsRemoval(-deltaCol);
        }
        // Insert columns
        else if (deltaCol > 0) {
            seamsInsertion(deltaCol);
        }

        // Remove rows
        if (deltaRow < 0) {
            outImage = rotateImage(outImage, true);
            if (protect) {
                mask = rotateMask(mask, true);
            }
            seamsRemoval(-deltaRow);
            outImage = rotateImage(outImage, false);
        }
        // Insert rows
        else if (deltaRow > 0) {
            outImage = rotateImage(outImage, true);
            if (protect) {
                mask = rotateMask(mask, true);
            }
            seamsInsertion(deltaRow);
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

        while (maskHasNonZero()) {
            double[][] energyMap = calcEnergyMap();
            applyMaskToEnergyMap(energyMap, mask, -constant);
            double[][] cumulativeMap = cumulativeMapForward(energyMap);
            int[] seamIdx = findSeam(cumulativeMap);
            deleteSeam(seamIdx);
            deleteSeamOnMask(seamIdx);
        }

        int numPixels;
        if (!rotate) {
            numPixels = inWidth - outImage.width();
        } else {
            numPixels = inHeight - outImage.height();
        }

        seamsInsertion(numPixels);
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
                    energyMap[i][j] *= factor; // 将掩码非零位置的能量值乘以指定的因子
                }
            }
        }
    }

    // seams removal
    private void seamsRemoval(int numPixels) {
        for (int i = 0; i < numPixels; i++) {
            double[][] energyMap = calcEnergyMap();
            if (protect) {
                // 应用掩码，增加保护区域的能量值
                applyMaskToEnergyMap(energyMap, mask, constant);
            }
            double[][] cumulativeMap = cumulativeMapForward(energyMap);
            int[] seamIdx = findSeam(cumulativeMap);
            deleteSeam(seamIdx);
            if (protect) {
                deleteSeamOnMask(seamIdx);
            }
        }
    }

    private void seamsInsertion(int numPixels) {
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

        for (int i = 0; i < numPixels; i++) {
            double[][] energyMap = calcEnergyMap();
            if (protect) {
                applyMaskToEnergyMap(energyMap, mask, constant);
            }
            double[][] cumulativeMap = cumulativeMapBackward(energyMap);
            int[] seamIdx = findSeam(cumulativeMap);
            seamsRecord.add(seamIdx);
            deleteSeam(seamIdx);
            if (protect) {
                deleteSeamOnMask(seamIdx);
            }
        }

        outImage = tempImage;
        if (protect) {
            mask = tempMask;
        }

        for (int i = 0; i < seamsRecord.size(); i++) {
            int[] seam = seamsRecord.remove(0);
            addSeam(seam);
            if (protect) {
                addSeamOnMask(seam);
            }
            seamsRecord = updateSeams(seamsRecord, seam);
        }
    }


    private double[][] calcEnergyMap() {
        // Implement energy map calculation
        return null; // Placeholder return
    }

    private double[][] cumulativeMapForward(double[][] energyMap) {
        // Implement forward energy map calculation
        return null; // Placeholder return
    }

    private int[] findSeam(double[][] cumulativeMap) {
        // Implement seam finding
        return null; // Placeholder return
    }

    private void deleteSeam(int[] seamIdx) {
        // Implement seam deletion
    }

    private void addSeam(int[] seamIdx) {
        // Implement seam addition
    }

    private int[] updateSeams(int[] remainingSeams, int currentSeam) {
        // Implement seam updates
        return null; // Placeholder return
    }

    private Picture rotateImage(Picture image, boolean ccw) {
        // Implement image rotation
        return null; // Placeholder return
    }

    private double[][] rotateMask(double[][] mask, boolean ccw) {
        // Implement mask rotation
        return null; // Placeholder return
    }
}
