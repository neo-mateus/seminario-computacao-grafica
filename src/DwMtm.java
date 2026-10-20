
import java.awt.Frame;
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

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author mateu
 */
public class DwMtm extends Frame{
    
    //parâmetros do filtro
    private final int n; //lado da janela pequena
    private final int q; //lado da janela grande
    private final double c; //limar de corte em torno da mediana
    
    //imagens e rasters
    private int w, h;
    private BufferedImage imgOriginal, imgPro;
    private Raster origRaster;
    private WritableRaster procRaster;
    
    public static void main(String[] args) {
        String nomeImg = "seminario.png";
        int n;
        int q;
        double c;
        
        DwMtm resultado = new DwMtm(new File(nomeImg, n, q, c));
        
        resultado.aplicarDwMtm();
        
        resultado.setTitle(String.format(
        "DW-MTM (N=%d, Q=%d, C=%.1f) - %s | PSNR vs original = %.2f dB",
                n, q, c, nomeImg, resultado.calcularPsnr()
        ));
        
        resultado.setLayout(new GridLayout(2,1,5,5));
        resultado.add(new PainelImagem("Original", resultado.imgOriginal));
        resultado.add(new PainelImagem("Processada (DW-MTM)", resultado.imgPro));
        resultado.setSize(600,900);
        resultado.setLocationRelativeTo(null);
        resultado.setVisible(true);
    }

    private DwMtm(File arquivo, int n, int q, double c) {
        
        if(arquivo == null || !arquivo.exists()){
            System.out.println("Arquivo não existe.");
            System.exit(0);
        }
        if(n<1 || n % 2 == 0 || q < 1 || q % 2 == 0 || c < 0){
        
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
        
        try{
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
        }catch(InterruptedException ex){
            System.out.println("Erro ao ler o arquivo");
            System.exit(0);
        }
        
        imgOriginal = new BufferedImage(w,h, BufferedImage.TYPE_INT_RGB);
        imgOriginal.setRGB(0, 0, w, h, pixels, 0, w);
        origRaster = imgOriginal.getRaster();
    }
    
    //método de reflexão das bordas
    private static int refletir(int i, int tamanho){
        if (tamanho == 1)
            return 0;
        int periodo = 2 * (tamanho - 1);
        i = Math.floorMod(i, periodo);
        if(i < tamanho)
            return i;
        else
            return periodo - i;
        
    }
    
    //Método da mediana do raster
    private int[][] calcularMediana(Raster raster, int banda){
    
        int r = n / 2;
        int[][] med = new int[h][w];
        int[] buf = new int[n*n];
        
        for(int y = 0; y < h; y++)
            for(int x = 0; x < w; x++){
            int k = 0;
                for( int dy = -r; dy <= r; dy++){
                    int yy = refletir(y + dy, h);
                    for (int dx = -r; dx <= r; dx++)
                        buf[k++] = raster.getSample(refletir(x + dx, w), yy, banda);
                }
               Arrays.sort(buf);
               med[y][x] = buf[buf.length/2];
            
            }
        return med;
    }
}
