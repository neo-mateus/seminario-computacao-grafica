
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

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

    private DwMtm(File file, int n, int q, double c) {
        
    }
}
