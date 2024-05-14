public class Main {
    public static void main(String[] args) {
        // Example usage
        //System.out.println("");

        // 记录开始时间
        long startTime = System.nanoTime();

        // 正确的字符串声明和变量名称
        String testFilename = "file:///D:/study/dsaab/final%20project/ex1.jpg";
        SeamCarver sc = new SeamCarver(testFilename, 500, 800, "", "");

        //SwingUtilities.invokeLater(SeamCarverGUI::new);

        // 记录结束时间
        long endTime = System.nanoTime();

        // 计算耗时（单位：纳秒）
        long duration = endTime - startTime;

        // 将纳秒转换为毫秒
        double durationInMillis = duration / 1_000_000.0;

        // 输出耗时
        System.out.println("Function execution time: " + duration + " nanoseconds");
        System.out.println("Function execution time: " + durationInMillis + " milliseconds");
    }
}
