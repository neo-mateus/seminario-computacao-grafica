//Uso: java Filtros [imagem]
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.util.Arrays;
import javax.swing.*;

public class Filtros extends JFrame {

    static final double C = 30; // limiar do MTM e do DW-MTM
    static final String[] FILTROS = {"media", "mediana", "lee", "mtm", "dwmtm"};

    int w, h;
    BufferedImage imgOriginal;
    Raster origRaster;

    public static void main(String[] args) {
        String nomeImg = args.length > 0 ? args[0] : "seminario.jpeg";

        Filtros jf = new Filtros(new File(nomeImg));
        jf.setTitle("Filtros - " + nomeImg);
        jf.getContentPane().setLayout(new GridLayout(2, 3, 5, 5));
        jf.getContentPane().add(painel("Original", jf.imgOriginal));

        for (String filtro : FILTROS) {
            BufferedImage pro = jf.aplicar(filtro);
            double psnr = jf.calcularPsnr(pro);
            jf.getContentPane().add(
                    painel(String.format("%s (PSNR %.2f dB)", filtro, psnr), pro));
        }

        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.setSize(1500, 950);
        jf.setVisible(true);
    }

    static JScrollPane painel(String titulo, BufferedImage img) {
        JScrollPane sp = new JScrollPane(new JLabel(new ImageIcon(img)));
        sp.setBorder(BorderFactory.createTitledBorder(titulo));
        return sp;
    }

    Filtros(File arquivo) {
        if (!arquivo.exists()) {
            System.out.println("Arquivo nao existe.");
            System.exit(0);
        }
        imgOriginal = carregar(arquivo);
        origRaster = imgOriginal.getRaster();
        w = imgOriginal.getWidth();
        h = imgOriginal.getHeight();
    }

    BufferedImage carregar(File arquivo) {
        try {
            Image img = Toolkit.getDefaultToolkit().getImage(arquivo.getPath());
            MediaTracker mt = new MediaTracker(this);
            mt.addImage(img, 0);
            mt.waitForID(0);

            int w = img.getWidth(null), h = img.getHeight(null);
            int[] pixels = new int[w * h];
            new PixelGrabber(img, 0, 0, w, h, pixels, 0, w).grabPixels();

            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            bi.setRGB(0, 0, w, h, pixels, 0, w);
            return bi;
        } catch (Exception ex) {
            System.out.println("Erro ao ler o arquivo");
            System.exit(0);
            return null;
        }
    }

    static int refletir(int i, int tamanho) {
        int periodo = 2 * (tamanho - 1);
        i = Math.floorMod(i, periodo);
        return i < tamanho ? i : periodo - i;
    }

    int[] janela(int x, int y, int lado, int banda) {
        int r = lado / 2, k = 0;
        int[] v = new int[lado * lado];
        for (int dy = -r; dy <= r; dy++)
            for (int dx = -r; dx <= r; dx++)
                v[k++] = origRaster.getSample(refletir(x + dx, w), refletir(y + dy, h), banda);
        return v;
    }

    static int mediana(int[] v) {
        int[] c = v.clone();
        Arrays.sort(c);
        return c[c.length / 2];
    }

    static double media(int[] v) {
        long s = 0;
        for (int a : v) s += a;
        return (double) s / v.length;
    }

    static double variancia(int[] v, double m) {
        double s = 0;
        for (int a : v) s += (a - m) * (a - m);
        return s / v.length;
    }

    static int aparada(int[] v, int ref) {
        long soma = 0;
        int cnt = 0;
        for (int a : v)
            if (Math.abs(a - ref) <= C) {
                soma += a;
                cnt++;
            }
        return cnt > 0 ? (int) Math.round((double) soma / cnt) : ref;
    }

    static int lee(int[] v, int x, double vr) {
        double m = media(v);
        double var = variancia(v, m);
        double k = Math.max(var - vr, 0) / Math.max(var, 1e-9);
        return Math.min(255, Math.max(0, (int) Math.round(m + k * (x - m))));
    }

    double estimarVarRuido(int banda) {
        double[] t = new double[w * h];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int[] v = janela(x, y, 5, banda);
                t[y * w + x] = variancia(v, media(v));
            }
        Arrays.sort(t);
        int n = t.length;
        return n % 2 == 1 ? t[n / 2] : 0.5 * (t[n / 2 - 1] + t[n / 2]);
    }

    BufferedImage aplicar(String filtro) {
        WritableRaster proc = origRaster.createCompatibleWritableRaster();

        for (int b = 0; b < origRaster.getNumBands(); b++) {
            double vr = filtro.equals("lee") ? estimarVarRuido(b) : 0;

            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    int s;
                    switch (filtro) {
                        case "media":
                            s = (int) Math.round(media(janela(x, y, 5, b)));
                            break;
                        case "mediana":
                            s = mediana(janela(x, y, 5, b));
                            break;
                        case "lee":
                            s = lee(janela(x, y, 5, b), origRaster.getSample(x, y, b), vr);
                            break;
                        case "mtm": {
                            int[] v = janela(x, y, 5, b);
                            s = aparada(v, mediana(v));
                            break;
                        }
                        default: // dwmtm
                            s = aparada(janela(x, y, 7, b), mediana(janela(x, y, 3, b)));
                    }
                    proc.setSample(x, y, b, s);
                }
        }

        return new BufferedImage(imgOriginal.getColorModel(), proc,
                imgOriginal.isAlphaPremultiplied(), null);
    }

    double calcularPsnr(BufferedImage pro) {
        Raster procRaster = pro.getRaster();
        double mse = 0;
        for (int b = 0; b < 3; b++)
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    double d = origRaster.getSample(x, y, b) - procRaster.getSample(x, y, b);
                    mse += d * d;
                }
        mse /= 3.0 * w * h;
        return mse == 0 ? Double.POSITIVE_INFINITY : 10 * Math.log10(255.0 * 255.0 / mse);
    }
}