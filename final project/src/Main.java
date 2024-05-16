public class Main {
    public static void main(String[] args) {
        // Example usage
        //System.out.println("");

        SeamCarverGUI seam = new SeamCarverGUI();

        // 在这里，我做了几个修改：isSelected变量用于检测是否有图像上传，上传后才能开始进行
        // 正常的Carving以及计时需要在图像选中之后才开始
        if(seam.isSelected) {
            // 记录开始时间
            long startTime = System.nanoTime();

            String testFilename = seam.imagePath;
            SeamCarver sc = new SeamCarver(testFilename, 500, 800, "", "");
            SeamCarver.saveResult(seam.savePath);

            // 记录结束时间以及展示
            // **但是我不知道为什么结果无法输出**
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
}


