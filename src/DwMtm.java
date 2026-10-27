import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
 
/**
 * Filtro DW-MTM (Double-Window Modified Trimmed Mean) com Raster / WritableRaster.
 * Janela no estilo do A08_1_Amostragem: JFrame + JLabel(ImageIcon) + JScrollPane.
 */
public class DwMtm extends JFrame {
 
    //parâmetros do filtro
    private final int n; //lado da janela pequena
    private final int q; //lado da janela grande
    private final double c; //limiar de corte em torno da mediana
 
    //imagens e rasters
    private int w, h;
    private BufferedImage imgOriginal, imgPro;
    private Raster origRaster;
    private WritableRaster procRaster;
 
    public static void main(String[] args) {
        String nomeImg = "seminario.jpeg";
        int n = 3;
        int q = 7;
        double c = 30;
 
        JFrame.setDefaultLookAndFeelDecorated(true);
 
        DwMtm jf = new DwMtm(new File(nomeImg), n, q, c);
 
        jf.aplicarDwMtm();
 
        jf.setTitle(String.format(
                "DW-MTM (N=%d, Q=%d, C=%.1f) - %s | PSNR vs original = %.2f dB",
                n, q, c, nomeImg, jf.calcularPsnr()));
 
        // GridLayout 2x1: original em cima, processada embaixo
        jf.getContentPane().setLayout(new GridLayout(2, 1, 5, 5));
        jf.getContentPane().add(painel("Original", jf.imgOriginal));
        jf.getContentPane().add(painel("Processada (DW-MTM)", jf.imgPro));
 
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.setSize(600, 900);
        jf.setLocationRelativeTo(null);
        jf.setVisible(true);
    }
 
    //monta um JScrollPane com a imagem dentro de um JLabel
    private static JScrollPane painel(String titulo, BufferedImage img) {
        JLabel view = new JLabel(new ImageIcon(img));
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createTitledBorder(titulo));
        return sp;
    }
 
    private DwMtm(File arquivo, int n, int q, double c) {
 
        if (arquivo == null || !arquivo.exists()) {
            System.out.println("Arquivo não existe.");
            System.exit(0);
        }
        if (n < 1 || n % 2 == 0 || q < 1 || q % 2 == 0 || c < 0) {
            System.out.println("Parametros invalidos: N e Q devem ser impares e N >= 1, Q >=1 e C >= 0");
            System.exit(0);
        }
 
        this.n = n;
        this.q = q;
        this.c = c;
 
        carregarImagem(arquivo);
    }
 
    //método de entrada
    private void carregarImagem(File arquivo) {
        int[] pixels = null;
 
        try {
            Image img = Toolkit.getDefaultToolkit().getImage(arquivo.getPath());
            MediaTracker mt = new MediaTracker(this);
            mt.addImage(img, 0);
            mt.waitForID(0);
 
            w = img.getWidth(null);
            h = img.getHeight(null);
            if (w <= 0 || h <= 0) {
                System.out.println("Erro lendo arquivo");
                System.exit(0);
            }
            pixels = new int[w * h];
            PixelGrabber pg = new PixelGrabber(img, 0, 0, w, h, pixels, 0, w);
            pg.grabPixels();
        } catch (InterruptedException ex) {
            System.out.println("Erro ao ler o arquivo");
            System.exit(0);
        }
 
        imgOriginal = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        imgOriginal.setRGB(0, 0, w, h, pixels, 0, w);
        origRaster = imgOriginal.getRaster();
    }
 
    //método de reflexão das bordas
    private static int refletir(int i, int tamanho) {
        if (tamanho == 1)
            return 0;
        int periodo = 2 * (tamanho - 1);
        i = Math.floorMod(i, periodo);
        if (i < tamanho)
            return i;
        else
            return periodo - i;
    }
 
    //Método da mediana do raster
    private int[][] calcularMediana(Raster raster, int banda) {
 
        int r = n / 2;
        int[][] med = new int[h][w];
        int[] buf = new int[n * n];
 
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int k = 0;
                for (int dy = -r; dy <= r; dy++) {
                    int yy = refletir(y + dy, h);
                    for (int dx = -r; dx <= r; dx++)
                        buf[k++] = raster.getSample(refletir(x + dx, w), yy, banda);
                }
                Arrays.sort(buf);
                med[y][x] = buf[buf.length / 2];
            }
        return med;
    }
 
    private void calcularMediaAparda(Raster entrada, WritableRaster saida, int banda, int[][] mediana) {
 
        int r = q / 2;
 
        for (int y = 0; y < h; y++)          // corrigido: era y < y
            for (int x = 0; x < w; x++) {
                int med = mediana[y][x];
                long soma = 0;
                int cnt = 0;
 
                for (int dy = -r; dy <= r; dy++) {
                    int yy = refletir(y + dy, h);
 
                    for (int dx = -r; dx <= r; dx++) {
                        int v = entrada.getSample(refletir(x + dx, w), yy, banda);
                        if (Math.abs(v - med) <= c) {
                            soma += v;
                            cnt++;
                        }
                    }
                }
                int resultado = cnt > 0 ? (int) Math.round((double) soma / cnt) : med;
                saida.setSample(x, y, banda, resultado);
            }
    }
 
    private void filtrarBanda(Raster entrada, WritableRaster saida, int banda) {
 
        int[][] mediana = calcularMediana(entrada, banda);
        calcularMediaAparda(entrada, saida, banda, mediana);
    }
 
    private BufferedImage montarImagem(WritableRaster raster) {
        return new BufferedImage(imgOriginal.getColorModel(), raster,
                imgOriginal.isAlphaPremultiplied(), null);
    }
 
    public void aplicarDwMtm() {
        procRaster = origRaster.createCompatibleWritableRaster();
 
        for (int banda = 0; banda < origRaster.getNumBands(); banda++)
            filtrarBanda(origRaster, procRaster, banda);
        imgPro = montarImagem(procRaster);
    }
 
    public double calcularPsnr() {
 
        int bandas = origRaster.getNumBands();
        double mse = 0;
        for (int b = 0; b < bandas; b++)
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    double d = origRaster.getSample(x, y, b) - procRaster.getSample(x, y, b);
                    mse += d * d;
                }
        mse /= (double) bandas * w * h;
        return mse == 0 ? Double.POSITIVE_INFINITY : 10 * Math.log10(255.0 * 255.0 / mse);
    }
}