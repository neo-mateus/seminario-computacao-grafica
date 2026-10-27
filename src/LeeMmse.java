
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
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
}