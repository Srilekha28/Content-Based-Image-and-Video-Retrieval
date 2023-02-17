import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.System.getProperty;

/**
 * this class is used to take the images as input and display all the images in the form of a video.
 * i have used timer and timertask classes from util package to schedule a task whenever user clicks on any icon
 * this class contains a start method which takes two parameters start and end as inputs which
 * will be the ranges in which the frames must be displayed.
 */
class SlideShow extends Component
{
    private Timer timer;
    private TimerTask slider;
    private Image currentImage;
    private Image nextImage;
    java.util.List<Image> images =  new ArrayList<>();

    public SlideShow(int start, int end)
    {
        start(start, end);
    }

    public void start(int start, int end) {
        images.clear();
        super.setSize(640,480);

        timer = new Timer();
        int i = start;
        while (i <= end) {
//            String img = "/Users/dim/Desktop/multiMediaVideoToPhoto/" + i + ".png";
            String img = getProperty("user.dir") + File.separator + "images" + File.separator + i + ".png";
            images.add(Toolkit.getDefaultToolkit().getImage(img));
            i++;
        }

        final int[] s = {0};
        nextImage = images.get(0);
        slider = new TimerTask()
        {
            public void run()
            {
                currentImage = nextImage;
                repaint();
                s[0]++;

                if (s[0] <= (end-start)) {
                    try {
                        nextImage = images.get(s[0]);
                    } catch (Exception e) {
                        // ignore exceptions here
                    }
                }
            }
        };
        timer.schedule(slider, 0, 40);
    }

    public void paint(Graphics g)
    {
        g.drawImage(currentImage,0,0,super.getWidth(),super.getHeight(),this);
    }

    public void update(Graphics g)
    {
        paint(g);
    }
}