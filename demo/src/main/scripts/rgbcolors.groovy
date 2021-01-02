/*
 * test rgb colors
 END_HELP
 */

import org.jline.console.SystemRegistry
import org.jline.utils.*

class RgbColors {
    def terminal = SystemRegistry.get().terminal()
    int r,g,b
/*
https://gist.github.com/XVilka/8346728
awk 'BEGIN{
    s="/\\/\\/\\/\\/\\"; s=s s s s s s s s;
    for (colnum = 0; colnum<77; colnum++) {
        r = 255-(colnum*255/76);
        g = (colnum*510/76);
        b = (colnum*255/76);
        if (g>255) g = 510-g;
        printf "\033[48;2;%d;%d;%dm", r,g,b;
        printf "\033[38;2;%d;%d;%dm", 255-r,255-g,255-b;
        printf "%s\033[0m", substr(s,colnum+1,1);
    }
    printf "\n";
}'
*/
    def getStyleRGB(def s) {
         "bg-rgb:" + String.format("#%02x%02x%02x", r, g, b) +
        ",fg-rgb:" + String.format("#%02x%02x%02x", 255 - r, 255 - g, 255 - b)
    }

    def printTrueColors() {
        def asb = new AttributedStringBuilder()
        int max = 127
        for (def colnum = 0; colnum < max + 1; colnum++) {
            r = (int) (255 - colnum * 255 / max)
            g = (int) (colnum * 510 / max)
            b = (int) (colnum * 255 / max)
            if (g > 255) {
                g = 510 - g
            }
            def style = new StyleResolver((ref) -> getStyleRGB(ref)).resolve('.rgb' + colnum)
            asb.style(style)
            asb.append(colnum % 2 == 0 ? '/' : '\\')
        }
        asb.toAttributedString().println(terminal)
    }

/*
curl -s https://raw.githubusercontent.com/JohnMorales/dotfiles/master/colors/24-bit-color.sh | bash

*/
    def rainbowColor(int arg) {
        int h = (int) (arg / 43)
        int f = arg - 43*h
        int t = (int) (f * 255 / 43)
        int q = 255 - t
        r = 0; g = 0; b = 0
        if ( h == 0 ) {
            r = 255
            g = t
        } else if ( h == 1 ) {
            r = q
            g = 255
        } else if ( h == 2 ) {
            g = 255
            b = t
        } else if ( h == 3 ) {
            g = q
            b = 255
        } else if ( h == 4 ) {
            r = t
            b = 255
        } else if ( h == 5 ) {
            r = 255
            b = q
        }
    }

    def printColorBars() {
        def asb = new AttributedStringBuilder()
        g = 0; b = 0
        for (r = 0; r < 128;  r++) {
            def style = new StyleResolver((ref) -> getStyleRGB(ref)).resolve('.red' + r)
            asb.style(style)
            asb.append(" ")
        }
        asb.style(AttributedStyle.DEFAULT).append("\n")
        for (r = 255; r > 127;  r--) {
            def style = new StyleResolver((ref) -> getStyleRGB(ref)).resolve('.red' + r)
            asb.style(style)
            asb.append(" ")
        }
        asb.style(AttributedStyle.DEFAULT).append("\n")
        r = 0
        for (g = 0; g < 128;  g++) {
            def style = new StyleResolver((ref) -> getStyleRGB(ref)).resolve('.green' + g)
            asb.style(style)
            asb.append(" ")
        }
        asb.style(AttributedStyle.DEFAULT).append("\n")
        for (g = 255; g > 127;  g--) {
            def style = new StyleResolver((ref) -> getStyleRGB(ref)).resolve('.green' + g)
            asb.style(style)
            asb.append(" ")
        }
        asb.style(AttributedStyle.DEFAULT).append("\n")
        g = 0
        for (b = 0; b < 128;  b++) {
            def style = new StyleResolver((ref) -> getStyleRGB(ref)).resolve('.blue' + b)
            asb.style(style)
            asb.append(" ")
        }
        asb.style(AttributedStyle.DEFAULT).append("\n")
        for (b = 255; b > 127;  b--) {
            def style = new StyleResolver((ref) -> getStyleRGB(ref)).resolve('.blue' + b)
            asb.style(style)
            asb.append(" ")
        }
        asb.style(AttributedStyle.DEFAULT).append("\n")
        for (int i = 0; i < 128;  i++) {
            rainbowColor(i)
            def style = new StyleResolver((ref) -> getStyleRGB(ref)).resolve('.rainbow' + i)
            asb.style(style)
            asb.append(" ")
        }
        asb.style(AttributedStyle.DEFAULT).append("\n")
        for (int i = 255; i > 127;  i--) {
            rainbowColor(i)
            def style = new StyleResolver((ref) -> getStyleRGB(ref)).resolve('.rainbow' + i)
            asb.style(style)
            asb.append(" ")
        }
        asb.style(AttributedStyle.DEFAULT).append("\n")
        asb.toAttributedString().println(terminal)
    }

}

def static main(def _args){
    def colorTest = new RgbColors()
    colorTest.printTrueColors()
    colorTest.printColorBars()
}

