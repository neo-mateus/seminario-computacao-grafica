
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author mateu
 */
public class LeeMmse extends JFrame {
 
    //parâmetros do filtro
    private final int tam = 0;          //lado da janela
    private final double varRuido = 0;  //variância do ruído (< 0 = estimar automaticamente)
 
    //imagens e rasters
    private int w, h;
    private BufferedImage imgOriginal, imgPro;
    private Raster origRaster;
    private WritableRaster procRaster;
 
    public static void main(String[] args) {
        String nomeImg = "seminario.jpeg";
        int tam = 5;
        double varRuido = -1; //automático
 
        JFrame.setDefaultLookAndFeelDecorated(true);
 
        LeeMmse jf = new LeeMmse(new File(nomeImg), tam, varRuido);
 
        jf.aplicarLee();
 
        jf.setTitle(String.format(
                "Lee/MMSE (janela=%dx%d) - %s | PSNR vs original = %.2f dB",
                tam, tam, nomeImg, jf.calcularPsnr()));
 
        // GridLayout 2x1: original em cima, processada embaixo
        jf.getContentPane().setLayout(new GridLayout(2, 1, 5, 5));
        jf.getContentPane().add(painel("Original", jf.imgOriginal));
        jf.getContentPane().add(painel("Processada (Lee/MMSE)", jf.imgPro));
 
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.setSize(600, 900);
        jf.setLocationRelativeTo(null);
        jf.setVisible(true);
    }

    private static JScrollPane painel(String titulo, BufferedImage img) {
        JLabel view = new JLabel(new ImageIcon(img));
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createTitledBorder(titulo));
        return sp;
    }
 
     private LeeMmse(File arquivo, int tam, double varRuido) {
 
        if (arquivo == null || !arquivo.exists()) {
            System.out.println("Arquivo não existe.");
            System.exit(0);
        }
        if (tam < 1 || tam % 2 == 0) {
            System.out.println("Parametro invalido: a janela deve ser impar e >= 1");
            System.exit(0);
        }
 
        this.tam = tam;
        this.varRuido = varRuido;
 
        carregarImagem(arquivo);
    }
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
     private double[][] calcularMedia(Raster raster, int banda) {
 
        int r = tam / 2;
        double[][] m = new double[h][w];
 
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                double soma = 0;
                for (int dy = -r; dy <= r; dy++) {
                    int yy = refletir(y + dy, h);
                    for (int dx = -r; dx <= r; dx++)
                        soma += raster.getSample(refletir(x + dx, w), yy, banda);
                }
                m[y][x] = soma / (tam * tam);
            }
        return m;
    }
     private double[][] calcularVariancia(Raster raster, int banda, double[][] media) {
 
        int r = tam / 2;
        double[][] v = new double[h][w];
 
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                double soma = 0;
                for (int dy = -r; dy <= r; dy++) {
                    int yy = refletir(y + dy, h);
                    for (int dx = -r; dx <= r; dx++) {
                        double d = raster.getSample(refletir(x + dx, w), yy, banda) - media[y][x];
                        soma += d * d;
                    }
                }
                v[y][x] = soma / (tam * tam);
            }
        return v;
    }
     
    private double estimarVarRuido(double[][] v) {
 
        double[] todas = new double[h * w];
        for (int y = 0; y < h; y++)
            System.arraycopy(v[y], 0, todas, y * w, w);
        Arrays.sort(todas);
 
        int n = todas.length;
        if (n % 2 == 1)
            return todas[n / 2];
        else
            return 0.5 * (todas[n / 2 - 1] + todas[n / 2]);
    }
 
    //TODO 3: ganho k e saída y = m + k (x - m), gravada no WritableRaster
    private void calcularLee(Raster entrada, WritableRaster saida, int banda,
                             double[][] media, double[][] variancia, double vr) {
 
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                double v = variancia[y][x];
                double k = Math.max(v - vr, 0) / Math.max(v, 1e-9);
                double m = media[y][x];
                double xv = entrada.getSample(x, y, banda);
 
                int resultado = (int) Math.round(m + k * (xv - m));
                resultado = Math.min(255, Math.max(0, resultado));
                saida.setSample(x, y, banda, resultado);
            }
    }

private void filtrarBanda(Raster entrada, WritableRaster saida, int banda) {
 
        double[][] media = calcularMedia(entrada, banda);
        double[][] variancia = calcularVariancia(entrada, banda, media);
        double vr = varRuido < 0 ? estimarVarRuido(variancia) : varRuido;
        calcularLee(entrada, saida, banda, media, variancia, vr);
    }
 
    private BufferedImage montarImagem(WritableRaster raster) {
        return new BufferedImage(imgOriginal.getColorModel(), raster,
                imgOriginal.isAlphaPremultiplied(), null);
    }
 
    public void aplicarLee() {
        procRaster = origRaster.createCompatibleWritableRaster();
 
        for (int banda = 0; banda < origRaster.getNumBands(); banda++)
            filtrarBanda(origRaster, procRaster, banda);
        imgPro = montarImagem(procRaster);
    }
}