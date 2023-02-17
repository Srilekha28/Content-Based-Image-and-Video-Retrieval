import org.opencv.core.Mat;
/***
 * this class is used to calculate the intensity of each frame and
 * distribute the intensities among 25 bins.
 * it takes the bufferedimage as input and calculates the
 * color intensity of each pixel and drops them in the respective bins.
 */
import java.awt.*;
import java.awt.image.BufferedImage;

public class FrameIntensity {

    private static final int intensityBinCount = 25;

    public float[] getFrameIntensity(BufferedImage img) {
        float[] intensity = new float[25];
        for (int i = 0; i < 25; i++) {
            intensity[i] = 0;
        }

        int width = img.getWidth();
        int height = img.getHeight();
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                Color color = new Color(img.getRGB(j, i));
                int bin = getIntensityBin(color);
                intensity[bin] += 1;
            }
        }
        return intensity;
    }

    /***
     * extracts the color of each pixel by using i,j variables.
     * the extracted colors will be intensities of red,green and blue colors respectively.
     * by using intensity formula we wil decide the binNumber.
     * @param color
     * @return binNumber
     */
    private int getIntensityBin(Color color){
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        // Intensity formula
        double intensity = 0.299*r + 0.587*g + 0.114*b;
        int binNumber = (int) Math.floor(intensity) / 10;

        if (binNumber == intensityBinCount)
            binNumber = intensityBinCount - 1;

        return binNumber;
    }

    /**
     * takes two bufferedimages as input and calculates the absolute difference between the two images.
     * @param frame1
     * @param frame2
     * @return sd between the two frames
     */
    private float frameToFrameSD(float[] frame1, float[] frame2) {
        float sd = 0;
        for (int i = 0; i < 25; i++) {
            sd += Math.abs(frame1[i] - frame2[i]);
        }
        return sd;
    }

    /**
     * stores the sds of all the frames in an array named SDs
     * by taking frameintensitites array from CBVR as input
     * @param intensities
     * @return SDs array
     */
    public float[] calculateAllSDs(float[][] intensities) {
        int totalSDs = 3999;
        float[] SDs = new float[totalSDs];
        for (int i = 0; i < totalSDs; i++) {
            SDs[i] = frameToFrameSD(intensities[i], intensities[i+1]);
        }
        return SDs;
     }
}
