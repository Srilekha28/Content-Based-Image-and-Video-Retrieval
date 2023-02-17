/* Project 1
 */

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.event.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.util.*;
import java.io.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.*;
import javax.swing.border.Border;

import static java.awt.Image.SCALE_DEFAULT;
import static java.lang.System.getProperty;

public class CBVR extends JFrame {
    JLabel pageLabel = new JLabel();
    private final JButton [] button; //creates an array of JButtons
    private final int [] buttonOrder = new int [101]; //creates an array to keep up with the image order
    private final JPanel imagesPanel;
    int picNo = -1;
    private static final Set<Integer> selectedImages = new HashSet<>();
    private final Checkbox relevant = new Checkbox("Relevance", false);
    private List<Integer> cutEnds = new ArrayList<>();
    private List<List<Integer>> gradTransitions = new ArrayList<>();
    private List<Frame> bufferedImages = new ArrayList<>();
    private List<Image> images = new ArrayList<>();
    private Java2DFrameConverter c = new Java2DFrameConverter();
    SlideShow slideShow;
    JPanel panelTop;

    private float[][] frameIntensities = new float[4000][25];

    private static final Border firstSelectedImageBorder = BorderFactory.createLineBorder(Color.GREEN, 8, true);
    private static final Border selectedImageBorder = BorderFactory.createLineBorder(Color.CYAN, 8, true);

    public static void main(String args[]) {
        SwingUtilities.invokeLater(() -> {
            CBVR app = null;
            try {
                app = new CBVR();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            app.setVisible(true);
        });
    }

    public CBVR() throws IOException {
        /**
         * takes the .mpg video as input and grab frames using FFmpegFrameGrabber().
         * we wil be converting the grabbed frames into bufferedimages by using Java2DFrameConverter()
         * and will be calculating the intensities of each image.
         * obtained intensity of each image will be stored in frameIntensities array.
         *
         */
        FrameIntensity frameIntensityHelper = new FrameIntensity();
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoMPGName);
        grabber.start();
        // converter
        Java2DFrameConverter c = new Java2DFrameConverter();

        String imagesDir = getProperty("user.dir") + File.separator + "images";
        boolean isFolderEmpty = Objects.requireNonNull(new File(imagesDir).list()).length == 0;

        for (int i = 0; i < 5000; i++) {
            Frame frame = grabber.grabImage();
            int frameNumber = grabber.getFrameNumber();

            if (1000 + 22 <= frameNumber && frameNumber <= 4999 + 22) {
                // System.out.println(frameNumber);
                if (frame == null) {
                    System.out.println("frame null - ");
                } else {
                    BufferedImage bufImg = c.convert(frame);
                    bufferedImages.add(frame.clone());
                    int fn = frameNumber - 1022;
                    if (isFolderEmpty) {
                        ImageIO.write(bufImg, "png",
                                new File(getProperty("user.dir") + File.separator + "images" + File.separator + fn + ".png"));
                    }
                    frameIntensities[fn] = frameIntensityHelper.getFrameIntensity(bufImg);
                }
            }
        }
        grabber.close();

        int totalSDs = 3999;
        /***
         * in this assignment we will have to consider 4000 frames as our input,
         * we have to calculate the absolute difference between each frame
         * which can be done using calculateAllSDs() method.
          */
        float[] SDs = frameIntensityHelper.calculateAllSDs(frameIntensities);
        float mean = 0;

        //mean of standard deviations
        for (int i = 0; i < totalSDs; i++) {
            mean += SDs[i];
        }
        mean = mean/totalSDs;

        //std of standard deviations
        float std = 0;
        for (int i = 0; i < totalSDs; i++) {
            std += Math.pow((SDs[i] - mean), 2);
        }
        std = (float) Math.sqrt(std / totalSDs);

        // tb = mean(SD) + std(SD)*11
        float Tb = mean + (std*11);

        // Ts = mean(SD)*2
        float Ts = mean*2;
        System.out.println("Tb - " + Tb  + " Ts - " + Ts);
        // Tor = 2
        int Tor = 2;

        int fS_temp = -1;
        int fE_temp = -1;
        int sdSum_temp = 0;
        int Tor_temp = 0;
        for (int i = 0; i < totalSDs; i++) {
            float currSD = SDs[i];
            if (currSD >= Tb) {
                // mark real fs and fe if exists
                if (fS_temp != -1 && fE_temp != -1)  {
                    int j = fS_temp;
                    while (j <= fE_temp) {
                        sdSum_temp += SDs[j];
                        j++;
                    }
                    if (sdSum_temp >= Tb) {
                        gradTransitions.add(Arrays.asList(fS_temp, fE_temp));
                    }
                }
                //adding Ce to cutEnds array.
                cutEnds.add(i + 1);
                fS_temp = -1;
                fE_temp = -1;
                sdSum_temp = 0;
                Tor_temp = 0;
            } else if (currSD >= Ts) {
                Tor_temp = 0;
                if (fS_temp == -1) {
                    fS_temp = i;
                } else {
                    fE_temp = i;
                }
            } else {
                Tor_temp += 1;
            }

            if (Tor_temp >= Tor) {
                if (fS_temp != -1 && fE_temp != -1) {
                    int j = fS_temp;
                    while (j <= fE_temp) {
                        sdSum_temp += SDs[j];
                        j++;
                    }
                    // checking if the transition is real gradual transition or not.
                    if (sdSum_temp >= Tb) {
                        //adding fs and fe values to gradTransitions array
                        gradTransitions.add(Arrays.asList(fS_temp, fE_temp));
                    }
                }
                fS_temp = -1;
                fE_temp = -1;
                sdSum_temp = 0;
                Tor_temp = 0;
            }
        }

        //================================================
        //The following lines set up the interface including the layout of the buttons and JPanels.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setTitle("Content Based Video Retrieval System");

        GridLayout overallLayout = new GridLayout(1, 2, 5, 5);
        setLayout(overallLayout);

        imagesPanel = new JPanel();
        // this layout shows the retrieved images in a page
        GridLayout gridLayout1 = new GridLayout(4, 4, 5, 5);
        imagesPanel.setLayout(gridLayout1);

        JButton previousPage = new JButton("<<");
        JButton nextPage = new JButton(">>");


        JPanel panelX = new JPanel();

        panelX.setLayout(new BorderLayout());
//        panelX.add(featurePanel, BorderLayout.CENTER);

        panelTop = new JPanel();
        panelTop.setLayout(new GridLayout(2,1));

        panelTop.add(panelX);
        setSize(1100, 750);

        int i = 0, j = 0;
        for (List<Integer> grad : gradTransitions) {
            // fs + 1
            cutEnds.add(grad.get(0) + 1);
        }

        List<Integer> shots = cutEnds.stream().sorted().collect(Collectors.toList());
        button = new JButton[shots.size() - 1];

        /*This for loop goes through the images in the database and stores them as icons and adds
         * the images to JButtons and then to the JButton array
         */
        ImageIcon icon;
        for (i = 0; i < shots.size() - 1; i++) {
            int curr = shots.get(i);
            int end = shots.get(i+1) - 1;
            BufferedImage bufImage = c.convert(bufferedImages.get(curr));
            Image displayImg = bufImage.getScaledInstance(bufImage.getWidth(), bufImage.getHeight(), SCALE_DEFAULT);
            icon = new ImageIcon(displayImg);

            JButton jb = new JButton();
            jb.setIcon(new StretchIcon(icon.getImage(), false));
            button[i] = jb;
            imagesPanel.add(button[i]);
            button[i].addActionListener(new IconButtonHandler(i, icon, curr, end));
            buttonOrder[i] = i;
        }
        add(panelTop);

        JPanel newPanel = new JPanel();
        newPanel.setLayout(new BorderLayout());
        newPanel.add(imagesPanel, BorderLayout.CENTER);

        pageLabel.setVerticalTextPosition(JLabel.BOTTOM);
        pageLabel.setHorizontalTextPosition(JLabel.CENTER);
        pageLabel.setHorizontalAlignment(JLabel.CENTER);
        pageLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel buttonPanel = new JPanel();

        GroupLayout groupLayout = new GroupLayout(buttonPanel);
        groupLayout.setAutoCreateGaps(true);
        groupLayout.setAutoCreateContainerGaps(true);

        groupLayout.setHorizontalGroup(groupLayout.createSequentialGroup()
                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(previousPage))
                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(pageLabel))
                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addComponent(nextPage)));
        groupLayout.setVerticalGroup(groupLayout.createSequentialGroup()
                .addGroup(groupLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(previousPage).addComponent(pageLabel).addComponent(nextPage)));

        buttonPanel.setLayout(new GridLayout(1,3));
        buttonPanel.add(previousPage);
        buttonPanel.add(pageLabel);
        buttonPanel.add(nextPage);
//        newPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(newPanel);

        setVisible(true);
    }


    /*This class implements an ActionListener for each iconButton.  When an icon button is clicked, the image on the
     * the button is added to the photographLabel and the picNo is set to the image number selected and being displayed.
     */
    private class IconButtonHandler implements ActionListener{
        int pNo = 0;
        ImageIcon iconUsed;
        boolean selected = false;
        List<Image> allImagesInShot;
        int start;
        int end;

        IconButtonHandler(int i, ImageIcon j, int start, int end){
            pNo = i;
            iconUsed = j;  //sets the icon to the one used in the button
            this.start = start;
            this.end = end;
        }


        //if any button among intensity/colorcode is selected then the required action will be performed.
        public void actionPerformed( ActionEvent e) {
            if (slideShow == null) {
                slideShow = new SlideShow(start, end);
                panelTop.add(slideShow, 0);
            } else {
                slideShow.start(start, end);
            }

            // panelTop.add(slideShow, 0);
//            panelTop.revalidate();
//            panelTop.repaint();

            //int i = start;
            //ImageIcon icon = new ImageIcon();
            // photographLabel.setIcon(icon);
//            slideShow.start();

//            java.awt.Frame f = new java.awt.Frame("slide show");
//            SlideShow s = new SlideShow(start, end);
//            f.add(s);
//            f.addWindowListener(new WindowAdapter()
//                                {
//                                    public void windowClosing(WindowEvent e){}
//                                }
//            );
//            f.pack();
//            f.setSize(640,480);
//            f.setVisible(true);
        }

        // all the selected images will be cleared
        public void clearAlreadySelectedImages()
        {
            selected = !selected;
            button[pNo].setBorder(BorderFactory.createEmptyBorder());
        }

        // this method is used to deselect an image
        public void iconClearSelection() {
            selected = !selected;
            if (selected) {
                selectedImages.add(pNo);
                if (picNo == -1) {
                    button[pNo].setBorder(firstSelectedImageBorder);
                } else {
                    button[pNo].setBorder(selectedImageBorder);
                }

            }
            else {
                selectedImages.remove(pNo);
                button[pNo].setBorder(BorderFactory.createEmptyBorder());
            }
            if (picNo == -1) {
                picNo = pNo;
            }
        }
    }

    // if the user wants to start from beginning this method is called to remve all the selected images.
    private void clearSelection() {
        for (Integer image: selectedImages) {
            if (picNo != image) {
                ((IconButtonHandler) button[image].getActionListeners()[0]).clearAlreadySelectedImages();
            }
        }
        selectedImages.clear();
        if (picNo != -1) {
            selectedImages.add(picNo);
            button[picNo].setBorder(firstSelectedImageBorder);
        }
        relevant.setState(false);
    }

    private static final String videoMPGName = getProperty("user.dir") + File.separator  + "20020924_juve_dk_02a.mpg";
}
